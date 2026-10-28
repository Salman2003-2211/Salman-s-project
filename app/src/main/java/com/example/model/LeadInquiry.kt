package com.example.model

enum class InquiryType(val label: String) {
    CALLBACK("Requested Callback"),
    MESSAGE("Sent Message"),
    SITE_VISIT("Scheduled Site Visit")
}

enum class InquiryStatus(val label: String) {
    NEW_LEAD("New Lead"),
    CONTACTED("Contacted"),
    SITE_VISIT_SCHEDULED("Site Visit Scheduled"),
    DEAL_CLOSED("Deal Closed"),
    NOT_INTERESTED("Not Interested")
}

data class LeadInquiry(
    val id: String,
    val propertyId: String,
    val propertyTitle: String,
    val clientName: String,
    val clientPhone: String,
    val clientEmail: String = "",
    val inquiryType: InquiryType,
    val inquiryTime: String,
    val status: InquiryStatus = InquiryStatus.NEW_LEAD,
    val clientNote: String = "Interested in scheduling an inspection this weekend."
)
