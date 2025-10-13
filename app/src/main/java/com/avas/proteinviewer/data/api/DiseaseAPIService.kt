package com.avas.proteinviewer.data.api

import com.avas.proteinviewer.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiseaseAPIService @Inject constructor() {
    
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
    
    // MARK: - Disease Association API
    
    /**
     * 아이폰과 동일: PDB ID로 질병 연관 정보 가져오기
     * 1. PDB ID -> UniProt ID 변환
     * 2. UniProt API에서 질병 정보 조회
     */
    suspend fun fetchDiseaseAssociations(pdbId: String): List<DiseaseAssociation> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("DiseaseAPI", "🔍 Fetching disease associations for PDB: $pdbId")
                
                // Step 1: PDB ID -> UniProt ID 변환
                val uniprotId = fetchUniProtIdFromPDB(pdbId)
                android.util.Log.d("DiseaseAPI", "✅ UniProt ID: $uniprotId")
                
                // Step 2: UniProt API에서 질병 정보 조회
                val diseases = fetchDiseaseFromUniProt(uniprotId)
                android.util.Log.d("DiseaseAPI", "✅ Found ${diseases.size} disease associations")
                
                diseases
            } catch (e: Exception) {
                android.util.Log.e("DiseaseAPI", "❌ Disease API failed: ${e.message}")
                e.printStackTrace()
                // 실제 데이터가 없을 때는 빈 리스트 반환 (샘플 데이터 제거)
                emptyList()
            }
        }
    }
    
    /**
     * 아이폰 DiseaseAssociationService.fetchUniProtIdFromPDB()와 동일
     * PDB ID를 UniProt ID로 변환
     */
    private suspend fun fetchUniProtIdFromPDB(pdbId: String): String {
        android.util.Log.d("DiseaseAPI", "🔍 Converting PDB ID $pdbId to UniProt ID")
        
        // 방법 1: RCSB PDB API - polymer_entity_instances 엔드포인트
        try {
            val url = "https://data.rcsb.org/rest/v1/core/polymer_entity_instances/$pdbId/1"
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonBody = response.body?.string() ?: ""
                val jsonObject = JSONObject(jsonBody)
                
                // rcsb_polymer_entity_instance_container_identifiers.uniprot_ids 추출
                if (jsonObject.has("rcsb_polymer_entity_instance_container_identifiers")) {
                    val identifiers = jsonObject.getJSONObject("rcsb_polymer_entity_instance_container_identifiers")
                    if (identifiers.has("uniprot_ids")) {
                        val uniprotIds = identifiers.getJSONArray("uniprot_ids")
                        if (uniprotIds.length() > 0) {
                            val uniprotId = uniprotIds.getString(0)
                            android.util.Log.d("DiseaseAPI", "✅ Found UniProt ID via RCSB: $uniprotId")
                            return uniprotId
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DiseaseAPI", "⚠️ RCSB API method 1 failed: ${e.message}")
        }
        
        // 방법 2: RCSB GraphQL API
        try {
            val graphQLURL = "https://data.rcsb.org/graphql"
            val query = """
                query {
                  entry(entry_id: "$pdbId") {
                    polymer_entities {
                      rcsb_polymer_entity_container_identifiers {
                        uniprot_ids
                      }
                    }
                  }
                }
            """.trimIndent()
            
            val requestBody = JSONObject().apply {
                put("query", query)
            }
            
            val request = Request.Builder()
                .url(graphQLURL)
                .post(requestBody.toString().toRequestBody(
                    "application/json".toMediaType()
                ))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonBody = response.body?.string() ?: ""
                val jsonObject = JSONObject(jsonBody)
                
                if (jsonObject.has("data")) {
                    val data = jsonObject.getJSONObject("data")
                    if (data.has("entry") && !data.isNull("entry")) {
                        val entry = data.getJSONObject("entry")
                        if (entry.has("polymer_entities")) {
                            val polymerEntities = entry.getJSONArray("polymer_entities")
                            if (polymerEntities.length() > 0) {
                                val firstEntity = polymerEntities.getJSONObject(0)
                                if (firstEntity.has("rcsb_polymer_entity_container_identifiers")) {
                                    val identifiers = firstEntity.getJSONObject("rcsb_polymer_entity_container_identifiers")
                                    if (identifiers.has("uniprot_ids")) {
                                        val uniprotIds = identifiers.getJSONArray("uniprot_ids")
                                        if (uniprotIds.length() > 0) {
                                            val uniprotId = uniprotIds.getString(0)
                                            android.util.Log.d("DiseaseAPI", "✅ Found UniProt ID via GraphQL: $uniprotId")
                                            return uniprotId
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DiseaseAPI", "⚠️ GraphQL method failed: ${e.message}")
        }
        
        // Fallback: Hardcoded mapping for common proteins
        val fallbackMapping = mapOf(
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
        
        val upperPdbId = pdbId.uppercase()
        if (fallbackMapping.containsKey(upperPdbId)) {
            val uniprotId = fallbackMapping[upperPdbId]!!
            android.util.Log.d("DiseaseAPI", "✅ Found UniProt ID via fallback: $uniprotId")
            return uniprotId
        }
        
        throw Exception("Could not resolve UniProt ID for PDB: $pdbId")
    }
    
    /**
     * 아이폰 DiseaseAssociationService.fetchDiseaseAssociations()와 동일
     * UniProt ID로 질병 정보 조회
     */
    private suspend fun fetchDiseaseFromUniProt(uniprotId: String): List<DiseaseAssociation> {
        android.util.Log.d("DiseaseAPI", "🔍 Fetching disease data from UniProt: $uniprotId")
        
        val url = "https://rest.uniprot.org/uniprotkb/$uniprotId"
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build()
        
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            android.util.Log.e("DiseaseAPI", "❌ UniProt API error: ${response.code}")
            throw Exception("UniProt API failed with code: ${response.code}")
        }
        
        val jsonBody = response.body?.string() ?: ""
        android.util.Log.d("DiseaseAPI", "✅ UniProt response received (${jsonBody.length} bytes)")
        
        return parseDiseaseData(jsonBody)
    }
    
    /**
     * 아이폰 DiseaseAssociationService.parseDiseaseAssociations()와 동일
     * UniProt JSON에서 질병 정보 추출
     */
    private fun parseDiseaseData(jsonBody: String): List<DiseaseAssociation> {
        val diseases = mutableListOf<DiseaseAssociation>()
        
        try {
            android.util.Log.d("DiseaseAPI", "🔍 Parsing UniProt response...")
            
            // 응답이 비어있거나 잘못된 형식인지 확인
            if (jsonBody.isEmpty() || jsonBody.startsWith("<?xml") || !jsonBody.trimStart().startsWith("{")) {
                android.util.Log.w("DiseaseAPI", "⚠️ Invalid JSON response format")
                return diseases // 빈 리스트 반환
            }
            
            val jsonObject = JSONObject(jsonBody)
            
            // Check if organism is plant
            if (jsonObject.has("organism")) {
                val organism = jsonObject.getJSONObject("organism")
                val scientificName = organism.optString("scientificName", "").lowercase()
                
                // Plant protein check (아이폰과 동일)
                val plantKeywords = listOf("arabidopsis", "oryza", "triticum", "zea", "glycine", "plant")
                if (plantKeywords.any { scientificName.contains(it) }) {
                    android.util.Log.d("DiseaseAPI", "⚠️ Plant protein detected: $scientificName")
                    throw Exception("This is a plant protein and is not typically associated with human diseases")
                }
            }
            
            // Extract disease associations from comments
            if (jsonObject.has("comments")) {
                val comments = jsonObject.getJSONArray("comments")
                
                for (i in 0 until comments.length()) {
                    val comment = comments.getJSONObject(i)
                    val commentType = comment.optString("commentType", "")
                    
                    // Disease involvement comments (아이폰과 동일)
                    if (commentType == "DISEASE") {
                        val disease = comment.optJSONObject("disease")
                        if (disease != null) {
                            val diseaseId = disease.optString("diseaseId", "UNKNOWN")
                            val diseaseName = disease.optString("diseaseName", "Unknown Disease")
                            
                            // Extract description from texts
                            var description = "Associated with disease"
                            if (comment.has("texts")) {
                                val texts = comment.getJSONArray("texts")
                                if (texts.length() > 0) {
                                    description = texts.getJSONObject(0).optString("value", description)
                                }
                            }
                            
                            // Determine risk level based on keywords (아이폰과 동일)
                            val riskLevel = determineRiskLevel(diseaseName, description)
                            
                            diseases.add(
                                DiseaseAssociation(
                                    id = diseaseId,
                                    name = diseaseName,
                                    description = description.take(150), // Limit description length
                                    riskLevel = riskLevel,
                                    omimId = disease.optString("diseaseAccession", null),
                                    source = "UniProt"
                                )
                            )
                        }
                    }
                }
            }
            
            android.util.Log.d("DiseaseAPI", "✅ Parsed ${diseases.size} disease associations")
        } catch (e: Exception) {
            android.util.Log.e("DiseaseAPI", "❌ Failed to parse disease data: ${e.message}")
            throw e
        }
        
        return diseases
    }
    
    /**
     * 아이폰과 동일: 질병명과 설명으로부터 위험도 추정
     */
    private fun determineRiskLevel(diseaseName: String, description: String): RiskLevel {
        val combinedText = "$diseaseName $description".lowercase()
        
        // High risk keywords
        val highRiskKeywords = listOf(
            "cancer", "carcinoma", "tumor", "malignant", "fatal",
            "alzheimer", "parkinson", "lethal", "severe", "aggressive"
        )
        
        // Medium risk keywords
        val mediumRiskKeywords = listOf(
            "diabetes", "hypertension", "cardiovascular", "stroke",
            "disease", "disorder", "syndrome", "deficiency"
        )
        
        return when {
            highRiskKeywords.any { combinedText.contains(it) } -> RiskLevel.HIGH
            mediumRiskKeywords.any { combinedText.contains(it) } -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    /**
     * Generate disease summary (아이폰과 동일)
     */
    fun createDiseaseSummary(diseases: List<DiseaseAssociation>): DiseaseSummary {
        return DiseaseSummary(
            total = diseases.size,
            highRisk = diseases.count { it.riskLevel == RiskLevel.HIGH },
            mediumRisk = diseases.count { it.riskLevel == RiskLevel.MEDIUM },
            lowRisk = diseases.count { it.riskLevel == RiskLevel.LOW }
        )
    }
}

