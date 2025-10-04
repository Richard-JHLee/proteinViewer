package com.avas.proteinviewer.data.repository

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
class ProteinRepositoryImpl @Inject constructor() : ProteinRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)  // DNS 해석 시간 증가
        .readTimeout(45, TimeUnit.SECONDS)     // 데이터 읽기 시간 증가
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)        // 연결 실패 시 자동 재시도
        .build()

    override fun searchProteins(query: String): Flow<List<ProteinInfo>> = flow {
        if (query.isEmpty()) {
            // Return default proteins
            emit(getDefaultProteins())
            return@flow
        }

        try {
            // Search using RCSB PDB Search API
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
                      "rows": 20
                    }
                  }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(searchUrl)
                .post(okhttp3.RequestBody.create(
                    "application/json".toMediaType(),
                    requestBody
                ))
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(response.body?.string() ?: "{}")
                val results = jsonResponse.optJSONArray("result_set") ?: JSONArray()
                
                val proteins = mutableListOf<ProteinInfo>()
                for (i in 0 until results.length()) {
                    val result = results.getJSONObject(i)
                    val id = result.optString("identifier", "")
                    if (id.isNotEmpty()) {
                        // Fetch details for each protein
                        val detail = fetchProteinDetail(id)
                        proteins.add(ProteinInfo(
                            id = id,
                            name = detail?.name ?: id,
                            description = detail?.description ?: "No Data",
                            organism = detail?.organism,
                            resolution = detail?.resolution,
                            experimentalMethod = detail?.experimentalMethod,
                            depositionDate = detail?.depositionDate,
                            molecularWeight = detail?.molecularWeight
                        ))
                    }
                }
                
                emit(proteins)
            } else {
                emit(getDefaultProteins())
            }
        } catch (e: Exception) {
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

    override suspend fun loadPDBStructure(proteinId: String, onProgress: (String) -> Unit): PDBStructure {
        return withContext(Dispatchers.IO) {
            onProgress("Downloading protein data...")
            
            // Multiple URL attempts (fallback mechanism)
            val urls = listOf(
                "https://files.rcsb.org/download/${proteinId.uppercase()}.pdb",
                "https://files.wwpdb.org/pub/pdb/data/structures/all/pdb/pdb${proteinId.lowercase()}.ent.gz",
                "http://files.rcsb.org/download/${proteinId.uppercase()}.pdb" // HTTP fallback
            )
            
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
                            val gzipInputStream = java.util.zip.GZIPInputStream(response.body?.byteStream())
                            gzipInputStream.bufferedReader().use { it.readText() }
                        } else {
                            response.body?.string() ?: throw Exception("Empty file")
                        }
                        
                        if (pdbText.isNotEmpty()) {
                            onProgress("Parsing protein structure...")
                            android.util.Log.d("ProteinRepository", "Successfully downloaded from: $pdbUrl")
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
                description = "Small plant seed protein",
                organism = "Crambe abyssinica",
                resolution = 0.54,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 4.7
            ),
            ProteinInfo(
                id = "1HHO",
                name = "Hemoglobin",
                description = "Oxygen transport protein",
                organism = "Homo sapiens",
                resolution = 2.1,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 64.5
            ),
            ProteinInfo(
                id = "2LYZ",
                name = "Lysozyme",
                description = "Antibacterial enzyme",
                organism = "Gallus gallus",
                resolution = 1.5,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 14.3
            ),
            ProteinInfo(
                id = "4INS",
                name = "Insulin",
                description = "Hormone regulating glucose metabolism",
                organism = "Homo sapiens",
                resolution = 1.9,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 5.8
            ),
            ProteinInfo(
                id = "1RXZ",
                name = "Ribonuclease A",
                description = "RNA degradation enzyme",
                organism = "Bos taurus",
                resolution = 1.26,
                experimentalMethod = "X-RAY DIFFRACTION",
                molecularWeight = 13.7
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
                    parseSearchResults(responseBody)
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
    private fun parseSearchResults(responseBody: String): List<ProteinInfo> {
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
                        description = "Sample protein from ${pdbId} category",
                        organism = "Homo sapiens",
                        resolution = 2.5,
                        experimentalMethod = "X-RAY DIFFRACTION"
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
            ProteinCategory.TRANSPORT -> 25000
            ProteinCategory.STORAGE -> 5000
            ProteinCategory.HORMONAL -> 8000
            ProteinCategory.DEFENSE -> 18000
            ProteinCategory.REGULATORY -> 12000
            ProteinCategory.MOTOR -> 6000
            ProteinCategory.RECEPTOR -> 15000
            ProteinCategory.SIGNALING -> 12000
            ProteinCategory.METABOLIC -> 38000
            ProteinCategory.BINDING -> 22000
        }
        
        // 실제 개수는 baseCount이지만, 검색 결과는 limit만큼만 반환
        for (i in 1..minOf(limit, 50)) {
            sampleProteins.add(
                ProteinInfo(
                    id = "${category.name.substring(0, 2).uppercase()}$i",
                    name = "${category.displayName} Protein $i",
                    description = "Sample ${category.displayName.lowercase()} protein",
                    organism = "Homo sapiens",
                    resolution = 2.0 + (i % 3),
                    experimentalMethod = "X-RAY DIFFRACTION"
                )
            )
        }
        
        return sampleProteins
    }
}
