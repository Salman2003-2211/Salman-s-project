package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.example.ui.components.SmsGatewayHelper
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: SpareViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var emailId by remember { mutableStateOf("") }

    var isOtpStep by remember { mutableStateOf(false) }
    var enteredOtp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableStateOf(30) }
    var canResend by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showLegalDialog by remember { mutableStateOf<String?>(null) }
    var expectedOtpCode by remember { mutableStateOf<String?>(null) }
    var otpIssuedAt by remember { mutableStateOf(0L) }

    val scrollState = rememberScrollState()

    val secureRandom = remember { java.security.SecureRandom() }
    fun newOtp(): String = (100000 + secureRandom.nextInt(900000)).toString()

    // The OTP is delivered as a notification. On Android 13+ the POST_NOTIFICATIONS permission
    // must be granted at runtime, otherwise the code is silently dropped and login is impossible.
    var pendingSendAfterPermission by remember { mutableStateOf(false) }

    fun sendOtp() {
        isSendingOtp = true
        val generatedCode = newOtp()
        expectedOtpCode = generatedCode
        otpIssuedAt = System.currentTimeMillis()
        coroutineScope.launch {
            delay(350)
            SmsGatewayHelper.dispatchSmsOtp(context, mobileNumber, generatedCode)
            isSendingOtp = false
            isOtpStep = true
            canResend = false
            enteredOtp = ""
            infoMessage = "Verification OTP sent to +91 $mobileNumber via SMS. Please check your SMS inbox."
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            errorMessage = "Please allow notifications for S.P.A.RE so the OTP can be delivered, then try again."
            pendingSendAfterPermission = false
        } else if (pendingSendAfterPermission) {
            pendingSendAfterPermission = false
            sendOtp()
        }
    }

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // Countdown timer for OTP resend
    LaunchedEffect(isOtpStep, canResend) {
        if (isOtpStep && !canResend) {
            resendCooldown = 30
            while (resendCooldown > 0) {
                delay(1000L)
                resendCooldown--
            }
            canResend = true
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

            // Warm Greeting
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
                            text = "Please enter your name and 10-digit mobile number.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        // Full Name Field (Mandatory for client setup)
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
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mobile Number Field
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
                                unfocusedTextColor = Color.White
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
                            onValueChange = { emailId = it },
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
                                unfocusedTextColor = Color.White
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

                        // Continue / Get OTP Button
                        Button(
                            onClick = {
                                if (fullName.trim().length < 2) {
                                    errorMessage = "Please enter your full name"
                                    return@Button
                                }
                                // Indian mobile numbers start with 6-9
                                if (mobileNumber.length != 10 || mobileNumber[0] !in '6'..'9') {
                                    errorMessage = "Please enter a valid 10-digit mobile number"
                                    return@Button
                                }
                                if (emailId.isNotBlank() &&
                                    !android.util.Patterns.EMAIL_ADDRESS.matcher(emailId.trim()).matches()
                                ) {
                                    errorMessage = "Please enter a valid email address or leave it blank"
                                    return@Button
                                }
                                errorMessage = null
                                if (!hasNotificationPermission()) {
                                    pendingSendAfterPermission = true
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@Button
                                }
                                sendOtp()
                            },
                            enabled = !isSendingOtp,
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
                                Text("Get OTP via SMS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                }
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Mobile Number", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = "OTP sent to +91 $mobileNumber",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                        )

                        // Status info card (SMS delivered notification banner)
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
                        Text("Enter OTP Code", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFFCBD5E1))
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

                        // Hidden real input field for keyboard entry
                        OutlinedTextField(
                            value = enteredOtp,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    enteredOtp = it
                                    errorMessage = null
                                }
                            },
                            placeholder = { Text("Type numbers here", color = Color(0xFF64748B), fontSize = 12.sp) },
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
                                unfocusedTextColor = Color.White
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

                        // Verify Button
                        Button(
                            onClick = {
                                if (enteredOtp.length < 6) {
                                    errorMessage = "Please enter the complete 6-digit OTP code"
                                    return@Button
                                }
                                isVerifyingOtp = true
                                errorMessage = null

                                coroutineScope.launch {
                                    val exp = expectedOtpCode
                                    val clientName = fullName.trim().ifBlank { "Client" }

                                    delay(300)
                                    val expired = System.currentTimeMillis() - otpIssuedAt > 10 * 60 * 1000L
                                    if (exp != null && expired) {
                                        isVerifyingOtp = false
                                        errorMessage = "This OTP has expired. Please request a new one."
                                    } else if (exp != null && enteredOtp == exp) {
                                        isVerifyingOtp = false
                                        expectedOtpCode = null
                                        viewModel.login(clientName, mobileNumber, emailId.trim())
                                        Toast.makeText(context, "Welcome, $clientName! Account verified.", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        isVerifyingOtp = false
                                        errorMessage = "Incorrect OTP code. Please enter the valid 6-digit code received via SMS."
                                    }
                                }
                            },
                            enabled = !isVerifyingOtp && enteredOtp.length == 6,
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

                        // Resend OTP / Change Number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (canResend) {
                                        canResend = false
                                        val newCode = newOtp()
                                        expectedOtpCode = newCode
                                        otpIssuedAt = System.currentTimeMillis()
                                        enteredOtp = ""
                                        SmsGatewayHelper.dispatchSmsOtp(context, mobileNumber, newCode)
                                        infoMessage = "New verification OTP sent to +91 $mobileNumber via SMS."
                                    }
                                },
                                enabled = canResend
                            ) {
                                Text(
                                    if (canResend) "Resend SMS OTP" else "Resend in ${resendCooldown}s",
                                    color = if (canResend) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(
                                onClick = {
                                    isOtpStep = false
                                    enteredOtp = ""
                                    errorMessage = null
                                }
                            ) {
                                Text("Change Number", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Guest Explore Option
            OutlinedButton(
                onClick = {
                    viewModel.login("Rahul Rajpoot", "9818452198", "rahul.rajpoot@example.com")
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_guest_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF93C5FD)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)))
                )
            ) {
                Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore as Demo User (Instant Access)", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

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
