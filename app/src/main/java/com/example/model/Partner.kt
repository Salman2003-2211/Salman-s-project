package com.example.model

enum class MeetingStatus(val label: String) {
    PENDING("Pending Confirmation"),
    CONFIRMED("Confirmed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

data class Partner(
    val id: String,
    val name: String,
    val agencyName: String,
    val operatingAreas: List<String>,
    val specializations: List<String>,
    val rating: Double,
    val reviewCount: Int,
    val propertiesListedCount: Int,
    val dealsClosedCount: Int,
    val experienceYears: Int,
    val phone: String,
    val isVerified: Boolean = true
)

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
