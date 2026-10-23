package com.parkshare.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.razorpay.Checkout

/**
 * Initialise Razorpay and its dependencies.
 *
 * Replace the placeholder "<YOUR_RAZORPAY_TEST_KEY>" with the actual test key you obtain
 * from the Razorpay Dashboard (it looks like "rzp_test_XXXXXXXXXXXXXXXX").
 */
class RazorpaySetup(application: Application) {
    init {
        // Initialise Firebase first – Razorpay reads the Firebase Installation ID for device‑fingerprinting.
        FirebaseApp.initializeApp(application)

        // Pre‑load the Razorpay checkout WebView to avoid the half‑screen flash on first use.
        Checkout.preload(application)

        // Set the Razorpay key.
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_Tp7pqboSsrUINF") // <--- replace this with your test key

        // Optional: enable verbose logging for easier debugging.

    }
}
