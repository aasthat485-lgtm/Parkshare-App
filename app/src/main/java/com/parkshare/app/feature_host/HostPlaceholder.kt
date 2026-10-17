package com.parkshare.app.feature_host

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Module 3: Host Space Management System Entry Point.
 */
@Composable
fun HostPlaceholder(
    modifier: Modifier = Modifier,
    onOpenScanner: () -> Unit = {},
    onBackToAuth: () -> Unit = {}
) {
    HostScreen(
        modifier = modifier,
        onOpenScanner = onOpenScanner,
        onBackToAuth = onBackToAuth
    )
}
