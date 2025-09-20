package com.avas.proteinviewer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Bond(
    val atomA: Int,
    val atomB: Int,
    val order: BondOrder,
    val distance: Float
) : Parcelable

enum class BondOrder(val value: Int) {
    SINGLE(1),
    DOUBLE(2),
    TRIPLE(3),
    AROMATIC(4)
}
