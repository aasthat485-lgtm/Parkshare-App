package com.parkshare.app.feature_booking

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying the confirmed parking booking pass,
 * including booking transaction summary card and rendered 512x512 ZXing QR code image bitmap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingPassScreen(
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val activeBooking by viewModel.activeBooking.collectAsState()
    val qrBitmap by viewModel.qrBitmap.collectAsState()
    val allBookings by viewModel.allBookings.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ConfirmationNumber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Digital Parking Pass",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Razorpay Verified Pass",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val booking = activeBooking

            if (booking == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Parking Pass",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select a spot from Discovery and book to get your pass.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Booking Summary Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BOOKING ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = booking.bookingId,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Payment Verified",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 512x512 ZXing QR Code Pass Bitmap Container
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "ZXing QR Code Pass Bitmap",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Scan at host entry gate for instant check-in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Parking Spot Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = booking.spotTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = booking.spotAddress.ifBlank { "Mumbai, Maharashtra" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Payment Details Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Duration", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${booking.durationHours} Hours",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Column {
                                Text("Rate", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "₹${booking.hourlyRate.toInt()}/hr",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Column {
                                Text("Paid via Razorpay", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "₹${booking.amountPaid.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Button(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Back to Discovery",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Pass Selector if multiple bookings exist
                if (allBookings.size > 1) {
                    Text(
                        text = "Your Recent Parking Passes (${allBookings.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )

                    allBookings.forEach { item ->
                        OutlinedCard(
                            onClick = { viewModel.setActiveBooking(item) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.spotTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "ID: ${item.bookingId} • ₹${item.amountPaid.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.bookingId == booking.bookingId) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (item.bookingId == booking.bookingId) "Viewing" else "Select",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
