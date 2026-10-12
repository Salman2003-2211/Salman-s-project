package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.AppTab
import com.example.ui.SpareViewModel
import com.example.ui.components.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAdsScreen(
    viewModel: SpareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val properties by viewModel.properties.collectAsState()
    val leadInquiries by viewModel.leadInquiries.collectAsState()

    var selectedStatusTab by remember { mutableIntStateOf(0) } // 0: All, 1: Active, 2: Under Review, 3: Expired/Sold
    var selectedPropertyForLeads by remember { mutableStateOf<Property?>(null) }
    var editingProperty by remember { mutableStateOf<Property?>(null) }

    // User's listings
    val userListings = remember(properties) {
        properties.filter { it.isOwnerListing }
    }

    val filteredListings = remember(userListings, selectedStatusTab) {
        when (selectedStatusTab) {
            1 -> userListings.filter { it.status == PropertyStatus.ACTIVE }
            2 -> userListings.filter { it.status == PropertyStatus.UNDER_REVIEW }
            3 -> userListings.filter { it.status == PropertyStatus.EXPIRED || it.status == PropertyStatus.SOLD }
            else -> userListings
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings & Leads", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeOverlay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            // 5. Fixed '+ Post New Ad' FAB
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.closeOverlay()
                    viewModel.setActiveTab(AppTab.POST)
                },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Post New Ad", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("post_ad_fab")
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
            // 1. Top Status Filter Tabs
            val statusTabs = listOf("All Ads", "Active / Live", "Under Review", "Expired / Sold")
            PrimaryTabRow(
                selectedTabIndex = selectedStatusTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFF2563EB)
            ) {
                statusTabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = selectedStatusTab == index,
                        onClick = { selectedStatusTab = index },
                        text = {
                            Text(
                                text = tabTitle,
                                fontSize = 12.sp,
                                fontWeight = if (selectedStatusTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredListings.isEmpty()) {
                // 5. Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.HomeWork,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No active listings found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Post your property to get free verified buyer and tenant leads.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.closeOverlay()
                                viewModel.setActiveTab(AppTab.POST)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Post New Property")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredListings) { listing ->
                        val listingLeadsCount = leadInquiries.count { it.propertyId == listing.id }

                        UserListingCard(
                            listing = listing,
                            leadsCount = listingLeadsCount,
                            onViewLeads = { selectedPropertyForLeads = listing },
                            onEdit = { editingProperty = listing },
                            onPause = {
                                val nextStatus = if (listing.status == PropertyStatus.ACTIVE) PropertyStatus.UNDER_REVIEW else PropertyStatus.ACTIVE
                                viewModel.repository.updatePropertyStatus(listing.id, nextStatus)
                                Toast.makeText(context, "Listing status updated to ${nextStatus.label}", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteOrSold = {
                                viewModel.repository.updatePropertyStatus(listing.id, PropertyStatus.SOLD)
                                Toast.makeText(context, "Listing marked as Sold / Rented!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // 3. Dedicated 'Leads / Responses' Modal Drawer / Bottom Sheet
        if (selectedPropertyForLeads != null) {
            val leadsForThisProperty = leadInquiries.filter { it.propertyId == selectedPropertyForLeads!!.id }

            ModalBottomSheet(
                onDismissRequest = { selectedPropertyForLeads = null }
            ) {
                LeadsInboxSheetContent(
                    property = selectedPropertyForLeads!!,
                    leads = leadsForThisProperty,
                    onUpdateLeadStatus = { leadId, newStatus ->
                        viewModel.repository.updateLeadStatus(leadId, newStatus)
                    },
                    onCallLead = { phone -> IntentHelper.dialPhone(context, phone) },
                    onWhatsAppLead = { phone, propTitle ->
                        IntentHelper.openWhatsApp(
                            context,
                            phone,
                            "Hi, I received your inquiry for '$propTitle' on S.P.A.RE. Let's discuss details."
                        )
                    }
                )
            }
        }

        // Edit Price / Details Quick Dialog
        if (editingProperty != null) {
            EditListingDialog(
                property = editingProperty!!,
                onDismiss = { editingProperty = null },
                onSave = { updatedPrice ->
                    viewModel.repository.postProperty(
                        editingProperty!!.copy(
                            price = updatedPrice,
                            priceDisplay = "₹$updatedPrice"
                        )
                    )
                    editingProperty = null
                    Toast.makeText(context, "Listing updated successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun UserListingCard(
    listing: Property,
    leadsCount: Int,
    onViewLeads: () -> Unit,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onDeleteOrSold: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Status Badge & 3-dots Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (listing.status) {
                        PropertyStatus.ACTIVE -> Color(0xFFDCFCE7)
                        PropertyStatus.UNDER_REVIEW -> Color(0xFFFEF3C7)
                        PropertyStatus.EXPIRED -> Color(0xFFF1F5F9)
                        PropertyStatus.SOLD -> Color(0xFFDBEAFE)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = listing.status.label,
                        color = when (listing.status) {
                            PropertyStatus.ACTIVE -> Color(0xFF15803D)
                            PropertyStatus.UNDER_REVIEW -> Color(0xFFB45309)
                            PropertyStatus.EXPIRED -> Color(0xFF64748B)
                            PropertyStatus.SOLD -> Color(0xFF1E40AF)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("✏️ Edit Listing") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (listing.status == PropertyStatus.ACTIVE) "⏸️ Pause Listing" else "▶️ Activate Listing") },
                            onClick = {
                                showMenu = false
                                onPause()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🗑️ Mark as Sold / Rented") },
                            onClick = {
                                showMenu = false
                                onDeleteOrSold()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Listing Title & Price
            Text(listing.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${listing.locality} • ${listing.priceDisplay}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.padding(top = 2.dp))

            Spacer(modifier = Modifier.height(12.dp))

            // Key Performance Metrics Strip (Views, Leads, Expiry)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Views
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${listing.viewsCount} Views", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Inquiries
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$leadsCount Inquiries", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Expiry countdown
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expires in ${listing.validityDaysRemaining}d", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. View Leads (X) Core Button
            Button(
                onClick = onViewLeads,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Leads & Responses ($leadsCount)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LeadsInboxSheetContent(
    property: Property,
    leads: List<LeadInquiry>,
    onUpdateLeadStatus: (String, InquiryStatus) -> Unit,
    onCallLead: (String) -> Unit,
    onWhatsAppLead: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
            .padding(bottom = 30.dp)
    ) {
        Text("Buyer & Tenant Leads", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text("For: ${property.title}", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

        if (leads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No customer inquiries on this property yet.", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(leads) { lead ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lead.clientName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = lead.inquiryType.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text("Inquiry: ${lead.inquiryTime} • Status: ${lead.status.label}", fontSize = 11.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp))
                            Text("\"${lead.clientNote}\"", fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onCallLead(lead.clientPhone) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                    }

                                    Button(
                                        onClick = { onWhatsAppLead(lead.clientPhone, property.title) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                    ) {
                                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp", fontSize = 11.sp, color = Color.White, maxLines = 1, softWrap = false)
                                    }
                                }

                                // Quick Status Dropdown toggle
                                if (lead.status != InquiryStatus.DEAL_CLOSED) {
                                    OutlinedButton(
                                        onClick = { onUpdateLeadStatus(lead.id, InquiryStatus.DEAL_CLOSED) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Mark Closed", fontSize = 10.sp, color = Color(0xFF10B981))
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
fun EditListingDialog(
    property: Property,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var newPrice by remember { mutableStateOf(property.price.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Listing Price", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(property.title, fontSize = 13.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPrice,
                    onValueChange = { newPrice = it },
                    label = { Text("New Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(newPrice.toDoubleOrNull() ?: property.price) }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
