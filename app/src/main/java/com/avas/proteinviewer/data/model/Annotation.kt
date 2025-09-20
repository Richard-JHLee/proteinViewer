package com.avas.proteinviewer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Annotation(
    val type: AnnotationType,
    val value: String,
    val description: String
) : Parcelable

enum class AnnotationType {
    RESOLUTION,
    MOLECULAR_WEIGHT,
    EXPERIMENTAL_METHOD,
    ORGANISM,
    FUNCTION,
    DEPOSITION_DATE,
    SPACE_GROUP;
    
    val displayName: String
        get() = when (this) {
            RESOLUTION -> "Resolution"
            MOLECULAR_WEIGHT -> "Molecular Weight"
            EXPERIMENTAL_METHOD -> "Experimental Method"
            ORGANISM -> "Organism"
            FUNCTION -> "Function"
            DEPOSITION_DATE -> "Deposition Date"
            SPACE_GROUP -> "Space Group"
        }
}
