package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PropertyStorageHelper
import com.example.data.SpareRepository
import com.example.data.UserSessionManager
import com.example.model.*
import com.example.ui.theme.AppLanguage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
}

class SpareViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sessionManager = UserSessionManager(application.applicationContext)
    val repository: SpareRepository = SpareRepository(sessionManager)

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

    private val _selectedPostedBy = MutableStateFlow("All")
    val selectedPostedBy: StateFlow<String> = _selectedPostedBy.asStateFlow()

    private val _selectedAmenities = MutableStateFlow<Set<String>>(emptySet())
    val selectedAmenities: StateFlow<Set<String>> = _selectedAmenities.asStateFlow()

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
        if (_selectedPurpose.value != purpose) _selectedBudgetRange.value = "All"
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

    fun setSelectedPostedBy(postedBy: String) {
        _selectedPostedBy.value = postedBy
    }

    fun setSelectedAmenities(amenities: Set<String>) {
        _selectedAmenities.value = amenities
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
        _selectedPostedBy.value = "All"
        _selectedAmenities.value = emptySet()
        _verifiedOnly.value = false
        _sortBy.value = "Relevance"
    }

    fun toggleFavorite(propertyId: String, context: Context? = null) {
        val currentProperty = properties.value.find { it.id == propertyId }
        val willBeFavorite = !(currentProperty?.isFavorite ?: false)

        // 1. Instant local visual feedback
        repository.toggleFavorite(propertyId)

        // 2. Persist lead inquiry and favorites sync to Firestore
        viewModelScope.launch {
            if (willBeFavorite && currentProperty != null) {
                val profile = userProfile.value
                val clientName = profile.fullName.ifBlank { "Client" }
                val clientPhone = profile.mobileNumber.ifBlank { "9818452198" }
                val clientEmail = profile.email

                firestoreRepository.recordPropertyInterestLead(
                    property = currentProperty,
                    clientName = clientName,
                    clientPhone = clientPhone,
                    clientEmail = clientEmail
                )

                if (context != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Marked as Interested! The owner/admin will get in touch.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (!willBeFavorite) {
                firestoreRepository.removePropertyFavorite(propertyId)
            }
        }
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
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        _currentOverlay.value = ScreenOverlay.Login
    }

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestoreRepository = com.example.data.FirestoreRepository(application.applicationContext)

    /**
     * Completes phone authentication: updates local session state,
     * updates Firebase Auth displayName, and persists the user profile in Firestore.
     */
    suspend fun onAuthSuccess(
        fullName: String,
        phone: String,
        email: String = ""
    ): Result<Unit> {
        val cleanName = fullName.trim().ifBlank { "User" }
        val cleanPhone = if (phone.startsWith("+")) phone else "+91$phone"
        val cleanEmail = email.trim()

        // 1. Update local repository & session manager
        login(cleanName, cleanPhone, cleanEmail)

        // 2. Update Firebase Auth displayName
        try {
            auth.currentUser?.let { user ->
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                user.updateProfile(profileUpdates).await()
            }
        } catch (e: Exception) {
            android.util.Log.w("SpareViewModel", "Failed to update Firebase Auth displayName: ${e.message}")
        }

        // 3. Save profile to Firestore users collection
        return try {
            firestoreRepository.saveUserProfile(
                fullName = cleanName,
                phone = cleanPhone,
                email = cleanEmail,
                language = userProfile.value.language.code
            )
        } catch (e: Exception) {
            android.util.Log.e("SpareViewModel", "Failed to save user profile to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

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

    private val _myPartnerApplication = MutableStateFlow<Partner?>(null)
    val myPartnerApplication: StateFlow<Partner?> = _myPartnerApplication.asStateFlow()

    init {
        // Sync active properties from Firestore in real-time (expiresAt > now && !isSold)
        viewModelScope.launch {
            try {
                firestoreRepository.listenActiveProperties().collect { remoteProps ->
                    repository.syncPropertiesFromFirestore(remoteProps)
                }
            } catch (e: Exception) {
                android.util.Log.w("SpareViewModel", "Error listening to active properties: ${e.message}")
            }
        }

        // Restore saved favorites from Firestore for current user
        viewModelScope.launch {
            try {
                val savedFavIds = firestoreRepository.loadUserFavorites().toSet()
                if (savedFavIds.isNotEmpty()) {
                    repository.syncFavoriteIds(savedFavIds)
                }
            } catch (e: Exception) {
                android.util.Log.w("SpareViewModel", "Error restoring saved favorites: ${e.message}")
            }
        }

        // Sync approved partners in real-time for client feed
        viewModelScope.launch {
            try {
                firestoreRepository.listenApprovedPartners().collect { approvedPartners ->
                    repository.syncPartnersFromFirestore(approvedPartners)
                }
            } catch (e: Exception) {
                android.util.Log.w("SpareViewModel", "Error listening to approved partners: ${e.message}")
            }
        }

        // Fetch current user's dealer application status if exists
        viewModelScope.launch {
            try {
                val myApp = firestoreRepository.getMyPartnerApplication()
                if (myApp != null) {
                    _myPartnerApplication.value = myApp
                }
            } catch (e: Exception) {
                android.util.Log.w("SpareViewModel", "Error checking partner application: ${e.message}")
            }
        }
    }

    suspend fun registerPartner(
        context: Context,
        name: String,
        businessName: String,
        phone: String,
        email: String,
        areas: List<String>,
        dealTypes: List<String>,
        experienceYears: Int,
        documents: List<com.example.ui.screens.PropertyPhotoItem> = emptyList()
    ): Result<Partner> {
        val uid = auth.currentUser?.uid ?: userProfile.value.id.ifBlank { "partner" }
        val partnerId = "part_${System.currentTimeMillis()}"

        // 1. Upload documents / visiting cards / photo to Firebase Storage
        // Path: partners/{partnerId}_{partnerName}/{fileName}.jpg
        val documentUrls = PropertyStorageHelper.uploadPartnerDocuments(
            context = context,
            partnerId = partnerId,
            partnerName = name,
            photos = documents
        )

        val partner = Partner(
            id = partnerId,
            userId = uid,
            name = name.trim(),
            businessName = businessName.trim(),
            agencyName = businessName.trim(),
            phone = phone.trim(),
            email = email.trim(),
            operatingAreas = areas,
            areas = areas,
            dealTypes = dealTypes,
            specializations = dealTypes,
            experienceYears = experienceYears,
            documentUrls = documentUrls,
            status = "PENDING_APPROVAL",
            rating = 5.0,
            reviewCount = 0,
            propertiesListedCount = 0,
            dealsClosedCount = 0,
            isVerified = false,
            createdAt = System.currentTimeMillis()
        )

        val result = firestoreRepository.registerPartner(partner)
        return if (result.isSuccess) {
            _myPartnerApplication.value = partner
            Result.success(partner)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to register partner"))
        }
    }

    suspend fun postNewProperty(
        context: Context,
        property: Property,
        photos: List<com.example.ui.screens.PropertyPhotoItem>,
        onProgress: ((uploaded: Int, total: Int) -> Unit)? = null
    ): Result<Property> {
        val purpose = property.listingType.toStoragePurpose()
        val ownerId = auth.currentUser?.uid ?: userProfile.value.id.ifBlank { "owner" }
        val ownerName = userProfile.value.fullName.ifBlank { "Owner" }

        // 1. Upload photos to organized Firebase Storage folder:
        // properties/{ownerId}_{ownerName}/{purpose}_{propertyId}/{filename}.jpg
        val uploadedUrls = PropertyStorageHelper.uploadPropertyPhotos(
            context = context,
            ownerId = ownerId,
            ownerName = ownerName,
            purpose = purpose,
            propertyId = property.id,
            photos = photos,
            onProgress = onProgress
        )

        val updatedProperty = property.copy(
            ownerId = ownerId,
            ownerName = ownerName,
            ownerPhone = userProfile.value.mobileNumber,
            imageUrls = uploadedUrls,
            imageUrl = uploadedUrls.firstOrNull() ?: property.imageUrl,
            isSold = false,
            status = PropertyStatus.ACTIVE,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000L
        )

        // 2. Save property details in Cloud Firestore
        firestoreRepository.savePropertyListing(updatedProperty)

        // 3. Update local repository state so feeds update immediately
        repository.postProperty(updatedProperty)

        return Result.success(updatedProperty)
    }

    companion object {
        const val BUY_OR_SALE = "Buy / Sale"
    }
}
