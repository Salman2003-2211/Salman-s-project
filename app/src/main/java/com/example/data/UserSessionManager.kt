package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.UserProfile
import com.example.ui.theme.AppLanguage

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "spare_client_session_prefs"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_FULL_NAME = "key_full_name"
        private const val KEY_MOBILE_NUMBER = "key_mobile_number"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_IS_VERIFIED = "key_is_verified"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_SESSION_TOKEN = "key_session_token"
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun saveSession(
        name: String,
        phone: String,
        email: String,
        isVerified: Boolean = true,
        token: String = "token_${System.currentTimeMillis()}"
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_FULL_NAME, name.trim())
            putString(KEY_MOBILE_NUMBER, phone.trim())
            putString(KEY_EMAIL, email.trim())
            putBoolean(KEY_IS_VERIFIED, isVerified)
            putString(KEY_SESSION_TOKEN, token)
            apply()
        }
    }

    fun loadLanguage(): AppLanguage {
        val langStr = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        return try {
            AppLanguage.valueOf(langStr)
        } catch (e: Exception) {
            AppLanguage.ENGLISH
        }
    }

    fun loadProfile(): UserProfile? {
        if (!isLoggedIn()) return null
        val name = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val phone = prefs.getString(KEY_MOBILE_NUMBER, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val isVerified = prefs.getBoolean(KEY_IS_VERIFIED, true)
        val langStr = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        val lang = try {
            AppLanguage.valueOf(langStr)
        } catch (e: Exception) {
            AppLanguage.ENGLISH
        }
        return UserProfile(
            id = prefs.getString(KEY_USER_ID, "usr_client") ?: "usr_client",
            fullName = name,
            mobileNumber = phone,
            email = email,
            isVerified = isVerified,
            language = lang
        )
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putString(KEY_SESSION_TOKEN, null)
            apply()
        }
    }

    fun updateProfile(name: String, phone: String, email: String) {
        prefs.edit().apply {
            putString(KEY_FULL_NAME, name.trim())
            putString(KEY_MOBILE_NUMBER, phone.trim())
            putString(KEY_EMAIL, email.trim())
            apply()
        }
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }
}
