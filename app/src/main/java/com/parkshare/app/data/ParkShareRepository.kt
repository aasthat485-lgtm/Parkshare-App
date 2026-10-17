package com.parkshare.app.data

import com.parkshare.app.feature_auth.UserRole
import com.parkshare.app.feature_auth.UserSession
import com.parkshare.app.feature_booking.BookingStatus
import com.parkshare.app.feature_booking.BookingTransaction
import com.parkshare.app.feature_discovery.ParkingSpot
import com.parkshare.app.feature_host.HostListing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Centralized Peer-to-Peer Repository & Firestore Mock Sync Engine.
 *
 * Implements real-time state synchronization across Driver Discovery, Host Listings,
 * and Driver Booking passes, mimicking Cloud Firestore collections:
 *  - "spots"
 *  - "host_listings"
 *  - "bookings"
 *  - "users"
 */
object ParkShareRepository {

    // Initial seed dataset around central Mumbai (Dadar, Matunga, BKC, Andheri, Powai)
    private val initialSpots = listOf(
        ParkingSpot(
            id = "spot-dadar-01",
            title = "Dadar Flower Market Covered Bay",
            address = "Senapati Bapat Marg, Dadar West, Mumbai",
            hourlyRate = 40.0,
            lat = 19.0178,
            lng = 72.8478,
            hasEvCharging = true,
            isAvailable = true,
            totalSlots = 2,
            hostId = "host-01"
        ),
        ParkingSpot(
            id = "spot-dadar-02",
            title = "Dadar Shivaji Park Dedicated Bay",
            address = "Cadell Road, Shivaji Park, Dadar West, Mumbai",
            hourlyRate = 50.0,
            lat = 19.0260,
            lng = 72.8380,
            hasEvCharging = true,
            isAvailable = true,
            totalSlots = 1,
            hostId = "host-02"
        ),
        ParkingSpot(
            id = "spot-matunga-01",
            title = "Matunga Central Residency Bay",
            address = "Lakhamsi Napoo Rd, Matunga East, Mumbai",
            hourlyRate = 35.0,
            lat = 19.0269,
            lng = 72.8553,
            hasEvCharging = false,
            isAvailable = true,
            totalSlots = 3,
            hostId = "host-03"
        ),
        ParkingSpot(
            id = "spot-bkc-01",
            title = "BKC Financial Hub Multi-Level Slot",
            address = "G Block, Bandra Kurla Complex, Mumbai",
            hourlyRate = 75.0,
            lat = 19.0674,
            lng = 72.8687,
            hasEvCharging = true,
            isAvailable = true,
            totalSlots = 4,
            hostId = "host-01"
        ),
        ParkingSpot(
            id = "spot-andheri-01",
            title = "Andheri Metro Station Hub Parking",
            address = "Andheri-Kurla Road, Andheri East, Mumbai",
            hourlyRate = 60.0,
            lat = 19.1136,
            lng = 72.8697,
            hasEvCharging = true,
            isAvailable = true,
            totalSlots = 2,
            hostId = "host-04"
        ),
        ParkingSpot(
            id = "spot-powai-01",
            title = "Powai Hiranandani Galleria Lot",
            address = "Central Avenue, Hiranandani Gardens, Powai, Mumbai",
            hourlyRate = 45.0,
            lat = 19.1176,
            lng = 72.9060,
            hasEvCharging = false,
            isAvailable = true,
            totalSlots = 2,
            hostId = "host-05"
        )
    )

