package com.parkshare.app.feature_host

import com.parkshare.app.feature_discovery.ParkingSpot

/**
 * Data model representing a parking space listed by a host on ParkShare.
 * Schema directly maps to Cloud Firestore "spots" and "host_listings" collections.
 *
 * @property id Unique identifier for the listing document.
 * @property hostId Identifier of the host user account owning this space.
 * @property title Friendly title or name of the parking space.
 * @property address Physical street address of the parking property.
 * @property hourlyRate Listing rate in INR (₹) per hour.
 * @property lat Geographic latitude coordinate of the property.
 * @property lng Geographic longitude coordinate of the property.
 * @property totalSlots Number of vehicle slots available at this space.
 * @property hasEvCharging Indicates whether EV charging equipment is provided.
 * @property availabilitySchedule Schedule hours description (e.g. "08:00 - 22:00 Daily").
 * @property isActive Status indicating if the property is currently accepting bookings.
 * @property createdAt Epoch timestamp when the listing was published.
 */
data class HostListing(
    val id: String,
    val hostId: String = "host-current",
    val title: String,
    val address: String,
    val hourlyRate: Double,
    val lat: Double = 19.0178,
    val lng: Double = 72.8478,
    val totalSlots: Int = 1,
    val hasEvCharging: Boolean = false,
    val availabilitySchedule: String = "24/7 Available",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts HostListing to a ParkingSpot model for Driver discovery.
     */
    fun toParkingSpot(): ParkingSpot {
        return ParkingSpot(
            spotId = id,
            hostId = hostId,
            title = title,
            lat = lat,
            lng = lng,
            pricePerHour = hourlyRate,
            isEvCharging = hasEvCharging,
            availabilitySchedule = availabilitySchedule,
            address = address,
            totalSlots = totalSlots,
            isAvailable = isActive
        )
    }

    /**
     * Converts HostListing to Cloud Firestore document Map.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "spotId" to id,
            "hostId" to hostId,
            "title" to title,
            "address" to address,
            "pricePerHour" to hourlyRate,
            "lat" to lat,
            "lng" to lng,
            "totalSlots" to totalSlots,
            "isEvCharging" to hasEvCharging,
            "availabilitySchedule" to availabilitySchedule,
            "isAvailable" to isActive,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromFirestoreMap(id: String, map: Map<String, Any?>): HostListing {
            return HostListing(
                id = id,
                hostId = map["hostId"] as? String ?: "host-current",
                title = map["title"] as? String ?: "",
                address = map["address"] as? String ?: "",
                hourlyRate = (map["pricePerHour"] as? Number)?.toDouble()
                    ?: (map["hourlyRate"] as? Number)?.toDouble() ?: 0.0,
                lat = (map["lat"] as? Number)?.toDouble() ?: 19.0178,
                lng = (map["lng"] as? Number)?.toDouble() ?: 72.8478,
                totalSlots = (map["totalSlots"] as? Number)?.toInt() ?: 1,
                hasEvCharging = map["isEvCharging"] as? Boolean
                    ?: map["hasEvCharging"] as? Boolean ?: false,
                availabilitySchedule = map["availabilitySchedule"] as? String ?: "24/7 Available",
                isActive = map["isAvailable"] as? Boolean
                    ?: map["isActive"] as? Boolean ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
