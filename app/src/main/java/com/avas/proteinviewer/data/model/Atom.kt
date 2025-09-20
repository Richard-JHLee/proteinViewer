package com.avas.proteinviewer.data.model

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
    
    // 색상 결정을 위한 computed property
    val atomicColor: Color3
        get() = when (element.uppercase()) {
            "C" -> Color3(0.2f, 0.2f, 0.2f)   // 진한 회색
            "N" -> Color3(0.2f, 0.2f, 1.0f)   // 파란색
            "O" -> Color3(1.0f, 0.2f, 0.2f)   // 빨간색
            "S" -> Color3(1.0f, 1.0f, 0.2f)   // 노란색
            "P" -> Color3(1.0f, 0.5f, 0.0f)   // 주황색
            "H" -> Color3(1.0f, 1.0f, 1.0f)   // 흰색
            else -> Color3(0.8f, 0.0f, 0.8f)  // 보라색
        }
}

@Parcelize
data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
) : Parcelable

@Parcelize
data class Color3(
    val r: Float,
    val g: Float,
    val b: Float
) : Parcelable

enum class SecondaryStructure {
    HELIX,
    SHEET,
    COIL,
    UNKNOWN;
    
    val displayName: String
        get() = when (this) {
            HELIX -> "α-Helix"
            SHEET -> "β-Sheet"
            COIL -> "Coil"
            UNKNOWN -> "Unknown"
        }
}

// Filament용 간단한 데이터 모델들
data class FilamentAtom(
    val id: Int,
    val chain: String,
    val resName: String,
    val x: Float, 
    val y: Float, 
    val z: Float,
    val element: String
)

data class FilamentBond(
    val a: Int, 
    val b: Int, 
    val order: Int = 1
)

data class FilamentSecondary(
    val start: Int, 
    val end: Int, 
    val type: String // helix/sheet/coil
)

data class FilamentLigand(
    val name: String, 
    val atomIds: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FilamentLigand
        if (name != other.name) return false
        if (!atomIds.contentEquals(other.atomIds)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + atomIds.contentHashCode()
        return result
    }
}

data class FilamentStructure(
    val atoms: List<FilamentAtom>,
    val bonds: List<FilamentBond>,
    val chains: Map<String, IntArray>,
    val secondary: List<FilamentSecondary> = emptyList(),
    val ligands: List<FilamentLigand> = emptyList()
)
