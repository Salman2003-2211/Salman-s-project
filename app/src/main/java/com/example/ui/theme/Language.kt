package com.example.ui.theme

enum class AppLanguage(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    HINDI("हिंदी (Hindi)", "hi")
}

object AppStrings {
    fun welcomeHeader(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Welcome to S.P.A.RE"
        AppLanguage.HINDI -> "S.P.A.RE में आपका स्वागत है"
    }

    fun tagline(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Your Trusted Partner in Finding the Perfect Property"
        AppLanguage.HINDI -> "सर्वश्रेष्ठ प्रॉपर्टी पाने में आपका विश्वसनीय साथी"
    }

    fun profile(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Profile"
        AppLanguage.HINDI -> "प्रोफाइल"
    }

    fun postProperty(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Post Your Property"
        AppLanguage.HINDI -> "प्रॉपर्टी पोस्ट करें"
    }

    fun myAds(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "My Ads"
        AppLanguage.HINDI -> "मेरे विज्ञापन"
    }

    fun about(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "About Us"
        AppLanguage.HINDI -> "हमारे बारे में"
    }

    fun meetPartner(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Meet Partner"
        AppLanguage.HINDI -> "पार्टनर से मिलें"
    }

    fun searchProperty(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Search Property"
        AppLanguage.HINDI -> "प्रॉपर्टी खोजें"
    }

    fun loan(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Loan Assistance"
        AppLanguage.HINDI -> "लोन सहायता"
    }

    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Search by city, locality, project..."
        AppLanguage.HINDI -> "शहर, इलाका या प्रोजेक्ट खोजें..."
    }

    fun buy(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Buy / Sale"
        AppLanguage.HINDI -> "खरीदें / बेचें"
    }

    fun rent(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Rent"
        AppLanguage.HINDI -> "किराया"
    }

    fun plot(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Plot / Land"
        AppLanguage.HINDI -> "प्लॉट / जमीन"
    }

    fun commercial(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Commercial"
        AppLanguage.HINDI -> "व्यावसायिक"
    }

    fun editProfile(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Edit Profile"
        AppLanguage.HINDI -> "प्रोफाइल एडिट करें"
    }

    fun logout(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Log Out"
        AppLanguage.HINDI -> "लॉग आउट करें"
    }

    fun language(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Language Preference"
        AppLanguage.HINDI -> "भाषा का चयन"
    }
}
