package com.avas.proteinviewer.data.api

import com.avas.proteinviewer.domain.model.ExperimentalDetails
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
class ExperimentalDetailsAPIService @Inject constructor() {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 아이폰 DetailedInfoSectionView.fetchExperimentalDetails()와 동일
     * GraphQL로 실험 상세 정보 가져오기
     */
    suspend fun fetchExperimentalDetails(pdbId: String): ExperimentalDetails {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ExperimentalDetailsAPI", "🔍 Fetching experimental details for: $pdbId")
                
                val query = """
                    query {
                      entries(entry_ids: ["${pdbId.uppercase()}"]) {
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
                        rcsb_primary_citation {
                          title
                          journal_abbrev
                        }
                        rcsb_accession_info {
                          deposit_date
                          initial_release_date
                        }
                      }
                    }
                """.trimIndent()
                
                val requestBody = JSONObject().apply {
                    put("query", query)
                }
                
                val request = Request.Builder()
                    .url("https://data.rcsb.org/graphql")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("GraphQL API failed: ${response.code}")
                }
                
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                
                if (!json.has("data")) {
                    throw Exception("No data in GraphQL response")
                }
                
                val data = json.getJSONObject("data")
                if (!data.has("entries")) {
                    throw Exception("No entries in GraphQL response")
                }
                
                val entries = data.getJSONArray("entries")
                if (entries.length() == 0) {
                    throw Exception("Empty entries array")
                }
                
                val entry = entries.getJSONObject(0)
                
                // Extract fields
                var experimentalMethod: String? = null
                var resolution: Double? = null
                var organism: String? = null
                var expression: String? = null
                var journal: String? = null
                var depositionDate: String? = null
                var releaseDate: String? = null
                
                // Experimental method
                if (entry.has("exptl")) {
                    val exptlArray = entry.getJSONArray("exptl")
                    if (exptlArray.length() > 0) {
                        experimentalMethod = exptlArray.getJSONObject(0).optString("method", null)
                    }
                }
                
                // Fallback: rcsb_entry_info.experimental_method
                if (experimentalMethod == null && entry.has("rcsb_entry_info")) {
                    val entryInfo = entry.getJSONObject("rcsb_entry_info")
                    experimentalMethod = entryInfo.optString("experimental_method", null)
                }
                
                // Resolution
                if (entry.has("rcsb_entry_info")) {
                    val entryInfo = entry.getJSONObject("rcsb_entry_info")
                    val resolutionArray = entryInfo.optJSONArray("resolution_combined")
                    if (resolutionArray != null && resolutionArray.length() > 0) {
                        resolution = resolutionArray.optDouble(0, -1.0)
                        if (resolution == -1.0) resolution = null
                    }
                }
                
                // Organism (from title)
                if (entry.has("struct")) {
                    val struct = entry.getJSONObject("struct")
                    val title = struct.optString("title", "")
                    organism = extractOrganism(title)
                }
                
                // Journal
                if (entry.has("rcsb_primary_citation")) {
                    val citation = entry.getJSONObject("rcsb_primary_citation")
                    journal = citation.optString("journal_abbrev", null)
                }
                
                // Dates
                if (entry.has("rcsb_accession_info")) {
                    val accessionInfo = entry.getJSONObject("rcsb_accession_info")
                    depositionDate = accessionInfo.optString("deposit_date", null)
                    releaseDate = accessionInfo.optString("initial_release_date", null)
                }
                
                // Expression host (기본값)
                expression = "E. coli"
                
                android.util.Log.d("ExperimentalDetailsAPI", "✅ Experimental details loaded")
                android.util.Log.d("ExperimentalDetailsAPI", "   Method: $experimentalMethod")
                android.util.Log.d("ExperimentalDetailsAPI", "   Resolution: $resolution")
                android.util.Log.d("ExperimentalDetailsAPI", "   Organism: $organism")
                android.util.Log.d("ExperimentalDetailsAPI", "   Journal: $journal")
                
                ExperimentalDetails(
                    experimentalMethod = experimentalMethod,
                    resolution = resolution,
                    organism = organism,
                    expression = expression,
                    journal = journal,
                    depositionDate = depositionDate,
                    releaseDate = releaseDate
                )
                
            } catch (e: Exception) {
                android.util.Log.e("ExperimentalDetailsAPI", "❌ Experimental details failed: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * 아이폰과 동일: 제목에서 생물체 정보 추출
     */
    private fun extractOrganism(title: String): String? {
        val upperTitle = title.uppercase()
        
        return when {
            upperTitle.contains("HUMAN") || upperTitle.contains("HOMO SAPIENS") -> "Homo sapiens"
            upperTitle.contains("MOUSE") || upperTitle.contains("MUS MUSCULUS") -> "Mus musculus"
            upperTitle.contains("BOVINE") || upperTitle.contains("BOS TAURUS") -> "Bos taurus"
            upperTitle.contains("CHICKEN") || upperTitle.contains("GALLUS GALLUS") -> "Gallus gallus"
            upperTitle.contains("RAT") || upperTitle.contains("RATTUS NORVEGICUS") -> "Rattus norvegicus"
            upperTitle.contains("E. COLI") || upperTitle.contains("ESCHERICHIA COLI") -> "Escherichia coli"
            upperTitle.contains("YEAST") || upperTitle.contains("SACCHAROMYCES") -> "Saccharomyces cerevisiae"
            else -> null
        }
    }
}

