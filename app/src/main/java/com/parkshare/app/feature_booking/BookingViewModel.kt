package com.parkshare.app.feature_booking

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.parkshare.app.data.ParkShareRepository
import com.parkshare.app.feature_discovery.ParkingSpot
import com.razorpay.Checkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.EnumMap
import java.util.UUID

/**
 * ViewModel managing booking checkout execution via Razorpay,
 * ZXing 512x512 QR code pass generation, and host QR scan verification.
 */
class BookingViewModel : ViewModel() {

    private val _activeBooking = MutableStateFlow<BookingTransaction?>(null)
    val activeBooking: StateFlow<BookingTransaction?> = _activeBooking.asStateFlow()

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    val allBookings: StateFlow<List<BookingTransaction>> = ParkShareRepository.bookingsFlow

    private val _scannedPassResult = MutableStateFlow<BookingTransaction?>(null)
    val scannedPassResult: StateFlow<BookingTransaction?> = _scannedPassResult.asStateFlow()

    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError: StateFlow<String?> = _paymentError.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        // Seed an initial demo booking pass if none exists so users can test immediately
        if (ParkShareRepository.bookingsFlow.value.isEmpty()) {
            val demo = createBookingTransaction(
                spot = ParkingSpot(
                    id = "spot-dadar-01",
                    title = "Dadar Flower Market Covered Bay",
                    address = "Senapati Bapat Marg, Dadar West, Mumbai",
                    hourlyRate = 40.0,
                    lat = 19.0178,
                    lng = 72.8478,
                    hasEvCharging = true
                ),
                hours = 2,
                paymentId = "pay_demo_dadar_123"
            )
            setActiveBooking(demo)
        }
    }

    /**
     * Executes standard Razorpay payment checkout flow for parking space booking.
     *
     * @param activity Hosting ComponentActivity for Razorpay checkout window
     * @param spot The ParkingSpot being reserved
     * @param hours Number of hours reserved
     */
    fun startRazorpayCheckout(
        activity: Activity,
        spot: ParkingSpot,
        hours: Int = 2
    ) {
        _paymentError.value = null
        _isProcessing.value = true

        try {
            val checkout = Checkout()
            // Standard Razorpay sandbox test key ID
            checkout.setKeyID("rzp_test_1DP5mmOlF5G5ag")

            val amountPaise = (spot.hourlyRate * hours * 100).toInt()
            val driverEmail = ParkShareRepository.sessionState.value.email.ifBlank { "driver@parkshare.com" }

            val options = JSONObject().apply {
                put("name", "ParkShare Marketplace")
                put("description", "Parking Pass: ${spot.title} ($hours hrs)")
                put("currency", "INR")
                put("amount", amountPaise)
                val prefill = JSONObject().apply {
                    put("email", driverEmail)
                    put("contact", "9876543210")
                }
                put("prefill", prefill)
                val theme = JSONObject().apply {
                    put("color", "#1976D2")
                }
                put("theme", theme)
            }

            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e("BookingViewModel", "Razorpay checkout invocation error: ${e.message}", e)
            _paymentError.value = "Payment initialization error: ${e.localizedMessage}. Generating simulated pass."
            // Fallback for seamless demo testing without active Google Play / Razorpay webview
            val simulated = createBookingTransaction(spot, hours, "pay_fallback_${UUID.randomUUID().toString().take(6)}")
            setActiveBooking(simulated)
        } finally {
            _isProcessing.value = false
        }
    }

    /**
     * Called upon successful Razorpay payment callback to record transaction and build QR pass.
     */
    fun onRazorpayPaymentSuccess(
        razorpayPaymentId: String,
        spot: ParkingSpot,
        hours: Int = 2
    ) {
        val transaction = createBookingTransaction(spot, hours, razorpayPaymentId)
        setActiveBooking(transaction)
    }

    /**
     * Called upon Razorpay payment failure callback.
     */
    fun onRazorpayPaymentFailed(code: Int, response: String?) {
        _paymentError.value = "Payment failed ($code): ${response ?: "Transaction cancelled"}"
        _isProcessing.value = false
    }

    /**
     * Directly generates and sets a booking pass (ideal for tests, emulators & quick reservation).
     */
    fun simulateTestBooking(spot: ParkingSpot, hours: Int = 2): BookingTransaction {
        val transaction = createBookingTransaction(
            spot = spot,
            hours = hours,
            paymentId = "pay_test_${UUID.randomUUID().toString().take(8)}"
        )
        setActiveBooking(transaction)
        return transaction
    }

    /**
     * Creates and registers a new BookingTransaction.
     */
    private fun createBookingTransaction(
        spot: ParkingSpot,
        hours: Int,
        paymentId: String
    ): BookingTransaction {
        val bookingId = "PS-${(100000..999999).random()}"
        val totalAmount = spot.hourlyRate * hours
        val driverEmail = ParkShareRepository.sessionState.value.email.ifBlank { "driver@parkshare.com" }

        // JSON QR pass payload for host scanner verification
        val payload = JSONObject().apply {
            put("bid", bookingId)
            put("spot", spot.title)
            put("amt", totalAmount)
            put("driver", driverEmail)
            put("host", spot.hostId)
            put("time", System.currentTimeMillis())
        }.toString()

        val transaction = BookingTransaction(
            bookingId = bookingId,
            spotId = spot.id,
            spotTitle = spot.title,
            spotAddress = spot.address,
            driverEmail = driverEmail,
            hostId = spot.hostId,
            durationHours = hours,
            hourlyRate = spot.hourlyRate,
            amountPaid = totalAmount,
            paymentId = paymentId,
            qrPayload = payload,
            status = BookingStatus.CONFIRMED
        )

        ParkShareRepository.recordBooking(transaction)
        return transaction
    }

    /**
     * Sets the active booking and renders its high-resolution 512x512 ZXing QR bitmap.
     */
    fun setActiveBooking(booking: BookingTransaction) {
        _activeBooking.value = booking
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = generateQrCodeBitmap(booking.qrPayload, 512, 512)
            withContext(Dispatchers.Main) {
                _qrBitmap.value = bitmap
            }
        }
    }

    /**
     * Generates a high-contrast 512x512 ZXing QR code image bitmap.
     *
     * @param payload Text/JSON content to encode in the QR code
     * @param width Width in pixels (default 512)
     * @param height Height in pixels (default 512)
     * @return Formatted Bitmap in ARGB_8888 configuration
     */
    fun generateQrCodeBitmap(
        payload: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 2)
        }

        val bitMatrix = QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            width,
            height,
            hints
        )

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Validates a scanned QR pass from CameraX Host Scanner.
     */
    fun verifyScannedPass(payload: String): BookingTransaction? {
        val verified = ParkShareRepository.verifyAndCheckInPass(payload)
        _scannedPassResult.value = verified
        return verified
    }

    fun clearScannedResult() {
        _scannedPassResult.value = null
    }

    fun clearPaymentError() {
        _paymentError.value = null
    }
}
