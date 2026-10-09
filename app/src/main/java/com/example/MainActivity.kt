package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.ScreenOverlay
import com.example.ui.SpareViewModel
import com.example.ui.screens.*
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SpareMainApp()
            }
        }
    }
}

@Composable
fun SpareMainApp(
    viewModel: SpareViewModel = viewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val currentOverlay by viewModel.currentOverlay.collectAsState()
    val lang = userProfile.language

    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    DisposableEffect(auth) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val name = user.displayName ?: "Google User"
                val email = user.email ?: ""
                viewModel.login(name, "9818452198", email)
            }
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    // Back handler for sub-screens & overlays
    BackHandler(enabled = currentOverlay !is ScreenOverlay.None) {
        viewModel.closeOverlay()
    }

    if (!isLoggedIn || currentOverlay is ScreenOverlay.Login) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                viewModel.closeOverlay()
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                if (currentOverlay is ScreenOverlay.None) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        // 1. Home
                        NavigationBarItem(
                            selected = activeTab == AppTab.HOME,
                            onClick = { viewModel.setActiveTab(AppTab.HOME) },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == AppTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text(if (lang == AppLanguage.HINDI) "होम" else "Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2563EB),
                                indicatorColor = Color(0xFFDBEAFE)
                            ),
                            modifier = Modifier.testTag("nav_item_home")
                        )

                        // 2. Search
                        NavigationBarItem(
                            selected = activeTab == AppTab.SEARCH,
                            onClick = { viewModel.setActiveTab(AppTab.SEARCH) },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == AppTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                    contentDescription = "Search"
                                )
                            },
                            label = { Text(if (lang == AppLanguage.HINDI) "खोजें" else "Search", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2563EB),
                                indicatorColor = Color(0xFFDBEAFE)
                            ),
                            modifier = Modifier.testTag("nav_item_search")
                        )

                        // 3. Post Property
                        NavigationBarItem(
                            selected = activeTab == AppTab.POST,
                            onClick = { viewModel.setActiveTab(AppTab.POST) },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == AppTab.POST) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                                    contentDescription = "Post Property",
                                    tint = if (activeTab == AppTab.POST) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text(if (lang == AppLanguage.HINDI) "पोस्ट करें" else "Post", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFD97706),
                                indicatorColor = Color(0xFFFEF3C7)
                            ),
                            modifier = Modifier.testTag("nav_item_post")
                        )

                        // 4. Meet Partner (Partners)
                        NavigationBarItem(
                            selected = activeTab == AppTab.PARTNERS,
                            onClick = { viewModel.setActiveTab(AppTab.PARTNERS) },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == AppTab.PARTNERS) Icons.Filled.Handshake else Icons.Outlined.Handshake,
                                    contentDescription = "Meet Partner"
                                )
                            },
                            label = { Text(if (lang == AppLanguage.HINDI) "पार्टनर्स" else "Partners", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2563EB),
                                indicatorColor = Color(0xFFDBEAFE)
                            ),
                            modifier = Modifier.testTag("nav_item_partners")
                        )

                        // 5. Loan Assistance
                        NavigationBarItem(
                            selected = activeTab == AppTab.LOAN,
                            onClick = { viewModel.setActiveTab(AppTab.LOAN) },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == AppTab.LOAN) Icons.Filled.AccountBalance else Icons.Outlined.AccountBalance,
                                    contentDescription = "Loan"
                                )
                            },
                            label = { Text(if (lang == AppLanguage.HINDI) "लोन" else "Loan", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2563EB),
                                indicatorColor = Color(0xFFDBEAFE)
                            ),
                            modifier = Modifier.testTag("nav_item_loan")
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab Content
                when (activeTab) {
                    AppTab.HOME -> HomeScreen(viewModel = viewModel)
                    AppTab.SEARCH -> SearchScreen(viewModel = viewModel)
                    AppTab.POST -> PostPropertyScreen(viewModel = viewModel)
                    AppTab.PARTNERS -> PartnersScreen(viewModel = viewModel)
                    AppTab.LOAN -> LoanScreen(viewModel = viewModel)
                }

                // Overlays
                when (val overlay = currentOverlay) {
                    is ScreenOverlay.Profile -> ProfileScreen(viewModel = viewModel)
                    is ScreenOverlay.MyAds -> MyAdsScreen(viewModel = viewModel)
                    is ScreenOverlay.AboutUs -> AboutUsScreen(viewModel = viewModel)
                    is ScreenOverlay.PropertyDetail -> PropertyDetailModal(propertyId = overlay.propertyId, viewModel = viewModel)
                    else -> Unit
                }
            }
        }
    }
}
