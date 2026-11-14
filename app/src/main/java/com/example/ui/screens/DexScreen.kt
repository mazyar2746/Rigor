@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.Candle
import com.example.data.model.DexPool
import com.example.data.model.DexQuote
import com.example.data.model.DexToken
import com.example.data.model.DexTokenInfo
import com.example.data.model.DexTokens
import com.example.data.model.TokenDecimals
import com.example.i18n.Language
import com.example.ui.components.CandlestickChartCanvas
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurface
import kotlinx.coroutines.delay

private fun networkName(chainId: Int): String = when (chainId) {
    56 -> "BSC"
    1 -> "Ethereum"
    137 -> "Polygon"
    42161 -> "Arbitrum"
    else -> "BSC"
}

private fun chainKey(chainId: Int): String = when (chainId) {
    56 -> "bsc"
    1 -> "eth"
    137 -> "pol"
    42161 -> "arb"
    else -> "bsc"
}

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
    onGetQuote: (String, String, String, String, String, Int) -> Unit,
    onLoadChart: (String, String) -> Unit,
    onLoadChartForSymbol: (String) -> Unit,
    onLoadPools: (String) -> Unit,
    onLookupToken: (String, (DexTokenInfo?) -> Unit) -> Unit,
    onClearState: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var chartSymbol by remember { mutableStateOf("BNB") }
    val tabs = listOf("سواپ", "چارت و توکن‌های جدید")

    LaunchedEffect(selectedTab, chartSymbol) {
        if (selectedTab == 1) {
            onLoadChartForSymbol(chartSymbol)
            onLoadPools("bsc")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(RigourBackground)) {
        TopAppBar(
            title = { Text("دکس چندشبکه‌ای", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RigourCyan)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RigourSurface)
        )

        TabRow(selectedTabIndex = selectedTab, containerColor = RigourSurface, contentColor = RigourCyan) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) RigourCyan else Color.Gray) })
            }
        }

        if (selectedTab == 0) {
            ProfessionalSwapTab(quote, isLoading, errorMessage, lang, onGetQuote, onClearState, onLookupToken)
        } else {
            ChartAndPoolsTab(chartData, isChartLoading, newPools, isPoolsLoading, chartSymbol, onSymbolChange = { chartSymbol = it })
        }
    }
}

