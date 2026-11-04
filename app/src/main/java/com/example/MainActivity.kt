package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.RigourViewModel
import com.example.ui.Screen
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: RigourViewModel = viewModel()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val layoutDirection = if (currentLanguage == Language.PERSIAN) LayoutDirection.Rtl else LayoutDirection.Ltr

            RigorTheme {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    BackHandler(enabled = currentScreen !is Screen.Markets) {
                        when (currentScreen) {
                            is Screen.IranStockDetail -> viewModel.navigateTo(Screen.IranStockSearch)
                            is Screen.IranStockSearch -> viewModel.navigateTo(Screen.Markets)
                            is Screen.Signals -> viewModel.navigateTo(Screen.Markets)
                            is Screen.Subscription -> viewModel.navigateTo(Screen.Markets)
                            is Screen.Settings -> viewModel.navigateTo(Screen.Markets)
                            else -> viewModel.navigateTo(Screen.Markets)
                        }
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = R.drawable.rigor_logo),
                                            contentDescription = "Rigor Logo",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, RigourCyan.copy(alpha = 0.5f.float), RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = StringsDictionary.get("app_name", currentLanguage),
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                actions = {
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("کرستو") },
                                            onClick = { menuExpanded = false; viewModel.navigateTo(Screen.Markets) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("«برست آرلای") },
                                            onClick = { menuExpanded = false; viewModel.navigateTo(Screen.IranStockSearch) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("فاركس (بهزود)") },
                                            onClick = { menuExpanded = false },
                                            enabled = false
                                        )
                                        DropdownMenuItem(
                                            text = { Text("دکس (بهزود)") },
                                            onClick = { menuExpanded = false },
                                            enabled = false
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = RigourSurface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = RigourSurface,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                val marketsSelected = currentScreen is Screen.Markets || currentScreen is Screen.CoinDetail || currentScreen is Screen.IranStockSearch || currentScreen is Screen.IranStockDetail
                                val signalsSelected = currentScreen is Screen.Signals
                                val subSelected = currentScreen is Screen.Subscription
                                val settingsSelected = currentScreen is Screen.Settings

                                val itemColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RigourCyan,
                                    selectedTextColor = RigourCyan,
                                    indicatorColor = RigourCyan.copy(alpha = 0.15f),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )

                                NavigationBarItem(
                                    selected = marketsSelected,
                                    onClick = { viewModel.navigateTo(Screen.Markets) },
                                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Markets") },
                                    label = { Text(StringsDictionary.get("nav_markets", currentLanguage), fontSize = 10.sp) },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = signalsSelected,
                                    onClick = { viewModel.navigateTo(Screen.Signals) },
                                    icon = { Icon(Icons.Default.AutoGraph, contentDescription = "Signals") },
                                    label = { Text(StringsDictionary.get("nav_signals", currentLanguage), fontSize = 10.sp) },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = subSelected,
                                    onClick = { viewModel.navigateTo(Screen.Subscription) },
                                    icon = { Icon(Icons.Default.Lock, contentDescription = "Pro") },
                                    label = { Text(StringsDictionary.get("nav_subscription", currentLanguage), fontSize = 10.sp) },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = settingsSelected,
                                    onClick = { viewModel.navigateTo(Screen.Settings) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text(StringsDictionary.get("nav_settings", currentLanguage), fontSize = 10.sp) },
                                    colors = itemColors
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(RigourBackground)
                        ) {
                            when (currentScreen) {
                                is Screen.IranStockDetail -> {
                                    IranStockDetailScreen(
                                        stockName = (currentScreen as Screen.IranStockDetail).stockName,
                                        onBack = { viewModel.navigateTo(Screen.IranStockSearch) },
                                        lang = currentLanguage
                                    )
                                }
                                is Screen.IranStockSearch -> {
                                    IranStockSearchScreen(
                                        onBack = { viewModel.navigateTo(Screen.Markets) },
                                        onStockSelected = { viewModel.navigateTo(Screen.IranStockDetail(it)) }
                                    )
                                }
                                is Screen.Markets -> {
                                    MarketDashboardScreen(
                                        onNavigateToIranStock = { viewModel.navigateTo(Screen.IranStockSearch) }
                                    )
                                }
                                else -> {
                                    MarketDashboardScreen(
                                        onNavigateToIranStock = { viewModel.navigateTo(Screen.IranStockSearch) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
