package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BookedMeeting
import com.example.model.MeetingStatus
import com.example.model.Partner
import com.example.ui.SpareViewModel
import com.example.ui.components.IntentHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnersScreen(
    viewModel: SpareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val partners by viewModel.partners.collectAsState()
    val bookedMeetings by viewModel.bookedMeetings.collectAsState()
    val myPartnerApplication by viewModel.myPartnerApplication.collectAsState()

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Explore Partners, 1: My Booked Meetings
    var selectedSpecialization by remember { mutableStateOf("All Partners") }
    var localityFilter by remember { mutableStateOf("") }

    var bookingPartner by remember { mutableStateOf<Partner?>(null) }
    var showRegisterPartnerModal by remember { mutableStateOf(false) }

    val specializations = listOf(
        "All Partners",
        "Residential",
        "Commercial",
        "Plots & Land",
        "Agricultural",
        "Rent / Lease",
        "Home Loan & Legal Assistance"
    )

    val filteredPartners = remember(partners, selectedSpecialization, localityFilter) {
        partners.filter { p ->
            // Public list ONLY displays documents where status == "APPROVED"
            val isApproved = p.status == "APPROVED"
            val matchesSpec = if (selectedSpecialization == "All Partners") true else {
                p.specializations.any { it.contains(selectedSpecialization, ignoreCase = true) } ||
                p.dealTypes.any { it.contains(selectedSpecialization, ignoreCase = true) }
            }
            val matchesLocality = if (localityFilter.isBlank()) true else {
                p.operatingAreas.any { it.contains(localityFilter, ignoreCase = true) } ||
                p.areas.any { it.contains(localityFilter, ignoreCase = true) } ||
                p.agencyName.contains(localityFilter, ignoreCase = true) ||
                p.businessName.contains(localityFilter, ignoreCase = true) ||
                p.name.contains(localityFilter, ignoreCase = true)
            }
            isApproved && matchesSpec && matchesLocality
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meet Trusted Partners", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 5. Sub-Tab Toggle: Explore Partners | My Booked Meetings
            TabRow(
                selectedTabIndex = activeSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFF2563EB)
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = { Text("Explore Partners", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("partners_tab_explore")
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = {
                        Text(
                            "My Booked Meetings (${bookedMeetings.count { it.status != MeetingStatus.CANCELLED }})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("partners_tab_meetings")
                )
            }

            if (activeSubTab == 0) {
                // EXPLORE PARTNERS TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Subtitle & Search
                    item {
                        Column {
                            Text(
                                text = "Connect with verified local real estate experts & agents",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Locality / City Search
                            OutlinedTextField(
                                value = localityFilter,
                                onValueChange = { localityFilter = it },
                                placeholder = { Text("Filter by locality (e.g. Dwarka, Noida, Rohini)") },
                                leadingIcon = {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF2563EB))
                                },
                                trailingIcon = {
                                    if (localityFilter.isNotBlank()) {
                                        IconButton(onClick = { localityFilter = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("partners_locality_search"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. Specialization Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                specializations.forEach { spec ->
                                    FilterChip(
                                        selected = selectedSpecialization == spec,
                                        onClick = { selectedSpecialization = spec },
                                        label = { Text(spec, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Dealer Status / 'Join as a Partner' Banner
                    item {
                        if (myPartnerApplication != null) {
                            val app = myPartnerApplication!!
                            when (app.status) {
                                "PENDING_APPROVAL" -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("partner_pending_review_banner"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Filled.HourglassTop,
                                                        contentDescription = null,
                                                        tint = Color(0xFFB45309),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Application Under Review",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF92400E)
                                                    )
                                                }

                                                Surface(
                                                    color = Color(0xFFFDE68A),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "PENDING_APPROVAL",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF92400E),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "Application submitted successfully! Your profile is under review and will go live once verified by our team.",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF78350F),
                                                lineHeight = 18.sp
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color.White.copy(alpha = 0.9f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = "${app.name} • ${app.displayBusinessName}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E293B)
                                                    )
                                                    Text(
                                                        text = "Areas: ${app.displayAreas.joinToString(", ")}",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF475569),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                    Text(
                                                        text = "Deals In: ${app.displayDealTypes.joinToString(", ")} • Exp: ${app.experienceYears} Yrs",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF2563EB),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                "APPROVED" -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("partner_approved_banner"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Verified,
                                                contentDescription = null,
                                                tint = Color(0xFF15803D),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Verified Partner Profile Active",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF166534)
                                                )
                                                Text(
                                                    text = "Your dealer profile is active and verified by admin. Clients can now find you on S.P.A.RE.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showRegisterPartnerModal = true }
                                            .testTag("partner_rejected_banner"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Info,
                                                contentDescription = null,
                                                tint = Color(0xFFB91C1C),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Application Status: ${app.status}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF991B1B)
                                                )
                                                Text(
                                                    text = "Tap here to update and resubmit your dealer partner profile.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFB91C1C)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRegisterPartnerModal = true }
                                    .testTag("join_partner_banner"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Are you a Real Estate Agent or Broker?",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Register to receive verified buyer/seller leads and expand your reach.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF93C5FD),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Button(
                                            onClick = { showRegisterPartnerModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Partner Profile Cards (Listing Feed)
                    items(filteredPartners) { partner ->
                        PartnerProfileCard(
                            partner = partner,
                            onCallClick = { IntentHelper.dialPhone(context, partner.phone) },
                            onWhatsAppClick = {
                                IntentHelper.openWhatsApp(
                                    context,
                                    partner.phone,
                                    "Hi ${partner.name}, I found your profile on S.P.A.RE and want to discuss property options."
                                )
                            },
                            onBookMeetingClick = { bookingPartner = partner }
                        )
                    }

                    if (filteredPartners.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No channel partners match '$localityFilter'. Try searching 'Dwarka', 'Noida', or 'Rohini'.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // MY BOOKED MEETINGS TAB
                if (bookedMeetings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.EventBusy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Booked Meetings Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Explore partners and schedule an in-person or call consultation.", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bookedMeetings) { meeting ->
                            BookedMeetingCard(
                                meeting = meeting,
                                onCall = { IntentHelper.dialPhone(context, meeting.partnerPhone) },
                                onCancel = { viewModel.repository.cancelMeeting(meeting.id) }
                            )
                        }
                    }
                }
            }
        }

        // Book Meeting Bottom Sheet / Dialog
        if (bookingPartner != null) {
            BookMeetingDialog(
                partner = bookingPartner!!,
                onDismiss = { bookingPartner = null },
                onConfirm = { date, time, interest ->
                    viewModel.repository.bookMeeting(
                        partnerId = bookingPartner!!.id,
                        partnerName = bookingPartner!!.name,
                        agencyName = bookingPartner!!.agencyName,
                        partnerPhone = bookingPartner!!.phone,
                        date = date,
                        timeSlot = time,
                        interest = interest
                    )
                    bookingPartner = null
                    activeSubTab = 1 // Switch to booked meetings tab
                }
            )
        }

        // Register as Partner Dialog
        if (showRegisterPartnerModal) {
            RegisterPartnerDialog(
                viewModel = viewModel,
                onDismiss = { showRegisterPartnerModal = false }
            )
        }
    }
}

@Composable
fun PartnerProfileCard(
    partner: Partner,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onBookMeetingClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Partner Top Row (Avatar, Name, Agency, Verified Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDBEAFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = partner.name.take(1),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E40AF)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = partner.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (partner.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = Color(0xFF2563EB), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Verified", color = Color(0xFF2563EB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = partner.displayBusinessName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F766E),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Text(
                        text = "Areas: ${partner.displayAreas.joinToString(", ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    if (partner.displayDealTypes.isNotEmpty()) {
                        Text(
                            text = "Deals in: ${partner.displayDealTypes.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trust & Performance Metrics Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${partner.rating} ★ (${partner.reviewCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Track Record
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Apartment, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${partner.propertiesListedCount}+ Listed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Experience
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HistoryToggleOff, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${partner.experienceYears}+ Yrs Exp", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Direct Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Now
                Button(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                // WhatsApp
                Button(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.weight(1.2f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                }

                // Book Meeting
                OutlinedButton(
                    onClick = onBookMeetingClick,
                    modifier = Modifier.weight(1.2f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Meeting", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
fun BookedMeetingCard(
    meeting: BookedMeeting,
    onCall: () -> Unit,
    onCancel: () -> Unit
) {
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
                Text(meeting.partnerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Surface(
                    color = when (meeting.status) {
                        MeetingStatus.CONFIRMED -> Color(0xFFDCFCE7)
                        MeetingStatus.PENDING -> Color(0xFFFEF3C7)
                        MeetingStatus.COMPLETED -> Color(0xFFDBEAFE)
                        MeetingStatus.CANCELLED -> Color(0xFFFEE2E2)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = meeting.status.label,
                        color = when (meeting.status) {
                            MeetingStatus.CONFIRMED -> Color(0xFF15803D)
                            MeetingStatus.PENDING -> Color(0xFFB45309)
                            MeetingStatus.COMPLETED -> Color(0xFF1E40AF)
                            MeetingStatus.CANCELLED -> Color(0xFFB91C1C)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(meeting.agencyName, fontSize = 12.sp, color = Color(0xFF0F766E), modifier = Modifier.padding(top = 2.dp))

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${meeting.scheduledDate} at ${meeting.timeSlot}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Text("Interest: ${meeting.propertyInterest}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCall, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Call Partner", fontSize = 11.sp)
                }
                if (meeting.status != MeetingStatus.CANCELLED) {
                    OutlinedButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Cancel Meeting", fontSize = 11.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
fun BookMeetingDialog(
    partner: Partner,
    onDismiss: () -> Unit,
    onConfirm: (date: String, time: String, interest: String) -> Unit
) {
    var selectedDate by remember { mutableStateOf("Tomorrow, 10th Oct") }
    var selectedTime by remember { mutableStateOf("11:30 AM") }
    var interestNote by remember { mutableStateOf("Looking for 2 or 3 BHK residential property.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book Meeting with ${partner.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select Preferred Date:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Today", "Tomorrow", "This Weekend").forEach { d ->
                        FilterChip(
                            selected = selectedDate.startsWith(d),
                            onClick = { selectedDate = "$d (Upcoming)" },
                            label = { Text(d, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Time Slot:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("11:00 AM", "3:00 PM", "5:30 PM").forEach { t ->
                        FilterChip(
                            selected = selectedTime == t,
                            onClick = { selectedTime = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Property Interest:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = interestNote,
                    onValueChange = { interestNote = it },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate, selectedTime, interestNote) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Confirm Meeting")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPartnerDialog(
    viewModel: SpareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()

    var fullName by remember { mutableStateOf(userProfile.fullName) }
    var businessName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(userProfile.mobileNumber.replace("+91", "").trim()) }
    var email by remember { mutableStateOf(userProfile.email) }
    var operatingAreas by remember { mutableStateOf("") }
    var experienceYears by remember { mutableStateOf("3") }

    val allDealTypes = listOf("Residential", "Commercial", "Plots", "Agricultural", "Rent/Sale")
    var selectedDealTypes by remember { mutableStateOf(setOf("Residential", "Rent/Sale")) }

    var uploadedDocs by remember { mutableStateOf(listOf<PropertyPhotoItem>()) }
    var isSubmitting by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var businessError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var areaError by remember { mutableStateOf<String?>(null) }
    var dealTypeError by remember { mutableStateOf<String?>(null) }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapIndexed { idx, uri ->
                PropertyPhotoItem(
                    id = "partner_doc_${System.currentTimeMillis()}_$idx",
                    uri = uri,
                    name = "Doc ${uploadedDocs.size + idx + 1}"
                )
            }
            uploadedDocs = (uploadedDocs + newItems).take(3)
        }
    }

    fun validate(): Boolean {
        var isValid = true
        if (fullName.trim().length < 2) {
            nameError = "Please enter contact person name (min 2 chars)"
            isValid = false
        } else {
            nameError = null
        }

        if (businessName.trim().isBlank()) {
            businessError = "Please enter agency or business name"
            isValid = false
        } else {
            businessError = null
        }

        val digits = phone.filter { it.isDigit() }
        if (digits.length != 10) {
            phoneError = "Please enter a valid 10-digit mobile number"
            isValid = false
        } else {
            phoneError = null
        }

        if (operatingAreas.trim().isBlank()) {
            areaError = "Please specify operating areas / cities (e.g. Noida, Dwarka)"
            isValid = false
        } else {
            areaError = null
        }

        if (selectedDealTypes.isEmpty()) {
            dealTypeError = "Please select at least one deal category"
            isValid = false
        } else {
            dealTypeError = null
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Handshake,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partner Registration", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(
                    text = "Register as a verified Dealer, Broker, or Builder on S.P.A.RE",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Full Name / Contact Person
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (nameError != null) nameError = null
                    },
                    label = { Text("Full Name / Contact Person *") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isSubmitting
                )

                // 2. Agency / Business Name
                OutlinedTextField(
                    value = businessName,
                    onValueChange = {
                        businessName = it
                        if (businessError != null) businessError = null
                    },
                    label = { Text("Agency / Business Name *") },
                    placeholder = { Text("e.g. Apex Realty Advisors") },
                    isError = businessError != null,
                    supportingText = businessError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isSubmitting
                )

                // 3. Phone Number & Email
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                                phone = it
                                if (phoneError != null) phoneError = null
                            }
                        },
                        label = { Text("Phone *") },
                        prefix = { Text("+91 ") },
                        isError = phoneError != null,
                        supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting
                    )
                }

                // 4. Operating Areas / Cities
                OutlinedTextField(
                    value = operatingAreas,
                    onValueChange = {
                        operatingAreas = it
                        if (areaError != null) areaError = null
                    },
                    label = { Text("Operating Areas / Cities *") },
                    placeholder = { Text("e.g. Noida Sector 62, Greater Noida, Dwarka") },
                    isError = areaError != null,
                    supportingText = areaError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isSubmitting
                )

                // 5. Deals In (Multi-choice)
                Column {
                    Text("Deals In *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allDealTypes.forEach { deal ->
                            val isSelected = selectedDealTypes.contains(deal)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDealTypes = if (isSelected) {
                                        selectedDealTypes - deal
                                    } else {
                                        selectedDealTypes + deal
                                    }
                                    if (dealTypeError != null) dealTypeError = null
                                },
                                label = { Text(deal, fontSize = 11.sp) },
                                enabled = !isSubmitting
                            )
                        }
                    }
                    if (dealTypeError != null) {
                        Text(dealTypeError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }

                // 6. Experience (Years)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Experience in Business:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("1", "3", "5", "10+").forEach { exp ->
                            FilterChip(
                                selected = experienceYears == exp.replace("+", ""),
                                onClick = { experienceYears = exp.replace("+", "") },
                                label = { Text("$exp Yrs", fontSize = 11.sp) },
                                enabled = !isSubmitting
                            )
                        }
                    }
                }

                // 7. Optional Uploads (Profile Photo / Visiting Card / RERA Certificate)
                Column {
                    Text(
                        "Upload Documents (Optional - Visiting Card / Photo / RERA)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                docPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            enabled = !isSubmitting && uploadedDocs.size < 3,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Files (${uploadedDocs.size}/3)", fontSize = 11.sp)
                        }

                        Text(
                            text = if (uploadedDocs.isEmpty()) "No files chosen" else "${uploadedDocs.size} file(s) attached",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (uploadedDocs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uploadedDocs.forEachIndexed { index, doc ->
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                ) {
                                    if (doc.uri != null) {
                                        AsyncImage(
                                            model = doc.uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (doc.bitmap != null) {
                                        Image(
                                            bitmap = doc.bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            uploadedDocs = uploadedDocs.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validate() && !isSubmitting) {
                        isSubmitting = true
                        coroutineScope.launch {
                            val parsedAreas = operatingAreas
                                .split(",", ";", "•")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            val cleanAreas = if (parsedAreas.isEmpty()) listOf(operatingAreas.trim()) else parsedAreas
                            val expYears = experienceYears.toIntOrNull() ?: 3
                            val cleanPhone = "+91${phone.filter { it.isDigit() }}"

                            val result = viewModel.registerPartner(
                                context = context,
                                name = fullName.trim(),
                                businessName = businessName.trim(),
                                phone = cleanPhone,
                                email = email.trim(),
                                areas = cleanAreas,
                                dealTypes = selectedDealTypes.toList(),
                                experienceYears = expYears,
                                documents = uploadedDocs
                            )

                            isSubmitting = false
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Application submitted successfully! Your profile is under review and will go live once verified by our team.",
                                    Toast.LENGTH_LONG
                                ).show()
                                onDismiss()
                            } else {
                                val err = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                Toast.makeText(
                                    context,
                                    "Submission failed: $err",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submitting...")
                } else {
                    Text("Submit Application")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}
