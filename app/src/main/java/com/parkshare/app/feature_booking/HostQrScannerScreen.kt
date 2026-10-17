package com.parkshare.app.feature_booking

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * CameraX Host QR Scanner Screen.
 * Hosts an AndroidView-embedded PreviewView with an integrated ZXing ImageAnalysis
 * Barcode Analyzer to scan driver passes and confirm check-ins in real time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostQrScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scannedBooking by viewModel.scannedPassResult.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }
    var isScanPaused by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Host Pass Scanner",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            camera?.cameraControl?.enableTorch(isTorchOn)
                        }
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight Toggle",
                            tint = if (isTorchOn) Color(0xFFFFD600) else Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            isBackCamera = !isBackCamera
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                // CameraX PreviewView with Barcode Analyzer
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, QrBarcodeAnalyzer { rawText ->
                                        if (!isScanPaused) {
                                            isScanPaused = true
                                            viewModel.verifyScannedPass(rawText)
                                        }
                                    })
                                }

                            val cameraSelector = if (isBackCamera) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            }

                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("HostQrScanner", "Camera binding failed: ${e.message}", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Viewfinder Target Reticle Overlay
                ViewfinderOverlay(modifier = Modifier.fillMaxSize())

                // Instructions and Quick Simulation Button
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Align Driver QR Pass within the frame",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test/Simulation trigger for emulators or testing without physical camera pass
                    Button(
                        onClick = {
                            val demoPassId = viewModel.activeBooking.value?.qrPayload
                                ?: "PS-101-DEMO"
                            viewModel.verifyScannedPass(demoPassId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Demo Pass Scan")
                    }
                }
            } else {
                // Permission Denied Request View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera access is needed to scan driver QR passes at the parking entrance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Access")
                    }
                }
            }

            // Scanned Pass Verification Modal Dialog
            scannedBooking?.let { booking ->
                AlertDialog(
                    onDismissRequest = {
                        viewModel.clearScannedResult()
                        isScanPaused = false
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Pass Verified Successfully!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Booking ID: ${booking.bookingId}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Spot: ${booking.spotTitle}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Driver: ${booking.driverEmail.ifBlank { "Verified Driver" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Amount Paid: ₹${booking.amountPaid.toInt()} (Razorpay Verified)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "STATUS: CHECKED-IN",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearScannedResult()
                                isScanPaused = false
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Confirm Entry & Next")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Camera viewfinder reticle rendering corner guides and dark surrounding vignette.
 */
@Composable
fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val boxSize = size.width * 0.72f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f - 40f
        val right = left + boxSize
        val bottom = top + boxSize

        // Corner bracket lines
        val cornerLength = 36f
        val strokeWidth = 5f
        val reticleColor = Color(0xFF00E676)

        // Top-Left
        drawLine(reticleColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(reticleColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

        // Top-Right
        drawLine(reticleColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
        drawLine(reticleColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)

        // Bottom-Left
        drawLine(reticleColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
        drawLine(reticleColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)

        // Bottom-Right
        drawLine(reticleColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
        drawLine(reticleColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)
    }
}

/**
 * ZXing Barcode Analyzer integrating with CameraX ImageAnalysis stream.
 */
class QrBarcodeAnalyzer(
    private val onQrScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val buffer: ByteBuffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val width = imageProxy.width
            val height = imageProxy.height

            val source = PlanarYUVLuminanceSource(
                bytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)

            result?.text?.let { text ->
                onQrScanned(text)
            }
        } catch (_: Exception) {
            // Frame did not contain a valid QR barcode, continue listening
        } finally {
            imageProxy.close()
        }
    }
}
