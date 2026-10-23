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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
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
import com.parkshare.app.feature_auth.ProfileScreen
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
import com.razorpay.Checkout
/**
 * Top-level destinations for the ParkShare application.
 */
sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Auth : Screen("auth", "Auth", Icons.Default.Lock)
    object Discovery : Screen("discovery", "Explore", Icons.Default.Explore)
    object Host : Screen("host", "Listed Spaces", Icons.Default.AddBusiness)
    object Pass : Screen("pass", "Active Passes", Icons.Default.ConfirmationNumber)
    object Scanner : Screen("scanner", "Host Scanner", Icons.Default.QrCodeScanner)
    object Profile : Screen("profile", "Profile", Icons.Default.AccountCircle)
}

/**
 * Main Activity and central application entry point.
 * Integrates Razorpay checkout callbacks, Jetpack Navigation NavHost,
 * strict Role-Based Access Control (RBAC) navigation flows for Driver & Host modes,
 * and Material 3 Bottom NavigationBar.
 */
class MainActivity : ComponentActivity(), PaymentResultListener {

    private val authViewModel: AuthViewModel by viewModels()
    private val discoveryViewModel: DiscoveryViewModel by viewModels()
    private val hostViewModel: HostViewModel by viewModels()
    private val bookingViewModel: BookingViewModel by viewModels()

    // Transient references to parking spot currently undergoing Razorpay checkout
    // Reference to NavController for navigation from callbacks
    private var appNavController: androidx.navigation.NavHostController? = null
    private var pendingCheckoutSpot: ParkingSpot? = null
    private var pendingHours: Int = 2
    private var pendingVehicleNo: String = "MH-01-AB-1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialise Razorpay helper (Firebase, key, pre‑load)
        RazorpaySetup(application)

        super.onCreate(savedInstanceState)
        // Preload Razorpay SDK to reduce UI lag and avoid first‑launch delay
        Checkout.preload(this)
        enableEdgeToEdge()

        setContent {
            ParkshareTheme {
                val navController = rememberNavController()
                appNavController = navController
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val session by authViewModel.sessionState.collectAsState()
                val activeRole = session.profile.activeRole

                // RBAC: Drivers ONLY see Discovery, Pass, Profile
                // Hosts ONLY see Host Spaces, Scanner, Profile
                val bottomBarScreens = remember(activeRole) {
                    if (activeRole == UserRole.HOST) {
                        listOf(
                            Screen.Host,
                            Screen.Scanner,
                            Screen.Profile
                        )
                    } else {
                        listOf(
                            Screen.Discovery,
                            Screen.Pass,
                            Screen.Profile
                        )
                    }
                }

                // Smooth role transition: redirect if current screen is forbidden in new active role
                LaunchedEffect(activeRole) {
                    if (currentRoute != null && currentRoute != Screen.Auth.route && currentRoute != Screen.Profile.route) {
                        if (activeRole == UserRole.HOST) {
                            if (currentRoute == Screen.Discovery.route || currentRoute == Screen.Pass.route) {
                                navController.navigate(Screen.Host.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        } else {
                            if (currentRoute == Screen.Host.route || currentRoute == Screen.Scanner.route) {
                                navController.navigate(Screen.Discovery.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }

                // Hide bottom navigation bar on Auth screen or when logged out
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
                        // 1. Auth Screen
                        composable(Screen.Auth.route) {
                            AuthScreen(
                                viewModel = authViewModel,
                                onAuthSuccess = { userSession ->
                                    val target = if (userSession.profile.activeRole == UserRole.HOST) {
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

                        // 2. Driver Discovery & Search Screen (Drivers Only)
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
                                    pendingVehicleNo = "MH-01-AB-1234"

                                    // Launch Razorpay Payment Window (with simulated fallback)
                                    bookingViewModel.startRazorpayCheckout(
                                        activity = this@MainActivity,
                                        spot = spot,
                                        hours = 2,
                                        vehicleNo = pendingVehicleNo
                                    )

                                    // Navigation to Pass screen moved to onPaymentSuccess callback
                                },
                                onBookSpotWithDetails = { spot, hours, vehicleNo ->
                                    pendingCheckoutSpot = spot
                                    pendingHours = hours
                                    pendingVehicleNo = vehicleNo

                                    // Launch Razorpay Payment Window
                                    bookingViewModel.startRazorpayCheckout(
                                        activity = this@MainActivity,
                                        spot = spot,
                                        hours = hours,
                                        vehicleNo = vehicleNo
                                    )

                                    // Navigation to Pass screen moved to onPaymentSuccess callback
                                },
                                onBackToAuth = {
                                    navController.navigate(Screen.Profile.route)
                                }
                            )
                        }

                        // 3. Driver Active Passes Screen (Drivers Only)
                        composable(Screen.Pass.route) {
                            BookingPassScreen(
                                viewModel = bookingViewModel,
                                onNavigateBack = {
                                    navController.navigate(Screen.Discovery.route)
                                }
                            )
                        }

                        // 4. Host Space Management Screen (Hosts Only)
                        composable(Screen.Host.route) {
                            HostScreen(
                                viewModel = hostViewModel,
                                onOpenScanner = {
                                    navController.navigate(Screen.Scanner.route)
                                },
                                onOpenProfile = {
                                    navController.navigate(Screen.Profile.route)
                                },
                                onBackToAuth = {
                                    navController.navigate(Screen.Profile.route)
                                }
                            )
                        }

                        // 5. Host QR Scanner Screen (Hosts Only - CameraX PreviewView)
                        composable(Screen.Scanner.route) {
                            HostQrScannerScreen(
                                viewModel = bookingViewModel,
                                onNavigateBack = {
                                    navController.navigate(Screen.Host.route)
                                }
                            )
                        }

                        // 6. User Profile & Role Switcher Screen (Both Drivers & Hosts)
                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                viewModel = authViewModel,
                                onSignOut = {
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        val spot = pendingCheckoutSpot
        if (spot != null) {
            bookingViewModel.onRazorpayPaymentSuccess(
                razorpayPaymentId = razorpayPaymentId ?: "pay_confirmed",
                spot = spot,
                hours = pendingHours,
                vehicleNo = pendingVehicleNo
            )
            Toast.makeText(this, "Payment Successful! Pass Generated.", Toast.LENGTH_LONG).show()
            // Navigate to Pass screen after successful payment
            appNavController?.navigate(Screen.Pass.route)
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
