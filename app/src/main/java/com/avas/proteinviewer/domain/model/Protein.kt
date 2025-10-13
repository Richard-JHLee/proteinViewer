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
    
    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0.0f) this / len else this
    }
    
    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
    
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

