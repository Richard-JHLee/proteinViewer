package com.avas.proteinviewer.domain.model

import kotlinx.serialization.Serializable

// Disease Association Models

@Serializable
data class DiseaseAssociation(
    val id: String,
    val name: String,
    val description: String,
    val riskLevel: RiskLevel,
    val omimId: String? = null,
    val source: String = "UniProt"
)

enum class RiskLevel(val displayName: String, val color: Long) {
    HIGH("High Risk", 0xFFFF3B30),      // Red
    MEDIUM("Medium Risk", 0xFFFF9500),   // Orange
    LOW("Low Risk", 0xFFFFCC00)          // Yellow
}

@Serializable
data class DiseaseSummary(
    val total: Int,
    val highRisk: Int,
    val mediumRisk: Int,
    val lowRisk: Int
)

// Research Status Models

@Serializable
data class ResearchStatus(
    val proteinId: String,
    val activeStudies: Int,
    val clinicalTrials: Int,
    val publications: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class ResearchSummary(
    val totalStudies: Int,
    val totalTrials: Int,
    val totalPublications: Int,
    val lastUpdated: String
)

// UniProt API Response Models

@Serializable
data class UniProtEntry(
    val primaryAccession: String? = null,
    val comments: List<UniProtComment>? = null,
    val organism: UniProtOrganism? = null,
    val proteinDescription: ProteinDescription? = null
)

@Serializable
data class UniProtComment(
    val commentType: String? = null,
    val disease: UniProtDisease? = null,
    val texts: List<UniProtText>? = null
)

@Serializable
data class UniProtDisease(
    val diseaseId: String? = null,
    val diseaseName: String? = null,
    val description: String? = null,
    val diseaseAccession: String? = null
)

@Serializable
data class UniProtText(
    val value: String? = null
)

@Serializable
data class UniProtOrganism(
    val scientificName: String? = null,
    val commonName: String? = null
)

@Serializable
data class ProteinDescription(
    val recommendedName: RecommendedName? = null
)

@Serializable
data class RecommendedName(
    val fullName: FullName? = null
)

@Serializable
data class FullName(
    val value: String? = null
)

// PubMed API Response Models

@Serializable
data class PubMedESearchResult(
    val esearchresult: ESearchResult? = null
)

@Serializable
data class ESearchResult(
    val count: String? = null,
    val retmax: String? = null,
    val retstart: String? = null,
    val idlist: List<String>? = null
)

// ClinicalTrials.gov API Response Models

@Serializable
data class ClinicalTrialsResult(
    val StudyFieldsResponse: StudyFieldsResponse? = null
)

@Serializable
data class StudyFieldsResponse(
    val NStudiesFound: Int? = null,
    val NStudiesReturned: Int? = null,
    val StudyFields: List<StudyField>? = null
)

@Serializable
data class StudyField(
    val NCTId: List<String>? = null,
    val BriefTitle: List<String>? = null,
    val OverallStatus: List<String>? = null
)

