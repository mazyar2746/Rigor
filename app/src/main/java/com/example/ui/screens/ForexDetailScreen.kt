package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.ForexAnalysisResult
import com.example.data.model.ForexPair
import com.example.data.model.ForexQuote
import com.example.data.model.SignalType
import com.example.data.model.TechnicalAnalysisResult
import com.example.i18n.Language
import com.example.ui.components.CandlestickChartCanvas
import com.example.ui.components.TargetsAndLevelsCard
import com.example.ui.theme.*

/**
 * ForexDetailScreen
 *
 * Detailed view for a forex pair: live price, candlestick chart,
 * technical analysis, and signal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForexDetailScreen(
    pair: ForexPair,
    quote: ForexQuote?,
    analysis: ForexAnalysisResult?,
    candles: List<Candle>,
    isLoading: Boolean,
    errorMessage: String?,
    lang: Language,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isProUser: Boolean = false,
    onNavigateToSubscription: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = pair.displayName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (quote != null) {
                        val isPositive = quote.changePercent >= 0.0
                        Text(
                            text = "${formatForexPrice(quote.price)} (${if (isPositive) "+" else ""}${"%.2f".format(quote.changePercent)}%)",
                            color = if (isPositive) RigourBullish else Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RigourCyan)
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = RigourCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = RigourCyan)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RigourSurface)
        )

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF7F1D1D))
                    .padding(12.dp)
            ) {
                Text(text = errorMessage, color = Color.White, fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (quote != null) {
                PriceCard(quote = quote)
            }

            // Chart card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RigourSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "چارت قیمت",
                        color = RigourCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (candles.isNotEmpty()) {
                        val bbUpper = analysis?.bbUpper ?: candles.maxOfOrNull { it.high } ?: 0.0
                        val bbMiddle = analysis?.bbMiddle ?: candles.last().close
                        val bbLower = analysis?.bbLower ?: candles.minOfOrNull { it.low } ?: 0.0
                        CandlestickChartCanvas(
                            candles = candles,
                            bbUpper = bbUpper,
                            bbMiddle = bbMiddle,
                            bbLower = bbLower,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = RigourCyan)
                            } else {
                                Text(
                                    text = "داده‌ای برای نمایش موجود نیست",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            if (analysis != null) {
                SignalCard(analysis = analysis)
                IndicatorsCard(analysis = analysis)
                // Convert ForexAnalysisResult -> TechnicalAnalysisResult for reuse
                TargetsAndLevelsCard(
                    result = analysis.toTechnicalResult(),
                    lang = lang
                )
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RigourCyan)
                }
            }
        }
    }
}

/**
 * Adapter: ForexAnalysisResult -> TechnicalAnalysisResult
 * So we can reuse TargetsAndLevelsCard without duplicating UI.
 */
private fun ForexAnalysisResult.toTechnicalResult(): TechnicalAnalysisResult {
    return TechnicalAnalysisResult(
        coinSymbol = this.symbol,
        currentPrice = this.currentPrice,
        rsi = this.rsi,
        rsiSignal = this.rsiSignal,
        rsiDescription = this.rsiDescription,
        macdLine = this.macdLine,
        macdSignalLine = this.macdSignalLine,
        macdHistogram = this.macdHistogram,
        macdSignal = this.macdSignal,
        macdDescription = this.macdDescription,
        bbUpper = this.bbUpper,
        bbMiddle = this.bbMiddle,
        bbLower = this.bbLower,
        bbSignal = this.bbSignal,
        bbDescription = this.bbDescription,
        overallSignal = this.overallSignal,
        confidencePercent = this.confidencePercent,
        shortTermTarget = this.shortTermTarget,
        mediumTermTarget = this.mediumTermTarget,
        stopLoss = this.stopLoss,
        resistance2 = this.resistance2,
        resistance1 = this.resistance1,
        pivotPoint = this.pivotPoint,
        support1 = this.support1,
        support2 = this.support2,
        candles = this.candles,
        provenance = this.provenance,
        dataSourceNotice = this.dataSourceNotice
    )
}

@Composable
private fun PriceCard(quote: ForexQuote) {
    val isPositive = quote.changePercent >= 0.0
    val changeColor = if (isPositive) RigourBullish else Color(0xFFEF4444)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RigourSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "قیمت لحظه‌ای", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatForexPrice(quote.price),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${if (isPositive) "+" else ""}${formatForexPrice(quote.change)} (${if (isPositive) "+" else ""}${"%.2f".format(quote.changePercent)}%)",
                color = changeColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "High: ${formatForexPrice(quote.dayHigh)}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(text = "Low: ${formatForexPrice(quote.dayLow)}", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SignalCard(analysis: ForexAnalysisResult) {
    val (signalText, signalColor) = signalLabel(analysis.overallSignal)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RigourSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "سیگنال کلی", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = signalText,
                    color = signalColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "اطمینان", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${analysis.confidencePercent}%",
                    color = RigourCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun IndicatorsCard(analysis: ForexAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RigourSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "اندیکاتورها",
                color = RigourCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            IndicatorRow(
                label = "RSI (14)",
                value = "%.2f".format(analysis.rsi),
                description = analysis.rsiDescription
            )
            HorizontalDivider(color = RigourCardBorder, modifier = Modifier.padding(vertical = 8.dp))
            IndicatorRow(
                label = "MACD",
                value = "%.4f".format(analysis.macdHistogram),
                description = analysis.macdDescription
            )
            HorizontalDivider(color = RigourCardBorder, modifier = Modifier.padding(vertical = 8.dp))
            IndicatorRow(
                label = "Bollinger",
                value = "${formatForexPrice(analysis.bbLower)} - ${formatForexPrice(analysis.bbUpper)}",
                description = analysis.bbDescription
            )
        }
    }
}

@Composable
private fun IndicatorRow(label: String, value: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
        Text(text = value, color = RigourCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun signalLabel(signal: SignalType): Pair<String, Color> = when (signal) {
    SignalType.STRONG_BUY -> "خرید قوی" to Color(0xFF10B981)
    SignalType.BUY -> "خرید" to Color(0xFF34D399)
    SignalType.NEUTRAL -> "خنثی" to Color(0xFF94A3B8)
    SignalType.SELL -> "فروش" to Color(0xFFF87171)
    SignalType.STRONG_SELL -> "فروش قوی" to Color(0xFFEF4444)
}

