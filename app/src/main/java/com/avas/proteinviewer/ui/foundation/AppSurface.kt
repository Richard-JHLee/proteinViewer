// AppSurface.kt
package com.avas.proteinviewer.ui.foundation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppSurface(
    style: SurfaceStyle,
    modifier: Modifier = Modifier,
    clickable: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = if (style.border != null) {
        modifier.border(style.border, style.shape)
    } else modifier

    val finalModifier = if (clickable != null) {
        base.clickable(onClick = clickable)
    } else base

    Surface(
        color = style.container,
        contentColor = style.onContainer,
        shape = style.shape,
        tonalElevation = style.tonalElevation,
        shadowElevation = style.shadowElevation,
        modifier = finalModifier
    ) {
        Box(Modifier.padding(style.contentPadding)) {
            content()
        }
    }
}

