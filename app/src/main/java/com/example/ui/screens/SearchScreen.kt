package com.example.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ListingType
import com.example.model.Property
import com.example.ui.ScreenOverlay
import com.example.ui.SpareViewModel
import com.example.ui.components.IntentHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SpareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val properties by viewModel.properties.collectAsState()
    val purpose by viewModel.selectedPurpose.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBhk by viewModel.selectedBhk.collectAsState()
    val selectedBudget by viewModel.selectedBudgetRange.collectAsState()
    val selectedPropertyType by viewModel.selectedPropertyType.collectAsState()
    val selectedFurnishing by viewModel.selectedFurnishing.collectAsState()
    val selectedPostedBy by viewModel.selectedPostedBy.collectAsState()
    val selectedAmenities by viewModel.selectedAmenities.collectAsState()
    val verifiedOnly by viewModel.verifiedOnly.collectAsState()
    val isMapView by viewModel.isMapView.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val showAdvancedFilters by viewModel.showAdvancedFilters.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedMapPinProperty by remember { mutableStateOf<Property?>(null) }
    var isVoiceListening by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    fun fetchCurrentGpsLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            isDetectingLocation = true
            Toast.makeText(context, "Detecting GPS location...", Toast.LENGTH_SHORT).show()
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            coroutineScope.launch {
                                val detectedQuery = withContext(Dispatchers.IO) {
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                        if (!list.isNullOrEmpty()) {
                                            val addr = list[0]
                                            val locName = addr.subLocality ?: addr.locality ?: addr.adminArea
                                            val cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                                            if (!locName.isNullOrBlank() && !cityName.isNullOrBlank() && locName != cityName) {
                                                "$locName, $cityName"
                                            } else {
                                                locName ?: cityName ?: "Current Location"
                                            }
                                        } else "Current Location"
                                    } catch (e: Exception) {
                                        "Current Location"
                                    }
                                }
                                viewModel.setSearchQuery(detectedQuery)
                                isDetectingLocation = false
                                Toast.makeText(context, "Location set: $detectedQuery", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                isDetectingLocation = false
                                if (lastLoc != null) {
                                    coroutineScope.launch {
                                        val detectedQuery = withContext(Dispatchers.IO) {
                                            try {
                                                val geocoder = Geocoder(context, Locale.getDefault())
                                                val list = geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1)
                                                if (!list.isNullOrEmpty()) {
                                                    val addr = list[0]
                                                    addr.subLocality ?: addr.locality ?: "Current Location"
                                                } else "Current Location"
                                            } catch (e: Exception) {
                                                "Current Location"
                                            }
                                        }
                                        viewModel.setSearchQuery(detectedQuery)
                                        Toast.makeText(context, "Location set: $detectedQuery", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Unable to get GPS fix. Please ensure location is enabled.", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener {
                                isDetectingLocation = false
                                Toast.makeText(context, "GPS detection failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        isDetectingLocation = false
                        Toast.makeText(context, "GPS location unavailable", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: SecurityException) {
                isDetectingLocation = false
                Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentGpsLocation()
        } else {
            Toast.makeText(context, "Location permission denied. You can type location manually.", Toast.LENGTH_SHORT).show()
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceListening = false
        val spoken = result.data
            ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !spoken.isNullOrBlank()) {
            viewModel.setSearchQuery(spoken)
        }
    }

    // Popular Suggestions
    val popularSuggestions = listOf("Sector 62, Noida", "Rohini Sector 24", "Dwarka Expressway", "Indirapuram", "Greater Noida West")

    // Filter properties
    val filteredProperties = remember(
        properties, purpose, searchQuery, selectedBhk, selectedBudget,
        selectedPropertyType, selectedFurnishing, selectedPostedBy, selectedAmenities,
        verifiedOnly, sortBy
    ) {
        val list = properties.filter { p ->
            // Purpose filter
            val matchesPurpose = when (purpose) {
                "Rent" -> p.listingType == ListingType.RENT
                "Buy / Sale" -> p.listingType == ListingType.SALE || p.listingType == ListingType.PLOT
                "Plot / Land" -> p.listingType == ListingType.PLOT
                "Commercial" -> p.listingType == ListingType.COMMERCIAL
                else -> true
            }

            // Search query filter
            val matchesQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim()
                p.title.contains(q, ignoreCase = true) ||
                        p.locality.contains(q, ignoreCase = true) ||
                        p.city.contains(q, ignoreCase = true) ||
                        p.description.contains(q, ignoreCase = true)
            }

            matchesPurpose && matchesQuery &&
                matchesBhkFilter(p.bhk, selectedBhk) &&
                matchesBudgetFilter(p, selectedBudget) &&
                // "Villa" chip must also match "Independent Villa" listings posted by users
                (selectedPropertyType == "All" || p.propertyType.contains(selectedPropertyType, ignoreCase = true)) &&
                (selectedFurnishing == "All" || p.furnishing.equals(selectedFurnishing, ignoreCase = true)) &&
                (selectedPostedBy == "All" || p.postedBy.startsWith(selectedPostedBy.substringBefore(" /"), ignoreCase = true)) &&
                (selectedAmenities.isEmpty() || selectedAmenities.all { a -> p.amenities.any { it.contains(a, ignoreCase = true) } }) &&
                (!verifiedOnly || p.isVerified)
        }

        // Sorting
        when (sortBy) {
            "Price: Low to High" -> list.sortedBy { it.price }
            "Price: High to Low" -> list.sortedByDescending { it.price }
            "Most Recent" -> list.sortedBy { postedDateToDays(it.postedDate) }
            else -> list
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 75.dp) // Bottom navigation space
        ) {
            // 1. Top Search Header & Purpose Switcher
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Segmented Purpose Tabs
                    val purposes = listOf("Buy / Sale", "Rent", "Plot / Land", "Commercial")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        purposes.forEachIndexed { index, p ->
                            SegmentedButton(
                                selected = purpose == p,
                                onClick = { viewModel.setPurpose(p) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = purposes.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = Color(0xFF2563EB),
                                    activeContentColor = Color.White
                                )
                            ) {
                                Text(p, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Smart Search Bar with Near Me GPS & Voice Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text("Search by city, locality, project...", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color(0xFF2563EB))
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Near Me GPS Button
                                IconButton(
                                    onClick = {
                                        val fineGranted = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        val coarseGranted = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (fineGranted || coarseGranted) {
                                            fetchCurrentGpsLocation()
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("search_gps_button")
                                ) {
                                    if (isDetectingLocation) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF059669)
                                        )
                                    } else {
                                        Icon(Icons.Filled.MyLocation, contentDescription = "Near Me", tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                    }
                                }

                                // Voice Search Mic
                                IconButton(
                                    onClick = {
                                        try {
                                            isVoiceListening = true
                                            voiceLauncher.launch(
                                                android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(
                                                        android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                        android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                                    )
                                                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say a locality or city")
                                                }
                                            )
                                        } catch (e: Exception) {
                                            isVoiceListening = false
                                            Toast.makeText(context, "Voice search is not available on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("search_voice_button")
                                ) {
                                    Icon(
                                        Icons.Filled.Mic,
                                        contentDescription = "Voice Search",
                                        tint = if (isVoiceListening) Color(0xFFEF4444) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_box"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB)
                        )
                    )

                    // Popular Suggestion Chips
                    if (searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Popular:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            popularSuggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { viewModel.setSearchQuery(suggestion) },
                                    label = { Text(suggestion, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Quick Filter Pills Bar (Horizontal Scroll)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Filters Modal Button
                        FilledTonalButton(
                            onClick = { viewModel.setShowAdvancedFilters(true) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_filters_drawer")
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Verified Listings Only Toggle Chip
                        FilterChip(
                            selected = verifiedOnly,
                            onClick = { viewModel.setVerifiedOnly(!verifiedOnly) },
                            label = { Text("Verified Only", fontSize = 11.sp) },
                            leadingIcon = {
                                if (verifiedOnly) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )

                        // BHK Pills
                        listOf("1 BHK", "2 BHK", "3 BHK", "4+ BHK").forEach { bhk ->
                            FilterChip(
                                selected = selectedBhk == bhk,
                                onClick = {
                                    viewModel.setSelectedBhk(if (selectedBhk == bhk) "All" else bhk)
                                },
                                label = { Text(bhk, fontSize = 11.sp) }
                            )
                        }

                        // Property Type Pills
                        listOf("Apartment", "Builder Floor", "Villa", "Plot").forEach { pType ->
                            FilterChip(
                                selected = selectedPropertyType == pType,
                                onClick = {
                                    viewModel.setSelectedPropertyType(if (selectedPropertyType == pType) "All" else pType)
                                },
                                label = { Text(pType, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Results Count & List / Map Toggle Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredProperties.size} Properties Available",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Sort Dropdown
                        Box {
                            TextButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(sortBy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                listOf("Relevance", "Price: Low to High", "Price: High to Low", "Most Recent").forEach { sortOption ->
                                    DropdownMenuItem(
                                        text = { Text(sortOption) },
                                        onClick = {
                                            viewModel.setSortBy(sortOption)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // List vs Map Toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                IconButton(
                                    onClick = { viewModel.setMapView(false) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.FormatListBulleted,
                                        contentDescription = "List",
                                        tint = if (!isMapView) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setMapView(true) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Map,
                                        contentDescription = "Map",
                                        tint = if (isMapView) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Results Display: List View OR Interactive Map View
            if (!isMapView) {
                // LIST VIEW FEED
                if (filteredProperties.isEmpty()) {
                    // 5. Empty State & Zero-Result Handling
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
                                Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "No properties match your search",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Try expanding your budget range or explore nearby localities in Delhi NCR.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.resetFilters() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear All Filters")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredProperties) { property ->
                            TrendingPropertyCard(
                                property = property,
                                onCardClick = { viewModel.openOverlay(ScreenOverlay.PropertyDetail(property.id)) },
                                onFavoriteClick = { viewModel.toggleFavorite(property.id) },
                                onCallClick = { IntentHelper.dialPhone(context, property.ownerPhone) },
                                onWhatsAppClick = {
                                    IntentHelper.openWhatsApp(
                                        context,
                                        property.ownerPhone,
                                        "Hi, I am interested in '${property.title}' listed for ${property.priceDisplay} on S.P.A.RE."
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                // MAP VIEW DISPLAY
                Box(modifier = Modifier.fillMaxSize()) {
                    // Stylized Interactive Simulated Map Canvas
                    InteractiveMapCanvas(
                        properties = filteredProperties,
                        selectedProperty = selectedMapPinProperty,
                        onSelectPin = { selectedMapPinProperty = it }
                    )

                    // Compact Preview Card at bottom of map
                    if (selectedMapPinProperty != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedMapPinProperty!!.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { selectedMapPinProperty = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Text(
                                        text = "${selectedMapPinProperty!!.locality} • ${selectedMapPinProperty!!.priceDisplay}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F766E)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.openOverlay(ScreenOverlay.PropertyDetail(selectedMapPinProperty!!.id))
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("View Details", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                IntentHelper.dialPhone(context, selectedMapPinProperty!!.ownerPhone)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }

                                        Button(
                                            onClick = {
                                                IntentHelper.openWhatsApp(
                                                    context,
                                                    selectedMapPinProperty!!.ownerPhone,
                                                    "Hi, inquiring about ${selectedMapPinProperty!!.title} from map view."
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Advanced Filter Bottom-Sheet / Modal
        if (showAdvancedFilters) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowAdvancedFilters(false) }
            ) {
                AdvancedFiltersSheetContent(
                    viewModel = viewModel,
                    totalMatches = filteredProperties.size,
                    onApply = { viewModel.setShowAdvancedFilters(false) }
                )
            }
        }
    }
}

@Composable
fun InteractiveMapCanvas(
    properties: List<Property>,
    selectedProperty: Property?,
    onSelectPin: (Property) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0))
    ) {
        // Map Grid & Road Canvas representation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Roads
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(0f, height * 0.35f),
                end = Offset(width, height * 0.35f),
                strokeWidth = 32f
            )
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(width * 0.5f, 0f),
                end = Offset(width * 0.5f, height),
                strokeWidth = 28f
            )
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(0f, height * 0.7f),
                end = Offset(width, height * 0.6f),
                strokeWidth = 24f
            )

            // Metro Corridor Green Line
            drawLine(
                color = Color(0xFF86EFAC),
                start = Offset(width * 0.15f, 0f),
                end = Offset(width * 0.85f, height),
                strokeWidth = 14f
            )
        }

        // Map Price Pins
        properties.forEachIndexed { index, prop ->
            // Distribute pins across realistic positions
            val pinX = when (index % 5) {
                0 -> 0.25f
                1 -> 0.72f
                2 -> 0.45f
                3 -> 0.80f
                else -> 0.30f
            }
            val pinY = when (index % 5) {
                0 -> 0.28f
                1 -> 0.32f
                2 -> 0.52f
                3 -> 0.65f
                else -> 0.75f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = (pinX * 300).dp,
                        top = (pinY * 500).dp
                    )
            ) {
                val isSelected = selectedProperty?.id == prop.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFEF4444) else Color(0xFF1E3A8A),
                    shadowElevation = if (isSelected) 8.dp else 4.dp,
                    modifier = Modifier
                        .clickable { onSelectPin(prop) }
                        .border(
                            1.5.dp,
                            if (isSelected) Color.White else Color(0xFFF59E0B),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = prop.priceDisplay.replace(" / mo", "/m"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedFiltersSheetContent(
    viewModel: SpareViewModel,
    totalMatches: Int,
    onApply: () -> Unit
) {
    val purpose by viewModel.selectedPurpose.collectAsState()
    val selectedBhk by viewModel.selectedBhk.collectAsState()
    val selectedPropertyType by viewModel.selectedPropertyType.collectAsState()
    val selectedFurnishing by viewModel.selectedFurnishing.collectAsState()
    val verifiedOnly by viewModel.verifiedOnly.collectAsState()

    val selectedPostedBy by viewModel.selectedPostedBy.collectAsState()
    val selectedAmenities by viewModel.selectedAmenities.collectAsState()
    val selectedBudget by viewModel.selectedBudgetRange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Advanced Filters", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            TextButton(onClick = { viewModel.resetFilters() }) {
                Text("Reset All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Property Type Selection
        Text("Property Type", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Apartment", "Builder Floor", "Villa", "Plot").forEach { t ->
                FilterChip(
                    selected = selectedPropertyType == t,
                    onClick = { viewModel.setSelectedPropertyType(t) },
                    label = { Text(t, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // BHK Type
        Text("BHK Configuration", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "1 BHK", "2 BHK", "3 BHK", "4+ BHK").forEach { b ->
                FilterChip(
                    selected = (selectedBhk ?: "All") == b,
                    onClick = { viewModel.setSelectedBhk(b) },
                    label = { Text(b, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Budget
        Text("Budget", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            budgetOptionsFor(purpose).forEach { option ->
                FilterChip(
                    selected = selectedBudget == option.label,
                    onClick = { viewModel.setSelectedBudget(option.label) },
                    label = { Text(option.label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Furnishing
        Text("Furnishing Status", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Furnished", "Semi-Furnished", "Unfurnished").forEach { f ->
                FilterChip(
                    selected = selectedFurnishing == f,
                    onClick = { viewModel.setSelectedFurnishing(f) },
                    label = { Text(f, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Posted By
        Text("Posted By", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Owner", "Broker / Agent", "Builder").forEach { p ->
                FilterChip(
                    selected = selectedPostedBy == p,
                    onClick = { viewModel.setSelectedPostedBy(p) },
                    label = { Text(p, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Amenities Checkboxes
        Text("Amenities & Facilities", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Column {
            listOf("Lift", "Parking", "Gated Security", "Power Backup", "Gym", "Park Facing").chunked(2).forEach { rowList ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowList.forEach { amenity ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.setSelectedAmenities(
                                        if (selectedAmenities.contains(amenity)) selectedAmenities - amenity
                                        else selectedAmenities + amenity
                                    )
                                }
                        ) {
                            Checkbox(
                                checked = selectedAmenities.contains(amenity),
                                onCheckedChange = {
                                    viewModel.setSelectedAmenities(
                                        if (it) selectedAmenities + amenity else selectedAmenities - amenity
                                    )
                                }
                            )
                            Text(amenity, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Apply Button
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("apply_filters_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text("Apply Filters (Shows $totalMatches Results)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}


// ---------------------------------------------------------------------------------------------
// Filter helpers
// ---------------------------------------------------------------------------------------------

data class BudgetOption(val label: String, val min: Double, val max: Double)

/** Budget buckets differ for rentals (₹/month) and purchases (₹ total). */
fun budgetOptionsFor(purpose: String): List<BudgetOption> = when (purpose) {
    "Rent" -> listOf(
        BudgetOption("All", 0.0, Double.MAX_VALUE),
        BudgetOption("Under ₹15K", 0.0, 15_000.0),
        BudgetOption("₹15K - 30K", 15_000.0, 30_000.0),
        BudgetOption("₹30K - 50K", 30_000.0, 50_000.0),
        BudgetOption("Above ₹50K", 50_000.0, Double.MAX_VALUE)
    )
    "Commercial" -> listOf(
        BudgetOption("All", 0.0, Double.MAX_VALUE),
        BudgetOption("Under ₹50K", 0.0, 50_000.0),
        BudgetOption("₹50K - 1.5L", 50_000.0, 150_000.0),
        BudgetOption("Above ₹1.5L", 150_000.0, Double.MAX_VALUE)
    )
    else -> listOf(
        BudgetOption("All", 0.0, Double.MAX_VALUE),
        BudgetOption("Under ₹50L", 0.0, 5_000_000.0),
        BudgetOption("₹50L - 1Cr", 5_000_000.0, 10_000_000.0),
        BudgetOption("₹1Cr - 2Cr", 10_000_000.0, 20_000_000.0),
        BudgetOption("Above ₹2Cr", 20_000_000.0, Double.MAX_VALUE)
    )
}

/**
 * The budget selection used to be stored but never applied. Because rent prices are monthly and
 * sale prices are totals, each listing is compared using the bucket set for its own kind.
 */
fun matchesBudgetFilter(property: Property, selectedBudget: String): Boolean {
    if (selectedBudget == "All") return true
    val monthly = property.listingType == ListingType.RENT || property.priceDisplay.contains("/ mo")
    val purpose = when {
        property.listingType == ListingType.COMMERCIAL -> "Commercial"
        monthly -> "Rent"
        else -> "Buy / Sale"
    }
    val option = budgetOptionsFor(purpose).firstOrNull { it.label == selectedBudget } ?: return true
    return property.price >= option.min && property.price < option.max
}

/** "4+ BHK" means four or more bedrooms; "4 BHK" listings used to be excluded by a plain contains(). */
fun matchesBhkFilter(propertyBhk: String, selected: String?): Boolean {
    if (selected == null || selected == "All") return true
    val wanted = selected.takeWhile { it.isDigit() }.toIntOrNull() ?: return true
    val actual = propertyBhk.takeWhile { it.isDigit() }.toIntOrNull() ?: return false
    return if (selected.contains("+")) actual >= wanted else actual == wanted
}

/** Converts labels such as "Just now", "2 days ago", "1 week ago" into a sortable number of days. */
fun postedDateToDays(label: String): Double {
    val text = label.trim().lowercase()
    if (text.contains("just now") || text.contains("today")) return 0.0
    val n = text.takeWhile { it.isDigit() }.toIntOrNull() ?: 1
    return when {
        text.contains("hour") || text.contains("min") -> 0.0
        text.contains("day") -> n.toDouble()
        text.contains("week") -> n * 7.0
        text.contains("month") -> n * 30.0
        else -> Double.MAX_VALUE
    }
}
