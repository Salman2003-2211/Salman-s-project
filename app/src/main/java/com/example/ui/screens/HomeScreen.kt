package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Property
import com.example.ui.AppTab
import com.example.ui.ScreenOverlay
import com.example.ui.SpareViewModel
import com.example.ui.components.IntentHelper
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.theme.AppStrings

@Composable
fun HomeScreen(
    viewModel: SpareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val lang = userProfile.language
    val scrollState = rememberScrollState()

    val trendingProperties = remember(properties) {
        properties.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp) // padding for navigation bar
    ) {
        // 1. Header & Greeting Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))
                                    )
                                )
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Apartment,
                                contentDescription = "S.P.A.RE",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val clientDisplayName = if (userProfile.fullName.isNotBlank()) userProfile.fullName else "Client"
                            Text(
                                text = "Hello, $clientDisplayName",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.VerifiedUser,
                                    contentDescription = "Verified Client",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (userProfile.mobileNumber.isNotBlank()) "+91 ${userProfile.mobileNumber}" else "Verified Member",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Profile Avatar Shortcut
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE))
                            .clickable { viewModel.openOverlay(ScreenOverlay.Profile) }
                            .testTag("home_avatar_profile"),
                        contentAlignment = Alignment.Center
                    ) {
                        val initialLetter = if (userProfile.fullName.isNotBlank()) {
                            userProfile.fullName.trim().take(1).uppercase()
                        } else "C"
                        Text(
                            text = initialLetter,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF),
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "What property or loan would you like to explore today?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Top Action Navigation Bar (Row of 4 Quick Action Cards/Buttons)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Quick Actions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopActionCard(
                    title = AppStrings.profile(lang),
                    icon = Icons.Filled.Person,
                    iconBg = Color(0xFFE0E7FF),
                    iconTint = Color(0xFF3730A3),
                    testTag = "quick_action_profile",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.openOverlay(ScreenOverlay.Profile)
                }

                TopActionCard(
                    title = AppStrings.postProperty(lang),
                    icon = Icons.Filled.AddHomeWork,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFB45309),
                    testTag = "quick_action_post_property",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setActiveTab(AppTab.POST)
                }

                TopActionCard(
                    title = AppStrings.myAds(lang),
                    icon = Icons.Filled.DashboardCustomize,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF047857),
                    testTag = "quick_action_my_ads",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.openOverlay(ScreenOverlay.MyAds)
                }

                TopActionCard(
                    title = AppStrings.about(lang),
                    icon = Icons.Filled.SupportAgent,
                    iconBg = Color(0xFFFCE7F3),
                    iconTint = Color(0xFFBE185D),
                    testTag = "quick_action_about",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.openOverlay(ScreenOverlay.AboutUs)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Center / Mid Section (Banner & Highlights Area)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Special Highlights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Verified Direct",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carousel / Horizontal Scroll of Promotional Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PromoBannerCard(
                    title = "Zero Brokerage Deals",
                    subtitle = "Direct Owner Contact • 100% Genuine Listings",
                    cta = "Explore Rentals",
                    gradient = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A)),
                    badgeColor = Color(0xFF10B981),
                    badgeText = "NO BROKER"
                ) {
                    viewModel.setPurpose("Rent")
                    viewModel.setActiveTab(AppTab.SEARCH)
                }

                PromoBannerCard(
                    title = "Instant Home Loan",
                    subtitle = "Lowest 8.25% ROI • ₹5L to ₹5Cr Pre-approved",
                    cta = "Check Eligibility",
                    gradient = listOf(Color(0xFF064E3B), Color(0xFF047857)),
                    badgeColor = Color(0xFFF59E0B),
                    badgeText = "PRE-APPROVED"
                ) {
                    viewModel.setActiveTab(AppTab.LOAN)
                }

                PromoBannerCard(
                    title = "Registry Freehold Plots",
                    subtitle = "50 to 500 Gaj with Bank Loan in Rohini & Noida",
                    cta = "View Plots",
                    gradient = listOf(Color(0xFF78350F), Color(0xFFD97706)),
                    badgeColor = Color(0xFF38BDF8),
                    badgeText = "GAJ PLOTS"
                ) {
                    viewModel.setPurpose("Plot / Land")
                    viewModel.setActiveTab(AppTab.SEARCH)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Primary Discovery & Services Bar (Row of 3 Prominent Cards/Pills)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Core Real Estate Services",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryServiceCard(
                    title = "Meet Partner",
                    subtitle = "Channel Partners & Agents",
                    icon = Icons.Filled.Handshake,
                    accentColor = Color(0xFF2563EB),
                    testTag = "service_card_partner",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setActiveTab(AppTab.PARTNERS)
                }

                PrimaryServiceCard(
                    title = "Search Property",
                    subtitle = "Buy, Rent, Plots & Map",
                    icon = Icons.Filled.Search,
                    accentColor = Color(0xFF0D9488),
                    testTag = "service_card_search",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setActiveTab(AppTab.SEARCH)
                }

                PrimaryServiceCard(
                    title = "Loan Services",
                    subtitle = "LAP, Home Loan, 5L+",
                    icon = Icons.Filled.AccountBalance,
                    accentColor = Color(0xFFD97706),
                    testTag = "service_card_loan",
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setActiveTab(AppTab.LOAN)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Trending Listings Preview Section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Properties near You",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { viewModel.setActiveTab(AppTab.SEARCH) }
                ) {
                    Text("View All", fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                trendingProperties.forEach { property ->
                    TrendingPropertyCard(
                        property = property,
                        onCardClick = { viewModel.openOverlay(ScreenOverlay.PropertyDetail(property.id)) },
                        onFavoriteClick = { viewModel.toggleFavorite(property.id, context) },
                        onCallClick = { IntentHelper.dialPhone(context, property.ownerPhone) },
                        onWhatsAppClick = {
                            IntentHelper.openWhatsApp(
                                context,
                                property.ownerPhone,
                                "Hello, I found your listing '${property.title}' on S.P.A.RE and want to discuss details."
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TopActionCard(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PromoBannerCard(
    title: String,
    subtitle: String,
    cta: String,
    gradient: List<Color>,
    badgeColor: Color,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        Icons.Filled.Stars,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE047)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrimaryServiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(125.dp)
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
fun TrendingPropertyCard(
    property: Property,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Top Bar with tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color(0xFFDBEAFE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = property.listingType.label,
                            color = Color(0xFF1E40AF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (property.isVerified) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Verified",
                                    color = Color(0xFF15803D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (property.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Save Property",
                        tint = if (property.isFavorite) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Photo Preview (via AsyncImage/Coil)
            val displayPhoto = property.imageUrls.firstOrNull() ?: property.imageUrl
            if (displayPhoto.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    AsyncImage(
                        model = displayPhoto,
                        contentDescription = property.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "${property.validityDaysRemaining}d left",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Title & Locality
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Text(
                    text = property.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${property.locality} • ${property.distanceInfo}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Price and Specs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = property.priceDisplay,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F766E)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (property.areaGaj > 0) {
                            SpecBadge("${property.areaGaj} Gaj")
                        }
                        if (property.bhk != "N/A") {
                            SpecBadge(property.bhk)
                        }
                        SpecBadge(property.furnishing)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Call & WhatsApp Action Strip
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Posted by: ${property.postedBy} (${property.ownerName})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onCallClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 34.dp)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = "Call", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = onWhatsAppClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.defaultMinSize(minWidth = 96.dp, minHeight = 34.dp)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun SpecBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.wrapContentWidth()
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
