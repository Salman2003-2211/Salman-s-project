package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LeadStatus
import com.example.model.LoanLead
import com.example.model.LoanType
import com.example.ui.AppTab
import com.example.ui.SpareViewModel
import com.example.ui.components.IntentHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    viewModel: SpareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val loanLeads by viewModel.loanLeads.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) } // 1: Type Selection, 2: Form, 3: Success
    var selectedLoanType by remember { mutableStateOf(LoanType.HOME_LOAN) }
    var loanAmountLakhs by remember { mutableFloatStateOf(35f) }
    var employmentType by remember { mutableStateOf("Salaried") }
    var monthlyIncome by remember { mutableStateOf("₹85,000 / month") }
    var propertyStatus by remember { mutableStateOf("Shortlisted") }
    var city by remember { mutableStateOf("Noida") }
    var lastSubmittedLead by remember { mutableStateOf<LoanLead?>(null) }
    var showOperatorDashboard by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showOperatorDashboard) "Operator CRM Portal" else "Loan Assistance",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Operator / Admin CRM Toggle Button
                    TextButton(
                        onClick = { showOperatorDashboard = !showOperatorDashboard },
                        modifier = Modifier.testTag("loan_operator_toggle")
                    ) {
                        Icon(
                            imageVector = if (showOperatorDashboard) Icons.Filled.Person else Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showOperatorDashboard) "User View" else "Admin CRM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (currentStep > 1 && !showOperatorDashboard) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (showOperatorDashboard) {
            // Operator / Admin CRM Dashboard
            OperatorCrmView(
                loanLeads = loanLeads,
                onUpdateStatus = { leadId, newStatus ->
                    viewModel.repository.updateLoanLeadStatus(leadId, newStatus)
                },
                onCallLead = { phone ->
                    IntentHelper.dialPhone(context, phone)
                },
                onWhatsAppLead = { phone, name ->
                    IntentHelper.openWhatsApp(
                        context,
                        phone,
                        "Hello $name, I am your S.P.A.RE loan advisor. Connecting regarding your loan application."
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // User Loan Application 3-step Flow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentStep) {
                    1 -> Screen1LoanTypeSelection(
                        selectedType = selectedLoanType,
                        onSelectType = { selectedLoanType = it },
                        onProceed = { currentStep = 2 }
                    )
                    2 -> Screen2RequirementForm(
                        loanType = selectedLoanType,
                        amountLakhs = loanAmountLakhs,
                        onAmountChange = { loanAmountLakhs = it },
                        employmentType = employmentType,
                        onEmploymentChange = { employmentType = it },
                        monthlyIncome = monthlyIncome,
                        onIncomeChange = { monthlyIncome = it },
                        propertyStatus = propertyStatus,
                        onPropertyStatusChange = { propertyStatus = it },
                        city = city,
                        onCityChange = { city = it },
                        userName = userProfile.fullName,
                        userPhone = userProfile.mobileNumber,
                        onSubmit = {
                            val lead = viewModel.repository.submitLoanApplication(
                                loanType = selectedLoanType,
                                amountLakhs = loanAmountLakhs.toDouble(),
                                employmentType = employmentType,
                                monthlyIncome = monthlyIncome,
                                propertyStatus = propertyStatus,
                                city = city,
                                clientName = userProfile.fullName,
                                phone = userProfile.mobileNumber
                            )
                            lastSubmittedLead = lead
                            currentStep = 3
                        }
                    )
                    3 -> Screen3SuccessConfirmation(
                        lead = lastSubmittedLead,
                        onContinue = {
                            currentStep = 1
                            viewModel.setActiveTab(AppTab.HOME)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Screen1LoanTypeSelection(
    selectedType: LoanType,
    onSelectType: (LoanType) -> Unit,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 70.dp)
    ) {
        Text(
            text = "Get Instant Loan Assistance",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select the type of loan you need. Competitive rates starting at 8.25% p.a.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // 3 Visual Loan Options
        LoanTypeCard(
            loanType = LoanType.LAP,
            isSelected = selectedType == LoanType.LAP,
            icon = Icons.Filled.HomeWork,
            badge = "UP TO ₹10 CR",
            onClick = { onSelectType(LoanType.LAP) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        LoanTypeCard(
            loanType = LoanType.HOME_LOAN,
            isSelected = selectedType == LoanType.HOME_LOAN,
            icon = Icons.Filled.AccountBalance,
            badge = "LOWEST ROI",
            onClick = { onSelectType(LoanType.HOME_LOAN) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        LoanTypeCard(
            loanType = LoanType.PURCHASE_5L,
            isSelected = selectedType == LoanType.PURCHASE_5L,
            icon = Icons.Filled.Payments,
            badge = "MICRO FINANCE",
            onClick = { onSelectType(LoanType.PURCHASE_5L) }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Features row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LoanFeatureBadge("Zero Processing", Icons.Filled.CheckCircle)
                LoanFeatureBadge("Minimal Docs", Icons.Filled.Description)
                LoanFeatureBadge("48h Disbursal", Icons.Filled.FlashOn)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("loan_proceed_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text("Proceed to Application", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun LoanTypeCard(
    loanType: LoanType,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("loan_type_${loanType.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF2563EB) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = loanType.title,
                    tint = if (isSelected) Color.White else Color(0xFF475569),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loanType.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = loanType.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
            )
        }
    }
}

@Composable
fun LoanFeatureBadge(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Screen2RequirementForm(
    loanType: LoanType,
    amountLakhs: Float,
    onAmountChange: (Float) -> Unit,
    employmentType: String,
    onEmploymentChange: (String) -> Unit,
    monthlyIncome: String,
    onIncomeChange: (String) -> Unit,
    propertyStatus: String,
    onPropertyStatusChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    userName: String,
    userPhone: String,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
            .padding(bottom = 70.dp)
    ) {
        Text(
            text = "Loan Requirement Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Applying for: ${loanType.title}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2563EB),
            modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
        )

        // Required Loan Amount Slider
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Required Loan Amount", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (amountLakhs >= 100f) "₹${String.format("%.2f", amountLakhs / 100f)} Cr" else "₹${amountLakhs.roundToInt()} Lakh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F766E)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = amountLakhs,
                    onValueChange = onAmountChange,
                    valueRange = 5f..500f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2563EB),
                        activeTrackColor = Color(0xFF2563EB)
                    ),
                    modifier = Modifier.testTag("loan_amount_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("₹5 Lakh", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("₹5 Crore", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Employment Type Chips
        Text("Employment Type", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Salaried", "Self-Employed", "Business Owner").forEach { type ->
                FilterChip(
                    selected = employmentType == type,
                    onClick = { onEmploymentChange(type) },
                    label = { Text(type, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly Income Field
        Text("Monthly Income / Turnover", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = monthlyIncome,
            onValueChange = onIncomeChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Property Status Chips
        Text("Property Status", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Shortlisted", "Already Owned", "Still Searching").forEach { status ->
                FilterChip(
                    selected = propertyStatus == status,
                    onClick = { onPropertyStatusChange(status) },
                    label = { Text(status, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contact Details (Pre-filled from Profile)
        Text("Contact & Location Details", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Client Name:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(userName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mobile Number:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+91 $userPhone (Verified)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("City / Locality", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            placeholder = { Text("e.g., Noida / Delhi NCR") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(26.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_loan_application_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit Application / Apply Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Screen3SuccessConfirmation(
    lead: LoanLead?,
    onContinue: () -> Unit
) {
    // Pulsating animation for the success checkmark
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFFD1FAE5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF059669),
                modifier = Modifier.size(68.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Thank you for your interest!",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Hamare loan advisor jald hi aapse contact karenge aur best interest rates provide karenge.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Lead Reference ID
        Surface(
            color = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Tag, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Lead Reference ID: ${lead?.referenceId ?: "LN-84920"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("loan_success_continue_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text("Continue to Home", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OperatorCrmView(
    loanLeads: List<LoanLead>,
    onUpdateStatus: (String, LeadStatus) -> Unit,
    onCallLead: (String) -> Unit,
    onWhatsAppLead: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Realtime Leads CRM",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Surface(
                color = Color(0xFFD1FAE5),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${loanLeads.size} Total Leads",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(loanLeads) { lead ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lead.clientName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(status = lead.status)
                        }

                        Text(
                            text = "${lead.loanTypeSelected.title} • ₹${lead.requiredAmountInLakhs} Lakh",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F766E),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Text(
                            text = "Income: ${lead.monthlyIncome} • ${lead.employmentType} • ${lead.city}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Text(
                            text = "Ref: ${lead.referenceId} • ${lead.submissionTimestamp}",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions: Call, WhatsApp, Status buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onCallLead(lead.phoneNumber) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Phone, contentDescription = "Call", modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { onWhatsAppLead(lead.phoneNumber, lead.clientName) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                ) {
                                    Icon(Icons.Filled.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(13.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WA", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            // Quick Status Switcher Dropdown/Chips
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (lead.status != LeadStatus.APPROVED) {
                                    OutlinedButton(
                                        onClick = { onUpdateStatus(lead.id, LeadStatus.APPROVED) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Approve", fontSize = 10.sp, color = Color(0xFF10B981))
                                    }
                                }
                                if (lead.status != LeadStatus.UNDER_REVIEW) {
                                    OutlinedButton(
                                        onClick = { onUpdateStatus(lead.id, LeadStatus.UNDER_REVIEW) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Review", fontSize = 10.sp, color = Color(0xFFD97706))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: LeadStatus) {
    val (bgColor, textColor) = when (status) {
        LeadStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        LeadStatus.UNDER_REVIEW -> Color(0xFFE0E7FF) to Color(0xFF3730A3)
        LeadStatus.APPROVED -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        LeadStatus.REJECTED -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
