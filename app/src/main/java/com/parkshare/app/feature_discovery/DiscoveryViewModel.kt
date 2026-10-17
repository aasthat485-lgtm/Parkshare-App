package com.parkshare.app.feature_discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkshare.app.data.ParkShareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing driver parking discovery, real-time Firestore sync state flow,
 * search query filtering, and EV/budget filter criteria.
 */
class DiscoveryViewModel : ViewModel() {

    // Master stream sourced directly from real-time Firestore mock sync repository
    val rawFirestoreSpots: StateFlow<List<ParkingSpot>> = ParkShareRepository.spotsFlow

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter states
    private val _onlyEv = MutableStateFlow(false)
    val onlyEv: StateFlow<Boolean> = _onlyEv.asStateFlow()

    private val _under50 = MutableStateFlow(false)
    val under50: StateFlow<Boolean> = _under50.asStateFlow()

    // Selected spot for detail/booking dialog
    private val _selectedSpot = MutableStateFlow<ParkingSpot?>(null)
    val selectedSpot: StateFlow<ParkingSpot?> = _selectedSpot.asStateFlow()

    // Toggle between interactive Google Map view and List view
    private val _isMapView = MutableStateFlow(true)
    val isMapView: StateFlow<Boolean> = _isMapView.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Combined reactive StateFlow representing filtered parking spots.
     */
    val parkingSpots: StateFlow<List<ParkingSpot>> = combine(
        rawFirestoreSpots,
        _searchQuery,
        _onlyEv,
        _under50
    ) { spots, query, evOnly, max50 ->
        val cleanQuery = query.trim().lowercase()
        spots.filter { spot ->
            val matchesQuery = cleanQuery.isEmpty() ||
                spot.title.lowercase().contains(cleanQuery) ||
                spot.address.lowercase().contains(cleanQuery)

            val matchesEv = !evOnly || spot.hasEvCharging
            val matchesPrice = !max50 || spot.hourlyRate <= 50.0

            matchesQuery && matchesEv && matchesPrice
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ParkShareRepository.spotsFlow.value
    )

    /**
     * Updates the search query and recalculates matching parking spots.
     */
    fun searchByQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggles the Electric Vehicle (EV) charging station filter.
     */
    fun toggleEvFilter(onlyEv: Boolean) {
        _onlyEv.value = onlyEv
    }

    /**
     * Toggles the budget filter for spots under ₹50/hr.
     */
    fun toggleUnder50Filter(under50: Boolean) {
        _under50.value = under50
    }

    /**
     * Sets or clears the currently selected spot.
     */
    fun selectSpot(spot: ParkingSpot?) {
        _selectedSpot.value = spot
    }

    /**
     * Toggles between Google Maps Compose view and List view.
     */
    fun toggleMapView() {
        _isMapView.value = !_isMapView.value
    }

    /**
     * Simulates a pull-to-refresh or live Firestore collection sync.
     */
    fun refreshSpotsFromFirestore() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(600)
            _isRefreshing.value = false
        }
    }
}
