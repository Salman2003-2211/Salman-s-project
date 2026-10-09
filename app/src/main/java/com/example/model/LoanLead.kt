package com.example.model

enum class LoanType(val title: String, val subtitle: String) {
    LAP(
        "Loan Against Property (LAP)",
        "Unlock property value for business or personal needs"
    ),
    HOME_LOAN(
        "Home Loan",
        "Low interest rates for buying your dream home"
    ),
    PURCHASE_5L(
        "Purchase at ₹5 Lakh",
        "Micro/budget property purchase & personal funding option"
    )
}

enum class LeadStatus(val label: String) {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

data class LoanLead(
    val id: String,
    val userId: String,
    val clientName: String,
    val phoneNumber: String,
    val loanTypeSelected: LoanType,
    val requiredAmountInLakhs: Double,
    val employmentType: String, // "Salaried", "Self-Employed", "Business Owner"
    val monthlyIncome: String,
    val propertyStatus: String, // "Shortlisted", "Already Owned", "Still Searching"
    val city: String,
    val submissionTimestamp: String,
    val status: LeadStatus = LeadStatus.PENDING,
    val referenceId: String = "LN-${(10000..99999).random()}"
)
