package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SignalType
import com.example.data.model.TechnicalAnalysisResult
import com.example.data.repository.IranStockAnalysisService
import com.example.ui.theme.RigourBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IranStockDetailScreen(
    stockName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<TechnicalAnalysisResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val service = remember { IranStockAnalysisService(context) }

    LaunchedEffect(stockName) {
        scope.launch {
            try {
                result = service.analyzeStock(stockName)
                if (result == null) {
                    errorMsg = "دیتای این نماد در دسترس نیست."
                }
            } catch (e: Exception) {
                errorMsg = "خطا: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تحلیل $stockName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00BCD4),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(RigourBackground)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMsg != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMsg!!, color = Color.Red, fontSize = 16.sp)
                }
            } else if (result != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // کارت قیمت و سیگنال اصلی
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "قیمت فعلی: ${result!!.currentPrice}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SignalBadge(signal = result!!.rsiSignal)
                        }
                    }

                    // کارت RSI
                    AnalysisCard(
                        title = "RSI (قدرت نسبی)",
                        value = result!!.rsi.toString(),
                        description = result!!.rsiDescription
                    )

                    // کارت MACD
                    AnalysisCard(
                        title = "MACD",
                        value = result!!.macdHistogram.toString(),
                        description = result!!.macdDescription
                    )

                    // کارت بولینگر
                    AnalysisCard(
                        title = "Bollinger Bands",
                        value = "بالا: ${result!!.bbUpper} / پایین: ${result!!.bbLower}",
                        description = "میانگین: ${result!!.bbMiddle}"
                    )
                }
            }
        }
    }
}

@Composable
fun SignalBadge(signal: SignalType) {
    val (text, color) = when (signal) {
        SignalType.STRONG_BUY -> "خرید قوی" to Color(0xFF22C55E)
        SignalType.BUY -> "خرید" to Color(0xFF86EFAC)
        SignalType.NEUTRAL -> "خنثی" to Color(0xFFEAB308)
        SignalType.SELL -> "فروش" to Color(0xFFFCA5A5)
        SignalType.STRONG_SELL -> "فروش قوی" to Color(0xFFEF4444)
    }
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun AnalysisCard(title: String, value: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = Color(0xFF94A3B8), fontSize = 14.sp)
        }
    }
}
