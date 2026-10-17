package com.parkshare.app.feature_host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkshare.app.data.ParkShareRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing host space listings, registration of new parking properties,
 * and real-time status toggling with Firestore mock synchronization.
 */
class HostViewModel : ViewModel() {

    // Sourced dynamically from centralized ParkShareRepository
    val listings: StateFlow<List<HostListing>> = ParkShareRepository.hostListingsFlow

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    /**
     * Publishes a new parking space into the host's active inventory and syncs to Firestore.
     *
     * @param title Title or descriptor of the parking spot
     * @param address Street address of the property
     * @param rateStr Rate per hour string
     * @param slotsStr Number of vehicle slots available
     * @param hasEv Whether EV charging is supported
     * @param lat Optional latitude (defaults to Dadar area)
     * @param lng Optional longitude (defaults to Dadar area)
     * @return Boolean indicating success or failure of validation
     */
    fun publishSpace(
        title: String,
        address: String,
        rateStr: String,
        slotsStr: String = "1",
        hasEv: Boolean = false,
        lat: Double = 19.0178,
        lng: Double = 72.8478,
        onSuccess: () -> Unit = {}
    ): Boolean {
        if (title.isBlank()) {
            _errorMessage.value = "Please provide a space title."
            _statusMessage.value = null
            return false
        }
        if (address.isBlank()) {
            _errorMessage.value = "Please enter the full address."
            _statusMessage.value = null
            return false
        }

        val rate = rateStr.toDoubleOrNull()
        if (rate == null || rate <= 0.0) {
            _errorMessage.value = "Please enter a valid hourly rate (e.g. 50)."
            _statusMessage.value = null
            return false
        }

        val slots = slotsStr.toIntOrNull() ?: 1
        if (slots <= 0) {
            _errorMessage.value = "Total slots must be at least 1."
            _statusMessage.value = null
            return false
        }

        _isPublishing.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            // Simulate Firestore document write latency
            delay(300)

            val newListing = HostListing(
                id = "host-spot-${UUID.randomUUID().toString().take(6)}",
                hostId = ParkShareRepository.sessionState.value.userId.ifBlank { "host-primary" },
                title = title.trim(),
                address = address.trim(),
                hourlyRate = rate,
                lat = lat,
                lng = lng,
                totalSlots = slots,
                hasEvCharging = hasEv,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )

            ParkShareRepository.addHostListing(newListing)
            _isPublishing.value = false
            _statusMessage.value = "Space \"${newListing.title}\" published successfully!"
            onSuccess()
        }

        return true
    }

    /**
     * Backward-compatible alias for addListing.
     */
    fun addListing(
        title: String,
        address: String,
        rateStr: String,
        slotsStr: String,
        hasEv: Boolean
    ): Boolean {
        return publishSpace(title, address, rateStr, slotsStr, hasEv)
    }

    /**
     * Toggles the active/inactive booking status for a specific listing in real time.
     *
     * @param listingId Identifier of the host listing
     */
    fun toggleListingStatus(listingId: String) {
        ParkShareRepository.toggleListingStatus(listingId)
        val listing = ParkShareRepository.hostListingsFlow.value.find { it.id == listingId }
        listing?.let {
            _statusMessage.value = "${it.title} is now ${if (it.isActive) "Active" else "Inactive"}."
        }
    }

    /**
     * Clears banner alerts.
     */
    fun clearFeedback() {
        _errorMessage.value = null
        _statusMessage.value = null
    }
}
