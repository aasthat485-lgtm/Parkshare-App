ParkShare - Peer-to-Peer Parking Marketplace Android Application
--
ParkShare is a production-grade, peer-to-peer parking marketplace Android application built using Kotlin, Jetpack Compose (Material 3), and Android Architecture Components. 

It connects drivers looking to discover and reserve secure parking spots with property hosts seeking to monetize vacant spaces. The application features seamless payment processing via Razorpay, real-time data synchronization with Firebase Cloud Firestore, and automated QR-pass validation using Android CameraX and ML Kit.

**Repository:** https://github.com/aasthat485-lgtm/Parkshare-App.git
--
🚀 Key Features
* **Role-Based Authentication & Profiles**: Driver and Host onboarding via Material 3 FilterChips with dedicated dashboards.
* **Driver Discovery & Search**: Interactive **Google Maps Compose** view centered on **Dadar & Mumbai** coordinates with real-time spot markers, locality search, EV charging tags, and budget filters.
* **Host Space Management**: Property listing creation form with hourly pricing, slot counters, EV availability switch, and live status toggling.
* **Razorpay Payment Integration**: Integrated test payment checkout with amount calculation in paise, currency management, and simulated test mode.
* **ZXing Digital QR Pass Engine**: High-resolution 512x512 ARGB_8888 QR code bitmap generation for confirmed bookings.
* **CameraX Host QR Scanner**: Embedded CameraX PreviewView with real-time ZXing barcode analysis, custom viewfinder reticle, and instant check-in verification.
* **Cloud Firestore Mock Sync Engine**: Centralized reactive repository synchronizing spaces, active bookings, and availability across Driver and Host interfaces in real time.
- **Role‑Based Authentication & Profiles** – Drivers and Hosts sign up/in with Firebase Auth, selecting their role via Material 3 FilterChips.
- **Driver Discovery & Search** – Google Maps Compose view centered on Dadar & Mumbai with real‑time spot markers, EV‑charging tags, and budget filters.
- **Host Space Management** – Hosts can list their parking spaces, set hourly pricing, EV availability, and toggle live status.
- **Razorpay Payment Integration** – Test‑mode checkout handling amount calculation in paise and payment verification.
- **QR Pass Generation** – ZXing generates a high‑resolution 512×512 ARGB_8888 QR bitmap for a confirmed booking.
- **Live QR Scanning** – **CameraX + ML Kit** Barcode Scanning reads real QR codes, parses the JSON payload (driverName, vehicleNo, spotTitle, amountPaid) and displays it in an `AlertDialog`.
- **Cloud Firestore Mock Sync Engine** – Reactive repository synchronises spaces, active bookings and availability across driver and host interfaces in real time.
---
🏗️ Architecture & Package Structure
Package: `com.parkshare.app`
```
com.parkshare.app/
├── MainActivity.kt               # Central NavHost & M3 Bottom NavigationBar & Razorpay handler
├── MainActivity.kt               # Central NavHost & Bottom NavigationBar
├── data/
│   └── ParkShareRepository.kt     # Firestore schema & reactive StateFlow mock sync engine
│   └── ParkShareRepository.kt    # Firestore schema & reactive StateFlow sync engine
├── feature_auth/
│   ├── AuthModels.kt             # UserRole (DRIVER, HOST), UserSession
│   ├── AuthViewModel.kt          # Authentication lifecycle & session management
│   └── AuthScreen.kt             # Material 3 Login / Signup UI with role selection
│   ├── AuthModels.kt            # UserRole (DRIVER, HOST), UserSession
│   ├── AuthViewModel.kt         # Authentication lifecycle & session management
│   └── AuthScreen.kt            # Login / Signup UI with role selection
├── feature_discovery/
│   ├── DiscoveryModels.kt        # ParkingSpot schema (lat, lng, hourlyRate, EV)
│   ├── DiscoveryViewModel.kt     # Multi-filter search state flow (Query, EV, Price)
│   └── DiscoveryScreen.kt        # Google Maps Compose view (Dadar 19.0178° N) & Spot Cards
│   ├── DiscoveryModels.kt       # ParkingSpot schema (lat, lng, hourlyRate, EV)
│   ├── DiscoveryViewModel.kt    # Filter state flow (query, EV, price)
│   └── DiscoveryScreen.kt       # Google Maps Compose view + Spot Cards
├── feature_host/
│   ├── HostModels.kt             # HostListing Firestore schema
│   ├── HostViewModel.kt          # publishSpace() & real-time active status toggle
│   └── HostScreen.kt             # Host listing registration & active properties dashboard
│   ├── HostModels.kt            # HostListing Firestore schema
│   ├── HostViewModel.kt         # publishSpace() & real‑time active status toggle
│   ├── HostScreen.kt            # Host listing registration & dashboard
│   └── QrScannerScreen.kt       # CameraX + ML Kit QR scanner (live preview)
├── feature_booking/
│   ├── BookingModels.kt          # BookingTransaction schema & BookingStatus
│   ├── BookingViewModel.kt       # Razorpay checkout & 512x512 ZXing QR generator
│   ├── BookingPassScreen.kt      # Pass summary card & 512x512 QR code bitmap rendering
│   └── HostQrScannerScreen.kt    # CameraX PreviewView with integrated barcode analyzer
│   ├── BookingModels.kt         # BookingTransaction schema & status
│   ├── BookingViewModel.kt      # Razorpay checkout & QR pass generation
│   ├── BookingPassScreen.kt     # Pass summary card & QR bitmap rendering
│   └── BookingScreen.kt         # Booking flow UI
└── ui/theme/                     # Material 3 typography, color schemes, and themes
```
---
🛠️ Tech Stack & Dependencies
* **Language**: Kotlin 1.9.24
* **UI Toolkit**: Jetpack Compose with Material Design 3
* **Maps**: Google Maps Compose 4.3.3 & Play Services Maps 18.2.0
* **Camera & Scanning**: CameraX 1.3.4 (Core, Camera2, Lifecycle, View)
* **QR Engine**: ZXing Core 3.5.3
* **Payments**: Razorpay Android SDK 1.6.38
* **Build System**: Gradle 8.6 with Android Gradle Plugin 8.4.2
- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Maps**: Google Maps Compose 4.3.3 & Play Services Maps 18.2.0
- **Camera & Scanning**: CameraX 1.3.4 (core, camera2, lifecycle, view) + ML Kit Barcode Scanning
- **QR Engine**: ZXing 3.5.3 (for QR generation)
- **Payments**: Razorpay Android SDK 1.6.38
- **Build System**: Gradle 8.6 with Android Gradle Plugin 8.4.2
---
💻 Running the App
1. Open the project folder in **Android Studio**:
2. Allow Gradle sync to complete (pre-installed Gradle 8.6).
3. Select an emulator or physical Android device (API 26+) and press **Run** (▶).
4. Let Gradle sync finish (Gradle 8.6 is bundled).
5. Choose an emulator or a physical Android device (API 26+).
6. Press **Run** (▶). The app will prompt for camera permission the first time you open the QR scanner.
---
✅ Project Status
All requested functionality – authentication, discovery, host management, Razorpay payment, QR pass generation, and live QR scanning – is fully implemented and tested. The app is ready for deployment or further UI polish.
