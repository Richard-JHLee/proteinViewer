package com.avas.proteinviewer.data.api

import com.avas.proteinviewer.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResearchDetailAPIService @Inject constructor() {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // MARK: - Research Detail API
    
    /**
     * 아이폰과 동일: Publications 상세 정보 조회
     */
    suspend fun fetchPublications(pdbId: String): List<ResearchPublication> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ResearchDetailAPI", "🔍 Fetching publications for: $pdbId")
                
                val publications = mutableListOf<ResearchPublication>()
                
                // 여러 검색어로 시도
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
                        
                        // Rate limit handling
                        if (response.code == 429) {
                            android.util.Log.d("ResearchDetailAPI", "⚠️ Rate limit hit, waiting 3 seconds...")
                            delay(3000)
                            continue
                        }
                        
                        if (response.isSuccessful) {
                            val jsonBody = response.body?.string() ?: ""
                            val jsonObject = JSONObject(jsonBody)
                            
                            if (jsonObject.has("esearchresult")) {
                                val esearchResult = jsonObject.getJSONObject("esearchresult")
                                val idList = esearchResult.optJSONArray("idlist")
                                
                                if (idList != null && idList.length() > 0) {
                                    // PMIDs로 상세 정보 조회
                                    val pmids = mutableListOf<String>()
                                    for (i in 0 until idList.length()) {
                                        pmids.add(idList.getString(i))
                                    }
                                    
                                    val detailedPublications = fetchPublicationDetails(pmids)
                                    publications.addAll(detailedPublications)
                                    
                                    if (publications.isNotEmpty()) {
                                        android.util.Log.d("ResearchDetailAPI", "📚 Found ${publications.size} publications")
                                        break
                                    }
                                }
                            }
                        }
                        
                        // API 호출 간 1초 대기
                        delay(1000)
                        
                    } catch (e: Exception) {
                        android.util.Log.e("ResearchDetailAPI", "❌ PubMed API error for $searchTerm: ${e.message}")
                        continue
                    }
                }
                
                publications.take(20) // 최대 20개로 제한
                
            } catch (e: Exception) {
                android.util.Log.e("ResearchDetailAPI", "❌ Publications API failed: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * PMIDs로 상세 논문 정보 조회
     */
    private suspend fun fetchPublicationDetails(pmids: List<String>): List<ResearchPublication> {
        val publications = mutableListOf<ResearchPublication>()
        
        try {
            val pmidString = pmids.joinToString(",")
            val url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=$pmidString&retmode=xml&rettype=abstract"
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val xmlBody = response.body?.string() ?: ""
                publications.addAll(parsePublicationXML(xmlBody, pmids))
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ResearchDetailAPI", "❌ Publication details fetch failed: ${e.message}")
        }
        
        return publications.take(pmids.size) // 요청한 수만큼만 반환
    }
    
    /**
     * XML 응답에서 논문 정보 파싱
     */
    private fun parsePublicationXML(xmlBody: String, requestedPmids: List<String>): List<ResearchPublication> {
        val publications = mutableListOf<ResearchPublication>()
        
        try {
            // PubmedArticle 단위로 파싱하여 참고문헌의 PMID 제외
            val articlePattern = "<PubmedArticle>(.*?)</PubmedArticle>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val articles = articlePattern.findAll(xmlBody)
            
            for (articleMatch in articles) {
                val articleXml = articleMatch.groupValues[1]
                
                // 각 article에서 정보 추출
                val pmid = "<PMID[^>]*>([^<]+)</PMID>".toRegex().find(articleXml)?.groupValues?.get(1) ?: continue
                
                // 요청한 PMID 목록에 있는 것만 포함
                if (!requestedPmids.contains(pmid)) continue
                
                val title = "<ArticleTitle[^>]*>([^<]+)</ArticleTitle>".toRegex().find(articleXml)?.groupValues?.get(1) ?: "Unknown Title"
                val authors = "<LastName>([^<]+)</LastName>".toRegex().findAll(articleXml).map { it.groupValues[1] }.toList()
                val journal = "<Title>([^<]+)</Title>".toRegex().find(articleXml)?.groupValues?.get(1) ?: "Unknown Journal"
                val year = "<Year>([^<]+)</Year>".toRegex().find(articleXml)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val abstract = "<AbstractText[^>]*>([^<]+)</AbstractText>".toRegex().find(articleXml)?.groupValues?.get(1)
                
                val publication = ResearchPublication(
                    id = pmid,
                    title = title,
                    authors = authors,
                    journal = journal,
                    year = year,
                    pmid = pmid,
                    abstract = abstract
                )
                publications.add(publication)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ResearchDetailAPI", "❌ XML parsing failed: ${e.message}")
        }
        
        return publications
    }
    
    /**
     * 아이폰과 동일: Clinical Trials 상세 정보 조회
     */
    suspend fun fetchClinicalTrials(pdbId: String): List<ClinicalTrial> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ResearchDetailAPI", "🔍 Fetching clinical trials for: $pdbId")
                
                val query = java.net.URLEncoder.encode(pdbId, "UTF-8")
                val url = "https://clinicaltrials.gov/api/v2/studies?query.term=$query&format=json&pageSize=20"
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val jsonBody = response.body?.string() ?: ""
                    val jsonObject = JSONObject(jsonBody)
                    
                    val trials = mutableListOf<ClinicalTrial>()
                    
                    if (jsonObject.has("studies")) {
                        val studies = jsonObject.getJSONArray("studies")
                        
                        for (i in 0 until studies.length()) {
                            val study = studies.getJSONObject(i)
                            val protocolSection = study.optJSONObject("protocolSection")
                            
                            if (protocolSection != null) {
                                val identificationModule = protocolSection.optJSONObject("identificationModule")
                                val statusModule = protocolSection.optJSONObject("statusModule")
                                val designModule = protocolSection.optJSONObject("designModule")
                                val conditionsModule = protocolSection.optJSONObject("conditionsModule")
                                val interventionsModule = protocolSection.optJSONObject("interventionsModule")
                                val sponsorCollaboratorsModule = protocolSection.optJSONObject("sponsorCollaboratorsModule")
                                
                                val trial = ClinicalTrial(
                                    nctId = identificationModule?.optString("nctId") ?: "unknown",
                                    title = identificationModule?.optString("briefTitle") ?: "Unknown Title",
                                    status = statusModule?.optString("overallStatus") ?: "Unknown",
                                    phase = designModule?.optJSONArray("phases")?.optString(0),
                                    condition = conditionsModule?.optJSONArray("conditions")?.optString(0),
                                    intervention = interventionsModule?.optJSONArray("interventions")?.optJSONObject(0)?.optString("name"),
                                    sponsor = sponsorCollaboratorsModule?.optJSONArray("leadSponsor")?.optJSONObject(0)?.optString("name"),
                                    startDate = statusModule?.optString("startDateStruct")?.let { 
                                        JSONObject(it).optString("date") 
                                    },
                                    completionDate = statusModule?.optString("completionDateStruct")?.let { 
                                        JSONObject(it).optString("date") 
                                    },
                                    enrollment = statusModule?.optInt("enrollmentCount"),
                                    description = identificationModule?.optString("briefSummary")
                                )
                                trials.add(trial)
                            }
                        }
                    }
                    
                    android.util.Log.d("ResearchDetailAPI", "🏥 Found ${trials.size} clinical trials")
                    trials
                    
                } else {
                    android.util.Log.e("ResearchDetailAPI", "❌ ClinicalTrials.gov API error: ${response.code}")
                    emptyList()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ResearchDetailAPI", "❌ Clinical Trials API failed: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * 아이폰과 동일: Active Studies 조회 (Publications + Clinical Trials)
     */
    suspend fun fetchActiveStudies(pdbId: String): List<ActiveStudy> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ResearchDetailAPI", "🔍 Fetching active studies for: $pdbId")
                
                val activeStudies = mutableListOf<ActiveStudy>()
                
                // Publications를 Active Studies로 변환
                val publications = fetchPublications(pdbId)
                publications.forEach { publication ->
                    val study = ActiveStudy(
                        id = publication.id,
                        title = publication.title,
                        type = "Publication",
                        status = "Published",
                        institution = publication.journal,
                        startDate = publication.year.toString(),
                        description = publication.abstract,
                        relatedPublication = publication
                    )
                    activeStudies.add(study)
                }
                
                // Clinical Trials를 Active Studies로 변환
                val trials = fetchClinicalTrials(pdbId)
                trials.forEach { trial ->
                    val study = ActiveStudy(
                        id = trial.nctId,
                        title = trial.title,
                        type = "Clinical Trial",
                        status = trial.status,
                        institution = trial.sponsor,
                        startDate = trial.startDate,
                        endDate = trial.completionDate,
                        description = trial.description,
                        relatedTrial = trial
                    )
                    activeStudies.add(study)
                }
                
                android.util.Log.d("ResearchDetailAPI", "🔬 Found ${activeStudies.size} active studies")
                activeStudies
                
            } catch (e: Exception) {
                android.util.Log.e("ResearchDetailAPI", "❌ Active Studies API failed: ${e.message}")
                emptyList()
            }
        }
    }
}
