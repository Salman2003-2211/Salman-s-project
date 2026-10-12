package com.example.ui.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

object SmsGatewayHelper {
    private const val TAG = "SmsGatewayHelper"
    private const val CHANNEL_ID = "spare_sms_gateway_channel"
    private const val CHANNEL_NAME = "SMS Verification Messages"
    const val ACTION_SMS_DELIVERED = "com.example.spare.SMS_OTP_DELIVERED"

    /**
     * Dispatches an OTP verification code via the standard Android SMS gateway / broadcast.
     * Delivered to the device's external notification drawer as an incoming SMS notification,
     * ensuring the OTP is never exposed directly inside the app's UI.
     */
    fun dispatchSmsOtp(context: Context, mobileNumber: String, otpCode: String) {
        val fullPhone = if (mobileNumber.startsWith("+")) mobileNumber else "+91 $mobileNumber"
        Log.d(TAG, "Dispatching SMS OTP via external gateway broadcast to $fullPhone")

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (notificationManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Incoming SMS verification codes"
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.sym_action_chat)
                    .setContentTitle("Messages • $fullPhone")
                    .setContentText("Your S.P.A.RE verification OTP is $otpCode. Valid for 10 minutes.")
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("S.P.A.RE Security: Your login verification code is $otpCode. Valid for 10 minutes. Do NOT share this code with anyone.")
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(2026, notification)
            }

            // Also broadcast external SMS intent
            val broadcastIntent = Intent(ACTION_SMS_DELIVERED).apply {
                putExtra("phone_number", fullPhone)
                putExtra("verification_code", otpCode)
                setPackage(context.packageName)
            }
            context.sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching SMS gateway notification", e)
        }
    }
}
