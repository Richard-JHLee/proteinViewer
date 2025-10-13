package com.avas.proteinviewer.data.api

import com.avas.proteinviewer.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResearchAPIService @Inject constructor() {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // MARK: - Research Status API
    
    /**
     * 아이폰 ResearchStatusService.fetchResearchStatus()와 동일
     * PDB ID로 연구 상태 정보 가져오기
     */
    suspend fun fetchResearchStatus(pdbId: String): ResearchStatus {
        return withContext(Dispatchers.IO) {
            android.util.Log.d("ResearchAPI", "🔍 Fetching research status for: $pdbId")
            
            var publications = 0
            var clinicalTrials = 0
            var activeStudies = 0
            
            // 각 API를 개별적으로 호출 (하나가 실패해도 다른 것들은 계속 진행)
            try {
                publications = fetchPublicationsCount(pdbId)
            } catch (e: Exception) {
                android.util.Log.e("ResearchAPI", "⚠️ Publications API failed: ${e.message}")
                publications = 0 // 하드코딩 제거: 실제 데이터 없을 때는 0
            }
            
            try {
                clinicalTrials = fetchClinicalTrialsCount(pdbId)
            } catch (e: Exception) {
                android.util.Log.e("ResearchAPI", "⚠️ Clinical Trials API failed: ${e.message}")
                clinicalTrials = 0 // 하드코딩 제거: 실제 데이터 없을 때는 0
            }
            
            // Active Studies = Publications + Clinical Trials (아이폰과 동일한 계산)
            activeStudies = publications + clinicalTrials
            
            android.util.Log.d("ResearchAPI", "✅ Research status fetched:")
            android.util.Log.d("ResearchAPI", "   📚 Publications: $publications")
            android.util.Log.d("ResearchAPI", "   🏥 Clinical Trials: $clinicalTrials")
            android.util.Log.d("ResearchAPI", "   🔬 Active Studies: $activeStudies")
            
            ResearchStatus(
                proteinId = pdbId,
                activeStudies = activeStudies,
                clinicalTrials = clinicalTrials,
                publications = publications,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * 아이폰 ResearchStatusService.fetchPublicationsCount()와 동일
     * PubMed API에서 논문 수 조회
     */
    private suspend fun fetchPublicationsCount(pdbId: String): Int {
        android.util.Log.d("ResearchAPI", "🔍 Fetching publications from PubMed for: $pdbId")
        
        // 검색어 생성 (아이폰과 동일한 전략)
        val searchTerms = listOf(
            "$pdbId[All Fields]",
            "\"$pdbId\"[Title/Abstract]",
            "$pdbId AND protein[Title/Abstract]",
            "$pdbId AND structure[Title/Abstract]"
        )
        
        for (searchTerm in searchTerms) {
            try {
                val encodedTerm = java.net.URLEncoder.encode(searchTerm, "UTF-8")
                val url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=$encodedTerm&retmode=json&retmax=20"
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                // Rate limit handling (아이폰과 동일)
                if (response.code == 429) {
                    android.util.Log.d("ResearchAPI", "⚠️ Rate limit hit, waiting 3 seconds...")
                    delay(3000)
                    continue
                }
                
                if (response.isSuccessful) {
                    val jsonBody = response.body?.string() ?: ""
                    val jsonObject = JSONObject(jsonBody)
                    
                    if (jsonObject.has("esearchresult")) {
                        val esearchResult = jsonObject.getJSONObject("esearchresult")
                        
                        // idlist의 실제 개수를 사용 (ResearchDetailAPIService와 동일)
                        val idList = esearchResult.optJSONArray("idlist")
                        val count = idList?.length() ?: 0
                        
                        if (count > 0) {
                            android.util.Log.d("ResearchAPI", "📚 PubMed results: $count publications for: $searchTerm")
                            return count
                        }
                    }
                }
                
                // API 호출 간 1초 대기 (Rate Limit 방지, 아이폰과 동일)
                delay(1000)
                
            } catch (e: Exception) {
                android.util.Log.e("ResearchAPI", "❌ PubMed API error for $searchTerm: ${e.message}")
                continue
            }
        }
        
        android.util.Log.d("ResearchAPI", "📚 No publications found with any search term")
        return 0 // 하드코딩 제거: 실제 데이터 없을 때는 0
    }
    
    /**
     * 아이폰 ResearchStatusService.fetchClinicalTrialsCount()와 동일
     * ClinicalTrials.gov API에서 임상시험 수 조회
     */
    private suspend fun fetchClinicalTrialsCount(pdbId: String): Int {
        android.util.Log.d("ResearchAPI", "🔍 Fetching clinical trials for: $pdbId")
        
        try {
            // ClinicalTrials.gov API v2 (아이폰과 동일)
            val query = java.net.URLEncoder.encode(pdbId, "UTF-8")
            val url = "https://clinicaltrials.gov/api/v2/studies?query.term=$query&format=json&pageSize=1"
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val jsonBody = response.body?.string() ?: ""
                val jsonObject = JSONObject(jsonBody)
                
                // totalCount 추출 (API v2 형식)
                if (jsonObject.has("totalCount")) {
                    val totalCount = jsonObject.getInt("totalCount")
                    android.util.Log.d("ResearchAPI", "🏥 ClinicalTrials.gov results: $totalCount trials")
                    return totalCount
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ResearchAPI", "❌ ClinicalTrials.gov API error: ${e.message}")
        }
        
        android.util.Log.d("ResearchAPI", "🏥 No clinical trials found")
        return 0 // 하드코딩 제거: 실제 데이터 없을 때는 0
    }
    
    /**
     * Generate research summary (아이폰과 동일)
     */
    fun createResearchSummary(researchStatus: ResearchStatus): ResearchSummary {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val lastUpdatedDate = Date(researchStatus.lastUpdated)
        
        return ResearchSummary(
            totalStudies = researchStatus.activeStudies,
            totalTrials = researchStatus.clinicalTrials,
            totalPublications = researchStatus.publications,
            lastUpdated = dateFormat.format(lastUpdatedDate)
        )
    }
}