    private val initialHostListings = listOf(
        HostListing(
            id = "host-spot-01",
            hostId = "host-01",
            title = "Bandra West Covered Driveway",
            address = "Pali Hill, Bandra West, Mumbai",
            hourlyRate = 60.0,
            lat = 19.0596,
            lng = 72.8295,
            totalSlots = 2,
            hasEvCharging = true,
            isActive = true
        ),
        HostListing(
            id = "host-spot-02",
            hostId = "host-02",
            title = "Dadar East Residential Garage",
            address = "Hindu Colony, Dadar East, Mumbai",
            hourlyRate = 45.0,
            lat = 19.0185,
            lng = 72.8490,
            totalSlots = 1,
            hasEvCharging = false,
            isActive = true
        ),
        HostListing(
            id = "host-spot-03",
            hostId = "host-03",
            title = "Andheri Lokhandwala Open Bay",
            address = "Link Road, Lokhandwala, Andheri West, Mumbai",
            hourlyRate = 50.0,
            lat = 19.1363,
            lng = 72.8276,
            totalSlots = 3,
            hasEvCharging = false,
            isActive = false
        )
    )

    // Observable Firestore-like collection streams
    private val _spotsFlow = MutableStateFlow<List<ParkingSpot>>(initialSpots)
    val spotsFlow: StateFlow<List<ParkingSpot>> = _spotsFlow.asStateFlow()

    private val _hostListingsFlow = MutableStateFlow<List<HostListing>>(initialHostListings)
    val hostListingsFlow: StateFlow<List<HostListing>> = _hostListingsFlow.asStateFlow()

    private val _bookingsFlow = MutableStateFlow<List<BookingTransaction>>(emptyList())
    val bookingsFlow: StateFlow<List<BookingTransaction>> = _bookingsFlow.asStateFlow()

    private val _sessionState = MutableStateFlow(
        UserSession(
            email = "driver@parkshare.com",
            role = UserRole.DRIVER,
            isAuthenticated = false
        )
    )
    val sessionState: StateFlow<UserSession> = _sessionState.asStateFlow()

    fun updateSession(session: UserSession) {
        _sessionState.value = session
    }

    /**
     * Publishes a new host parking space, persisting to "host_listings"
     * and propagating to "spots" collection for live driver discovery.
     */
    fun addHostListing(listing: HostListing) {
        _hostListingsFlow.update { current ->
            listOf(listing) + current
        }

        // Real-time synchronization to Driver discovery collection
        val newSpot = ParkingSpot(
            id = listing.id,
            title = listing.title,
            address = listing.address,
            hourlyRate = listing.hourlyRate,
            lat = listing.lat,
            lng = listing.lng,
            hasEvCharging = listing.hasEvCharging,
            isAvailable = listing.isActive,
            totalSlots = listing.totalSlots,
            hostId = listing.hostId
        )

        _spotsFlow.update { current ->
            listOf(newSpot) + current.filterNot { it.id == listing.id }
        }
    }

    /**
     * Toggles host listing active status, immediately updating driver search availability.
     */
    fun toggleListingStatus(listingId: String) {
        var updatedListing: HostListing? = null
        _hostListingsFlow.update { list ->
            list.map { item ->
                if (item.id == listingId) {
                    val toggled = item.copy(isActive = !item.isActive)
                    updatedListing = toggled
                    toggled
                } else item
            }
        }

        updatedListing?.let { listing ->
            _spotsFlow.update { list ->
                list.map { spot ->
                    if (spot.id == listing.id) {
                        spot.copy(isAvailable = listing.isActive)
                    } else spot
                }
            }
        }
    }

    /**
     * Registers a verified BookingTransaction and updates spot state.
     */
    fun recordBooking(transaction: BookingTransaction) {
        _bookingsFlow.update { current ->
            listOf(transaction) + current
        }
    }

    /**
     * Verifies and checks-in a booking pass by its QR payload or booking ID.
     */
    fun verifyAndCheckInPass(payload: String): BookingTransaction? {
        var matchedBooking: BookingTransaction? = null
        _bookingsFlow.update { list ->
            list.map { booking ->
                if (booking.qrPayload == payload || booking.bookingId == payload) {
                    val verified = booking.copy(status = BookingStatus.CHECKED_IN)
                    matchedBooking = verified
                    verified
                } else booking
            }
        }
        return matchedBooking
    }

    /**
     * Finds a spot by ID.
     */
    fun getSpotById(spotId: String): ParkingSpot? {
        return _spotsFlow.value.find { it.id == spotId }
    }
}
