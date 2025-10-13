package com.avas.proteinviewer.domain.model

import kotlinx.serialization.Serializable

// Research Detail Models

@Serializable
data class ResearchPublication(
    val id: String,
    val title: String,
    val authors: List<String>,
    val journal: String,
    val year: Int,
    val doi: String? = null,
    val pmid: String? = null,
    val abstract: String? = null,
    val keywords: List<String> = emptyList()
) {
    val displayAuthors: String
        get() = when {
            authors.isEmpty() -> "Unknown Authors"
            authors.size <= 3 -> authors.joinToString(", ")
            else -> "${authors.take(2).joinToString(", ")} et al."
        }
}

@Serializable
data class ClinicalTrial(
    val nctId: String,
    val title: String,
    val status: String,
    val phase: String? = null,
    val condition: String? = null,
    val intervention: String? = null,
    val sponsor: String? = null,
    val startDate: String? = null,
    val completionDate: String? = null,
    val enrollment: Int? = null,
    val description: String? = null
) {
    val statusColor: Long
        get() = when (status.lowercase()) {
            "recruiting" -> 0xFF34C759  // Green
            "active" -> 0xFF007AFF      // Blue
            "completed" -> 0xFF8E8E93   // Gray
            "terminated" -> 0xFFFF3B30  // Red
            "suspended" -> 0xFFFF9500   // Orange
            else -> 0xFF8E8E93          // Gray
        }
}

@Serializable
data class ActiveStudy(
    val id: String,
    val title: String,
    val type: String, // "Publication", "Clinical Trial", "Research"
    val status: String,
    val institution: String? = null,
    val principalInvestigator: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val description: String? = null,
    val relatedPublication: ResearchPublication? = null,
    val relatedTrial: ClinicalTrial? = null
) {
    val typeColor: Long
        get() = when (type.lowercase()) {
            "publication" -> 0xFF9C27B0  // Purple
            "clinical trial" -> 0xFF007AFF  // Blue
            "research" -> 0xFF34C759  // Green
            else -> 0xFF8E8E93  // Gray
        }
}

enum class ResearchDetailType {
    PUBLICATIONS,
    CLINICAL_TRIALS,
    ACTIVE_STUDIES
}
