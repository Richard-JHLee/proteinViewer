package com.avas.proteinviewer.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RcsbPolymerEntityDto(
    @Json(name = "entity_poly") val entityPoly: EntityPolyDto? = null,
    @Json(name = "rcsb_polymer_entity") val polymerEntity: PolymerEntityInfoDto? = null,
    @Json(name = "rcsb_entity_source_organism") val sourceOrganisms: List<SourceOrganismDto>? = null,
    @Json(name = "rcsb_polymer_entity_annotation") val annotations: List<PolymerAnnotationDto>? = null,
    @Json(name = "rcsb_polymer_entity_feature") val features: List<PolymerFeatureDto>? = null,
    @Json(name = "rcsb_polymer_entity_align") val alignments: List<PolymerAlignmentDto>? = null,
    @Json(name = "rcsb_cluster_membership") val clusterMembership: List<ClusterMembershipDto>? = null,
    @Json(name = "rcsb_cluster_flexibility") val clusterFlexibility: ClusterFlexibilityDto? = null
)

@JsonClass(generateAdapter = true)
data class EntityPolyDto(
    @Json(name = "pdbx_seq_one_letter_code") val sequence: String? = null,
    @Json(name = "rcsb_sample_sequence_length") val sequenceLength: Int? = null,
    @Json(name = "rcsb_entity_polymer_type") val polymerType: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "pdbx_strand_id") val strandId: String? = null
)

@JsonClass(generateAdapter = true)
data class PolymerEntityInfoDto(
    @Json(name = "pdbx_description") val description: String? = null,
    @Json(name = "formula_weight") val formulaWeight: Double? = null,
    @Json(name = "rcsb_macromolecular_names_combined") val macromolecularNames: List<MacromolecularNameDto>? = null
)

@JsonClass(generateAdapter = true)
data class MacromolecularNameDto(
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class SourceOrganismDto(
    @Json(name = "scientific_name") val scientificName: String? = null,
    @Json(name = "ncbi_common_names") val commonNames: List<String>? = null,
    @Json(name = "ncbi_taxonomy_id") val taxonomyId: Int? = null
)

@JsonClass(generateAdapter = true)
data class PolymerAnnotationDto(
    @Json(name = "annotation_id") val annotationId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class PolymerFeatureDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class PolymerAlignmentDto(
    @Json(name = "reference_database_accession") val accession: String? = null,
    @Json(name = "reference_database_name") val databaseName: String? = null
)

@JsonClass(generateAdapter = true)
data class ClusterMembershipDto(
    @Json(name = "cluster_id") val clusterId: Int? = null,
    @Json(name = "identity") val identity: Int? = null
)

@JsonClass(generateAdapter = true)
data class ClusterFlexibilityDto(
    @Json(name = "avg_rmsd") val averageRmsd: Double? = null,
    @Json(name = "max_rmsd") val maxRmsd: Double? = null,
    @Json(name = "label") val label: String? = null
)
