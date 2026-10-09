package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ListingType
import com.example.model.Property
import com.example.model.PropertyStatus
import com.example.ui.AppTab
import com.example.ui.ScreenOverlay
import com.example.ui.SpareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostPropertyScreen(
    viewModel: SpareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) } // 1..7

    // Step 1: Listing Type
    var listingType by remember { mutableStateOf(ListingType.RENT) }

    // Step 2: Dynamic Property Details
    var bhkType by remember { mutableStateOf("2 BHK") }
    var propertyType by remember { mutableStateOf("Builder Floor") }
    var plotPurpose by remember { mutableStateOf("Sale") }
    var plotSize by remember { mutableStateOf("150") }
    var plotUnit by remember { mutableStateOf("Gaj / Sq. Yards") }

    // Step 3: Location
    var localitySearch by remember { mutableStateOf("Sector 62, Noida") }
    var streetAddress by remember { mutableStateOf("Tower B, Plot 42") }
    var cityName by remember { mutableStateOf("Noida") }
    var pincode by remember { mutableStateOf("201309") }

    // Step 4: Photos
    var uploadedPhotos by remember {
        mutableStateOf(
            listOf("Exterior Front View", "Living Area & Balcony", "Master Bedroom")
        )
    }

    // Step 5: Description & Highlights
    var title by remember { mutableStateOf("Spacious 2 BHK Builder Floor with Park View") }
    var description by remember {
        mutableStateOf("Brand new ready to move builder floor with modular kitchen, 40ft wide road, 24/7 sweet water supply, close to metro station and reputed schools.")
    }
    var selectedFeatureTags by remember {
        mutableStateOf(setOf("Gated Society", "Park Facing", "24/7 Water", "Near Metro"))
    }

    // Step 6: Pricing
    var rentAmount by remember { mutableStateOf("24000") }
    var securityDeposit by remember { mutableStateOf("48000") }
    var maintenanceType by remember { mutableStateOf("Included") }
    var salePriceLakhs by remember { mutableStateOf("75") }
    var isNegotiable by remember { mutableStateOf(true) }

    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Property (Step $currentStep of 7)", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.setActiveTab(AppTab.HOME) }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            // Step Progress Bar
            LinearProgressIndicator(
                progress = { currentStep / 7f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF2563EB),
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(18.dp))

            when (currentStep) {
                // ==================== STEP 1: LISTING TYPE ====================
                1 -> {
                    Text("Step 1: Listing Type", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Select what you want to do with your property.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    listOf(
                        Triple(ListingType.RENT, "Rent / Lease", "Find verified tenants for apartments or commercial spaces."),
                        Triple(ListingType.SALE, "Sale", "Sell residential houses, builder floors, or luxury villas."),
                        Triple(ListingType.PLOT, "Plot / Land", "List residential, agricultural, or commercial plots in Gaj.")
                    ).forEach { (type, label, desc) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { listingType = type }
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (listingType == type) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (listingType == type) 2.dp else 1.dp,
                                color = if (listingType == type) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = listingType == type,
                                    onClick = { listingType = type },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(desc, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }

                // ==================== STEP 2: DYNAMIC PROPERTY DETAILS ====================
                2 -> {
                    Text("Step 2: Property Specifications", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Tell us more about the size and configuration.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    if (listingType == ListingType.RENT || listingType == ListingType.SALE) {
                        Text("BHK Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1 RK", "1 BHK", "2 BHK", "3 BHK", "4 BHK", "4+ BHK").forEach { bhk ->
                                FilterChip(
                                    selected = bhkType == bhk,
                                    onClick = { bhkType = bhk },
                                    label = { Text(bhk, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text("Property Type", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Apartment", "Builder Floor", "Independent House", "Villa").forEach { pType ->
                                FilterChip(
                                    selected = propertyType == pType,
                                    onClick = { propertyType = pType },
                                    label = { Text(pType, fontSize = 12.sp) }
                                )
                            }
                        }
                    } else {
                        // Plot / Land
                        Text("Plot Purpose", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Sale", "Lease / Rent").forEach { purpose ->
                                FilterChip(
                                    selected = plotPurpose == purpose,
                                    onClick = { plotPurpose = purpose },
                                    label = { Text(purpose, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text("Plot Size & Measuring Unit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = plotSize,
                                onValueChange = { plotSize = it },
                                label = { Text("Area Size") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = plotUnit,
                                onValueChange = { plotUnit = it },
                                label = { Text("Unit") },
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // ==================== STEP 3: LOCATION SELECTION ====================
                3 -> {
                    Text("Step 3: Location Selection", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Pinpoint your property location on the map.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    OutlinedTextField(
                        value = localitySearch,
                        onValueChange = { localitySearch = it },
                        label = { Text("Locality / Sector") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF2563EB)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Map Pin Canvas Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = "Pin", tint = Color(0xFFEF4444), modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Map Pin set at: $localitySearch", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Drag pin or click below to adjust location coordinates.", fontSize = 11.sp, color = Color(0xFF64748B))
                            }

                            Button(
                                onClick = {
                                    localitySearch = "Sector 62, Electronic City, Noida"
                                    Toast.makeText(context, "Current GPS location pinned!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Use Current Location", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Address Breakdown", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = streetAddress,
                        onValueChange = { streetAddress = it },
                        label = { Text("Street / Flat / Plot No.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cityName,
                            onValueChange = { cityName = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { pincode = it },
                            label = { Text("Pincode") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // ==================== STEP 4: MEDIA / PHOTO UPLOAD ====================
                4 -> {
                    Text("Step 4: Media & Photos", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Properties with 3+ photos get 5x more buyer inquiries.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    // Guideline card
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Guidelines: Add at least 3 photos for better reach: Exterior, Bedroom, Living Area.",
                                fontSize = 12.sp,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Photo list
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uploadedPhotos.forEachIndexed { index, photoName ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color(0xFF2563EB))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(photoName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (index == 0) {
                                                Text("Cover Photo (Main)", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            Toast.makeText(context, "Photo rotated 90°", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = {
                                            uploadedPhotos = uploadedPhotos.filterIndexed { i, _ -> i != index }
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Add photo button
                        OutlinedButton(
                            onClick = {
                                uploadedPhotos = uploadedPhotos + "New Interior Photo ${uploadedPhotos.size + 1}"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Add More Photos")
                        }
                    }
                }

                // ==================== STEP 5: DESCRIPTION & HIGHLIGHTS ====================
                5 -> {
                    Text("Step 5: Description & Highlights", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Highlight special features to attract buyers.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    Text("Listing Title / Heading", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Detailed Description (min 50 chars)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text("${description.length} / 1000 characters", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.align(Alignment.End))

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Quick Feature Tags / Chips", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    listOf(
                        "Gated Society", "Park Facing", "24/7 Water", "Corner Property",
                        "Near Metro", "Road Width 40ft", "Vastu Compliant", "Lift Facility"
                    ).chunked(2).forEach { tagRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tagRow.forEach { tag ->
                                FilterChip(
                                    selected = selectedFeatureTags.contains(tag),
                                    onClick = {
                                        selectedFeatureTags = if (selectedFeatureTags.contains(tag)) {
                                            selectedFeatureTags - tag
                                        } else {
                                            selectedFeatureTags + tag
                                        }
                                    },
                                    label = { Text(tag, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ==================== STEP 6: PRICING & COMMERCIALS ====================
                6 -> {
                    Text("Step 6: Pricing & Commercials", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Set competitive pricing for faster closing.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    if (listingType == ListingType.RENT) {
                        Text("Monthly Rent (₹ / month)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = rentAmount,
                            onValueChange = { rentAmount = it },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Security Deposit (₹)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = securityDeposit,
                            onValueChange = { securityDeposit = it },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Maintenance Charges", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Included", "Separate (₹1500/mo)").forEach { m ->
                                FilterChip(
                                    selected = maintenanceType == m,
                                    onClick = { maintenanceType = m },
                                    label = { Text(m, fontSize = 12.sp) }
                                )
                            }
                        }
                    } else {
                        // Sale or Plot
                        Text("Expected Selling Price (₹ Lakhs)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = salePriceLakhs,
                            onValueChange = { salePriceLakhs = it },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        val priceLakhsNum = salePriceLakhs.toDoubleOrNull() ?: 0.0
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = if (priceLakhsNum >= 100) "Total: ₹${String.format("%.2f", priceLakhsNum / 100)} Crore" else "Total: ₹${priceLakhsNum.toInt()} Lakh",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isNegotiable, onCheckedChange = { isNegotiable = it })
                            Text("Price Negotiable", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                // ==================== STEP 7: REVIEW & FINAL ACTION ====================
                7 -> {
                    Text("Step 7: Review & Submit Listing", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Check all details before publishing your listing live.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(color = Color(0xFFDBEAFE), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = listingType.label,
                                        color = Color(0xFF1E40AF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text("Ready to Publish", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("$streetAddress, $localitySearch, $cityName", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp))

                            Spacer(modifier = Modifier.height(10.dp))

                            val finalPrice = if (listingType == ListingType.RENT) "₹$rentAmount / mo" else "₹$salePriceLakhs Lakh"
                            Text(finalPrice, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F766E))

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), maxLines = 3)

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Key Highlights: ${selectedFeatureTags.joinToString(" • ")}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                        }
                    }
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(validationError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Navigation Buttons (Next / Post Property Now)
            Button(
                onClick = {
                    validationError = null
                    if (currentStep < 7) {
                        // Validation checks
                        if (currentStep == 5 && description.length < 30) {
                            validationError = "Please write at least 30 characters in property description."
                        } else {
                            currentStep++
                        }
                    } else {
                        // Final Submit
                        val priceNum = if (listingType == ListingType.RENT) rentAmount.toDoubleOrNull() ?: 24000.0 else (salePriceLakhs.toDoubleOrNull() ?: 75.0) * 100000.0
                        val priceDisplay = if (listingType == ListingType.RENT) "₹$rentAmount / mo" else "₹$salePriceLakhs Lakh"

                        val newProp = Property(
                            id = "prop_${System.currentTimeMillis()}",
                            title = title,
                            listingType = listingType,
                            propertyType = if (listingType == ListingType.PLOT) "Plot" else propertyType,
                            bhk = if (listingType == ListingType.PLOT) "N/A" else bhkType,
                            locality = localitySearch,
                            city = cityName,
                            distanceInfo = "Near Sector 62 Metro",
                            price = priceNum,
                            priceDisplay = priceDisplay,
                            areaGaj = plotSize.toIntOrNull() ?: 150,
                            carpetAreaSqFt = 1150,
                            furnishing = "Semi-Furnished",
                            isVerified = true,
                            isOwnerListing = true,
                            postedBy = "Owner",
                            ownerName = userProfile.fullName,
                            ownerPhone = userProfile.mobileNumber,
                            amenities = selectedFeatureTags.toList(),
                            description = description,
                            viewsCount = 1,
                            inquiriesCount = 0,
                            postedDate = "Just now",
                            validityDaysRemaining = 30,
                            status = PropertyStatus.ACTIVE
                        )

                        viewModel.repository.postProperty(newProp)
                        Toast.makeText(context, "Property listing published successfully!", Toast.LENGTH_LONG).show()
                        viewModel.openOverlay(ScreenOverlay.MyAds)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("post_property_next_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentStep == 7) Color(0xFF059669) else Color(0xFF2563EB)
                )
            ) {
                Text(
                    text = if (currentStep == 7) "Post Property Now / Submit Listing" else "Next Step",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (currentStep == 7) Icons.Filled.CheckCircle else Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
