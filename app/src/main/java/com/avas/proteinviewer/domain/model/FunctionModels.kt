package com.avas.proteinviewer.domain.model

import kotlinx.serialization.Serializable

// Function Details Models

@Serializable
data class FunctionDetails(
    val molecularFunction: String,
    val biologicalProcess: String,
    val cellularComponent: String,
    val goTerms: List<GOTerm> = emptyList(),
    val ecNumbers: List<String> = emptyList(),
    val catalyticActivity: String,
    val resolution: Double? = null,
    val method: String? = null,
    val depositionDate: String? = null,
    val releaseDate: String? = null,
    val numberOfChains: Int? = null,
    val numberOfResidues: Int? = null
)

@Serializable
data class GOTerm(
    val id: String,
    val name: String,
    val category: String // MF, BP, CC
) {
    val categoryColor: Long
        get() = when (category) {
            "MF" -> 0xFF007AFF  // Blue - Molecular Function
            "BP" -> 0xFF34C759  // Green - Biological Process
            "CC" -> 0xFFFF9500  // Orange - Cellular Component
            else -> 0xFF8E8E93  // Gray
        }
    
    val categoryDisplayName: String
        get() = when (category) {
            "MF" -> "Molecular Function"
            "BP" -> "Biological Process"
            "CC" -> "Cellular Component"
            else -> category
        }
}

// UniProt API Response Models for Function

@Serializable
data class UniProtFunctionResponse(
    val comments: List<UniProtFunctionComment>? = null,
    val features: List<UniProtFunctionFeature>? = null,
    val proteinDescription: UniProtFunctionProteinDescription? = null
)

@Serializable
data class UniProtFunctionComment(
    val commentType: String? = null,
    val texts: List<UniProtFunctionText>? = null
)

@Serializable
data class UniProtFunctionText(
    val value: String? = null
)

@Serializable
data class UniProtFunctionFeature(
    val type: String? = null,
    val properties: UniProtFunctionFeatureProperties? = null
)

@Serializable
data class UniProtFunctionFeatureProperties(
    val goId: String? = null,
    val goName: String? = null,
    val goCategory: String? = null
)

@Serializable
data class UniProtFunctionProteinDescription(
    val recommendedName: UniProtFunctionRecommendedName? = null
)

@Serializable
data class UniProtFunctionRecommendedName(
    val ecNumbers: List<UniProtFunctionECNumber>? = null
)

@Serializable
data class UniProtFunctionECNumber(
    val value: String? = null
)

// RCSB PDB API Response Models

@Serializable
data class EntryDetailsResponse(
    val rcsb_entry_container_identifiers: RCSBEntryContainerIdentifiers? = null,
    val refine: List<RefineDetails>? = null,
    val exptl: List<ExptlDetails>? = null
)

@Serializable
data class RCSBEntryContainerIdentifiers(
    val polymer_entity_ids: List<String>? = null
)

@Serializable
data class RefineDetails(
    val ls_d_res_high: Double? = null
)

@Serializable
data class ExptlDetails(
    val method: String? = null
)

@Serializable
data class PolymerEntityDetailsResponse(
    val entity_poly: EntityPoly? = null,
    val rcsb_polymer_entity: RCSBPolymerEntity? = null
)

@Serializable
data class EntityPoly(
    val type: String? = null
)

@Serializable
data class RCSBPolymerEntity(
    val rcsb_polymer_entity_container_identifiers: RCSBPolymerEntityContainerIdentifiers? = null
)

@Serializable
data class RCSBPolymerEntityContainerIdentifiers(
    val uniprot_accession: List<String>? = null
)

