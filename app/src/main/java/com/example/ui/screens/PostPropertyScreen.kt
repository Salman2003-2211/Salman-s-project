package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ListingType
import com.example.model.Property
import com.example.model.PropertyStatus
import com.example.model.formatPriceDisplay
import com.example.ui.AppTab
import com.example.ui.ScreenOverlay
import com.example.ui.SpareViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PropertyPhotoItem(
    val id: String,
    val uri: Uri? = null,
    val bitmap: Bitmap? = null,
    val name: String
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostPropertyScreen(
    viewModel: SpareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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

    // Step 3: Location (Fresh, not hardcoded)
    var localitySearch by remember { mutableStateOf("") }
    var streetAddress by remember { mutableStateOf("") }
    var cityName by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var detectedLatitude by remember { mutableDoubleStateOf(28.6280) }
    var detectedLongitude by remember { mutableDoubleStateOf(77.3649) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    // Step 4: Photos (Fresh list)
    var uploadedPhotos by remember { mutableStateOf(listOf<PropertyPhotoItem>()) }
    var showPhotoSourceSheet by remember { mutableStateOf(false) }

    // Photo Launchers
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapIndexed { idx, uri ->
                PropertyPhotoItem(
                    id = "gallery_${System.currentTimeMillis()}_$idx",
                    uri = uri,
                    name = "Gallery Photo ${uploadedPhotos.size + idx + 1}"
                )
            }
            uploadedPhotos = (uploadedPhotos + newItems).take(8)
            Toast.makeText(context, "${uris.size} photo(s) selected from Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val newItem = PropertyPhotoItem(
                id = "camera_${System.currentTimeMillis()}",
                bitmap = bitmap,
                name = "Camera Capture ${uploadedPhotos.size + 1}"
            )
            uploadedPhotos = (uploadedPhotos + newItem).take(8)
            Toast.makeText(context, "Photo captured from Camera", Toast.LENGTH_SHORT).show()
        }
    }

    // GPS Location function
    fun detectDeviceLocation() {
        isDetectingLocation = true
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    detectedLatitude = location.latitude
                    detectedLongitude = location.longitude
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            withContext(Dispatchers.Main) {
                                isDetectingLocation = false
                                if (!addresses.isNullOrEmpty()) {
                                    val addr = addresses[0]
                                    localitySearch = addr.subLocality ?: addr.locality ?: "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                                    cityName = addr.locality ?: addr.subAdminArea ?: "Delhi NCR"
                                    pincode = addr.postalCode ?: ""
                                    streetAddress = addr.thoroughfare ?: addr.featureName ?: ""
                                    Toast.makeText(context, "Current location detected: $localitySearch", Toast.LENGTH_SHORT).show()
                                } else {
                                    localitySearch = "GPS: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                                    Toast.makeText(context, "Coordinates captured from GPS", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isDetectingLocation = false
                                localitySearch = "GPS: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                                Toast.makeText(context, "Location coordinates detected via GPS", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    isDetectingLocation = false
                    Toast.makeText(context, "Unable to get GPS fix. Please ensure Location is enabled in phone settings.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                isDetectingLocation = false
                Toast.makeText(context, "GPS detection failed: ${e.localizedMessage}. Enter location manually.", Toast.LENGTH_SHORT).show()
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            detectDeviceLocation()
        } else {
            Toast.makeText(context, "Location permission denied. Please enter address manually.", Toast.LENGTH_SHORT).show()
        }
    }

    // Step 5: Description & Highlights
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFeatureTags by remember {
        mutableStateOf(setOf("Gated Society", "Park Facing", "24/7 Water", "Near Metro"))
    }

    // Step 6: Pricing
    var rentAmount by remember { mutableStateOf("") }
    var securityDeposit by remember { mutableStateOf("") }
    var maintenanceType by remember { mutableStateOf("Included") }
    var salePriceLakhs by remember { mutableStateOf("") }
    var isNegotiable by remember { mutableStateOf(true) }

    var validationError by remember { mutableStateOf<String?>(null) }
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
                // ==================== STEP 1: LISTING PURPOSE ====================
                1 -> {
                    Text("Step 1: What do you want to do?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Select your listing category.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 18.dp))

                    val listingChoices = listOf(
                        ListingType.RENT to ("Rent / Lease" to Icons.Filled.Key),
                        ListingType.SALE to ("Sell Property" to Icons.Filled.Sell),
                        ListingType.PLOT to ("Plot / Land" to Icons.Filled.Landscape),
                        ListingType.COMMERCIAL to ("Commercial" to Icons.Filled.Business)
                    )

                    listingChoices.forEach { (type, pair) ->
                        val (label, icon) = pair
                        val isSelected = listingType == type
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { listingType = type }
                                .testTag("listing_type_${type.name}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2563EB) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { listingType = type },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                                )
                            }
                        }
                    }
                }

                // ==================== STEP 2: PROPERTY CONFIGURATION ====================
                2 -> {
                    Text("Step 2: Property Type & Specs", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Configure layout and size details.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 18.dp))

                    if (listingType != ListingType.PLOT) {
                        Text("BHK Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1 RK", "1 BHK", "2 BHK", "3 BHK", "4+ BHK").forEach { bhk ->
                                FilterChip(
                                    selected = bhkType == bhk,
                                    onClick = { bhkType = bhk },
                                    label = { Text(bhk, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Property Type", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Apartment", "Builder Floor", "Independent Villa", "Penthouse", "Studio").forEach { pType ->
                                FilterChip(
                                    selected = propertyType == pType,
                                    onClick = { propertyType = pType },
                                    label = { Text(pType, fontSize = 12.sp) }
                                )
                            }
                        }
                    } else {
                        // Plot specifics (Gaj selection)
                        Text("Plot Details (Gaj / Area Selection)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Popular Plot Sizes in Gaj", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("50 Gaj", "100 Gaj", "150 Gaj", "200 Gaj", "300 Gaj", "500 Gaj").forEach { gaj ->
                                val num = gaj.split(" ")[0]
                                FilterChip(
                                    selected = plotSize == num,
                                    onClick = { plotSize = num },
                                    label = { Text(gaj, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = plotSize,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) plotSize = it },
                            label = { Text("Custom Area Size (Gaj)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // ==================== STEP 3: LOCATION SELECTION (FRESH & GPS AUTO-DETECT) ====================
                3 -> {
                    Text("Step 3: Location Selection", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Enter location or auto-detect using GPS.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    // GPS Auto Detect Button (Prominent)
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        enabled = !isDetectingLocation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("detect_gps_location_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting current GPS location...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Detect My Current Location (GPS)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = localitySearch,
                        onValueChange = {
                            localitySearch = it
                            validationError = null
                        },
                        label = { Text("Locality / Sector / Area *") },
                        placeholder = { Text("Enter locality (e.g., Sector 62, Indirapuram, Malviya Nagar)") },
                        leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null, tint = Color(0xFF2563EB)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Address Breakdown", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = streetAddress,
                        onValueChange = { streetAddress = it },
                        label = { Text("Street / Building / Plot No. (Optional)") },
                        placeholder = { Text("e.g., Tower B, Plot 42, Flat 301") },
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
                            label = { Text("City *") },
                            placeholder = { Text("e.g., Noida, Delhi") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { if (it.length <= 6) pincode = it },
                            label = { Text("Pincode") },
                            placeholder = { Text("e.g., 201301") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Location Pin Confirmation Card
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (localitySearch.isNotBlank()) "Pin: $localitySearch${if (cityName.isNotBlank()) ", $cityName" else ""}" else "No location entered yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (localitySearch.isNotBlank()) Color(0xFF1E293B) else Color(0xFF64748B)
                                )
                                Text(
                                    text = "Lat: ${String.format("%.4f", detectedLatitude)}, Lng: ${String.format("%.4f", detectedLongitude)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // ==================== STEP 4: MEDIA & PHOTO UPLOAD (GALLERY & CAMERA) ====================
                4 -> {
                    Text("Step 4: Media & Photos", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Select photos from your Gallery or capture using Camera.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

                    // Guidance Banner
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Add photos for better response: Front exterior, Living hall, Bedrooms, and Kitchen.",
                                fontSize = 12.sp,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons to open Gallery & Camera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Gallery Button
                        Button(
                            onClick = {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_gallery_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Camera Button
                        Button(
                            onClick = {
                                cameraLauncher.launch(null)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_camera_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Selected photos list / thumbnails
                    if (uploadedPhotos.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No photos selected yet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF475569))
                                Text("Tap 'Open Gallery' or 'Take Camera' above to upload photos.", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    } else {
                        Text("${uploadedPhotos.size} Photo(s) Attached", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uploadedPhotos.forEachIndexed { index, photoItem ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Thumbnail
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFE2E8F0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (photoItem.uri != null) {
                                                    AsyncImage(
                                                        model = photoItem.uri,
                                                        contentDescription = photoItem.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else if (photoItem.bitmap != null) {
                                                    Image(
                                                        bitmap = photoItem.bitmap.asImageBitmap(),
                                                        contentDescription = photoItem.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(Icons.Filled.Image, contentDescription = null, tint = Color(0xFF64748B))
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(photoItem.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (index == 0) {
                                                    Text("Cover Photo (Featured)", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                                } else {
                                                    Text("Gallery Image #${index + 1}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                }
                                            }
                                        }

                                        IconButton(onClick = {
                                            uploadedPhotos = uploadedPhotos.filterIndexed { i, _ -> i != index }
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== STEP 5: DESCRIPTION & HIGHLIGHTS ====================
                5 -> {
                    Text("Step 5: Description & Highlights", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Highlight special features to attract buyers/tenants.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    Text("Listing Title / Heading", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g., Spacious $bhkType $propertyType near Metro") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Detailed Description", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Mention details like road width, water supply, modular kitchen, ventilation, nearby landmarks...") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Key Amenities / Highlights", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val allFeatures = listOf("Gated Society", "Park Facing", "24/7 Water", "Near Metro", "Lift", "Parking", "Power Backup", "Corner Property")
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        allFeatures.forEach { tag ->
                            val isSel = selectedFeatureTags.contains(tag)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedFeatureTags = if (isSel) selectedFeatureTags - tag else selectedFeatureTags + tag
                                },
                                label = { Text(tag, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // ==================== STEP 6: PRICING DETAILS ====================
                6 -> {
                    Text("Step 6: Pricing Details", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Set fair market pricing for fast inquiries.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                    if (listingType == ListingType.RENT) {
                        Text("Monthly Rent (₹)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = rentAmount,
                            onValueChange = { if (it.all { c -> c.isDigit() }) rentAmount = it },
                            placeholder = { Text("e.g., 22000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Security Deposit (₹)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = securityDeposit,
                            onValueChange = { if (it.all { c -> c.isDigit() }) securityDeposit = it },
                            placeholder = { Text("e.g., 44000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        Text("Total Price in Lakhs (₹)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = salePriceLakhs,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) salePriceLakhs = it },
                            placeholder = { Text("e.g., 65 (for 65 Lakhs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isNegotiable, onCheckedChange = { isNegotiable = it })
                        Text("Price Negotiable", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                // ==================== STEP 7: REVIEW & SUBMIT ====================
                7 -> {
                    Text("Step 7: Review & Submit Listing", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Check all details before publishing live.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

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

                            val finalTitle = title.ifBlank { "$bhkType $propertyType in ${localitySearch.ifBlank { "NCR" }}" }
                            Text(finalTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "${streetAddress.ifBlank { "" }} ${localitySearch.ifBlank { "Locality" }}, ${cityName.ifBlank { "City" }}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val finalPrice = if (listingType == ListingType.RENT) "₹${rentAmount.ifBlank { "—" }} / mo" else "₹${salePriceLakhs.ifBlank { "—" }} Lakh"
                            Text(finalPrice, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F766E))

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = description.ifBlank { "Well-maintained $bhkType $propertyType in prime locality with all amenities." },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Photos attached: ${uploadedPhotos.size}", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
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
                        val priceText = if (listingType == ListingType.RENT) rentAmount else salePriceLakhs
                        if (currentStep == 2 && listingType == ListingType.PLOT && (plotSize.toIntOrNull() ?: 0) <= 0) {
                            validationError = "Please enter a valid plot size in Gaj."
                        } else if (currentStep == 3 && (localitySearch.isBlank() || cityName.isBlank())) {
                            validationError = "Please enter both locality and city, or tap 'Auto-Detect My Current Location'."
                        } else if (currentStep == 3 && pincode.isNotBlank() && pincode.length != 6) {
                            validationError = "Pincode must be 6 digits."
                        } else if (currentStep == 6 && (priceText.toDoubleOrNull() ?: 0.0) <= 0.0) {
                            validationError = if (listingType == ListingType.RENT) {
                                "Please enter the monthly rent."
                            } else {
                                "Please enter the total price in Lakhs."
                            }
                        } else {
                            currentStep++
                        }
                    } else {
                        // Final Submit
                        val priceNum = if (listingType == ListingType.RENT) {
                            rentAmount.toDoubleOrNull() ?: 0.0
                        } else {
                            (salePriceLakhs.toDoubleOrNull() ?: 0.0) * 100000.0
                        }
                        if (priceNum <= 0.0) {
                            validationError = "Please go back and enter a valid price."
                            return@Button
                        }
                        val priceDisplay = formatPriceDisplay(priceNum, monthly = listingType == ListingType.RENT)

                        val resolvedLocality = localitySearch.ifBlank { "Local Area" }
                        val resolvedCity = cityName.ifBlank { "Delhi NCR" }
                        val resolvedTitle = title.ifBlank { "$bhkType $propertyType in $resolvedLocality" }
                        val resolvedDesc = description.ifBlank { "Modern and spacious $bhkType property in $resolvedLocality, $resolvedCity." }

                        // Camera captures only exist as in-memory bitmaps; write the first one to
                        // app storage so the listing keeps a cover image instead of losing it.
                        val coverPhoto = uploadedPhotos.firstOrNull()
                        val coverImageUrl = when {
                            coverPhoto?.uri != null -> coverPhoto.uri.toString()
                            coverPhoto?.bitmap != null -> try {
                                val file = java.io.File(context.filesDir, "listing_${System.currentTimeMillis()}.jpg")
                                java.io.FileOutputStream(file).use { out ->
                                    coverPhoto.bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                }
                                Uri.fromFile(file).toString()
                            } catch (e: Exception) {
                                ""
                            }
                            else -> ""
                        }

                        val newProp = Property(
                            id = "prop_${System.currentTimeMillis()}",
                            title = resolvedTitle,
                            listingType = listingType,
                            propertyType = if (listingType == ListingType.PLOT) "Plot" else propertyType,
                            bhk = if (listingType == ListingType.PLOT) "N/A" else bhkType,
                            locality = resolvedLocality,
                            city = resolvedCity,
                            distanceInfo = "Near Local Hub",
                            price = priceNum,
                            priceDisplay = priceDisplay,
                            areaGaj = if (listingType == ListingType.PLOT) (plotSize.toIntOrNull() ?: 0) else 0,
                            carpetAreaSqFt = 1150,
                            furnishing = "Semi-Furnished",
                            isVerified = true,
                            isOwnerListing = true,
                            postedBy = "Owner",
                            ownerName = userProfile.fullName,
                            ownerPhone = userProfile.mobileNumber,
                            amenities = selectedFeatureTags.toList(),
                            imageUrl = coverImageUrl,
                            description = resolvedDesc,
                            viewsCount = 1,
                            inquiriesCount = 0,
                            postedDate = "Just now",
                            validityDaysRemaining = 30,
                            status = PropertyStatus.ACTIVE,
                            lat = detectedLatitude,
                            lng = detectedLongitude
                        )

                        viewModel.repository.postProperty(newProp)
                        Toast.makeText(context, "Property listing published successfully!", Toast.LENGTH_LONG).show()

                        // Reset the wizard so the next listing starts clean
                        currentStep = 1
                        listingType = ListingType.RENT
                        bhkType = "2 BHK"
                        propertyType = "Builder Floor"
                        plotSize = "150"
                        localitySearch = ""
                        streetAddress = ""
                        cityName = ""
                        pincode = ""
                        detectedLatitude = 28.6280
                        detectedLongitude = 77.3649
                        uploadedPhotos = emptyList()
                        title = ""
                        description = ""
                        selectedFeatureTags = setOf("Gated Society", "Park Facing", "24/7 Water", "Near Metro")
                        rentAmount = ""
                        securityDeposit = ""
                        salePriceLakhs = ""
                        isNegotiable = true

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
