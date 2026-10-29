package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SpareViewModel
import com.example.ui.components.PhoneAuthHelper
import com.example.ui.theme.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LoginScreen(
    viewModel: SpareViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }

    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var emailId by remember { mutableStateOf("") }

    var isOtpStep by remember { mutableStateOf(false) }
    var enteredOtp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showLegalDialog by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Countdown timer for OTP resend (60 seconds)
    LaunchedEffect(isOtpStep, canResend) {
        if (isOtpStep && !canResend) {
            resendCooldown = 60
            while (resendCooldown > 0) {
                delay(1000L)
                resendCooldown--
            }
            canResend = true
        }
    }

    fun triggerPhoneVerification(isResend: Boolean = false) {
        val activity = context.findActivity()
        if (activity == null) {
            errorMessage = "Unable to start phone verification: Activity context not available."
            return
        }

        val formattedPhone = if (mobileNumber.startsWith("+")) mobileNumber else "+91$mobileNumber"
        isSendingOtp = true
        errorMessage = null

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Instant SMS verification (auto-retrieval / instant check)
                isSendingOtp = false
                isVerifyingOtp = true
                coroutineScope.launch {
                    try {
                        val authResult = auth.signInWithCredential(credential).await()
                        if (authResult.user != null) {
                            viewModel.onAuthSuccess(fullName, mobileNumber, emailId)
                            Toast.makeText(
                                context,
                                "Welcome, ${fullName.trim()}! Automatically verified.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onLoginSuccess()
                        } else {
                            errorMessage = "Instant verification failed. Please enter the OTP manually."
                        }
                    } catch (e: Exception) {
                        errorMessage = PhoneAuthHelper.formatErrorMessage(e)
                    } finally {
                        isVerifyingOtp = false
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isSendingOtp = false
                if (isResend) canResend = true
                errorMessage = PhoneAuthHelper.formatErrorMessage(e)
                Toast.makeText(context, errorMessage ?: "Verification failed", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                vId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = vId
                resendToken = token
                isSendingOtp = false
                isOtpStep = true
                canResend = false
                enteredOtp = ""
                infoMessage = "Verification OTP sent to +91 $mobileNumber via SMS. Please enter the 6-digit code."
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (isResend && resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken!!)
        }

        try {
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            isSendingOtp = false
            if (isResend) canResend = true
            errorMessage = PhoneAuthHelper.formatErrorMessage(e)
            Toast.makeText(context, errorMessage ?: "Verification failed", Toast.LENGTH_LONG).show()
        }
    }

    fun verifyOtpAndLogin() {
        if (enteredOtp.length < 6) {
            errorMessage = "Please enter the complete 6-digit OTP code"
            return
        }
        val currentVerificationId = verificationId
        if (currentVerificationId.isNullOrBlank()) {
            errorMessage = "Verification session expired. Please tap 'Resend SMS OTP' to request a new code."
            return
        }

        isVerifyingOtp = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val credential = PhoneAuthProvider.getCredential(currentVerificationId, enteredOtp)
                val authResult = auth.signInWithCredential(credential).await()
                if (authResult.user != null) {
                    viewModel.onAuthSuccess(fullName, mobileNumber, emailId)
                    Toast.makeText(
                        context,
                        "Welcome, ${fullName.trim()}! Account verified successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                    onLoginSuccess()
                } else {
                    errorMessage = "Verification failed. Could not authenticate user."
                }
            } catch (e: Exception) {
                errorMessage = PhoneAuthHelper.formatErrorMessage(e)
            } finally {
                isVerifyingOtp = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Branding & Architectural Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                        )
                    )
                    .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Apartment,
                    contentDescription = "S.P.A.RE Logo",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Greeting Header
            Text(
                text = if (!isOtpStep) "Create Account / Login" else "Verification Code",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = if (!isOtpStep) "Enter your details to get started" else "Verify your mobile number to continue",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    if (!isOtpStep) {
                        Text(
                            text = "Client Registration",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Please enter your full name and 10-digit mobile number.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        // Full Name Field (Mandatory)
                        Text(
                            text = "Your Full Name *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                errorMessage = null
                            },
                            enabled = !isSendingOtp,
                            placeholder = {
                                Text("Enter your full name", fontSize = 13.sp, color = Color(0xFF64748B))
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = "Name", tint = Color(0xFF94A3B8))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mobile Number Field (Mandatory)
                        Text(
                            text = "Mobile Number *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = {
                                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                    mobileNumber = it
                                    errorMessage = null
                                }
                            },
                            enabled = !isSendingOtp,
                            placeholder = {
                                Text("10-digit mobile number", fontSize = 13.sp, color = Color(0xFF64748B))
                            },
                            prefix = {
                                Text("+91 ", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Phone, contentDescription = "Phone", tint = Color(0xFF94A3B8))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Email Field (Optional)
                        Text(
                            text = "Email ID (Optional)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = emailId,
                            onValueChange = {
                                emailId = it
                                errorMessage = null
                            },
                            enabled = !isSendingOtp,
                            placeholder = {
                                Text("Optional email address", fontSize = 13.sp, color = Color(0xFF64748B))
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = "Email", tint = Color(0xFF94A3B8))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Send OTP / Get OTP Button
                        Button(
                            onClick = {
                                if (fullName.trim().length < 2) {
                                    errorMessage = "Please enter your full name (minimum 2 characters)"
                                    return@Button
                                }
                                if (mobileNumber.length != 10 || mobileNumber[0] !in '6'..'9') {
                                    errorMessage = "Please enter a valid 10-digit Indian mobile number"
                                    return@Button
                                }
                                if (emailId.isNotBlank() &&
                                    !android.util.Patterns.EMAIL_ADDRESS.matcher(emailId.trim()).matches()
                                ) {
                                    errorMessage = "Please enter a valid email address or leave it blank"
                                    return@Button
                                }
                                errorMessage = null
                                triggerPhoneVerification(isResend = false)
                            },
                            enabled = !isSendingOtp && !isVerifyingOtp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("get_otp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB)
                            )
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sending OTP...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Text("Send OTP via SMS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        // OTP Verification Step
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enter Verification Code",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = {
                                    isOtpStep = false
                                    enteredOtp = ""
                                    errorMessage = null
                                    infoMessage = null
                                },
                                enabled = !isVerifyingOtp && !isSendingOtp
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit Mobile Number",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "OTP sent to +91 $mobileNumber",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                        )

                        // Status info banner
                        if (infoMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F2537),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Sms,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        infoMessage!!,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE0F2FE)
                                    )
                                }
                            }
                        }

                        // Modern 6-digit OTP Box Visuals
                        Text("Enter 6-Digit OTP Code", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 6) {
                                val digit = enteredOtp.getOrNull(i)?.toString() ?: ""
                                val isFocused = enteredOtp.length == i || (i == 5 && enteredOtp.length == 6)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(
                                            width = if (isFocused) 2.dp else 1.dp,
                                            color = when {
                                                digit.isNotEmpty() -> Color(0xFF10B981)
                                                isFocused -> Color(0xFF38BDF8)
                                                else -> Color(0xFF334155)
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = digit,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Real keyboard input field
                        OutlinedTextField(
                            value = enteredOtp,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    enteredOtp = it
                                    errorMessage = null
                                }
                            },
                            enabled = !isVerifyingOtp && !isSendingOtp,
                            placeholder = { Text("Type 6 digits here", color = Color(0xFF64748B), fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF94A3B8))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Verify & Login Button
                        Button(
                            onClick = { verifyOtpAndLogin() },
                            enabled = !isVerifyingOtp && !isSendingOtp && enteredOtp.length == 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("verify_otp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            if (isVerifyingOtp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify & Login", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Resend OTP / Change Number Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (canResend) {
                                        triggerPhoneVerification(isResend = true)
                                    }
                                },
                                enabled = canResend && !isSendingOtp && !isVerifyingOtp
                            ) {
                                Text(
                                    text = when {
                                        isSendingOtp -> "Resending..."
                                        canResend -> "Resend SMS OTP"
                                        else -> "Resend in ${resendCooldown}s"
                                    },
                                    color = if (canResend && !isSendingOtp && !isVerifyingOtp) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(
                                onClick = {
                                    isOtpStep = false
                                    enteredOtp = ""
                                    errorMessage = null
                                    infoMessage = null
                                },
                                enabled = !isVerifyingOtp && !isSendingOtp
                            ) {
                                Text("Change Number", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Disclaimer Links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By continuing, you agree to our ",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Terms",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.clickable { showLegalDialog = "Terms & Conditions" }
                )
                Text(
                    text = " & ",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Privacy Policy",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.clickable { showLegalDialog = "Privacy Policy" }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Legal Modal Dialog
        if (showLegalDialog != null) {
            AlertDialog(
                onDismissRequest = { showLegalDialog = null },
                title = { Text(showLegalDialog!!, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "S.P.A.RE ensures end-to-end user privacy, verified agent connections, zero spam policy, and transparent property & loan facilitation adhering to Indian Real Estate (RERA) compliance standards.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showLegalDialog = null }) {
                        Text("Understood")
                    }
                }
            )
        }
    }
}
