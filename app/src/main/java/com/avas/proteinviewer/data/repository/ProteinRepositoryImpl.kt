package com.avas.proteinviewer.data.repository

import com.avas.proteinviewer.data.api.PDBAPIService
import com.avas.proteinviewer.data.parser.PDBParser
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.ProteinDetail
import com.avas.proteinviewer.domain.model.ProteinInfo
import com.avas.proteinviewer.domain.model.ProteinCategory
import com.avas.proteinviewer.domain.repository.ProteinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProteinRepositoryImpl @Inject constructor(
    val apiService: PDBAPIService // private -> val (public access)
) : ProteinRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)  // DNS 해석 시간 증가
        .readTimeout(45, TimeUnit.SECONDS)     // 데이터 읽기 시간 증가
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)        // 연결 실패 시 자동 재시도
        .build()
    
    // PDB 파일 메모리 캐시
    private val pdbCache = mutableMapOf<String, String>()

    override fun searchProteins(query: String): Flow<List<ProteinInfo>> = flow {
        if (query.isEmpty()) {
            emit(getDefaultProteins())
            return@flow
        }

        try {
            // 1단계: Search API로 PDB IDs 가져오기 (카테고리와 동일한 방식)
            val searchUrl = "https://search.rcsb.org/rcsbsearch/v2/query"
            val requestBody = """
                {
                  "query": {
                    "type": "terminal",
                    "service": "text",
                    "parameters": {
                      "value": "$query"
                    }
                  },
                  "return_type": "entry",
                  "request_options": {
                    "results_content_type": ["experimental"],
                    "return_all_hits": false,
                    "pager": {
                      "start": 0,
                      "rows": 30
                    }
                  }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(searchUrl)
                .post(requestBody.toRequestBody(
                    "application/json".toMediaType()
                ))
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(response.body?.string() ?: "{}")
                val results = jsonResponse.optJSONArray("result_set") ?: JSONArray()
                
                // PDB IDs 수집
                val pdbIds = mutableListOf<String>()
                for (i in 0 until results.length()) {
                    val result = results.getJSONObject(i)
                    val id = result.optString("identifier", "")
                    if (id.isNotEmpty()) {
                        pdbIds.add(id)
                    }
                }
                
                // 2단계: GraphQL로 상세 정보 가져오기 (카테고리와 동일한 방식)
                if (pdbIds.isNotEmpty()) {
                    android.util.Log.d("ProteinRepositoryImpl", "🔍 검색 결과 ${pdbIds.size}개 PDB ID 발견, GraphQL로 상세 정보 조회")
                    val proteins = fetchProteinDetailsViaGraphQL(pdbIds)
                    emit(proteins)
                } else {
                    emit(emptyList())
                }
            } else {
                emit(getDefaultProteins())
            }
        } catch (e: Exception) {
            android.util.Log.e("ProteinRepositoryImpl", "❌ 검색 실패: ${e.message}")
            emit(getDefaultProteins())
        }
    }.flowOn(Dispatchers.IO)

    override fun getProteinDetail(proteinId: String): Flow<ProteinDetail> = flow {
        val detail = fetchProteinDetail(proteinId)
        if (detail != null) {
            emit(detail)
        } else {
            throw Exception("Protein not found: $proteinId")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getPDBContent(proteinId: String): String {
        return withContext(Dispatchers.IO) {
            val upperProteinId = proteinId.uppercase()
            val lowerProteinId = proteinId.lowercase()
            
            // 캐시에서 먼저 확인
            pdbCache[upperProteinId]?.let { cachedPdbText ->
                android.util.Log.d("ProteinRepository", "Using cached PDB content for $upperProteinId")
                return@withContext cachedPdbText
            }
            
            // 여러 URL 시도 (fallback 메커니즘)
            val middle2 = if (lowerProteinId.length >= 3) lowerProteinId.substring(1, 3) else "xx"
            val urls = listOf(
                "https://files.rcsb.org/pub/pdb/data/structures/divided/pdb/${middle2}/pdb${lowerProteinId}.ent.gz",
                "https://files.wwpdb.org/pub/pdb/data/structures/divided/pdb/${middle2}/pdb${lowerProteinId}.ent.gz",
                "https://files.rcsb.org/download/${upperProteinId}.pdb"
            )
            
            for ((index, pdbUrl) in urls.withIndex()) {
                try {
                    android.util.Log.d("ProteinRepository", "Trying URL ${index + 1}/${urls.size}: $pdbUrl")
                    
                    val request = Request.Builder()
                        .url(pdbUrl)
                        .header("User-Agent", "ProteinViewer/1.0")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val bodyBytes = response.body?.bytes()
                        if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                            // gzip 압축 여부 확인
                            val pdbText = if (pdbUrl.endsWith(".gz")) {
                                // gzip 압축 해제
                                try {
                                    java.util.zip.GZIPInputStream(bodyBytes.inputStream()).bufferedReader().use { it.readText() }
                                } catch (e: Exception) {
                                    android.util.Log.w("ProteinRepository", "Failed to decompress gzip from $pdbUrl: ${e.message}")
                                    throw Exception("Gzip decompression failed: ${e.message}")
                                }
                            } else {
                                String(bodyBytes)
                            }
                            
                            if (pdbText.isNotEmpty()) {
                                // 캐시에 저장
                                pdbCache[upperProteinId] = pdbText
                                android.util.Log.d("ProteinRepository", "✅ Downloaded and cached PDB from URL ${index + 1}")
                                return@withContext pdbText
                            }
                        }
                    } else {
                        android.util.Log.w("ProteinRepository", "❌ HTTP ${response.code} from URL ${index + 1}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ProteinRepository", "❌ Error from URL ${index + 1}: ${e.message}")
                    if (index == urls.size - 1) {
                        // 마지막 URL도 실패하면 예외 던지기
                        throw Exception("Failed to download PDB content for $proteinId from all URLs")
                    }
                }
            }
            
            throw Exception("Failed to download PDB content for $proteinId")
        }
    }

    override suspend fun loadPDBStructure(proteinId: String, onProgress: (String) -> Unit): PDBStructure {
        return withContext(Dispatchers.IO) {
            val upperProteinId = proteinId.uppercase()
            
            // 캐시에서 먼저 확인
            pdbCache[upperProteinId]?.let { cachedPdbText ->
                android.util.Log.d("ProteinRepository", "Using cached PDB for $upperProteinId")
                onProgress("Parsing cached protein structure...")
                return@withContext PDBParser.parse(cachedPdbText)
            }
            
            onProgress("Downloading protein data...")
            
            // Multiple URL attempts (fallback mechanism)
            val lowerProteinId = proteinId.lowercase()
            val middle2 = if (lowerProteinId.length >= 3) lowerProteinId.substring(1, 3) else "xx"
            val urls = listOf(
                "https://files.rcsb.org/pub/pdb/data/structures/divided/pdb/${middle2}/pdb${lowerProteinId}.ent.gz",
                "https://files.wwpdb.org/pub/pdb/data/structures/divided/pdb/${middle2}/pdb${lowerProteinId}.ent.gz",
                "https://files.rcsb.org/download/${upperProteinId}.pdb",
                "http://files.rcsb.org/download/${upperProteinId}.pdb" // HTTP fallback
            )
            
            android.util.Log.d("ProteinRepository", "📋 PDB Download URLs for $upperProteinId:")
            urls.forEachIndexed { index, url ->
                android.util.Log.d("ProteinRepository", "  ${index + 1}. $url")
            }
            
            var lastException: Exception? = null
            
            for ((index, pdbUrl) in urls.withIndex()) {
                try {
                    val serverName = when {
                        pdbUrl.contains("files.rcsb.org") -> "RCSB"
                        pdbUrl.contains("files.wwpdb.org") -> "wwPDB"
                        else -> "Mirror"
                    }
                    onProgress("Connecting to $serverName server... (${index + 1}/${urls.size})")
                    android.util.Log.d("ProteinRepository", "Attempting to download from: $pdbUrl")
                    
                    val request = Request.Builder()
                        .url(pdbUrl)
                        .header("User-Agent", "ProteinViewer/1.0")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        onProgress("Receiving data...")
                        val pdbText = if (pdbUrl.endsWith(".gz")) {
                            // Decompress gzip if needed
                            onProgress("Decompressing data...")
                            try {
                                val gzipInputStream = java.util.zip.GZIPInputStream(response.body?.byteStream())
                                gzipInputStream.bufferedReader().use { it.readText() }
                            } catch (e: Exception) {
                                android.util.Log.w("ProteinRepository", "Failed to decompress gzip from $pdbUrl: ${e.message}")
                                throw Exception("Gzip decompression failed: ${e.message}")
                            }
                        } else {
                            response.body?.string() ?: throw Exception("Empty file")
                        }
                        
                        if (pdbText.isNotEmpty()) {
                            onProgress("Parsing protein structure...")
                            android.util.Log.d("ProteinRepository", "Successfully downloaded from: $pdbUrl")
                            
                            // 캐시에 저장
                            pdbCache[upperProteinId] = pdbText
                            android.util.Log.d("ProteinRepository", "Cached PDB for $upperProteinId")
                            
                            return@withContext PDBParser.parse(pdbText)
                        }
                    } else {
                        android.util.Log.w("ProteinRepository", "Failed: ${response.code} from $pdbUrl")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProteinRepository", "Error downloading from $pdbUrl: ${e.message}")
                    lastException = e
                    if (index < urls.size - 1) {
                        onProgress("Trying alternative server...")
                        delay(500) // Brief wait before retry
                    }
                }
            }
            
            // All URLs failed - User-friendly error message
            val errorMessage = when {
                lastException?.message?.contains("No address associated with hostname") == true ||
                lastException?.message?.contains("Unable to resolve host") == true -> {
                    "Network Connection Error\n\n" +
                    "Please check:\n" +
                    "• Wi-Fi or mobile data is enabled\n" +
                    "• Internet connection is active\n" +
                    "• Try disabling VPN if active"
                }
                lastException?.message?.contains("timeout") == true -> {
                    "Connection Timeout\n\n" +
                    "The server is not responding:\n" +
                    "• Your network may be slow\n" +
                    "• Please try again later"
                }
                lastException?.message?.contains("Connection refused") == true -> {
                    "Server Connection Failed\n\n" +
                    "Unable to reach PDB servers:\n" +
                    "• Servers may be under maintenance\n" +
                    "• Please try again later"
                }
                else -> {
                    "Download Failed\n\n" +
                    "Could not download protein data:\n" +
                    "• Tried ${urls.size} different servers\n" +
                    "• Error: ${lastException?.message?.take(80) ?: "No Data"}"
                }
            }
            throw Exception(errorMessage)
        }
    }

    private suspend fun fetchProteinDetail(proteinId: String): ProteinDetail? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://data.rcsb.org/rest/v1/core/entry/$proteinId"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    
                    // Struct 정보
                    val struct = json.optJSONObject("struct")
                    val title = struct?.optString("title") ?: proteinId
                    val description = struct?.optString("pdbx_descriptor") ?: "No Data"
                    
                    // Entry 정보
                    val entryInfo = json.optJSONObject("rcsb_entry_info")
                    val molecularWeight = entryInfo?.optDouble("molecular_weight")?.takeIf { !it.isNaN() }
                    val depositionDate = entryInfo?.optString("deposition_date")
                    
                    // 실험 방법
                    val exptl = json.optJSONArray("exptl")
                    val experimentalMethod = if (exptl != null && exptl.length() > 0) {
                        (0 until exptl.length()).map { exptl.getJSONObject(it).optString("method") }.joinToString(", ")
                    } else null
                    
                    // Resolution
                    val refine = json.optJSONArray("refine")
                    val resolution = refine?.optJSONObject(0)?.optDouble("ls_d_res_high")?.takeIf { !it.isNaN() }
                    
                    // Organism (Source organism)
                    val organism = try {
                        val entitySrcGen = json.optJSONArray("entity_src_gen")
                        if (entitySrcGen != null && entitySrcGen.length() > 0) {
                            entitySrcGen.getJSONObject(0).optString("pdbx_gene_src_scientific_name")
                        } else {
                            val entitySrcNat = json.optJSONArray("entity_src_nat")
                            if (entitySrcNat != null && entitySrcNat.length() > 0) {
                                entitySrcNat.getJSONObject(0).optString("pdbx_organism_scientific")
                            } else {
                                "No Data"
                            }
                        }
                    } catch (e: Exception) {
                        "Unknown"
                    }
                    
                    ProteinDetail(
                        id = proteinId,
                        name = title,
                        description = description,
                        organism = organism,
                        molecularWeight = molecularWeight,
                        resolution = resolution,
                        experimentalMethod = experimentalMethod,
                        depositionDate = depositionDate
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getDefaultProteins(): List<ProteinInfo> {
        return listOf(
            ProteinInfo(
                id = "1CRN",
                name = "Crambin",
                category = ProteinCategory.STRUCTURAL,
                description = "Small plant seed protein",
                organism = "Crambe abyssinica",
                resolution = 0.54f,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 4.7f
            ),
            ProteinInfo(
                id = "1HHO",
                name = "Hemoglobin",
                category = ProteinCategory.TRANSPORT,
                description = "Oxygen transport protein",
                organism = "Homo sapiens",
                resolution = 2.1f,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 64.5f
            ),
            ProteinInfo(
                id = "2LYZ",
                name = "Lysozyme",
                category = ProteinCategory.ENZYMES,
                description = "Antibacterial enzyme",
                organism = "Gallus gallus",
                resolution = 1.5f,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 14.3f
            ),
            ProteinInfo(
                id = "4INS",
                name = "Insulin",
                category = ProteinCategory.HORMONES,
                description = "Hormone regulating glucose metabolism",
                organism = "Homo sapiens",
                resolution = 1.9f,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 5.8f
            ),
            ProteinInfo(
                id = "1RXZ",
                name = "Ribonuclease A",
                category = ProteinCategory.ENZYMES,
                description = "RNA degradation enzyme",
                organism = "Bos taurus",
                resolution = 1.26f,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 13.7f
            )
        )
    }
    
    // 아이폰과 동일한 카테고리별 단백질 검색
    override suspend fun searchProteinsByCategory(category: ProteinCategory, limit: Int): List<ProteinInfo> {
        return withContext(Dispatchers.IO) {
            try {
                // 아이폰과 동일한 RCSB PDB Search API 사용
                val searchUrl = "https://search.rcsb.org/rcsbsearch/v2/query"
                
                // 카테고리별 검색 쿼리 구성 (아이폰과 동일)
                val query = buildCategorySearchQuery(category)
                
                val requestBody = """
                    {
                        "query": $query,
                        "return_type": "entry",
                        "request_options": {
                            "pager": {
                                "start": 0,
                                "rows": $limit
                            },
                            "scoring_strategy": "combined",
                            "sort": [
                                {
                                    "sort_by": "score",
                                    "direction": "desc"
                                }
                            ]
                        }
                    }
                """.trimIndent()

                val request = Request.Builder()
                    .url(searchUrl)
                    .post(requestBody.toByteArray().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    parseSearchResults(responseBody, category)
                } else {
                    // API 실패 시 샘플 데이터 반환
                    getSampleProteinsForCategory(category, limit)
                }
                
            } catch (e: Exception) {
                // 오류 시 샘플 데이터 반환
                getSampleProteinsForCategory(category, limit)
            }
        }
    }
    
    // 카테고리별 검색 쿼리 구성 (아이폰과 동일)
    private fun buildCategorySearchQuery(category: ProteinCategory): String {
        return when (category) {
            ProteinCategory.ENZYMES -> """
                {
                    "type": "group",
                    "logical_operator": "or",
                    "nodes": [
                        {
                            "type": "terminal",
                            "service": "text",
                            "parameters": {
                                "attribute": "struct_keywords.pdbx_keywords",
                                "operator": "contains_phrase",
                                "value": "enzyme"
                            }
                        },
                        {
                            "type": "terminal",
                            "service": "text",
                            "parameters": {
                                "attribute": "struct.title",
                                "operator": "contains_phrase",
                                "value": "enzyme"
                            }
                        }
                    ]
                }
            """.trimIndent()
            
            ProteinCategory.STRUCTURAL -> """
                {
                    "type": "group",
                    "logical_operator": "or",
                    "nodes": [
                        {
                            "type": "terminal",
                            "service": "text",
                            "parameters": {
                                "attribute": "struct_keywords.pdbx_keywords",
                                "operator": "contains_phrase",
                                "value": "structural"
                            }
                        },
                        {
                            "type": "terminal",
                            "service": "text",
                            "parameters": {
                                "attribute": "struct.title",
                                "operator": "contains_phrase",
                                "value": "structural"
                            }
                        }
                    ]
                }
            """.trimIndent()
            
            // 다른 카테고리들도 유사하게 구성
            else -> """
                {
                    "type": "terminal",
                    "service": "text",
                    "parameters": {
                        "attribute": "struct_keywords.pdbx_keywords",
                        "operator": "contains_phrase",
                        "value": "${category.displayName.lowercase()}"
                    }
                }
            """.trimIndent()
        }
    }
    
    // 검색 결과 파싱
    private fun parseSearchResults(responseBody: String, category: ProteinCategory): List<ProteinInfo> {
        return try {
            val jsonObject = JSONObject(responseBody)
            val resultSet = jsonObject.getJSONObject("result_set")
            val identifiers = resultSet.getJSONArray("identifiers")
            
            val proteins = mutableListOf<ProteinInfo>()
            for (i in 0 until identifiers.length()) {
                val pdbId = identifiers.getString(i)
                proteins.add(
                    ProteinInfo(
                        id = pdbId,
                        name = "Protein $pdbId",
                        category = category,
                        description = "Sample protein from ${pdbId} category",
                        organism = "Homo sapiens",
                        resolution = 2.5f,
                        experimentalMethod = "X-RAY DIFFRACTION",
                        molecularWeight = 25.0f
                    )
                )
            }
            proteins
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // 카테고리별 샘플 데이터 반환
    private fun getSampleProteinsForCategory(category: ProteinCategory, limit: Int): List<ProteinInfo> {
        val sampleProteins = mutableListOf<ProteinInfo>()
        val baseCount = when (category) {
            ProteinCategory.ENZYMES -> 45000
            ProteinCategory.STRUCTURAL -> 32000
            ProteinCategory.DEFENSE -> 18000
            ProteinCategory.TRANSPORT -> 25000
            ProteinCategory.HORMONES -> 8000
            ProteinCategory.STORAGE -> 5000
            ProteinCategory.RECEPTORS -> 15000
            ProteinCategory.MEMBRANE -> 12000
            ProteinCategory.MOTOR -> 6000
            ProteinCategory.SIGNALING -> 12000
            ProteinCategory.CHAPERONES -> 3000
            ProteinCategory.METABOLIC -> 38000
        }
        
        // 실제 개수는 baseCount이지만, 검색 결과는 limit만큼만 반환
        for (i in 1..minOf(limit, 50)) {
            sampleProteins.add(
                ProteinInfo(
                    id = "${category.name.substring(0, 2).uppercase()}$i",
                    name = "${category.displayName} Protein $i",
                    category = category,
                    description = "Sample ${category.displayName.lowercase()} protein",
                    organism = "Homo sapiens",
                    resolution = (2.0 + (i % 3)).toFloat(),
                    experimentalMethod = "X-RAY DIFFRACTION",
                    molecularWeight = (20.0 + i * 5).toFloat()
                )
            )
        }
        
        return sampleProteins
    }
    
    // 카테고리별 총 개수만 가져오는 함수 (UI 카운트 표시용)
    override suspend fun getCategoryCount(category: ProteinCategory): Int {
        return withContext(Dispatchers.IO) {
            try {
                // API 서비스를 직접 사용하여 총 개수만 가져오기
                val (_, totalCount) = apiService.searchProteinsByCategory(category, limit = 100)
                totalCount
            } catch (e: Exception) {
                // 실패 시 샘플 데이터 개수 반환
                getSampleProteinsForCategory(category, 100).size
            }
        }
    }
    
    /**
     * 아이폰과 동일한 PDB ID 검색
     */
    override suspend fun searchProteinByID(pdbId: String): ProteinInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // GraphQL로 상세 정보 가져오기 (카테고리와 동일한 방식)
                android.util.Log.d("ProteinRepositoryImpl", "🔍 PDB ID로 GraphQL 조회: $pdbId")
                val proteins = fetchProteinDetailsViaGraphQL(listOf(pdbId))
                if (proteins.isNotEmpty()) {
                    android.util.Log.d("ProteinRepositoryImpl", "✅ GraphQL 성공: ${proteins[0].name}")
                    proteins[0]
                } else {
                    android.util.Log.e("ProteinRepositoryImpl", "❌ GraphQL 결과 없음")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("ProteinRepositoryImpl", "❌ PDB ID 검색 실패: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 아이폰과 동일한 텍스트 검색
     */
    override suspend fun searchProteinsByText(searchText: String, limit: Int): List<ProteinInfo> {
        return withContext(Dispatchers.IO) {
            try {
                // 1단계: Search API로 PDB IDs 가져오기
                android.util.Log.d("ProteinRepositoryImpl", "🔍 텍스트 검색: $searchText")
                val pdbIds = apiService.searchProteinIdsByText(searchText, limit)
                
                // 2단계: GraphQL로 상세 정보 가져오기
                if (pdbIds.isNotEmpty()) {
                    android.util.Log.d("ProteinRepositoryImpl", "🔍 ${pdbIds.size}개 PDB ID 발견, GraphQL로 상세 정보 조회")
                    fetchProteinDetailsViaGraphQL(pdbIds)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProteinRepositoryImpl", "❌ 텍스트 검색 실패: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * 아이폰과 동일한 2단계 검색: PDB ID 목록 가져오기
     */
    override suspend fun searchProteinIdsByCategory(category: ProteinCategory, limit: Int, skip: Int): Pair<List<String>, Int> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.searchProteinsByCategory(category, limit, skip)
            } catch (e: Exception) {
                android.util.Log.e("ProteinRepositoryImpl", "❌ 카테고리별 ID 검색 실패: ${e.message}")
                Pair(emptyList(), 0)
            }
        }
    }
    
    /**
     * PDB ID 목록으로 단백질 상세 정보 가져오기 (아이폰과 동일: GraphQL)
     */
    override suspend fun getProteinsByIds(pdbIds: List<String>): List<ProteinInfo> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProteinRepositoryImpl", "🔍 PDB IDs로 GraphQL 상세 정보 가져오기: ${pdbIds.size}개")
                
                // 아이폰과 동일: GraphQL API로 batch fetch
                fetchProteinDetailsViaGraphQL(pdbIds)
            } catch (e: Exception) {
                android.util.Log.e("ProteinRepositoryImpl", "❌ GraphQL 실패, fallback 데이터 생성: ${e.message}")
                // GraphQL 실패 시 기본 정보 생성
                pdbIds.map { pdbId ->
                    ProteinInfo(
                        id = pdbId,
                        name = pdbId,
                        category = ProteinCategory.ENZYMES,
                        description = "PDB ID: $pdbId",
                        keywords = emptyList()
                    )
                }
            }
        }
    }
    
    /**
     * GraphQL API로 단백질 상세 정보 가져오기 (아이폰과 동일)
     */
    private suspend fun fetchProteinDetailsViaGraphQL(pdbIds: List<String>): List<ProteinInfo> {
        if (pdbIds.isEmpty()) return emptyList()
        
        val graphQLURL = "https://data.rcsb.org/graphql"
        
        // 아이폰과 동일한 GraphQL 쿼리
        val query = """
        query (${"$"}ids: [String!]!) {
          entries(entry_ids: ${"$"}ids) {
            rcsb_id
            struct { 
              title 
              pdbx_descriptor 
            }
            exptl { 
              method 
            }
            rcsb_entry_info { 
              resolution_combined 
              experimental_method
            }
            struct_keywords {
              pdbx_keywords
            }
          }
        }
        """.trimIndent()
        
        val requestBody = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply {
                put("ids", JSONArray(pdbIds))
            })
        }
        
        val request = Request.Builder()
            .url(graphQLURL)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    parseGraphQLResponse(responseBody)
                } else {
                    emptyList()
                }
            } else {
                android.util.Log.e("ProteinRepositoryImpl", "❌ GraphQL HTTP error: ${response.code}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("ProteinRepositoryImpl", "❌ GraphQL 요청 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * GraphQL 응답 파싱 (아이폰과 동일)
     */
    private fun parseGraphQLResponse(responseBody: String): List<ProteinInfo> {
        return try {
            val json = JSONObject(responseBody)
            val data = json.getJSONObject("data")
            val entries = data.getJSONArray("entries")
            
            val proteins = mutableListOf<ProteinInfo>()
            
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                val proteinInfo = convertGraphQLToProteinInfo(entry)
                if (proteinInfo != null) {
                    proteins.add(proteinInfo)
                }
            }
            
            android.util.Log.d("ProteinRepositoryImpl", "🧬 GraphQL 성공: ${proteins.size}개 단백질 정보 변환")
            proteins
        } catch (e: Exception) {
            android.util.Log.e("ProteinRepositoryImpl", "❌ GraphQL 응답 파싱 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * GraphQL 엔트리를 ProteinInfo로 변환 (아이폰과 동일)
     */
    private fun convertGraphQLToProteinInfo(entry: JSONObject): ProteinInfo? {
        return try {
            val rcsbId = entry.optString("rcsb_id", "")
            if (rcsbId.isEmpty()) return null
            
            // Name 생성 (아이폰과 동일)
            val structObj = entry.optJSONObject("struct")
            val title = structObj?.optString("title", "") ?: ""
            val name = generateNameFromTitle(title, rcsbId)
            
            // Description 생성 (아이폰과 동일)
            val description = buildDescriptionFromEntry(entry)
            
            // Category 추론 (아이폰과 동일)
            val category = inferCategoryFromEntry(entry)
            
            // Keywords 추출
            val keywords = extractKeywordsFromEntry(entry)
            
            ProteinInfo(
                id = rcsbId,
                name = name,
                category = category,
                description = description,
                keywords = keywords
            )
        } catch (e: Exception) {
            android.util.Log.e("ProteinRepositoryImpl", "❌ GraphQL 엔트리 변환 실패: ${e.message}")
            null
        }
    }
    
    /**
     * Title로부터 Name 생성 (아이폰과 동일)
     */
    private fun generateNameFromTitle(title: String, rcsbId: String): String {
        if (title.isNotEmpty()) {
            val cleanTitle = title
                .replace("CRYSTAL STRUCTURE OF", "", ignoreCase = true)
                .replace("X-RAY STRUCTURE OF", "", ignoreCase = true)
                .trim()
            
            if (cleanTitle.isNotEmpty()) {
                return cleanTitle.split(" ").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
            }
        }
        return "Protein $rcsbId"
    }
    
    /**
     * GraphQL 엔트리로부터 Description 생성 (아이폰과 동일)
     */
    private fun buildDescriptionFromEntry(entry: JSONObject): String {
        val parts = mutableListOf<String>()
        
        val structObj = entry.optJSONObject("struct")
        val title = structObj?.optString("title", "") ?: ""
        if (title.isNotEmpty()) {
            parts.add(title)
        }
        
        val classification = structObj?.optString("pdbx_descriptor", "") ?: ""
        if (classification.isNotEmpty()) {
            parts.add("Classification: $classification")
        }
        
        val exptlArray = entry.optJSONArray("exptl")
        if (exptlArray != null && exptlArray.length() > 0) {
            val methods = mutableListOf<String>()
            for (i in 0 until exptlArray.length()) {
                val method = exptlArray.getJSONObject(i).optString("method", "")
                if (method.isNotEmpty()) methods.add(method)
            }
            if (methods.isNotEmpty()) {
                parts.add("Method: ${methods.joinToString(", ")}")
            }
        }
        
        // Resolution 추가 (아이폰과 동일)
        val entryInfo = entry.optJSONObject("rcsb_entry_info")
        val resolutionArray = entryInfo?.optJSONArray("resolution_combined")
        if (resolutionArray != null && resolutionArray.length() > 0) {
            val resolution = resolutionArray.optDouble(0, -1.0)
            if (resolution > 0) {
                parts.add("Resolution: ${String.format("%.2f", resolution)}Å")
            }
        }
        
        return if (parts.isNotEmpty()) {
            parts.joinToString(" | ")
        } else {
            "No description available"
        }
    }
    
    /**
     * GraphQL 엔트리로부터 Category 추론 (아이폰과 동일)
     */
    private fun inferCategoryFromEntry(entry: JSONObject): ProteinCategory {
        val structObj = entry.optJSONObject("struct")
        val title = (structObj?.optString("title", "") ?: "").lowercase()
        val classification = (structObj?.optString("pdbx_descriptor", "") ?: "").lowercase()
        val keywordsObj = entry.optJSONObject("struct_keywords")
        val keywords = (keywordsObj?.optString("pdbx_keywords", "") ?: "").lowercase()
        
        val allText = "$title $classification $keywords"
        
        return inferCategoryFromText(allText)
    }
    
    /**
     * 텍스트로부터 카테고리 추론 (아이폰과 동일)
     */
    private fun inferCategoryFromText(text: String): ProteinCategory {
        val lowercaseText = text.lowercase()
        
        // 효소
        if (lowercaseText.contains("enzyme") || lowercaseText.contains("kinase") || 
            lowercaseText.contains("transferase") || lowercaseText.contains("hydrolase") ||
            lowercaseText.contains("oxidoreductase")) {
            return ProteinCategory.ENZYMES
        }
        
        // 구조 단백질
        if (lowercaseText.contains("collagen") || lowercaseText.contains("actin") || 
            lowercaseText.contains("tubulin") || lowercaseText.contains("keratin") ||
            lowercaseText.contains("structural") || lowercaseText.contains("cytoskeleton")) {
            return ProteinCategory.STRUCTURAL
        }
        
        // 방어 단백질
        if (lowercaseText.contains("antibody") || lowercaseText.contains("immunoglobulin") || 
            lowercaseText.contains("complement") || lowercaseText.contains("immune")) {
            return ProteinCategory.DEFENSE
        }
        
        // 운반 단백질
        if (lowercaseText.contains("hemoglobin") || lowercaseText.contains("myoglobin") || 
            lowercaseText.contains("transport") || lowercaseText.contains("carrier")) {
            return ProteinCategory.TRANSPORT
        }
        
        // 호르몬
        if (lowercaseText.contains("hormone") || lowercaseText.contains("insulin") || 
            lowercaseText.contains("growth factor")) {
            return ProteinCategory.HORMONES
        }
        
        // 저장 단백질
        if (lowercaseText.contains("ferritin") || lowercaseText.contains("storage")) {
            return ProteinCategory.STORAGE
        }
        
        // 수용체
        if (lowercaseText.contains("receptor") || lowercaseText.contains("gpcr")) {
            return ProteinCategory.RECEPTORS
        }
        
        // 막 단백질
        if (lowercaseText.contains("membrane") || lowercaseText.contains("channel") || 
            lowercaseText.contains("pump")) {
            return ProteinCategory.MEMBRANE
        }
        
        // 모터 단백질
        if (lowercaseText.contains("motor") || lowercaseText.contains("myosin") || 
            lowercaseText.contains("kinesin") || lowercaseText.contains("dynein")) {
            return ProteinCategory.MOTOR
        }
        
        // 신호전달
        if (lowercaseText.contains("signaling") || lowercaseText.contains("signal transduction")) {
            return ProteinCategory.SIGNALING
        }
        
        // 샤페론
        if (lowercaseText.contains("chaperone") || lowercaseText.contains("hsp")) {
            return ProteinCategory.CHAPERONES
        }
        
        // 대사
        if (lowercaseText.contains("metabolic") || lowercaseText.contains("metabolism")) {
            return ProteinCategory.METABOLIC
        }
        
        return ProteinCategory.ENZYMES // 기본값
    }
    
    /**
     * GraphQL 엔트리로부터 키워드 추출
     */
    private fun extractKeywordsFromEntry(entry: JSONObject): List<String> {
        val keywords = mutableListOf<String>()
        
        val rcsbId = entry.optString("rcsb_id", "")
        if (rcsbId.isNotEmpty()) {
            keywords.add(rcsbId.lowercase())
        }
        
        val structObj = entry.optJSONObject("struct")
        val title = structObj?.optString("title", "") ?: ""
        if (title.isNotEmpty()) {
            title.split(" ").filter { it.length > 3 }.take(5).forEach {
                keywords.add(it.lowercase())
            }
        }
        
        return keywords.take(5)
    }
}
