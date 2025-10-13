package com.avas.proteinviewer.data.api

import com.avas.proteinviewer.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FunctionAPIService @Inject constructor() {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    // MARK: - Function Details API
    
    /**
     * 아이폰과 동일: PDB ID로 기능 정보 가져오기
     * 1. RCSB PDB Entry API에서 기본 정보 조회
     * 2. UniProt API에서 상세 기능 정보 조회
     */
    suspend fun fetchFunctionDetails(pdbId: String, proteinDescription: String): FunctionDetails {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("FunctionAPI", "🔍 Fetching function details for PDB: $pdbId")
                
                // Step 1: RCSB PDB Entry API에서 기본 정보 조회
                val entryDetails = fetchEntryDetailsFromPDB(pdbId)
                android.util.Log.d("FunctionAPI", "✅ Entry details loaded")
                
                // Step 2: UniProt API에서 상세 기능 정보 조회
                val uniprotDetails = fetchFunctionFromUniProt(pdbId)
                android.util.Log.d("FunctionAPI", "✅ UniProt details loaded")
                
                // Step 3: 데이터 결합
                val functionDetails = combineFunctionData(entryDetails, uniprotDetails, proteinDescription)
                android.util.Log.d("FunctionAPI", "✅ Function details combined successfully")
                
                functionDetails
                
            } catch (e: Exception) {
                android.util.Log.e("FunctionAPI", "❌ Function API failed: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }
    
    /**
     * RCSB PDB Entry API에서 기본 정보 조회
     */
    private suspend fun fetchEntryDetailsFromPDB(pdbId: String): Map<String, Any> {
        android.util.Log.d("FunctionAPI", "🔍 Fetching entry details from RCSB PDB")
        
        val url = "https://data.rcsb.org/rest/v1/core/entry/$pdbId"
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build()
        
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            android.util.Log.e("FunctionAPI", "❌ RCSB PDB API error: ${response.code}")
            throw Exception("RCSB PDB API failed with code: ${response.code}")
        }
        
        val jsonBody = response.body?.string() ?: ""
        val jsonObject = JSONObject(jsonBody)
        
        return mapOf(
            "resolution" to (jsonObject.optJSONObject("refine")?.optDouble("ls_d_res_high") ?: -1.0),
            "method" to (jsonObject.optJSONArray("exptl")?.optJSONObject(0)?.optString("method") ?: "Unknown"),
            "depositionDate" to (jsonObject.optString("deposit_date", "")),
            "releaseDate" to (jsonObject.optString("release_date", "")),
            "numberOfChains" to (jsonObject.optJSONArray("polymer_entities")?.length() ?: 0),
            "numberOfResidues" to (jsonObject.optJSONObject("rcsb_entry_info")?.optInt("deposited_atom_count", 0) ?: 0)
        )
    }
    
    /**
     * UniProt API에서 기능 정보 조회
     */
    private suspend fun fetchFunctionFromUniProt(pdbId: String): Map<String, Any> {
        android.util.Log.d("FunctionAPI", "🔍 Fetching function details from UniProt")
        
        // PDB ID를 UniProt ID로 변환 (간단한 매핑 사용)
        val uniprotId = mapPdbToUniProt(pdbId)
        if (uniprotId.isEmpty()) {
            android.util.Log.w("FunctionAPI", "⚠️ No UniProt ID found for PDB: $pdbId")
            return emptyMap()
        }
        
        val url = "https://rest.uniprot.org/uniprotkb/$uniprotId"
        val request = Request.Builder()
            .url(url)
                                .addHeader("Accept", "application/json")
                                .build()
                            
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            android.util.Log.e("FunctionAPI", "❌ UniProt API error: ${response.code}")
            return emptyMap()
        }
        
        val jsonBody = response.body?.string() ?: ""
        return parseUniProtFunctionData(jsonBody)
    }
    
    /**
     * PDB ID를 UniProt ID로 매핑
     */
    private fun mapPdbToUniProt(pdbId: String): String {
        val mapping = mapOf(
            "1HHO" to "P69905",  // Hemoglobin
            "2HHB" to "P69905",  // Hemoglobin
            "1A00" to "P01112",  // H-RAS
            "1AKE" to "P69441",  // Adenylate Kinase
            "1BRS" to "P00974",  // Barnase
            "1CRN" to "P01542",  // Crambin
            "1GFL" to "P00720",  // GFP
            "1MBO" to "P02185",  // Myoglobin
            "1UBQ" to "P0CG48",  // Ubiquitin
            "2LYZ" to "P00698",  // Lysozyme
            "3I40" to "P04637",  // p53
            "4HHB" to "P69905"   // Hemoglobin
        )
        
        return mapping[pdbId.uppercase()] ?: ""
    }
    
    /**
     * UniProt JSON에서 기능 정보 추출
     */
    private fun parseUniProtFunctionData(jsonBody: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        try {
            val jsonObject = JSONObject(jsonBody)
            
            // Extract function information from comments
            if (jsonObject.has("comments")) {
                val comments = jsonObject.getJSONArray("comments")
                
                                            for (i in 0 until comments.length()) {
                                                val comment = comments.getJSONObject(i)
                                                val commentType = comment.optString("commentType", "")
                                                
                                                when (commentType) {
                                                    "FUNCTION" -> {
                                                        if (comment.has("texts")) {
                                                            val texts = comment.getJSONArray("texts")
                                                            if (texts.length() > 0) {
                                    val functionText = texts.getJSONObject(0).optString("value", "")
                                    result["molecularFunction"] = functionText
                                                            }
                                                        }
                                                    }
                        "CATALYTIC_ACTIVITY" -> {
                                                        if (comment.has("texts")) {
                                                            val texts = comment.getJSONArray("texts")
                                                            if (texts.length() > 0) {
                                    val catalyticText = texts.getJSONObject(0).optString("value", "")
                                    result["catalyticActivity"] = catalyticText
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
            // Extract GO terms
            val goTerms = mutableListOf<GOTerm>()
            if (jsonObject.has("uniProtKBCrossReferences")) {
                val crossRefs = jsonObject.getJSONArray("uniProtKBCrossReferences")
                                            for (i in 0 until crossRefs.length()) {
                    val crossRef = crossRefs.getJSONObject(i)
                    if (crossRef.optString("database") == "GO") {
                        val goTerm = crossRef.optString("id", "")
                        val goName = crossRef.optString("properties")?.let { 
                            JSONObject(it).optString("term", "")
                        } ?: ""
                        val category = when {
                            goName.contains("molecular function", ignoreCase = true) -> "MF"
                            goName.contains("biological process", ignoreCase = true) -> "BP"
                            goName.contains("cellular component", ignoreCase = true) -> "CC"
                            else -> "MF"
                        }
                        if (goTerm.isNotEmpty()) {
                            goTerms.add(GOTerm(id = goTerm, name = goName, category = category))
                        }
                    }
                }
            }
            result["goTerms"] = goTerms
            
            // Extract EC numbers
            val ecNumbers = mutableListOf<String>()
            if (jsonObject.has("uniProtKBCrossReferences")) {
                val crossRefs = jsonObject.getJSONArray("uniProtKBCrossReferences")
                for (i in 0 until crossRefs.length()) {
                    val crossRef = crossRefs.getJSONObject(i)
                    if (crossRef.optString("database") == "EC") {
                        val ecNumber = crossRef.optString("id", "")
                        if (ecNumber.isNotEmpty()) {
                            ecNumbers.add(ecNumber)
                        }
                    }
                }
            }
            result["ecNumbers"] = ecNumbers
            
        } catch (e: Exception) {
            android.util.Log.e("FunctionAPI", "❌ Failed to parse UniProt function data: ${e.message}")
        }
        
        return result
    }
    
    /**
     * Entry details와 UniProt details를 결합하여 FunctionDetails 생성
     */
    private fun combineFunctionData(
        entryDetails: Map<String, Any>,
        uniprotDetails: Map<String, Any>,
        proteinDescription: String
    ): FunctionDetails {
        val molecularFunction = uniprotDetails["molecularFunction"] as? String ?: "Function information not available"
        val biologicalProcess = "Biological process information not available"
        val cellularComponent = "Cellular component information not available"
        val goTerms = uniprotDetails["goTerms"] as? List<GOTerm> ?: emptyList()
        val ecNumbers = uniprotDetails["ecNumbers"] as? List<String> ?: emptyList()
        val catalyticActivity = uniprotDetails["catalyticActivity"] as? String ?: "Catalytic activity information not available"
        
        val resolution = entryDetails["resolution"] as? Double ?: -1.0
        val method = entryDetails["method"] as? String ?: "Unknown"
        val depositionDate = entryDetails["depositionDate"] as? String ?: ""
        val releaseDate = entryDetails["releaseDate"] as? String ?: ""
        val numberOfChains = entryDetails["numberOfChains"] as? Int ?: 0
        val numberOfResidues = entryDetails["numberOfResidues"] as? Int ?: 0
                
                // Fallback: 기본 description 사용
        val finalMolecularFunction = if (molecularFunction == "Function information not available") {
            proteinDescription
        } else {
            molecularFunction
                }
                
                android.util.Log.d("FunctionAPI", "✅ Function details loaded successfully")
        android.util.Log.d("FunctionAPI", "📊 Final catalyticActivity: '$catalyticActivity'")
        android.util.Log.d("FunctionAPI", "📊 Is default? ${catalyticActivity == "Catalytic activity information not available"}")
                
        return FunctionDetails(
            molecularFunction = finalMolecularFunction,
                    biologicalProcess = biologicalProcess,
                    cellularComponent = cellularComponent,
                    goTerms = goTerms,
                    ecNumbers = ecNumbers,
                    catalyticActivity = catalyticActivity,
                    resolution = resolution,
            method = method,
            depositionDate = depositionDate,
            releaseDate = releaseDate,
            numberOfChains = numberOfChains,
            numberOfResidues = numberOfResidues
        )
    }
}