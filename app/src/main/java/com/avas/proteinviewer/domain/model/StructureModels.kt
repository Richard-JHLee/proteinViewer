package com.avas.proteinviewer.domain.model

import kotlinx.serialization.Serializable

// Primary Structure Models

@Serializable
data class PrimaryStructureData(
    val chains: List<ChainInfo>,
    val organism: String,
    val gene: String? = null,
    val expressionHost: String,
    val resolution: String,
    val proteinFamily: String,
    val uniprotAccession: String? = null,
    val cathClassification: String? = null
)

@Serializable
data class ChainInfo(
    val chainId: String,
    val residueCount: Int,
    val sequence: String? = null
)

// Secondary Structure Models

@Serializable
data class SecondaryStructureData(
    val type: String, // α-helix, 3₁₀-helix, π-helix, β-strand, turn, bend, coil
    val start: Int,
    val end: Int,
    val chainId: String,
    val confidence: Double = 1.0,
    val color: Long
) {
    val length: Int
        get() = end - start + 1
    
    val displayName: String
        get() = when {
            type.contains("α-helix", ignoreCase = true) || type.contains("alpha-helix", ignoreCase = true) -> "Alpha Helix"
            type.contains("3₁₀-helix", ignoreCase = true) || type.contains("3_10-helix", ignoreCase = true) -> "3₁₀-Helix"
            type.contains("π-helix", ignoreCase = true) || type.contains("pi-helix", ignoreCase = true) -> "π-Helix"
            type.contains("β-strand", ignoreCase = true) || type.contains("beta-strand", ignoreCase = true) || type.contains("strand", ignoreCase = true) || type.contains("sheet", ignoreCase = true) -> "Beta Sheet"
            type.contains("turn", ignoreCase = true) -> "Turn"
            type.contains("bend", ignoreCase = true) -> "Bend"
            else -> "Coil"
        }
}

// Tertiary Structure Models

@Serializable
data class TertiaryStructureData(
    val domains: List<DomainInfo>,
    val activeSites: List<ActiveSite>,
    val bindingSites: List<BindingSite>,
    val structuralMotifs: List<StructuralMotif> = emptyList(),
    val ligandInteractions: List<LigandInteraction> = emptyList()
)

@Serializable
data class DomainInfo(
    val name: String,
    val start: Int,
    val end: Int,
    val description: String
)

@Serializable
data class ActiveSite(
    val name: String,
    val start: Int,
    val end: Int,
    val residueCount: Int,
    val description: String
)

@Serializable
data class BindingSite(
    val name: String,
    val ligandId: String,
    val residues: List<String>,
    val description: String
)

@Serializable
data class StructuralMotif(
    val name: String,
    val type: String,
    val description: String,
    val residues: List<Int>
)

@Serializable
data class LigandInteraction(
    val ligandName: String,
    val chemicalFormula: String?,
    val bindingPocket: List<Int>,
    val interactionType: String,
    val coordinates: Triple<Double, Double, Double>?
)

// Quaternary Structure Models

@Serializable
data class QuaternaryStructureData(
    val subunits: List<SubunitInfo>,
    val assembly: AssemblyInfo,
    val interactions: List<SubunitInteraction> = emptyList()
)

@Serializable
data class SubunitInfo(
    val id: String,
    val residueCount: Int,
    val description: String
)

@Serializable
data class AssemblyInfo(
    val type: String,
    val symmetry: String,
    val oligomericState: String,
    val totalChains: Int,
    val oligomericCount: Int = totalChains,
    val polymerComposition: String = "Unknown",
    val totalMass: Double = 0.0,
    val atomCount: Int = 0,
    val methodDetails: String? = null,
    val isCandidateAssembly: Boolean? = null,
    val symmetryDetails: List<SymmetryDetail> = emptyList(),
    val biologicalRelevance: String? = null
)

@Serializable
data class SymmetryDetail(
    val symbol: String,
    val kind: String,
    val oligomericState: String,
    val stoichiometry: List<String>,
    val avgRmsd: Double?
)

@Serializable
data class SubunitInteraction(
    val subunit1: String,
    val subunit2: String,
    val contactCount: Int,
    val description: String
)

