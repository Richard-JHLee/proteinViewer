package com.avas.proteinviewer.data.api

import com.avas.proteinviewer.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelatedProteinsAPIService @Inject constructor() {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 아이폰 RelatedProteinsView.fetchRelatedProteinsFromPDB()와 동일
     * GraphQL 사용 (더 안정적)
     */
    suspend fun fetchRelatedProteins(pdbId: String, category: ProteinCategory): List<RelatedProtein> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("RelatedProteinsAPI", "🚀 Starting Search API call (GET method like iPhone)...")
                
                // 개선된 방식: 선택한 카테고리에 맞는 키워드 사용
                val searchKeyword = getCategoryKeyword(category)
                android.util.Log.d("RelatedProteinsAPI", "🔍 Search keyword for category ${category.displayName}: $searchKeyword")
                
                val simpleQuery = JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "terminal")
                        put("service", "text")
                        put("parameters", JSONObject().apply {
                            put("attribute", "struct_keywords.pdbx_keywords")
                            put("operator", "contains_phrase")
                            put("value", searchKeyword)
                        })
                    })
                    put("return_type", "entry")
                }
                
                val encodedQuery = java.net.URLEncoder.encode(simpleQuery.toString(), "UTF-8")
                val url = "https://search.rcsb.org/rcsbsearch/v2/query?json=$encodedQuery&return_type=entry&rows=20"
                
                android.util.Log.d("RelatedProteinsAPI", "🔍 Search API 요청 (GET): $url")
                android.util.Log.d("RelatedProteinsAPI", "📋 Query JSON: ${simpleQuery.toString()}")
                
                val request = Request.Builder()
                    .url(url)
                    .get() // GET 방식
                    .build()
                
                val response = httpClient.newCall(request).execute()
                android.util.Log.d("RelatedProteinsAPI", "📥 HTTP 응답 상태: ${response.code}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No error body"
                    android.util.Log.e("RelatedProteinsAPI", "❌ Search API failed: ${response.code}")
                    android.util.Log.e("RelatedProteinsAPI", "❌ Error body: $errorBody")
                    throw Exception("Search API failed with code: ${response.code}")
                }
                
                val responseBody = response.body?.string() ?: ""
                android.util.Log.d("RelatedProteinsAPI", "📦 받은 데이터 크기: ${responseBody.length} bytes")
                
                val responseJson = JSONObject(responseBody)
                
                // Search API 결과 파싱
                val relatedProteins = mutableListOf<RelatedProtein>()
                
                if (responseJson.has("result_set")) {
                    val resultSet = responseJson.getJSONArray("result_set")
                    android.util.Log.d("RelatedProteinsAPI", "📦 검색 결과: ${resultSet.length()} 개 단백질")
                    
                    val totalCount = responseJson.optInt("total_count", 0)
                    android.util.Log.d("RelatedProteinsAPI", "📊 전체 개수: $totalCount")
                    
                    // 아이폰과 동일: result_set의 각 identifier에 대해 Entry API 호출
                    for (i in 0 until resultSet.length().coerceAtMost(10)) {
                        val result = resultSet.getJSONObject(i)
                        val identifier = result.optString("identifier", "")
                        
                        if (identifier.isNotEmpty() && identifier != pdbId) {
                            android.util.Log.d("RelatedProteinsAPI", "🔍 Processing: $identifier")
                            
                            // Entry API로 상세 정보 가져오기 (아이폰과 동일)
                            try {
                                val entryUrl = "https://data.rcsb.org/rest/v1/core/entry/$identifier"
                                val entryRequest = Request.Builder()
                                    .url(entryUrl)
                                    .get()
                                    .addHeader("Accept", "application/json")
                                    .build()
                                
                                val entryResponse = httpClient.newCall(entryRequest).execute()
                                if (entryResponse.isSuccessful) {
                                    val entryBody = entryResponse.body?.string() ?: ""
                                    val entryJson = JSONObject(entryBody)
                                    
                                    // Extract data
                                    val structObj = entryJson.optJSONObject("struct")
                                    val title = structObj?.optString("title", "Unknown Protein") ?: "Unknown Protein"
                                    val name = title.replace("CRYSTAL STRUCTURE OF", "", ignoreCase = true)
                                        .replace("X-RAY STRUCTURE OF", "", ignoreCase = true)
                                        .trim()
                                        .take(100)
                                    
                                    val pdbxDescriptor = structObj?.optString("pdbx_descriptor", "") ?: ""
                                    
                                    var resolution: Double? = null
                                    if (entryJson.has("refine")) {
                                        val refineArray = entryJson.getJSONArray("refine")
                                        if (refineArray.length() > 0) {
                                            resolution = refineArray.getJSONObject(0).optDouble("ls_d_res_high", -1.0)
                                            if (resolution == -1.0) resolution = null
                                        }
                                    }
                                    
                                    var atomCount = 0
                                    var chainCount = 1
                                    if (entryJson.has("rcsb_entry_info")) {
                                        val entryInfo = entryJson.getJSONObject("rcsb_entry_info")
                                        atomCount = entryInfo.optInt("deposited_atom_count", 0)
                                        chainCount = entryInfo.optInt("polymer_entity_count_protein", 1)
                                    }
                                    
                                    // Keywords도 함께 사용하여 더 정확한 카테고리 추론
                                    val keywords = entryJson.optJSONObject("struct_keywords")?.optString("pdbx_keywords", "") ?: ""
                                    val relatedCategory = inferCategoryFromText("$title $pdbxDescriptor $keywords")
                                    
                                    // Relationship 결정 (아이폰과 동일한 로직)
                                    val relationship = determineRelationship(keywords)
                                    
                                    // Similarity 계산 (간단한 휴리스틱)
                                    val lowercaseKeywords = keywords.lowercase()
                                    val similarity = when {
                                        lowercaseKeywords.contains("homolog") -> 0.8 + (Math.random() * 0.15)
                                        lowercaseKeywords.contains("family") -> 0.7 + (Math.random() * 0.2)
                                        relatedCategory == category -> 0.6 + (Math.random() * 0.2)
                                        else -> 0.5 + (Math.random() * 0.3)
                                    }
                                    
                                    relatedProteins.add(
                                        RelatedProtein(
                                            id = identifier,
                                            name = name,
                                            category = relatedCategory,
                                            description = pdbxDescriptor,
                                            chainCount = chainCount,
                                            atomCount = atomCount,
                                            resolution = resolution,
                                            relationship = relationship,
                                            similarity = similarity
                                        )
                                    )
                                    
                                    android.util.Log.d("RelatedProteinsAPI", "✅ Added: $identifier - $name")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("RelatedProteinsAPI", "⚠️ Failed to fetch entry details for $identifier: ${e.message}")
                                continue
                            }
                        }
                    }
                }
                
                android.util.Log.d("RelatedProteinsAPI", "✅ 최종 결과: ${relatedProteins.size} proteins")
                relatedProteins
                
            } catch (e: Exception) {
                android.util.Log.e("RelatedProteinsAPI", "❌ Related proteins API failed: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }
    
    /**
     * 아이폰과 동일: 단백질 간 관계 결정
     */
    private fun determineRelationship(keywords: String): String {
        val lowercaseKeywords = keywords.lowercase()
        
        return when {
            lowercaseKeywords.contains("homolog") -> "Structural homolog"
            lowercaseKeywords.contains("family") -> "Protein family"
            lowercaseKeywords.contains("binding") || lowercaseKeywords.contains("interaction") -> "Binding partner"
            lowercaseKeywords.contains("regulatory") || lowercaseKeywords.contains("regulation") -> "Regulatory partner"
            else -> "Similar function"
        }
    }
    
    /**
     * 아이폰과 동일: 텍스트에서 카테고리 추론
     */
    private fun inferCategoryFromText(text: String): ProteinCategory {
        val lowercaseText = text.lowercase()
        
        return when {
            lowercaseText.contains("enzyme") || lowercaseText.contains("kinase") ||
            lowercaseText.contains("transferase") || lowercaseText.contains("hydrolase") ||
            lowercaseText.contains("oxidoreductase") -> ProteinCategory.ENZYMES
            
            lowercaseText.contains("collagen") || lowercaseText.contains("actin") ||
            lowercaseText.contains("tubulin") || lowercaseText.contains("keratin") ||
            lowercaseText.contains("structural") || lowercaseText.contains("cytoskeleton") -> ProteinCategory.STRUCTURAL
            
            lowercaseText.contains("antibody") || lowercaseText.contains("immunoglobulin") ||
            lowercaseText.contains("complement") || lowercaseText.contains("immune") -> ProteinCategory.DEFENSE
            
            lowercaseText.contains("hemoglobin") || lowercaseText.contains("myoglobin") ||
            lowercaseText.contains("transport") || lowercaseText.contains("carrier") -> ProteinCategory.TRANSPORT
            
            lowercaseText.contains("hormone") || lowercaseText.contains("insulin") ||
            lowercaseText.contains("growth factor") -> ProteinCategory.HORMONES
            
            lowercaseText.contains("ferritin") || lowercaseText.contains("storage") -> ProteinCategory.STORAGE
            
            lowercaseText.contains("receptor") || lowercaseText.contains("gpcr") -> ProteinCategory.RECEPTORS
            
            lowercaseText.contains("membrane") || lowercaseText.contains("channel") ||
            lowercaseText.contains("pump") -> ProteinCategory.MEMBRANE
            
            lowercaseText.contains("motor") || lowercaseText.contains("myosin") ||
            lowercaseText.contains("kinesin") || lowercaseText.contains("dynein") -> ProteinCategory.MOTOR
            
            lowercaseText.contains("signaling") || lowercaseText.contains("signal transduction") -> ProteinCategory.SIGNALING
            
            lowercaseText.contains("chaperone") || lowercaseText.contains("hsp") -> ProteinCategory.CHAPERONES
            
            lowercaseText.contains("metabolic") || lowercaseText.contains("metabolism") -> ProteinCategory.METABOLIC
            
            else -> ProteinCategory.ENZYMES
        }
    }
    
    /**
     * 카테고리별 검색 키워드
     */
    private fun getCategoryKeyword(category: ProteinCategory): String {
        return when (category) {
            ProteinCategory.ENZYMES -> "enzyme"
            ProteinCategory.STRUCTURAL -> "structural"
            ProteinCategory.DEFENSE -> "antibody"
            ProteinCategory.TRANSPORT -> "transport"
            ProteinCategory.HORMONES -> "hormone"
            ProteinCategory.STORAGE -> "storage"
            ProteinCategory.RECEPTORS -> "receptor"
            ProteinCategory.MEMBRANE -> "membrane"
            ProteinCategory.MOTOR -> "motor"
            ProteinCategory.SIGNALING -> "signaling"
            ProteinCategory.CHAPERONES -> "chaperone"
            ProteinCategory.METABOLIC -> "metabolic"
        }
    }
}

/**
 * 임시 데이터 클래스
 */
data class ProteinDetailData(
    val name: String,
    val description: String,
    val category: ProteinCategory,
    val resolution: Double?,
    val atomCount: Int,
    val chainCount: Int
)

