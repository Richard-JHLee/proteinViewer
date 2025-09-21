package com.avas.proteinviewer.ui.state

import android.os.Parcelable
import com.avas.proteinviewer.data.model.Vector3
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppState(
    val currentProteinId: String? = null,
    val selectedTab: Int = 0,
    val showBottomSheet: Boolean = true,
    val cameraPosition: Vector3? = null,
    val cameraRotation: Vector3? = null,
    val renderMode: RenderMode = RenderMode.SOLID,
    val colorMode: ColorMode = ColorMode.ELEMENT,
    val showBonds: Boolean = true,
    val isDarkTheme: Boolean = false,
    val highlightedPockets: Set<String> = emptySet(),
    val focusedPocket: String? = null
) : Parcelable

@Parcelize
enum class RenderMode : Parcelable {
    SOLID, WIREFRAME, POINTS
}

@Parcelize
enum class ColorMode : Parcelable {
    ELEMENT, CHAIN, RESIDUE
}
