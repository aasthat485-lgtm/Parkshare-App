package com.parkshare.app.feature_discovery

/**
 * Data model representing a parking spot available for discovery and booking in Parkshare.
 * Matches Cloud Firestore "spots" collection document schema.
 *
 * @property id Unique identifier for the parking spot.
 * @property title Friendly title or descriptor of the parking spot.
 * @property address Physical street address or location description.
 * @property hourlyRate Rate per hour in INR (₹).
 * @property lat Geographic latitude coordinate (e.g., Dadar 19.0178° N).
 * @property lng Geographic longitude coordinate (e.g., Dadar 72.8478° E).
 * @property hasEvCharging Flag indicating if EV charging infrastructure is available.
 * @property isAvailable Flag indicating if the spot is currently free and open for reservation.
 * @property totalSlots Total number of vehicle bays at this location.
 * @property hostId Identifier of the host listing owner.
 */
data class ParkingSpot(
    val id: String,
    val title: String,
    val address: String,
    val hourlyRate: Double,
    val lat: Double,
    val lng: Double,
    val hasEvCharging: Boolean,
    val isAvailable: Boolean = true,
    val totalSlots: Int = 1,
    val hostId: String = ""
) {
    // Convenience alias property for backwards compatibility
    val pricePerHour: Double
        get() = hourlyRate

    /**
     * Converts ParkingSpot model to a Cloud Firestore document Map.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "address" to address,
            "hourlyRate" to hourlyRate,
            "lat" to lat,
            "lng" to lng,
            "hasEvCharging" to hasEvCharging,
            "isAvailable" to isAvailable,
            "totalSlots" to totalSlots,
            "hostId" to hostId
        )
    }

    companion object {
        fun fromFirestoreMap(id: String, data: Map<String, Any?>): ParkingSpot {
            return ParkingSpot(
                id = id,
                title = data["title"] as? String ?: "",
                address = data["address"] as? String ?: "",
                hourlyRate = (data["hourlyRate"] as? Number)?.toDouble() ?: 0.0,
                lat = (data["lat"] as? Number)?.toDouble() ?: 19.0178,
                lng = (data["lng"] as? Number)?.toDouble() ?: 72.8478,
                hasEvCharging = data["hasEvCharging"] as? Boolean ?: false,
                isAvailable = data["isAvailable"] as? Boolean ?: true,
                totalSlots = (data["totalSlots"] as? Number)?.toInt() ?: 1,
                hostId = data["hostId"] as? String ?: ""
            )
        }
    }
}
