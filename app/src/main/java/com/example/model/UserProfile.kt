package com.example.model

import com.example.ui.theme.AppLanguage

data class UserProfile(
    val id: String = "usr_001",
    val fullName: String = "Rahul Rajpoot",
    val mobileNumber: String = "9818452198",
    val email: String = "rahul.rajpoot@example.com",
    val isVerified: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH
)
