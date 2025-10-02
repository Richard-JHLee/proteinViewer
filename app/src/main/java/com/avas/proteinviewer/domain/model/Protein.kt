package com.avas.proteinviewer.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Atom(
    val id: Int,
    val element: String,
    val name: String,
    val chain: String,
    val residueName: String,
    val residueNumber: Int,
    val position: Vector3,
    val secondaryStructure: SecondaryStructure,
    val isBackbone: Boolean,
    val isLigand: Boolean,
    val isPocket: Boolean,
    val occupancy: Float,
    val temperatureFactor: Float
) : Parcelable {
    
    val atomicColor: Triple<Float, Float, Float>
        get() = when (element.uppercase()) {
            "C" -> Triple(0.2f, 0.2f, 0.2f)     // 진한 회색
            "N" -> Triple(0.2f, 0.2f, 1.0f)     // 파란색
            "O" -> Triple(1.0f, 0.2f, 0.2f)     // 빨간색
            "S" -> Triple(1.0f, 1.0f, 0.2f)     // 노란색
            "P" -> Triple(1.0f, 0.5f, 0.0f)     // 주황색
            "H" -> Triple(1.0f, 1.0f, 1.0f)     // 흰색
            else -> Triple(0.8f, 0.0f, 0.8f)    // 보라색
        }
}

@Parcelize
data class Bond(
    val atomA: Int,
    val atomB: Int,
    val order: BondOrder,
    val distance: Float
) : Parcelable

enum class BondOrder {
    SINGLE, DOUBLE, TRIPLE, AROMATIC
}

@Parcelize
data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
) : Parcelable {
    fun length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
    
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
}

enum class SecondaryStructure(val displayName: String) {
    HELIX("α-Helix"),
    SHEET("β-Sheet"),
    COIL("Coil"),
    UNKNOWN("Unknown");
    
    companion object {
        fun fromCode(code: String): SecondaryStructure = when (code.uppercase()) {
            "H" -> HELIX
            "S", "E" -> SHEET
            "C", "T" -> COIL
            else -> UNKNOWN
        }
    }
}

@Parcelize
data class Annotation(
    val type: AnnotationType,
    val value: String,
    val description: String
) : Parcelable

enum class AnnotationType(val displayName: String) {
    RESOLUTION("Resolution"),
    MOLECULAR_WEIGHT("Molecular Weight"),
    EXPERIMENTAL_METHOD("Experimental Method"),
    ORGANISM("Organism"),
    FUNCTION("Function"),
    DEPOSITION_DATE("Deposition Date"),
    SPACE_GROUP("Space Group")
}

@Parcelize
data class PDBStructure(
    val atoms: List<Atom>,
    val bonds: List<Bond>,
    val annotations: List<Annotation>,
    val boundingBoxMin: Vector3,
    val boundingBoxMax: Vector3,
    val centerOfMass: Vector3
) : Parcelable {
    
    val atomCount: Int get() = atoms.size
    val residueCount: Int get() = atoms.map { "${it.chain}_${it.residueNumber}" }.toSet().size
    val chainCount: Int get() = atoms.map { it.chain }.toSet().size
    val chains: Set<String> get() = atoms.map { it.chain }.toSet()
}

@Parcelize
data class ProteinInfo(
    val id: String,
    val name: String,
    val description: String,
    val organism: String? = null,
    val resolution: Double? = null,
    val experimentalMethod: String? = null,
    val depositionDate: String? = null,
    val molecularWeight: Double? = null,
    val classification: String? = null
) : Parcelable

data class ProteinDetail(
    val id: String,
    val name: String,
    val description: String,
    val organism: String,
    val molecularWeight: Double?,
    val resolution: Double?,
    val experimentalMethod: String?,
    val depositionDate: String?
)

enum class RenderStyle(val displayName: String, val icon: String) {
    RIBBON("Ribbon", "waveform_path_ecg"),
    SPHERES("Spheres", "circle"),
    STICKS("Sticks", "line_horizontal"),
    CARTOON("Cartoon", "waveform_path"),
    SURFACE("Surface", "globe")
}

enum class ColorMode(val displayName: String, val icon: String) {
    ELEMENT("Element", "atom"),
    CHAIN("Chain", "link"),
    UNIFORM("Uniform", "palette"),
    SECONDARY_STRUCTURE("Secondary Structure", "waveform")
}

enum class ProteinCategory(
    val displayName: String,
    val description: String,
    val icon: String,
    val color: Long
) {
    ENZYMES("Enzymes", "Catalytic proteins that speed up biochemical reactions", "enzyme", 0xFF4CAF50),
    STRUCTURAL("Structural", "Proteins that provide structural support", "structure", 0xFF2196F3),
    TRANSPORT("Transport", "Proteins that move molecules across membranes", "transport", 0xFFFF9800),
    STORAGE("Storage", "Proteins that store nutrients and molecules", "storage", 0xFF9C27B0),
    HORMONAL("Hormonal", "Proteins that act as chemical messengers", "hormone", 0xFFE91E63),
    DEFENSE("Defense", "Proteins involved in immune response", "defense", 0xFFF44336),
    REGULATORY("Regulatory", "Proteins that control gene expression", "regulatory", 0xFF795548),
    MOTOR("Motor", "Proteins that generate mechanical force", "motor", 0xFF607D8B),
    RECEPTOR("Receptor", "Proteins that receive chemical signals", "receptor", 0xFF3F51B5),
    SIGNALING("Signaling", "Proteins involved in cell communication", "signaling", 0xFF00BCD4),
    METABOLIC("Metabolic", "Proteins involved in metabolic processes", "metabolic", 0xFF8BC34A),
    BINDING("Binding", "Proteins that bind to specific molecules", "binding", 0xFFFFC107)
}
