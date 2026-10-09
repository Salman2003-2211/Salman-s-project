package com.example.ui.components

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

object PhoneAuthHelper {
    private const val TAG = "PhoneAuthHelper"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    var storedVerificationId: String? = null
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationCompleted: (credential: PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d(TAG, "Instant verification completed")
                        onVerificationCompleted(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e(TAG, "Verification failed: ${e.message}", e)
                        onVerificationFailed(e)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d(TAG, "onCodeSent: $verificationId")
                        storedVerificationId = verificationId
                        resendToken = token
                        onCodeSent(verificationId)
                    }
                })

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "verifyPhoneNumber call failed", e)
            onVerificationFailed(e)
        }
    }

    fun resendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationCompleted: (credential: PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        try {
            val token = resendToken
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        onVerificationCompleted(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        onVerificationFailed(e)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        storedVerificationId = verificationId
                        resendToken = token
                        onCodeSent(verificationId)
                    }
                })

            if (token != null) {
                optionsBuilder.setForceResendingToken(token)
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "resendVerificationCode call failed", e)
            onVerificationFailed(e)
        }
    }

    suspend fun verifyCode(
        verificationId: String,
        code: String
    ): Result<com.google.firebase.auth.FirebaseUser?> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = auth.signInWithCredential(credential).await()
            Result.success(authResult.user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in with credential failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
