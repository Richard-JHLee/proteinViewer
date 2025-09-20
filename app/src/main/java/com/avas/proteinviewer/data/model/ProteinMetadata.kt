package com.avas.proteinviewer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProteinMetadata(
    val pdbId: String,
    val title: String,
    val sequence: String,
    val sequenceLength: Int,
    val polymerType: String?,
    val description: String?,
    val formulaWeight: Double?,
    val sourceOrganism: String?,
    val annotations: List<ProteinAnnotation>
) : Parcelable

@Parcelize
data class ProteinAnnotation(
    val id: String?,
    val name: String?,
    val type: String?
) : Parcelable
