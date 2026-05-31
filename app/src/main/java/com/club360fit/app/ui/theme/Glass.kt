package com.club360fit.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Cream "glass" palette and helpers that mirror the iOS `Club360Theme` look.
 * Scoped to the coach Hub so the rest of the Android app keeps its existing colors.
 */
object Club360Glass {
    // Canonical brand colors (matching iOS Theme.swift)
    val burgundy = Color(0xFF800020)
    val burgundyLight = Color(0xFFA6415C) // iOS peachDeep
    val burgundyDeep = Color(0xFF5C0017)
    val cream = Color(0xFFF9F9DC)
    val creamWarm = Color(0xFFF0EBDB) // iOS mintDeep
    val taupe = Color(0xFF8A7E72) // iOS teal

    // Typography / surfaces
    val cardTitle = Color(0xFF1F1A17)
    val captionOnGlass = Color(0xFF4D423B)

    // iOS stat-tile accent aliases
    val tealDark = burgundy
    val teal = taupe
    val mintDeep = creamWarm
    val peachDeep = burgundyLight

    // 3-stop cream background gradient (top-leading -> bottom-trailing on iOS)
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            cream,
            Color(0xFFF5F2E0),
            Color(0xFFEDE8DB)
        )
    )
}

/** Full-bleed cream gradient background, mirrors iOS `Club360ScreenBackground`. */
fun Modifier.club360ScreenBackground(): Modifier =
    this.background(brush = Club360Glass.backgroundGradient)

/**
 * Cream "glass" card surface mirroring iOS `club360Glass(cornerRadius:)`.
 *
 * Compose cannot cheaply reproduce iOS `.thinMaterial` blur, so the frosted look is
 * approximated with a high-opacity cream fill plus a subtle light/dark border and shadow.
 */
fun Modifier.club360Glass(cornerRadius: Int = 26): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    return this
        .shadow(elevation = 10.dp, shape = shape, clip = false)
        .background(color = Club360Glass.cream.copy(alpha = 0.97f), shape = shape)
        .border(
            width = 1.25.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.Black.copy(alpha = 0.14f)
                )
            ),
            shape = shape
        )
}
