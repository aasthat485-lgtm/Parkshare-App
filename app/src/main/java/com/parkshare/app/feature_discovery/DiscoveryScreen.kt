package com.parkshare.app.feature_discovery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// Reference coordinate: Dadar, Mumbai (19.0178° N, 72.8478° E)
val DADAR_MUMBAI = LatLng(19.0178, 72.8478)

/**
 * Driver Discovery & Search Screen.
 * Includes interactive Google Maps Compose view with spot markers,
 * search query bar, EV/Price FilterChips, and bottom spot cards.
 */
@Composable
fun DiscoveryScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoveryViewModel = viewModel(),
    onSpotSelected: (ParkingSpot) -> Unit = {},
    onBookSpot: (ParkingSpot) -> Unit = {},
    onBackToAuth: () -> Unit = {}
) {
    val spots by viewModel.parkingSpots.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onlyEv by viewModel.onlyEv.collectAsState()
    val under50 by viewModel.under50.collectAsState()
    val isMapView by viewModel.isMapView.collectAsState()
    val selectedSpot by viewModel.selectedSpot.collectAsState()

    BackHandler {
        onBackToAuth()
    }

    DiscoveryContent(
        spots = spots,
        searchQuery = searchQuery,
        onlyEv = onlyEv,
        under50 = under50,
        isMapView = isMapView,
        selectedSpot = selectedSpot,
        onSearchChanged = { viewModel.searchByQuery(it) },
        onEvToggled = { viewModel.toggleEvFilter(it) },
        onUnder50Toggled = { viewModel.toggleUnder50Filter(it) },
        onToggleMapView = { viewModel.toggleMapView() },
        onSelectSpot = { viewModel.selectSpot(it) },
        onBookSpot = onBookSpot,
        onSpotSelected = onSpotSelected,
        onBackToAuth = onBackToAuth,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryContent(
    spots: List<ParkingSpot>,
    searchQuery: String,
    onlyEv: Boolean,
    under50: Boolean,
    isMapView: Boolean,
    selectedSpot: ParkingSpot?,
    onSearchChanged: (String) -> Unit,
    onEvToggled: (Boolean) -> Unit,
    onUnder50Toggled: (Boolean) -> Unit,
    onToggleMapView: () -> Unit,
    onSelectSpot: (ParkingSpot?) -> Unit,
    onBookSpot: (ParkingSpot) -> Unit,
    onSpotSelected: (ParkingSpot) -> Unit,
    onBackToAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dadar, Mumbai default camera position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DADAR_MUMBAI, 13f)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Find Parking",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Dadar & Central Mumbai Hub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleMapView) {
                                Icon(
                                    imageVector = if (isMapView) Icons.Default.List else Icons.Default.Map,
                                    contentDescription = "Toggle View Mode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onBackToAuth) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = { Text("Search Dadar, Matunga, BKC, street...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = onlyEv,
                            onClick = { onEvToggled(!onlyEv) },
                            label = { Text("EV Charging ⚡") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (onlyEv) Icons.Default.Check else Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        FilterChip(
                            selected = under50,
                            onClick = { onUnder50Toggled(!under50) },
                            label = { Text("Under ₹50/hr") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (under50) Icons.Default.Check else Icons.Default.CurrencyRupee,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        AssistChip(
                            onClick = onToggleMapView,
                            label = { Text(if (isMapView) "Switch to List" else "Switch to Map") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isMapView) Icons.Default.List else Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isMapView) {
                // Google Maps Compose View
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    ),
                    onMapClick = {
                        onSelectSpot(null)
                    }
                ) {
                    spots.forEach { spot ->
                        Marker(
                            state = MarkerState(position = LatLng(spot.lat, spot.lng)),
                            title = "${spot.title} (₹${spot.hourlyRate.toInt()}/hr)",
                            snippet = if (spot.hasEvCharging) "EV Charging Available ⚡" else "Standard Spot",
                            onClick = {
                                onSelectSpot(spot)
                                true
                            }
                        )
                    }
                }

                // Recenter Fab on Dadar
                FloatingActionButton(
                    onClick = {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(DADAR_MUMBAI, 13.5f)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter on Dadar")
                }

                // Bottom Floating Carousel of Parking Spots
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    if (selectedSpot != null) {
                        // Highlighted Selected Spot Card
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ParkingSpotCard(
                                spot = selectedSpot,
                                isSelected = true,
                                onClick = { onSpotSelected(selectedSpot) },
                                onBookClick = { onBookSpot(selectedSpot) }
                            )
                        }
                    } else if (spots.isNotEmpty()) {
                        // Horizontal preview carousel of spots
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = spots, key = { it.id }) { spot ->
                                Box(modifier = Modifier.width(300.dp)) {
                                    ParkingSpotCard(
                                        spot = spot,
                                        isSelected = false,
                                        onClick = {
                                            onSelectSpot(spot)
                                            cameraPositionState.position =
                                                CameraPosition.fromLatLngZoom(LatLng(spot.lat, spot.lng), 15f)
                                        },
                                        onBookClick = { onBookSpot(spot) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // List View Mode
                if (spots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "No Results",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Parking Spots Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your search query or filter tags.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "${spots.size} parking spots found",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(items = spots, key = { it.id }) { spot ->
                            ParkingSpotCard(
                                spot = spot,
                                isSelected = selectedSpot?.id == spot.id,
                                onClick = { onSpotSelected(spot) },
                                onBookClick = { onBookSpot(spot) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Card component representing an individual parking spot.
 */
@Composable
fun ParkingSpotCard(
    spot: ParkingSpot,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onBookClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = spot.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Availability Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (spot.isAvailable) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = if (spot.isAvailable) "Available" else "Occupied",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = if (spot.isAvailable) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Address Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = spot.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Rate, EV, and Book Now CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hourly Rate Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "₹${spot.hourlyRate.toInt()}/hr",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // EV Badge
                    if (spot.hasEvCharging) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "EV Charging",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "EV ⚡",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onBookClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Book Now",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
