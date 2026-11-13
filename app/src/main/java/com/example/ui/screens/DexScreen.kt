package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.DexPool
import com.example.data.model.DexQuote
import com.example.data.model.DexToken
import com.example.data.model.DexTokens
import com.example.i18n.Language
import com.example.ui.components.CandlestickChartCanvas
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexScreen(
    quote: DexQuote?,
    isLoading: Boolean,
    errorMessage: String?,
    chartData: List<Candle>,
    isChartLoading: Boolean,
    newPools: List<DexPool>,
    isPoolsLoading: Boolean,
    lang: Language,
    onBack: () -> Unit,
    onGetQuote: (String, String, String, String) -> Unit,
    onLoadChart: (String, String) -> Unit,
    onLoadPools: (String) -> Unit,
    onClearState: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("سواپ", "چارت و توکن‌های جدید")
    val defaultPoolAddress = "0x16b9a82891338f9bA80E2D6970FdDbE312E9690f" // WBNB/USDT pool on BSC

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            onLoadChart("bsc", defaultPoolAddress)
            onLoadPools("bsc")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(RigourBackground)
    ) {
        TopAppBar(
            title = { Text("دکس (BSC)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RigourCyan)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RigourSurface)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = RigourSurface,
            contentColor = RigourCyan
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) RigourCyan else Color.Gray) }
                )
            }
        }

        if (selectedTab == 0) {
            ProfessionalSwapTab(quote, isLoading, errorMessage, lang, onGetQuote, onClearState)
        } else {
            ChartAndPoolsTab(chartData, isChartLoading, newPools, isPoolsLoading)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalSwapTab(
    quote: DexQuote?,
    isLoading: Boolean,
    errorMessage: String?,
    lang: Language,
    onGetQuote: (String, String, String, String) -> Unit,
    onClearState: () -> Unit
) {
    var sellAmount by remember { mutableStateOf("0.1") }
    val defaultTokens = DexTokens.BSC_TOKENS
    var sellToken by remember { mutableStateOf(defaultTokens.getOrNull(0) ?: defaultTokens.first()) } // BNB
    var buyToken by remember { mutableStateOf(defaultTokens.getOrNull(1) ?: defaultTokens.last()) } // USDT
    val dummyWallet = "0x736CC22F2b19adcAE6Ef4e5e13eab6BbF35B1Aab"
    
    var showSellSelector by remember { mutableStateOf(false) }
    var showBuySelector by remember { mutableStateOf(false) }

    LaunchedEffect(sellAmount, sellToken, buyToken) {
        val amount = sellAmount.toDoubleOrNull() ?: 0.0
        if (amount > 0) {
            onGetQuote(sellToken.address, buyToken.address, sellAmount, dummyWallet)
        } else {
            onClearState()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // --- SELL BOX ---
        Card(colors = CardDefaults.cardColors(containerColor = RigourSurface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("شما می‌فروشید", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = sellAmount,
                        onValueChange = { sellAmount = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    // TOKEN SELECTOR BUTTON
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RigourBackground)
                            .clickable { showSellSelector = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sellToken.symbol, color = RigourCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = RigourCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // --- SWAP ICON ---
        Box(
            modifier = Modifier
                .offset(y = (-12).dp)
                .align(Alignment.CenterHorizontally)
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(RigourSurface)
                .clickable {
                    val temp = sellToken
                    sellToken = buyToken
                    buyToken = temp
                },
            contentAlignment = Alignment.Center
        ) {
            Text("↓", color = RigourCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // --- BUY BOX ---
        Card(colors = CardDefaults.cardColors(containerColor = RigourSurface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("شما دریافت می‌کنید", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLoading) "..." else {
                            val amt = quote?.buyAmount?.toDoubleOrNull()?.div(1e18) ?: 0.0
                            if (amt > 0) String.format("%.4f", amt) else "0.0000"
                        },
                        color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f)
                    )
                    // TOKEN SELECTOR BUTTON
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RigourBackground)
                            .clickable { showBuySelector = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(buyToken.symbol, color = RigourCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = RigourCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (errorMessage != null) {
            Text(text = errorMessage, color = Color.Red, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = { 
                android.widget.Toast.makeText(
                    androidx.compose.ui.platform.LocalContext.current,
                    "اتصال کیف پول در نسخه بعدی فعال می‌شود",
                    android.widget.Toast.LENGTH_SHORT
                ).show() 
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RigourCyan),
            shape = RoundedCornerShape(16.dp),
            enabled = quote != null && !isLoading
        ) {
            Text(text = if (isLoading) "در حال دریافت قیمت..." else "اتصال کیف پول و سواپ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }

    // --- TOKEN SELECTOR BOTTOM SHEETS ---
    if (showSellSelector) {
        TokenSelectorSheet(onDismiss = { showSellSelector = false }, onSelect = { sellToken = it; showSellSelector = false })
    }
    if (showBuySelector) {
        TokenSelectorSheet(onDismiss = { showBuySelector = false }, onSelect = { buyToken = it; showBuySelector = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenSelectorSheet(onDismiss: () -> Unit, onSelect: (DexToken) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RigourSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("انتخاب توکن", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(DexTokens.BSC_TOKENS) { token ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(token) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(RigourBackground), contentAlignment = Alignment.Center) {
                            Text(token.symbol.take(1), color = RigourCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(token.symbol, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(token.name, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartAndPoolsTab(chartData: List<Candle>, isChartLoading: Boolean, newPools: List<DexPool>, isPoolsLoading: Boolean) {
    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("چارت لحظه‌ای WBNB/USDT", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = RigourSurface), shape = RoundedCornerShape(12.dp)) {
                if (isChartLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RigourCyan) }
                } else if (chartData.isNotEmpty()) {
                    CandlestickChartCanvas(
                        candles = chartData,
                        bbUpper = chartData.maxOfOrNull { it.close } ?: 0.0,
                        bbMiddle = chartData.map { it.close }.average().takeIf { !it.isNaN() } ?: 0.0,
                        bbLower = chartData.minOfOrNull { it.close } ?: 0.0,
                        modifier = Modifier.fillMaxWidth().height(250.dp).padding(8.dp)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) { Text("داده‌ای برای نمایش نیست", color = Color.Gray) }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("توکن‌های جدید (BSC)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isPoolsLoading) {
            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RigourCyan) } }
        } else {
            items(newPools) { pool ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RigourSurface), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(pool.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(pool.network.uppercase(), color = RigourCyan, fontSize = 12.sp)
                        }
                        Text("$" + "%.4f".format(pool.priceUsd), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
