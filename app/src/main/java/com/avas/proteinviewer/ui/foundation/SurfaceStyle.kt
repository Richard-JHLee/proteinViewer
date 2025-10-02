// SurfaceStyle.kt
package com.avas.proteinviewer.ui.foundation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SurfaceStyle(
    val container: Color,
    val onContainer: Color,
    val outline: Color? = null,
    val border: BorderStroke? = null,
    val shape: Shape = ShapeDefaults.Medium,
    val shadowElevation: Dp = 0.dp,   // 실제 그림자(플랫폼 섀도우)
    val tonalElevation: Dp = 0.dp,    // M3 톤 고도(색 톤 변화)
    val contentPadding: PaddingValues = PaddingValues(16.dp)
)

object AppSurfaces {
    @Composable
    fun Base() = SurfaceStyle(
        container = MaterialTheme.colorScheme.surface,
        onContainer = MaterialTheme.colorScheme.onSurface,
        outline = MaterialTheme.colorScheme.outlineVariant,
        shape = ShapeDefaults.Medium,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    )

    // M3 권장 "surface container*" 계열 (fallback to surface variants)
    @Composable
    fun ContainerLow() = Base().copy(
        container = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    )

    @Composable
    fun Container() = Base().copy(
        container = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    )

    @Composable
    fun ContainerHigh() = Base().copy(
        container = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp
    )

    @Composable
    fun Variant() = Base().copy(
        container = MaterialTheme.colorScheme.surfaceVariant,
        onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    )

    @Composable
    fun Outlined() = Container().copy(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    )

    @Composable
    fun Elevated() = ContainerHigh().copy(
        shadowElevation = 6.dp, // 실제 그림자
        tonalElevation = 3.dp
    )
}

