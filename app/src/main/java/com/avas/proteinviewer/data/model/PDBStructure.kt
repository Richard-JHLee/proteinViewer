package com.avas.proteinviewer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PDBStructure(
    val atoms: List<Atom>,
    val bonds: List<Bond>,
    val annotations: List<Annotation>,
    val boundingBox: BoundingBox,
    val centerOfMass: Vector3
) : Parcelable {
    
    // 통계 정보
    val atomCount: Int get() = atoms.size
    val residueCount: Int get() = atoms.map { "${it.chain}_${it.residueNumber}" }.distinct().size
    val chainCount: Int get() = atoms.map { it.chain }.distinct().size
    
    // 체인과 잔기 정보
    val chains: List<String> get() = atoms.map { it.chain }.distinct().sorted()
    val residues: List<ResidueInfo> get() = atoms
        .groupBy { "${it.chain}_${it.residueNumber}" }
        .map { (key, atoms) ->
            val firstAtom = atoms.first()
            ResidueInfo(
                chain = firstAtom.chain,
                residueNumber = firstAtom.residueNumber,
                residueName = firstAtom.residueName,
                atomCount = atoms.size
            )
        }
        .sortedWith(compareBy({ it.chain }, { it.residueNumber }))
}

@Parcelize
data class ResidueInfo(
    val chain: String,
    val residueNumber: Int,
    val residueName: String,
    val atomCount: Int
) : Parcelable

@Parcelize
data class BoundingBox(
    val min: Vector3,
    val max: Vector3
) : Parcelable
