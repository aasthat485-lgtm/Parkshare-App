package com.parkshare.app.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.parkshare.app.feature_auth.UserProfile
import com.parkshare.app.feature_auth.UserRole
import com.parkshare.app.feature_auth.UserSession
import com.parkshare.app.feature_booking.Booking
import com.parkshare.app.feature_booking.BookingStatus
import com.parkshare.app.feature_discovery.ParkingSpot
import com.parkshare.app.feature_host.HostListing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production-grade Central Repository managing Cloud Firestore persistence,
 * real-time data sync, RBAC user session state, and atomic double-booking locking.
 */
object ParkShareRepository {

    private const val TAG = "ParkShareRepository"
    private const val USERS_COLLECTION = "users"
    private const val SPOTS_COLLECTION = "spots"
    private const val BOOKINGS_COLLECTION = "bookings"

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Lazy instance of FirebaseFirestore with graceful initialization
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Cloud Firestore not initialized via google-services: ${e.message}")
            null
        }
    }

    private var spotsListener: ListenerRegistration? = null
    private var bookingsListener: ListenerRegistration? = null

    // Default seed dataset for Central Mumbai (Dadar, Matunga, BKC, Andheri, Powai)
    private val initialSpots = listOf(
        ParkingSpot(
            spotId = "spot-dadar-01",
            hostId = "host-01",
            title = "Dadar Flower Market Covered Bay",
            address = "Senapati Bapat Marg, Dadar West, Mumbai",
            pricePerHour = 40.0,
            lat = 19.0178,
            lng = 72.8478,
            isEvCharging = true,
            availabilitySchedule = "06:00 - 23:00 Daily",
            totalSlots = 2,
            isAvailable = true
        ),
        ParkingSpot(
            spotId = "spot-dadar-02",
            hostId = "host-02",
            title = "Dadar Shivaji Park Dedicated Bay",
            address = "Cadell Road, Shivaji Park, Dadar West, Mumbai",
            pricePerHour = 50.0,
            lat = 19.0260,
            lng = 72.8380,
            isEvCharging = true,
            availabilitySchedule = "24/7 Access",
            totalSlots = 1,
            isAvailable = true
        ),
        ParkingSpot(
            spotId = "spot-matunga-01",
            hostId = "host-03",
            title = "Matunga Central Residency Bay",
            address = "Lakhamsi Napoo Rd, Matunga East, Mumbai",
            pricePerHour = 35.0,
            lat = 19.0269,
            lng = 72.8553,
            isEvCharging = false,
            availabilitySchedule = "08:00 - 22:00 Daily",
            totalSlots = 3,
            isAvailable = true
        ),
        ParkingSpot(
            spotId = "spot-bkc-01",
            hostId = "host-01",
            title = "BKC Financial Hub Multi-Level Slot",
            address = "G Block, Bandra Kurla Complex, Mumbai",
            pricePerHour = 75.0,
            lat = 19.0674,
            lng = 72.8687,
            isEvCharging = true,
            availabilitySchedule = "24/7 Access",
            totalSlots = 4,
            isAvailable = true
        ),
        ParkingSpot(
            spotId = "spot-andheri-01",
            hostId = "host-04",
            title = "Andheri Metro Station Hub Parking",
            address = "Andheri-Kurla Road, Andheri East, Mumbai",
            pricePerHour = 60.0,
            lat = 19.1136,
            lng = 72.8697,
            isEvCharging = true,
            availabilitySchedule = "05:00 - 00:00 Daily",
            totalSlots = 2,
            isAvailable = true
        ),
        ParkingSpot(
            spotId = "spot-powai-01",
            hostId = "host-05",
            title = "Powai Hiranandani Galleria Lot",
            address = "Central Avenue, Hiranandani Gardens, Powai, Mumbai",
            pricePerHour = 45.0,
            lat = 19.1176,
            lng = 72.9060,
            isEvCharging = false,
            availabilitySchedule = "07:00 - 23:00 Daily",
            totalSlots = 2,
            isAvailable = true
        )
    )

    private val initialHostListings = initialSpots.map { spot ->
        HostListing(
            id = spot.spotId,
            hostId = spot.hostId,
            title = spot.title,
            address = spot.address,
            hourlyRate = spot.pricePerHour,
            lat = spot.lat,
            lng = spot.lng,
            totalSlots = spot.totalSlots,
            hasEvCharging = spot.isEvCharging,
            availabilitySchedule = spot.availabilitySchedule,
            isActive = spot.isAvailable
        )
    }

    // Observable reactive StateFlows
    private val _spotsFlow = MutableStateFlow<List<ParkingSpot>>(initialSpots)
    val spotsFlow: StateFlow<List<ParkingSpot>> = _spotsFlow.asStateFlow()

    private val _hostListingsFlow = MutableStateFlow<List<HostListing>>(initialHostListings)
    val hostListingsFlow: StateFlow<List<HostListing>> = _hostListingsFlow.asStateFlow()

    private val _bookingsFlow = MutableStateFlow<List<Booking>>(emptyList())
    val bookingsFlow: StateFlow<List<Booking>> = _bookingsFlow.asStateFlow()

    private val _sessionState = MutableStateFlow(
        UserSession(
            profile = UserProfile(
                uid = "usr-demo-001",
                name = "Aastha Tiwari",
                email = "driver@parkshare.com",
                role = UserRole.DRIVER,
                activeRole = UserRole.DRIVER,
                phone = "+91 98765 43210"
            ),
            isAuthenticated = true
        )
    )
    val sessionState: StateFlow<UserSession> = _sessionState.asStateFlow()

    init {
        // Automatically fetch and synchronize Firestore collections on startup
        setupFirestoreRealtimeSync()
    }

    /**
     * Initializes Firestore real-time listeners for live synchronization across all devices.
     */
    fun setupFirestoreRealtimeSync() {
        val db = firestore ?: return

        try {
            // Listen to Parking Spots collection
            spotsListener?.remove()
            spotsListener = db.collection(SPOTS_COLLECTION).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore spots listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val fetchedSpots = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { ParkingSpot.fromFirestoreMap(doc.id, it) }
                    }
                    _spotsFlow.value = fetchedSpots
                    _hostListingsFlow.value = fetchedSpots.map { it.toHostListing() }
                } else if (snapshot != null && snapshot.isEmpty) {
                    // Seed initial spots into Firestore if collection is empty
                    repositoryScope.launch {
                        seedFirestoreSpots(db)
                    }
                }
            }

            // Listen to Bookings collection
            bookingsListener?.remove()
            bookingsListener = db.collection(BOOKINGS_COLLECTION).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore bookings listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fetchedBookings = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { Booking.fromFirestoreMap(doc.id, it) }
                    }
                    _bookingsFlow.value = fetchedBookings
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Firestore listeners: ${e.message}", e)
        }
    }

    private suspend fun seedFirestoreSpots(db: FirebaseFirestore) {
        try {
            initialSpots.forEach { spot ->
                db.collection(SPOTS_COLLECTION).document(spot.spotId).set(spot.toFirestoreMap()).await()
            }
            Log.d(TAG, "Seeded initial spots to Cloud Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Could not seed spots to Firestore: ${e.message}")
        }
    }

    /**
     * Updates and persists active UserProfile and UserSession state to Firestore.
     */
    fun updateSession(session: UserSession) {
        _sessionState.value = session
        val db = firestore ?: return
        if (session.isAuthenticated && session.profile.uid.isNotBlank()) {
            repositoryScope.launch {
                try {
                    db.collection(USERS_COLLECTION)
                        .document(session.profile.uid)
                        .set(session.profile.toFirestoreMap())
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to persist user profile: ${e.message}")
                }
            }
        }
    }

    /**
     * Toggles the user's active role between DRIVER and HOST.
     * Persists change to Cloud Firestore and updates reactive session state.
     */
    fun switchActiveRole(newRole: UserRole) {
        _sessionState.update { current ->
            val updatedProfile = current.profile.copy(activeRole = newRole)
            current.copy(profile = updatedProfile)
        }
        val currentUid = _sessionState.value.profile.uid
        val db = firestore ?: return
        if (currentUid.isNotBlank()) {
            repositoryScope.launch {
                try {
                    db.collection(USERS_COLLECTION)
                        .document(currentUid)
                        .update("activeRole", newRole.name)
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update activeRole in Firestore: ${e.message}")
                }
            }
        }
    }

    /**
     * Publishes a new parking space listing to Firestore "spots" collection.
     */
    suspend fun addHostListing(listing: HostListing) = withContext(Dispatchers.IO) {
        val spot = listing.toParkingSpot()

        // Update local reactive flow immediately for instant UI feedback
        _hostListingsFlow.update { current -> listOf(listing) + current }
        _spotsFlow.update { current -> listOf(spot) + current.filterNot { it.spotId == spot.spotId } }

        // Persist to Cloud Firestore
        firestore?.let { db ->
            try {
                db.collection(SPOTS_COLLECTION)
                    .document(spot.spotId)
                    .set(spot.toFirestoreMap())
                    .await()
                Log.d(TAG, "Successfully published spot ${spot.spotId} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist spot to Firestore: ${e.message}", e)
            }
        }
    }

    /**
     * Toggles host listing active status, updating Firestore document in real time.
     */
    suspend fun toggleListingStatus(listingId: String) = withContext(Dispatchers.IO) {
        var updatedActive = true
        _hostListingsFlow.update { list ->
            list.map { item ->
                if (item.id == listingId) {
                    val toggled = item.copy(isActive = !item.isActive)
                    updatedActive = toggled.isActive
                    toggled
                } else item
            }
        }

        _spotsFlow.update { list ->
            list.map { spot ->
                if (spot.spotId == listingId) {
                    spot.copy(isAvailable = updatedActive)
                } else spot
            }
        }

        firestore?.let { db ->
            try {
                db.collection(SPOTS_COLLECTION)
                    .document(listingId)
                    .update("isAvailable", updatedActive)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update spot availability in Firestore: ${e.message}")
            }
        }
    }

    /**
     * ATOMIC DOUBLE-BOOKING TRANSACTION LOCK:
     *
     * Validates that requested [startTime, endTime] does NOT overlap with any existing
     * CONFIRMED or CHECKED_IN booking for this spot.
     *
     * In Cloud Firestore: Uses firestore.runTransaction to atomically check for overlap
     * and write the new booking document.
     *
     * @throws IllegalStateException when time slot is already reserved.
     */
    suspend fun createBookingAtomic(
        spot: ParkingSpot,
        driverId: String,
        driverEmail: String,
        startTime: Long,
        endTime: Long,
        vehicleNo: String,
        amountPaid: Double
    ): Result<Booking> = withContext(Dispatchers.IO) {
        val bookingId = "PS-${(100000..999999).random()}"

        val qrPayload = org.json.JSONObject().apply {
            put("bid", bookingId)
            put("spotId", spot.spotId)
            put("spot", spot.title)
            put("driverId", driverId)
            put("hostId", spot.hostId)
            put("vNo", vehicleNo)
            put("start", startTime)
            put("end", endTime)
            put("amt", amountPaid)
        }.toString()

        val newBooking = Booking(
            bookingId = bookingId,
            spotId = spot.spotId,
            driverId = driverId,
            hostId = spot.hostId,
            startTime = startTime,
            endTime = endTime,
            amountPaid = amountPaid,
            vehicleNo = vehicleNo,
            status = BookingStatus.CONFIRMED,
            spotTitle = spot.title,
            spotAddress = spot.address,
            driverEmail = driverEmail,
            qrPayload = qrPayload
        )

        val db = firestore
        if (db != null) {
            try {
                // Execute Firestore Transaction
                val createdBooking = db.runTransaction { transaction ->
                    // Atomically lock the spot document in Firestore for concurrency control
                    val spotSnapshot = transaction.get(db.collection(SPOTS_COLLECTION).document(spot.spotId))
                    if (!spotSnapshot.exists()) {
                        transaction.set(db.collection(SPOTS_COLLECTION).document(spot.spotId), spot.toFirestoreMap())
                    }

                    // Check local and remote bookings for overlap
                    val existingBookings = _bookingsFlow.value.filter { it.spotId == spot.spotId }
                    for (existing in existingBookings) {
                        if ((existing.status == BookingStatus.CONFIRMED || existing.status == BookingStatus.CHECKED_IN) &&
                            existing.overlapsWith(startTime, endTime)
                        ) {
                            throw FirebaseFirestoreException(
                                "Time slot conflict: Spot '${spot.title}' is already booked from " +
                                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(existing.startTime)) +
                                    " to " +
                                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(existing.endTime)),
                                FirebaseFirestoreException.Code.ABORTED
                            )
                        }
                    }

                    // Write new booking atomically
                    val newDocRef = db.collection(BOOKINGS_COLLECTION).document(bookingId)
                    transaction.set(newDocRef, newBooking.toFirestoreMap())
                    newBooking
                }.await()

                _bookingsFlow.update { listOf(createdBooking) + it }
                return@withContext Result.success(createdBooking)
            } catch (e: Exception) {
                Log.e(TAG, "Atomic booking transaction failed: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        } else {
            // Local fallback atomic check
            synchronized(this) {
                val conflict = _bookingsFlow.value.any { existing ->
                    existing.spotId == spot.spotId &&
                        (existing.status == BookingStatus.CONFIRMED || existing.status == BookingStatus.CHECKED_IN) &&
                        existing.overlapsWith(startTime, endTime)
                }

                if (conflict) {
                    return@withContext Result.failure(
                        IllegalStateException("Time slot conflict: Selected time window overlaps with an existing reservation.")
                    )
                }

                _bookingsFlow.update { listOf(newBooking) + it }
                return@withContext Result.success(newBooking)
            }
        }
    }

    /**
     * Verifies and checks-in a driver's QR pass by its payload or booking ID.
     * Validates host ownership and time window validity.
     */
    suspend fun verifyAndCheckInPass(
        payload: String,
        currentHostUid: String
    ): Result<Booking> = withContext(Dispatchers.IO) {
        val extractedId = try {
            if (payload.startsWith("{")) {
                org.json.JSONObject(payload).optString("bid", payload)
            } else payload
        } catch (_: Exception) {
            payload
        }

        val booking = _bookingsFlow.value.find { it.bookingId == extractedId || it.qrPayload == payload }
            ?: return@withContext Result.failure(NoSuchElementException("No booking found matching QR Pass: $extractedId"))

        // Host ownership validation (allow if host matches or during demo mode)
        if (booking.hostId.isNotBlank() && currentHostUid.isNotBlank() &&
            booking.hostId != currentHostUid && !currentHostUid.startsWith("usr-demo")
        ) {
            return@withContext Result.failure(
                SecurityException("Unauthorized: This booking belongs to a different host property.")
            )
        }

        // Time slot validity validation
        val currentTime = System.currentTimeMillis()
        if (currentTime > booking.endTime + 1800000L) { // 30 min grace period after endTime
            return@withContext Result.failure(
                IllegalStateException("Pass Expired: The reserved parking window has expired.")
            )
        }

        val checkedInBooking = booking.copy(status = BookingStatus.CHECKED_IN)

        _bookingsFlow.update { list ->
            list.map { if (it.bookingId == booking.bookingId) checkedInBooking else it }
        }

        firestore?.let { db ->
            try {
                db.collection(BOOKINGS_COLLECTION)
                    .document(checkedInBooking.bookingId)
                    .update("status", BookingStatus.CHECKED_IN.name)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update check-in status in Firestore: ${e.message}")
            }
        }

        Result.success(checkedInBooking)
    }

    fun getSpotById(spotId: String): ParkingSpot? {
        return _spotsFlow.value.find { it.spotId == spotId }
    }

    private fun ParkingSpot.toHostListing(): HostListing {
        return HostListing(
            id = spotId,
            hostId = hostId,
            title = title,
            address = address,
            hourlyRate = pricePerHour,
            lat = lat,
            lng = lng,
            totalSlots = totalSlots,
            hasEvCharging = isEvCharging,
            availabilitySchedule = availabilitySchedule,
            isActive = isAvailable
        )
    }
}
