package com.example.model

import com.example.ui.theme.AppLanguage

data class UserProfile(
    val id: String = "usr_client",
    val fullName: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val isVerified: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH
)
