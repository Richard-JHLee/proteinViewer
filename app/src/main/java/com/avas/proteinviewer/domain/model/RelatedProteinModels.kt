package com.avas.proteinviewer.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RelatedProtein(
    val id: String,
    val name: String,
    val category: ProteinCategory,
    val description: String,
    val chainCount: Int,
    val atomCount: Int,
    val resolution: Double? = null,
    val relationship: String, // "Structural homolog", "Protein family", etc.
    val similarity: Double // 0.0 to 1.0
)

@Serializable
data class ExperimentalDetails(
    val experimentalMethod: String? = null,
    val resolution: Double? = null,
    val organism: String? = null,
    val expression: String? = null,
    val journal: String? = null,
    val depositionDate: String? = null,
    val releaseDate: String? = null
)

