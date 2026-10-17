package com.parkshare.app.feature_booking

/**
 * Status of a parking reservation lifecycle.
 */
enum class BookingStatus {
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED
}

/**
 * Data model representing a completed parking reservation transaction in ParkShare.
 * Schema directly maps to Cloud Firestore "bookings" collection documents.
 *
 * @property bookingId Unique alphanumeric confirmation code.
 * @property spotId Identifier of the booked parking spot.
 * @property spotTitle Name/title of the booked space.
 * @property spotAddress Physical address of the parking spot.
 * @property driverEmail Email address of the driver holding the pass.
 * @property hostId Identifier of the host receiving the booking.
 * @property durationHours Duration of the booked parking session in hours.
 * @property hourlyRate Rate per hour in INR (₹).
 * @property amountPaid Total amount paid in INR (₹) via Razorpay.
 * @property paymentId Razorpay payment reference ID (e.g., pay_test_xxx).
 * @property qrPayload Cryptographic/JSON payload encoded inside the ZXing QR pass bitmap.
 * @property timestamp Epoch millis when reservation was confirmed.
 * @property status Current verification status of the pass.
 */
data class BookingTransaction(
    val bookingId: String,
    val spotId: String = "",
    val spotTitle: String,
    val spotAddress: String = "",
    val driverEmail: String = "",
    val hostId: String = "",
    val durationHours: Int = 2,
    val hourlyRate: Double = 40.0,
    val amountPaid: Double,
    val paymentId: String = "pay_simulated_test",
    val qrPayload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: BookingStatus = BookingStatus.CONFIRMED
) {
    /**
     * Converts BookingTransaction to Cloud Firestore document Map.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "bookingId" to bookingId,
            "spotId" to spotId,
            "spotTitle" to spotTitle,
            "spotAddress" to spotAddress,
            "driverEmail" to driverEmail,
            "hostId" to hostId,
            "durationHours" to durationHours,
            "hourlyRate" to hourlyRate,
            "amountPaid" to amountPaid,
            "paymentId" to paymentId,
            "qrPayload" to qrPayload,
            "timestamp" to timestamp,
            "status" to status.name
        )
    }

    companion object {
        fun fromFirestoreMap(id: String, map: Map<String, Any?>): BookingTransaction {
            return BookingTransaction(
                bookingId = map["bookingId"] as? String ?: id,
                spotId = map["spotId"] as? String ?: "",
                spotTitle = map["spotTitle"] as? String ?: "Parking Spot",
                spotAddress = map["spotAddress"] as? String ?: "",
                driverEmail = map["driverEmail"] as? String ?: "",
                hostId = map["hostId"] as? String ?: "",
                durationHours = (map["durationHours"] as? Number)?.toInt() ?: 1,
                hourlyRate = (map["hourlyRate"] as? Number)?.toDouble() ?: 0.0,
                amountPaid = (map["amountPaid"] as? Number)?.toDouble() ?: 0.0,
                paymentId = map["paymentId"] as? String ?: "",
                qrPayload = map["qrPayload"] as? String ?: id,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                status = (map["status"] as? String)?.let {
                    runCatching { BookingStatus.valueOf(it) }.getOrNull()
                } ?: BookingStatus.CONFIRMED
            )
        }
    }
}
