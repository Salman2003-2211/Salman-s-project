package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.SpareRepository
import com.example.model.*
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppTab {
    HOME,
    SEARCH,
    POST,
    PARTNERS,
    LOAN
}

sealed class ScreenOverlay {
    object None : ScreenOverlay()
    object Login : ScreenOverlay()
    object Profile : ScreenOverlay()
    object MyAds : ScreenOverlay()
    object AboutUs : ScreenOverlay()
    data class LeadInbox(val propertyId: String? = null) : ScreenOverlay()
    data class PropertyDetail(val propertyId: String) : ScreenOverlay()
    object OperatorCrm : ScreenOverlay()
}

class SpareViewModel(
    val repository: SpareRepository = SpareRepository()
) : ViewModel() {

    val isLoggedIn = repository.isLoggedIn
    val userProfile = repository.userProfile
    val properties = repository.properties
    val leadInquiries = repository.leadInquiries
    val partners = repository.partners
    val bookedMeetings = repository.bookedMeetings
    val loanLeads = repository.loanLeads

    private val _activeTab = MutableStateFlow(AppTab.HOME)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _currentOverlay = MutableStateFlow<ScreenOverlay>(ScreenOverlay.None)
    val currentOverlay: StateFlow<ScreenOverlay> = _currentOverlay.asStateFlow()

    // Search & Filter State
    private val _selectedPurpose = MutableStateFlow("Buy / Sale")
    val selectedPurpose: StateFlow<String> = _selectedPurpose.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBhk = MutableStateFlow<String?>("All")
    val selectedBhk: StateFlow<String?> = _selectedBhk.asStateFlow()

    private val _selectedBudgetRange = MutableStateFlow("All")
    val selectedBudgetRange: StateFlow<String> = _selectedBudgetRange.asStateFlow()

    private val _selectedPropertyType = MutableStateFlow("All")
    val selectedPropertyType: StateFlow<String> = _selectedPropertyType.asStateFlow()

    private val _selectedFurnishing = MutableStateFlow("All")
    val selectedFurnishing: StateFlow<String> = _selectedFurnishing.asStateFlow()

    private val _verifiedOnly = MutableStateFlow(false)
    val verifiedOnly: StateFlow<Boolean> = _verifiedOnly.asStateFlow()

    private val _isMapView = MutableStateFlow(false)
    val isMapView: StateFlow<Boolean> = _isMapView.asStateFlow()

    private val _sortBy = MutableStateFlow("Relevance")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _showAdvancedFilters = MutableStateFlow(false)
    val showAdvancedFilters: StateFlow<Boolean> = _showAdvancedFilters.asStateFlow()

    // Navigation & Overlay controls
    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
        _currentOverlay.value = ScreenOverlay.None
    }

    fun openOverlay(overlay: ScreenOverlay) {
        _currentOverlay.value = overlay
    }

    fun closeOverlay() {
        _currentOverlay.value = ScreenOverlay.None
    }

    // Search Controls
    fun setPurpose(purpose: String) {
        _selectedPurpose.value = purpose
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedBhk(bhk: String?) {
        _selectedBhk.value = bhk
    }

    fun setSelectedBudget(budget: String) {
        _selectedBudgetRange.value = budget
    }

    fun setSelectedPropertyType(type: String) {
        _selectedPropertyType.value = type
    }

    fun setSelectedFurnishing(furnishing: String) {
        _selectedFurnishing.value = furnishing
    }

    fun setVerifiedOnly(verified: Boolean) {
        _verifiedOnly.value = verified
    }

    fun setMapView(isMap: Boolean) {
        _isMapView.value = isMap
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun setShowAdvancedFilters(show: Boolean) {
        _showAdvancedFilters.value = show
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedBhk.value = "All"
        _selectedBudgetRange.value = "All"
        _selectedPropertyType.value = "All"
        _selectedFurnishing.value = "All"
        _verifiedOnly.value = false
        _sortBy.value = "Relevance"
    }

    fun toggleFavorite(propertyId: String) {
        repository.toggleFavorite(propertyId)
    }

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
    }

    fun updateProfile(name: String, phone: String, email: String) {
        repository.updateProfile(name, phone, email)
    }

    fun login(name: String, phone: String, email: String) {
        repository.login(name, phone, email)
        _currentOverlay.value = ScreenOverlay.None
    }

    fun logout() {
        repository.logout()
        _currentOverlay.value = ScreenOverlay.Login
    }

    val firestoreRepository = com.example.data.FirestoreRepository()

    suspend fun submitPropertyLead(
        mobileNumber: String,
        propertyQuery: String,
        loanClientRequirements: String,
        clientName: String = ""
    ): Result<String> {
        return firestoreRepository.savePropertyLead(
            mobileNumber = mobileNumber,
            propertyQuery = propertyQuery,
            loanClientRequirements = loanClientRequirements,
            clientName = clientName
        )
    }

    companion object {
        const val BUY_OR_SALE = "Buy / Sale"
    }
}
