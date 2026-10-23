package com.parkshare.app.feature_booking

/**
 * Status of a parking reservation lifecycle in Cloud Firestore.
 */
enum class BookingStatus {
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED
}

/**
 * Data model representing a confirmed parking reservation transaction in Cloud Firestore.
 * Matches Firestore "bookings" collection document schema.
 *
 * @property bookingId Unique alphanumeric confirmation code (e.g., PS-938210).
 * @property spotId Identifier of the booked parking spot document.
 * @property driverId UID of the authenticated driver.
 * @property hostId UID of the host owning the space.
 * @property startTime Epoch millis when the reservation window begins.
 * @property endTime Epoch millis when the reservation window ends.
 * @property amountPaid Total amount paid in INR (₹).
 * @property vehicleNo Vehicle license plate number provided during reservation.
 * @property status Current status of the booking (CONFIRMED, CHECKED_IN, etc.).
 * @property spotTitle Friendly title of the parking spot.
 * @property spotAddress Physical address of the spot.
 * @property driverEmail Contact email of the driver.
 * @property qrPayload Encrypted/verifiable payload string encoded into ZXing QR code.
 */
data class Booking(
    val bookingId: String,
    val spotId: String,
    val driverId: String,
    val hostId: String,
    val startTime: Long,
    val endTime: Long,
    val amountPaid: Double,
    val vehicleNo: String = "MH-01-AB-1234",
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val spotTitle: String = "Parking Space",
    val spotAddress: String = "Mumbai, Maharashtra",
    val driverEmail: String = "",
    val qrPayload: String = bookingId
) {
    /**
     * Checks if this booking overlaps in time with another time interval [otherStart, otherEnd].
     * Standard interval intersection: startA < endB && endA > startB
     */
    fun overlapsWith(otherStart: Long, otherEnd: Long): Boolean {
        return (startTime < otherEnd) && (endTime > otherStart)
    }

    /**
     * Converts Booking to Cloud Firestore document Map.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "bookingId" to bookingId,
            "spotId" to spotId,
            "driverId" to driverId,
            "hostId" to hostId,
            "startTime" to startTime,
            "endTime" to endTime,
            "amountPaid" to amountPaid,
            "vehicleNo" to vehicleNo,
            "status" to status.name,
            "spotTitle" to spotTitle,
            "spotAddress" to spotAddress,
            "driverEmail" to driverEmail,
            "qrPayload" to qrPayload
        )
    }

    companion object {
        fun fromFirestoreMap(id: String, map: Map<String, Any?>): Booking {
            val statusVal = (map["status"] as? String)?.let {
                runCatching { BookingStatus.valueOf(it) }.getOrNull()
            } ?: BookingStatus.CONFIRMED

            return Booking(
                bookingId = map["bookingId"] as? String ?: id,
                spotId = map["spotId"] as? String ?: "",
                driverId = map["driverId"] as? String ?: "",
                hostId = map["hostId"] as? String ?: "",
                startTime = (map["startTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                endTime = (map["endTime"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 7200000L),
                amountPaid = (map["amountPaid"] as? Number)?.toDouble() ?: 0.0,
                vehicleNo = map["vehicleNo"] as? String ?: "MH-01-AB-1234",
                status = statusVal,
                spotTitle = map["spotTitle"] as? String ?: "Parking Space",
                spotAddress = map["spotAddress"] as? String ?: "Mumbai, Maharashtra",
                driverEmail = map["driverEmail"] as? String ?: "",
                qrPayload = map["qrPayload"] as? String ?: id
            )
        }
    }
}

// Typealias for backward compatibility with previous BookingTransaction references
typealias BookingTransaction = Booking
