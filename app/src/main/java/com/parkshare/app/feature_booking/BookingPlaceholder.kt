package com.parkshare.app.feature_booking

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Module 4: Booking & QR System Entry Point.
 */
@Composable
fun BookingPlaceholder(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    BookingPassScreen(
        modifier = modifier,
        onNavigateBack = onNavigateBack
    )
}
