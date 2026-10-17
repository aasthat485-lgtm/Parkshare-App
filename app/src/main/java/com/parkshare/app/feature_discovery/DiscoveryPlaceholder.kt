package com.parkshare.app.feature_discovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Module 2: Discovery & Search System Entry Point.
 */
@Composable
fun DiscoveryPlaceholder(
    modifier: Modifier = Modifier,
    onSpotSelected: (ParkingSpot) -> Unit = {},
    onBookSpot: (ParkingSpot) -> Unit = {},
    onBackToAuth: () -> Unit = {}
) {
    DiscoveryScreen(
        modifier = modifier,
        onSpotSelected = onSpotSelected,
        onBookSpot = onBookSpot,
        onBackToAuth = onBackToAuth
    )
}
