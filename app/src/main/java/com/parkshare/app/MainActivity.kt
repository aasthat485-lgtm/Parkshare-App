package com.parkshare.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.parkshare.app.feature_auth.AuthScreen
import com.parkshare.app.feature_auth.AuthViewModel
import com.parkshare.app.feature_auth.UserRole
import com.parkshare.app.feature_booking.BookingPassScreen
import com.parkshare.app.feature_booking.BookingViewModel
import com.parkshare.app.feature_booking.HostQrScannerScreen
import com.parkshare.app.feature_discovery.DiscoveryScreen
import com.parkshare.app.feature_discovery.DiscoveryViewModel
import com.parkshare.app.feature_discovery.ParkingSpot
import com.parkshare.app.feature_host.HostScreen
import com.parkshare.app.feature_host.HostViewModel
import com.parkshare.app.ui.theme.ParkshareTheme
import com.razorpay.PaymentResultListener

/**
 * Top-level destinations for the ParkShare application.
 */
sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Auth : Screen("auth", "Auth", Icons.Default.Explore)
    object Discovery : Screen("discovery", "Explore", Icons.Default.Explore)
    object Host : Screen("host", "Host Space", Icons.Default.AddBusiness)
    object Pass : Screen("pass", "My Passes", Icons.Default.ConfirmationNumber)
    object Scanner : Screen("scanner", "Scanner", Icons.Default.QrCodeScanner)
}

/**
 * Main Activity and central application entry point.
 * Integrates Razorpay checkout callbacks, Jetpack Navigation NavHost,
 * and Material 3 Bottom NavigationBar.
 */
class MainActivity : ComponentActivity(), PaymentResultListener {

    private val authViewModel: AuthViewModel by viewModels()
    private val discoveryViewModel: DiscoveryViewModel by viewModels()
    private val hostViewModel: HostViewModel by viewModels()
    private val bookingViewModel: BookingViewModel by viewModels()

    // Transient reference to parking spot currently undergoing Razorpay checkout
    private var pendingCheckoutSpot: ParkingSpot? = null
    private var pendingHours: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ParkshareTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Bottom bar items
                val bottomBarScreens = listOf(
                    Screen.Discovery,
                    Screen.Host,
                    Screen.Pass,
                    Screen.Scanner
                )

                // Hide bottom navigation bar on Auth screen
                val isBottomBarVisible = currentRoute != null && currentRoute != Screen.Auth.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AnimatedVisibility(
                            visible = isBottomBarVisible,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            NavigationBar {
                                bottomBarScreens.forEach { screen ->
                                    val isSelected = currentRoute == screen.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title
                                            )
                                        },
                                        label = { Text(screen.title) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Auth.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 1. Auth & User Profile Screen
                        composable(Screen.Auth.route) {
                            AuthScreen(
                                viewModel = authViewModel,
                                onAuthSuccess = { session ->
                                    val target = if (session.role == UserRole.HOST) {
                                        Screen.Host.route
                                    } else {
                                        Screen.Discovery.route
                                    }
                                    navController.navigate(target) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Driver Discovery & Search Screen
                        composable(Screen.Discovery.route) {
                            DiscoveryScreen(
                                viewModel = discoveryViewModel,
                                onSpotSelected = { spot ->
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Selected: ${spot.title} (₹${spot.hourlyRate.toInt()}/hr)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onBookSpot = { spot ->
                                    pendingCheckoutSpot = spot
                                    pendingHours = 2

                                    // Launch Razorpay Payment Window (with simulated fallback)
                                    bookingViewModel.startRazorpayCheckout(
                                        activity = this@MainActivity,
                                        spot = spot,
                                        hours = 2
                                    )

                                    // Navigate to Booking Pass screen where generated ZXing QR pass is shown
                                    navController.navigate(Screen.Pass.route)
                                },
                                onBackToAuth = {
                                    navController.navigate(Screen.Auth.route)
                                }
                            )
                        }

                        // 3. Host Space Management Screen
                        composable(Screen.Host.route) {
                            HostScreen(
                                viewModel = hostViewModel,
                                onOpenScanner = {
                                    navController.navigate(Screen.Scanner.route)
                                },
                                onBackToAuth = {
                                    navController.navigate(Screen.Auth.route)
                                }
                            )
                        }

                        // 4. Booking Pass Screen (ZXing 512x512 QR Pass)
                        composable(Screen.Pass.route) {
                            BookingPassScreen(
                                viewModel = bookingViewModel,
                                onNavigateBack = {
                                    navController.navigate(Screen.Discovery.route)
                                }
                            )
                        }

                        // 5. Host QR Scanner Screen (CameraX PreviewView)
                        composable(Screen.Scanner.route) {
                            HostQrScannerScreen(
                                viewModel = bookingViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Razorpay Callback: Payment Success
     */
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        val spot = pendingCheckoutSpot
        if (spot != null) {
            bookingViewModel.onRazorpayPaymentSuccess(
                razorpayPaymentId = razorpayPaymentId ?: "pay_confirmed",
                spot = spot,
                hours = pendingHours
            )
            Toast.makeText(this, "Payment Successful! Pass Generated.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Razorpay Callback: Payment Failure
     */
    override fun onPaymentError(code: Int, response: String?) {
        bookingViewModel.onRazorpayPaymentFailed(code, response)
        Toast.makeText(this, "Payment Failed or Cancelled ($code)", Toast.LENGTH_SHORT).show()
    }
}