@Composable
fun ProfessionalSwapTab(
    quote: DexQuote?,
    isLoading: Boolean,
    errorMessage: String?,
    lang: Language,
    onGetQuote: (String, String, String, String, String, Int) -> Unit,
    onClearState: () -> Unit,
    onLookupToken: (String, (DexTokenInfo?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var sellAmount by remember { mutableStateOf("0.1") }
    var sellToken by remember { mutableStateOf(DexTokens.BSC_TOKENS[0]) }
    var buyToken by remember { mutableStateOf(DexTokens.BSC_TOKENS[1]) }
    val dummyWallet = "0x736CC22F2b19adcAE6Ef4e5e13eab6BbF35B1Aab"
    var showSellSelector by remember { mutableStateOf(false) }
    var showBuySelector by remember { mutableStateOf(false) }

    val sameNetwork = sellToken.chainId == buyToken.chainId

    LaunchedEffect(sellAmount, sellToken, buyToken) {
        val amount = sellAmount.toDoubleOrNull() ?: 0.0
        if (amount > 0 && sameNetwork) {
            onGetQuote(sellToken.address, buyToken.address, sellAmount, dummyWallet, sellToken.symbol, sellToken.chainId)
        } else {
            onClearState()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = RigourSurface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("شما می‌فروشید", color = Color.Gray, fontSize = 12.sp)
                    Text(networkName(sellToken.chainId), color = RigourCyan.copy(alpha = 0.7f), fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = sellAmount,
                        onValueChange = { sellAmount = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(RigourBackground).clickable { showSellSelector = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sellToken.symbol, color = RigourCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = RigourCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier.offset(y = (-12).dp).align(Alignment.CenterHorizontally).size(40.dp).clip(RoundedCornerShape(50)).background(RigourSurface).clickable {
                val temp = sellToken; sellToken = buyToken; buyToken = temp
            },
            contentAlignment = Alignment.Center
        ) { Text("↓", color = RigourCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold) }

        Card(colors = CardDefaults.cardColors(containerColor = RigourSurface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("شما دریافت می‌کنید", color = Color.Gray, fontSize = 12.sp)
                    Text(networkName(buyToken.chainId), color = RigourCyan.copy(alpha = 0.7f), fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLoading) "..." else {
                            val amt = quote?.buyAmount?.toDoubleOrNull()?.div(java.math.BigDecimal.TEN.pow(TokenDecimals.get(buyToken.symbol)).toDouble()) ?: 0.0
                            String.format("%.4f", amt)
                        },
                        color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(RigourBackground).clickable { showBuySelector = true }.padding(horizontal = 12.dp, vertical = 8.dp),
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

        if (sameNetwork) {
            Button(
                onClick = { android.widget.Toast.makeText(context, "اتصال کیف پول در نسخه بعدی فعال می‌شود", android.widget.Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RigourCyan),
                shape = RoundedCornerShape(16.dp),
                enabled = quote != null && !isLoading
            ) { Text(text = if (isLoading) "در حال دریافت قیمت..." else "اتصال کیف پول و سواپ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        } else {
            Text(
                "شبکه‌های این دو توکن متفاوت هستند.",
                color = Color(0xFFF59E0B), fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = {
                    val fromChain = chainKey(sellToken.chainId)
                    val toChain = chainKey(buyToken.chainId)
                    val amount = (sellAmount.toDoubleOrNull() ?: 0.1) * Math.pow(10.0, TokenDecimals.get(sellToken.symbol).toDouble())
                    val rangoUrl = "https://app.rango.exchange/swap/" +
                            "${sellToken.symbol}.${sellToken.address}-${sellToken.chainId}/" +
                            "${buyToken.symbol}.${buyToken.address}-${buyToken.chainId}/" +
                            "?${sellAmount}"
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rangoUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "خطا در باز کردن مرورگر: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("سواپ Cross-Chain (اتصال به Jumper)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    if (showSellSelector) TokenSelectorSheet(onDismiss = { showSellSelector = false }, onSelect = { sellToken = it; showSellSelector = false }, onLookupToken = onLookupToken)
    if (showBuySelector) TokenSelectorSheet(onDismiss = { showBuySelector = false }, onSelect = { buyToken = it; showBuySelector = false }, onLookupToken = onLookupToken)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenSelectorSheet(
    onDismiss: () -> Unit,
    onSelect: (DexToken) -> Unit,
    onLookupToken: (String, (DexTokenInfo?) -> Unit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var foundToken by remember { mutableStateOf<DexTokenInfo?>(null) }
    var isSearchingAddress by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        val cleanAddress = searchQuery.trim().replace("\\s".toRegex(), "")
        if (cleanAddress.startsWith("0x") && cleanAddress.length == 42) {
            isSearchingAddress = true
            delay(500)
            onLookupToken(cleanAddress) { result ->
                foundToken = result
                isSearchingAddress = false
            }
        } else {
            foundToken = null
            isSearchingAddress = false
        }
    }

    val filteredTokens = DexTokens.BSC_TOKENS.filter {
        it.symbol.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RigourSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("انتخاب توکن", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجوی نام، نماد یا آدرس قرارداد (0x...)", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = RigourCyan) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = RigourBackground, unfocusedContainerColor = RigourBackground,
                    focusedBorderColor = RigourCyan, unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val localFoundToken = foundToken
            if (isSearchingAddress) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RigourCyan, modifier = Modifier.size(30.dp))
                }
            } else if (searchQuery.trim().replace("\\s".toRegex(), "").startsWith("0x") && searchQuery.trim().replace("\\s".toRegex(), "").length == 42) {
                if (localFoundToken != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onSelect(DexToken(
                                symbol = localFoundToken.symbol,
                                name = localFoundToken.name,
                                address = localFoundToken.address,
                                priceUsd = 0.0,
                                imageUrl = null,
                                chainId = localFoundToken.chainId
                            ))
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(RigourCyan.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(localFoundToken.symbol.take(1), color = RigourCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(localFoundToken.symbol, color = RigourCyan, fontWeight = FontWeight.Bold)
                            Text(localFoundToken.name, color = Color.White, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(RigourCyan.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(networkName(localFoundToken.chainId), color = RigourCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("توکنی با این آدرس پیدا نشد.", color = Color.Red, modifier = Modifier.padding(16.dp), fontSize = 13.sp)
                }
            } else {
                LazyColumn {
                    items(filteredTokens) { token ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(token) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(RigourBackground), contentAlignment = Alignment.Center) {
                                Text(token.symbol.take(1), color = RigourCyan, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(token.symbol, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(token.name, color = Color.Gray, fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(RigourBackground).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(networkName(token.chainId), color = RigourCyan.copy(alpha = 0.8f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartAndPoolsTab(
    chartData: List<Candle>,
    isChartLoading: Boolean,
    newPools: List<DexPool>,
    isPoolsLoading: Boolean,
    chartSymbol: String,
    onSymbolChange: (String) -> Unit
) {
    var showSymbolSelector by remember { mutableStateOf(false) }
    val popularSymbols = listOf("BNB", "BTC", "ETH", "SOL", "XRP", "DOGE", "ADA", "AVAX")

    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("چارت $chartSymbol/USDT", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RigourSurface).clickable { showSymbolSelector = true }.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(chartSymbol, color = RigourCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = RigourCyan, modifier = Modifier.size(16.dp))
                }
            }
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
            Text("توکن‌های پرطرفدار", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isPoolsLoading) {
            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RigourCyan) } }
        } else {
            items(newPools) { pool ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RigourSurface), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(RigourBackground), contentAlignment = Alignment.Center) {
                                    Text(pool.symbol.take(1), color = RigourCyan, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(pool.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(pool.name, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$" + "%.6f".format(pool.priceUsd), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val changeColor = if (pool.priceChange24h >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                Text(
                                    (if (pool.priceChange24h >= 0) "+" else "") + "%.2f".format(pool.priceChange24h) + "%",
                                    color = changeColor, fontSize = 11.sp, fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSymbolSelector) {
        ModalBottomSheet(onDismissRequest = { showSymbolSelector = false }, containerColor = RigourSurface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("انتخاب ارز برای چارت", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn {
                    items(popularSymbols) { sym ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSymbolChange(sym); showSymbolSelector = false }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sym, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("/ USDT", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
