package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.model.Property
import com.example.ui.SpareViewModel
import com.example.ui.components.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailModal(
    propertyId: String,
    viewModel: SpareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val properties by viewModel.properties.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val property = properties.find { it.id == propertyId }

    var showInquiryDialog by remember { mutableStateOf(false) }
    var inquiryMobile by remember(userProfile.mobileNumber) { mutableStateOf(userProfile.mobileNumber) }
    var inquiryPropertyQuery by remember(property) {
        mutableStateOf(if (property != null) "Inquiry for ${property.title} in ${property.locality} (${property.priceDisplay})" else "")
    }
    var inquiryLoanRequirements by remember(property) {
        mutableStateOf(if (property != null) "Home Loan / Financial assistance required for ${property.priceDisplay}" else "Home Loan assistance required")
    }
    var isInquirySubmitting by remember { mutableStateOf(false) }
    var inquiryError by remember { mutableStateOf<String?>(null) }
    var inquirySuccessDocId by remember { mutableStateOf<String?>(null) }

    if (property == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Property not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Property Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeOverlay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(property.id) }) {
                        Icon(
                            imageVector = if (property.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Save",
                            tint = if (property.isFavorite) Color(0xFFEF4444) else Color(0xFF64748B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Firestore Lead Submission Button
                    Button(
                        onClick = {
                            inquiryError = null
                            inquirySuccessDocId = null
                            showInquiryDialog = true
                        },
                        modifier = Modifier.weight(1.3f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enquire & Loan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = { IntentHelper.dialPhone(context, property.ownerPhone) },
                        modifier = Modifier.weight(0.9f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            IntentHelper.openWhatsApp(
                                context,
                                property.ownerPhone,
                                "Hi ${property.ownerName}, I saw your property '${property.title}' on S.P.A.RE. Is it available for inspection?"
                            )
                        },
                        modifier = Modifier.weight(1.1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Hero Photo Placeholder Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Apartment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("S.P.A.RE Verified Property Media", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${property.carpetAreaSqFt} Sq.Ft • ${property.facing}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text("1 / 6 Photos", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(property.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${property.locality} • ${property.distanceInfo}", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp))
                }
                Text(property.priceDisplay, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F766E))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specifications Grid
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SpecColumn(label = "BHK", value = property.bhk)
                    SpecColumn(label = "Floor", value = property.floor)
                    SpecColumn(label = "Facing", value = property.facing)
                    SpecColumn(label = "Furnishing", value = property.furnishing)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text("About this Property", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = property.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amenities
            Text("Amenities & Facilities", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                property.amenities.take(4).forEach { amenity ->
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✓ $amenity",
                            fontSize = 12.sp,
                            color = Color(0xFF1E40AF),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Owner Info Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF1E40AF))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Posted by ${property.postedBy}: ${property.ownerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Phone: +91 ${property.ownerPhone} (Verified)", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Property & Loan Inquiry Modal Dialog (Firestore "property_leads")
        if (showInquiryDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isInquirySubmitting) {
                        showInquiryDialog = false
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Property & Loan Inquiry", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (inquirySuccessDocId != null) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Saved to Firestore!", fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Lead successfully recorded in collection 'property_leads'.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF047857)
                                    )
                                    Text(
                                        "Document ID: $inquirySuccessDocId",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Enter details to save your inquiry directly to Firebase Firestore collection 'property_leads':",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 1. Mobile Number
                            Text("1. Mobile Number *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = inquiryMobile,
                                onValueChange = {
                                    inquiryMobile = it
                                    inquiryError = null
                                },
                                placeholder = { Text("10-digit mobile number") },
                                prefix = { Text("+91 ", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF10B981)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // 2. Property Query
                            Text("2. Property Query *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = inquiryPropertyQuery,
                                onValueChange = {
                                    inquiryPropertyQuery = it
                                    inquiryError = null
                                },
                                placeholder = { Text("Property title, requirements, or inspection query") },
                                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, tint = Color(0xFF0D9488)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // 3. Loan Client Requirements
                            Text("3. Loan Client Requirements *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = inquiryLoanRequirements,
                                onValueChange = {
                                    inquiryLoanRequirements = it
                                    inquiryError = null
                                },
                                placeholder = { Text("e.g. Home Loan ₹35 Lakh / LAP / Self finance") },
                                leadingIcon = { Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = Color(0xFF2563EB)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (inquiryError != null) {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(inquiryError!!, color = Color(0xFFB91C1C), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (inquirySuccessDocId != null) {
                        Button(
                            onClick = { showInquiryDialog = false }
                        ) {
                            Text("Done")
                        }
                    } else {
                        Button(
                            onClick = {
                                val cleanPhone = inquiryMobile.trim().filter { it.isDigit() }
                                if (cleanPhone.length < 10) {
                                    inquiryError = "Mobile Number must be at least 10 digits."
                                    return@Button
                                }
                                if (inquiryPropertyQuery.trim().length < 3) {
                                    inquiryError = "Property Query is required."
                                    return@Button
                                }
                                if (inquiryLoanRequirements.trim().length < 3) {
                                    inquiryError = "Loan Client Requirements is required."
                                    return@Button
                                }

                                inquiryError = null
                                isInquirySubmitting = true
                                coroutineScope.launch {
                                    val result = viewModel.submitPropertyLead(
                                        mobileNumber = cleanPhone,
                                        propertyQuery = inquiryPropertyQuery.trim(),
                                        loanClientRequirements = inquiryLoanRequirements.trim(),
                                        clientName = userProfile.fullName.ifBlank { "Client" }
                                    )
                                    isInquirySubmitting = false
                                    if (result.isSuccess) {
                                        inquirySuccessDocId = result.getOrNull()
                                        Toast.makeText(context, "Lead saved to Firestore 'property_leads'!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        inquiryError = result.exceptionOrNull()?.localizedMessage ?: "Failed to save to Firestore. Check connection."
                                    }
                                }
                            },
                            enabled = !isInquirySubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            if (isInquirySubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saving...")
                            } else {
                                Text("Save to Firestore")
                            }
                        }
                    }
                },
                dismissButton = {
                    if (inquirySuccessDocId == null) {
                        TextButton(
                            onClick = { showInquiryDialog = false },
                            enabled = !isInquirySubmitting
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SpecColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
