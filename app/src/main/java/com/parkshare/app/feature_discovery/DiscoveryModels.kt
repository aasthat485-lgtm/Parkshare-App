package com.parkshare.app.feature_discovery

/**
 * Data model representing a parking spot available in ParkShare.
 * Schema maps directly to Cloud Firestore "spots" collection documents.
 *
 * @property spotId Unique identifier for the parking spot document.
 * @property hostId UID of the host user who owns and listed this spot.
 * @property title Name/description of the spot (e.g. Covered Driveway, Basement Slot).
 * @property lat Geographic latitude coordinate (e.g. Dadar 19.0178° N).
 * @property lng Geographic longitude coordinate (e.g. Dadar 72.8478° E).
 * @property pricePerHour Rate per hour in INR (₹).
 * @property isEvCharging Flag indicating if EV charging infrastructure is available.
 * @property availabilitySchedule Schedule description (e.g. "08:00 - 22:00 Daily", "24/7 Access").
 * @property address Physical street address description.
 * @property totalSlots Number of vehicle slots.
 * @property isAvailable Current real-time availability status.
 */
data class ParkingSpot(
    val spotId: String,
    val hostId: String = "",
    val title: String,
    val lat: Double,
    val lng: Double,
    val pricePerHour: Double,
    val isEvCharging: Boolean = false,
    val availabilitySchedule: String = "24/7 Available",
    val address: String = "Mumbai, Maharashtra",
    val totalSlots: Int = 1,
    val isAvailable: Boolean = true
) {
    // Backward-compatibility aliases
    val id: String get() = spotId
    val hourlyRate: Double get() = pricePerHour
    val hasEvCharging: Boolean get() = isEvCharging

    /**
     * Converts ParkingSpot model to a Cloud Firestore document Map.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "spotId" to spotId,
            "hostId" to hostId,
            "title" to title,
            "lat" to lat,
            "lng" to lng,
            "pricePerHour" to pricePerHour,
            "isEvCharging" to isEvCharging,
            "availabilitySchedule" to availabilitySchedule,
            "address" to address,
            "totalSlots" to totalSlots,
            "isAvailable" to isAvailable
        )
    }

    companion object {
        fun fromFirestoreMap(id: String, data: Map<String, Any?>): ParkingSpot {
            return ParkingSpot(
                spotId = (data["spotId"] as? String)?.ifBlank { id } ?: id,
                hostId = data["hostId"] as? String ?: "",
                title = data["title"] as? String ?: "Parking Spot",
                lat = (data["lat"] as? Number)?.toDouble() ?: 19.0178,
                lng = (data["lng"] as? Number)?.toDouble() ?: 72.8478,
                pricePerHour = (data["pricePerHour"] as? Number)?.toDouble()
                    ?: (data["hourlyRate"] as? Number)?.toDouble() ?: 40.0,
                isEvCharging = data["isEvCharging"] as? Boolean
                    ?: data["hasEvCharging"] as? Boolean ?: false,
                availabilitySchedule = data["availabilitySchedule"] as? String ?: "24/7 Available",
                address = data["address"] as? String ?: "Mumbai, Maharashtra",
                totalSlots = (data["totalSlots"] as? Number)?.toInt() ?: 1,
                isAvailable = data["isAvailable"] as? Boolean ?: true
            )
        }
    }
}
