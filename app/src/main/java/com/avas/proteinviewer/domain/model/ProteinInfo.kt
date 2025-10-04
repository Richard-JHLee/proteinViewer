package com.avas.proteinviewer.domain.model

data class ProteinInfo(
    val id: String,
    val name: String,
    val category: ProteinCategory,
    val description: String,
    val keywords: List<String> = emptyList(),
    val resolution: Float? = null,
    val method: String? = null,
    val organism: String? = null,
    val isFavorite: Boolean = false,
    val experimentalMethod: String? = null,
    val depositionDate: String? = null,
    val molecularWeight: Float? = null
) {
    companion object {
        fun createSample(
            id: String,
            name: String,
            category: ProteinCategory,
            description: String,
            keywords: List<String> = emptyList()
        ): ProteinInfo {
            return ProteinInfo(
                id = id,
                name = name,
                category = category,
                description = description,
                keywords = keywords
            )
        }
    }
}
