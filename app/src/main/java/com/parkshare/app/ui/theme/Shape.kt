package com.parkshare.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shapes used throughout the app. All cards and surfaces use a 16dp rounded corner to match the
 * Material 3 design guidelines for a modern, urban look.
 */
val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(16.dp), // primary rounded cards
    large = RoundedCornerShape(0.dp)
)
