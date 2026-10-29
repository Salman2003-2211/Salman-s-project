package com.example.model

enum class MeetingStatus(val label: String) {
    PENDING("Pending Confirmation"),
    CONFIRMED("Confirmed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

data class Partner(
    val id: String,
    val userId: String = "",
    val name: String,
    val businessName: String = "",
    val agencyName: String = businessName,
    val phone: String,
    val email: String = "",
    val operatingAreas: List<String> = emptyList(),
    val areas: List<String> = operatingAreas,
    val dealTypes: List<String> = emptyList(),
    val specializations: List<String> = dealTypes,
    val experienceYears: Int = 0,
    val documentUrls: List<String> = emptyList(),
    val status: String = "APPROVED", // "PENDING_APPROVAL", "APPROVED", "REJECTED"
    val rating: Double = 4.8,
    val reviewCount: Int = 12,
    val propertiesListedCount: Int = 5,
    val dealsClosedCount: Int = 20,
    val isVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null
) {
    val displayBusinessName: String
        get() = businessName.ifBlank { agencyName }.ifBlank { "Independent Broker" }

    val displayAreas: List<String>
        get() = if (areas.isNotEmpty()) areas else operatingAreas

    val displayDealTypes: List<String>
        get() = if (dealTypes.isNotEmpty()) dealTypes else specializations
}

data class BookedMeeting(
    val id: String,
    val partnerId: String,
    val partnerName: String,
    val agencyName: String,
    val partnerPhone: String,
    val scheduledDate: String,
    val timeSlot: String,
    val propertyInterest: String,
    val status: MeetingStatus = MeetingStatus.PENDING
)
