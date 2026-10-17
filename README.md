# ParkShare - Peer-to-Peer Parking Marketplace Android Application

ParkShare is a modern, production-grade Android application built using **Kotlin**, **Jetpack Compose (Material Design 3)**, and Android Architecture Components, designed to facilitate peer-to-peer parking reservations, host property monetization, and instant QR-based entry verification.

Repository: [https://github.com/aasthat485-lgtm/Parkshare-App.git](https://github.com/aasthat485-lgtm/Parkshare-App.git)

---

## 🚀 Key Features

* **Role-Based Authentication & Profiles**: Driver and Host onboarding via Material 3 FilterChips with dedicated dashboards.
* **Driver Discovery & Search**: Interactive **Google Maps Compose** view centered on **Dadar & Mumbai** coordinates with real-time spot markers, locality search, EV charging tags, and budget filters.
* **Host Space Management**: Property listing creation form with hourly pricing, slot counters, EV availability switch, and live status toggling.
* **Razorpay Payment Integration**: Integrated test payment checkout with amount calculation in paise, currency management, and simulated test mode.
* **ZXing Digital QR Pass Engine**: High-resolution 512x512 ARGB_8888 QR code bitmap generation for confirmed bookings.
* **CameraX Host QR Scanner**: Embedded CameraX PreviewView with real-time ZXing barcode analysis, custom viewfinder reticle, and instant check-in verification.
* **Cloud Firestore Mock Sync Engine**: Centralized reactive repository synchronizing spaces, active bookings, and availability across Driver and Host interfaces in real time.

---

## 🏗️ Architecture & Package Structure

Package: `com.parkshare.app`

```
com.parkshare.app/
├── MainActivity.kt               # Central NavHost & M3 Bottom NavigationBar & Razorpay handler
├── data/
│   └── ParkShareRepository.kt     # Firestore schema & reactive StateFlow mock sync engine
├── feature_auth/
│   ├── AuthModels.kt             # UserRole (DRIVER, HOST), UserSession
│   ├── AuthViewModel.kt          # Authentication lifecycle & session management
│   └── AuthScreen.kt             # Material 3 Login / Signup UI with role selection
├── feature_discovery/
│   ├── DiscoveryModels.kt        # ParkingSpot schema (lat, lng, hourlyRate, EV)
│   ├── DiscoveryViewModel.kt     # Multi-filter search state flow (Query, EV, Price)
│   └── DiscoveryScreen.kt        # Google Maps Compose view (Dadar 19.0178° N) & Spot Cards
├── feature_host/
│   ├── HostModels.kt             # HostListing Firestore schema
│   ├── HostViewModel.kt          # publishSpace() & real-time active status toggle
│   └── HostScreen.kt             # Host listing registration & active properties dashboard
├── feature_booking/
│   ├── BookingModels.kt          # BookingTransaction schema & BookingStatus
│   ├── BookingViewModel.kt       # Razorpay checkout & 512x512 ZXing QR generator
│   ├── BookingPassScreen.kt      # Pass summary card & 512x512 QR code bitmap rendering
│   └── HostQrScannerScreen.kt    # CameraX PreviewView with integrated barcode analyzer
└── ui/theme/                     # Material 3 typography, color schemes, and themes
```

---

## 🛠️ Tech Stack & Dependencies

* **Language**: Kotlin 1.9.24
* **UI Toolkit**: Jetpack Compose with Material Design 3
* **Maps**: Google Maps Compose 4.3.3 & Play Services Maps 18.2.0
* **Camera & Scanning**: CameraX 1.3.4 (Core, Camera2, Lifecycle, View)
* **QR Engine**: ZXing Core 3.5.3
* **Payments**: Razorpay Android SDK 1.6.38
* **Build System**: Gradle 8.6 with Android Gradle Plugin 8.4.2

---

## 💻 Running the App

1. Open this folder in **Android Studio**:
   ```bash
   /Users/aasthatiwari/aasthatiwari-ParkshareApp
   ```
2. Allow Gradle sync to complete (pre-installed Gradle 8.6).
3. Select an emulator or physical Android device (API 26+) and press **Run** (▶).
