package com.example.data

import com.example.model.*
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SpareRepository(private val sessionManager: UserSessionManager? = null) {

    private val _isLoggedIn = MutableStateFlow(sessionManager?.isLoggedIn() ?: false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userProfile = MutableStateFlow(
        sessionManager?.loadProfile() ?: UserProfile(
            id = "usr_client",
            fullName = "",
            mobileNumber = "",
            email = "",
            isVerified = false,
            language = sessionManager?.loadLanguage() ?: AppLanguage.ENGLISH
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _properties = MutableStateFlow(SampleData.initialProperties)
    val properties: StateFlow<List<Property>> = _properties.asStateFlow()

    private val _leadInquiries = MutableStateFlow(SampleData.initialLeads)
    val leadInquiries: StateFlow<List<LeadInquiry>> = _leadInquiries.asStateFlow()

    private val _partners = MutableStateFlow(SampleData.initialPartners)
    val partners: StateFlow<List<Partner>> = _partners.asStateFlow()

    private val _bookedMeetings = MutableStateFlow(SampleData.initialMeetings)
    val bookedMeetings: StateFlow<List<BookedMeeting>> = _bookedMeetings.asStateFlow()

    private val _loanLeads = MutableStateFlow(SampleData.initialLoanLeads)
    val loanLeads: StateFlow<List<LoanLead>> = _loanLeads.asStateFlow()

    fun login(name: String, phone: String, email: String) {
        val cleanName = name.trim().ifBlank { "User" }
        _userProfile.update {
            it.copy(
                fullName = cleanName,
                mobileNumber = phone.trim(),
                email = email.trim(),
                isVerified = true
            )
        }
        _isLoggedIn.value = true
        sessionManager?.saveSession(
            name = cleanName,
            phone = phone.trim(),
            email = email.trim(),
            isVerified = true
        )
    }

    fun logout() {
        _isLoggedIn.value = false
        _userProfile.update {
            UserProfile(language = it.language)
        }
        sessionManager?.clearSession()
    }

    fun updateProfile(name: String, phone: String, email: String) {
        _userProfile.update {
            it.copy(
                fullName = name.trim(),
                mobileNumber = phone.trim(),
                email = email.trim()
            )
        }
        sessionManager?.updateProfile(name, phone, email)
    }

    fun setLanguage(language: AppLanguage) {
        _userProfile.update { it.copy(language = language) }
        sessionManager?.setLanguage(language)
    }

    fun toggleFavorite(propertyId: String) {
        _properties.update { list ->
            list.map {
                if (it.id == propertyId) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }

    fun postProperty(newProperty: Property) {
        _properties.update { listOf(newProperty) + it }
    }

    fun updateProperty(updated: Property) {
        _properties.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
    }

    fun updatePropertyStatus(propertyId: String, newStatus: PropertyStatus) {
        _properties.update { list ->
            list.map {
                if (it.id == propertyId) it.copy(status = newStatus) else it
            }
        }
    }

    fun deleteProperty(propertyId: String) {
        _properties.update { list ->
            list.filterNot { it.id == propertyId }
        }
    }

    fun updateLeadStatus(leadId: String, newStatus: InquiryStatus) {
        _leadInquiries.update { list ->
            list.map {
                if (it.id == leadId) it.copy(status = newStatus) else it
            }
        }
    }

    fun submitLoanApplication(
        loanType: LoanType,
        amountLakhs: Double,
        employmentType: String,
        monthlyIncome: String,
        propertyStatus: String,
        city: String,
        clientName: String,
        phone: String
    ): LoanLead {
        val ref = "LN-${(10000..99999).random()}"
        val lead = LoanLead(
            id = "loan_${System.currentTimeMillis()}",
            userId = _userProfile.value.id,
            clientName = clientName,
            phoneNumber = phone,
            loanTypeSelected = loanType,
            requiredAmountInLakhs = amountLakhs,
            employmentType = employmentType,
            monthlyIncome = monthlyIncome,
            propertyStatus = propertyStatus,
            city = city,
            submissionTimestamp = "Just now",
            status = LeadStatus.PENDING,
            referenceId = ref
        )
        _loanLeads.update { listOf(lead) + it }
        return lead
    }

    fun updateLoanLeadStatus(leadId: String, newStatus: LeadStatus) {
        _loanLeads.update { list ->
            list.map {
                if (it.id == leadId) it.copy(status = newStatus) else it
            }
        }
    }

    fun bookMeeting(
        partnerId: String,
        partnerName: String,
        agencyName: String,
        partnerPhone: String,
        date: String,
        timeSlot: String,
        interest: String
    ): BookedMeeting {
        val meeting = BookedMeeting(
            id = "meet_${System.currentTimeMillis()}",
            partnerId = partnerId,
            partnerName = partnerName,
            agencyName = agencyName,
            partnerPhone = partnerPhone,
            scheduledDate = date,
            timeSlot = timeSlot,
            propertyInterest = interest,
            status = MeetingStatus.CONFIRMED
        )
        _bookedMeetings.update { listOf(meeting) + it }
        return meeting
    }

    fun cancelMeeting(meetingId: String) {
        _bookedMeetings.update { list ->
            list.map {
                if (it.id == meetingId) it.copy(status = MeetingStatus.CANCELLED) else it
            }
        }
    }

    fun registerPartner(
        name: String,
        agencyName: String,
        operatingAreas: List<String>,
        specializations: List<String>,
        phone: String
    ) {
        val partner = Partner(
            id = "part_${System.currentTimeMillis()}",
            name = name,
            agencyName = agencyName,
            operatingAreas = operatingAreas,
            specializations = specializations,
            rating = 5.0,
            reviewCount = 1,
            propertiesListedCount = 1,
            dealsClosedCount = 0,
            experienceYears = 2,
            phone = phone,
            isVerified = true
        )
        _partners.update { listOf(partner) + it }
    }
}
