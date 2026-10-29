package com.example.ui.components

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

object PhoneAuthHelper {
    private const val TAG = "PhoneAuthHelper"
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    var storedVerificationId: String? = null
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    /**
     * Starts phone number verification with Firebase Phone Authentication.
     */
    fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (credential: PhoneAuthCredential) -> Unit,
        onVerificationFailed: (FirebaseException) -> Unit
    ) {
        try {
            val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d(TAG, "Instant phone verification completed")
                        onVerificationCompleted(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e(TAG, "Phone verification failed: ${e.message}", e)
                        onVerificationFailed(e)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d(TAG, "OTP Code sent with verificationId: $verificationId")
                        storedVerificationId = verificationId
                        resendToken = token
                        onCodeSent(verificationId, token)
                    }
                })

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "verifyPhoneNumber call failed", e)
            val firebaseEx = (e as? FirebaseException) ?: FirebaseException(e.message ?: "Phone verification failed")
            onVerificationFailed(firebaseEx)
        }
    }

    /**
     * Resends SMS verification code using the saved ForceResendingToken.
     */
    fun resendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken? = resendToken,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (credential: PhoneAuthCredential) -> Unit,
        onVerificationFailed: (FirebaseException) -> Unit
    ) {
        try {
            val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d(TAG, "Instant verification completed on resend")
                        onVerificationCompleted(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e(TAG, "Resend verification failed: ${e.message}", e)
                        onVerificationFailed(e)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        newToken: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d(TAG, "Resent OTP Code with verificationId: $verificationId")
                        storedVerificationId = verificationId
                        resendToken = newToken
                        onCodeSent(verificationId, newToken)
                    }
                })

            if (token != null) {
                optionsBuilder.setForceResendingToken(token)
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "resendVerificationCode call failed", e)
            val firebaseEx = (e as? FirebaseException) ?: FirebaseException(e.message ?: "Resend verification failed")
            onVerificationFailed(firebaseEx)
        }
    }

    /**
     * Signs in with the given PhoneAuthCredential.
     */
    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<FirebaseUser?> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            Result.success(authResult.user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in with credential failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a credential with verificationId and code and signs in.
     */
    suspend fun verifyCode(
        verificationId: String,
        code: String
    ): Result<FirebaseUser?> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithCredential(credential)
        } catch (e: Exception) {
            Log.e(TAG, "verifyCode failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Returns friendly human-readable error messages for common Firebase Phone Auth errors.
     */
    fun formatErrorMessage(e: Throwable): String {
        val msg = e.message ?: return "Verification failed. Please try again."
        return when {
            msg.contains("The SMS verification code", ignoreCase = true) ||
            msg.contains("ERROR_INVALID_VERIFICATION_CODE", ignoreCase = true) ||
            msg.contains("invalid-verification-code", ignoreCase = true) ->
                "Incorrect OTP code. Please enter the valid 6-digit code received via SMS."

            msg.contains("ERROR_SESSION_EXPIRED", ignoreCase = true) ||
            msg.contains("session-expired", ignoreCase = true) ->
                "The OTP has expired. Please tap 'Resend SMS OTP' to request a new code."

            msg.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) ||
            msg.contains("too-many-requests", ignoreCase = true) ||
            msg.contains("quota exceeded", ignoreCase = true) ->
                "SMS quota exceeded or too many attempts. Please try again later."

            msg.contains("invalid phone number", ignoreCase = true) ||
            msg.contains("invalid-phone-number", ignoreCase = true) ->
                "Invalid phone number. Please enter a valid 10-digit mobile number."

            msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
            msg.contains("app-not-authorized", ignoreCase = true) ->
                "Phone authentication is not configured in Firebase Console. Please verify SHA-1 fingerprint."

            msg.contains("network", ignoreCase = true) ->
                "Network error. Please check your internet connection and try again."

            else -> e.localizedMessage ?: "Verification failed. Please try again."
        }
    }
}
