package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ForexPair
import com.example.data.model.ForexPairs
import com.example.data.model.ForexQuote
import com.example.data.model.ForexNewsItem
import com.example.ui.components.NewsTicker
import com.example.i18n.Language
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourBullish
import com.example.ui.theme.RigourCardBorder
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurface
import kotlinx.coroutines.launch

/**
 * ForexScreen
 *
 * Displays forex pairs in 3 horizontal pages (10 pairs each).
 * Swipe left/right to change page. Tap a pair to open detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForexScreen(
    quotes: Map<String, ForexQuote>,
    isLoading: Boolean,
    errorMessage: String?,
    lang: Language,
    onBack: () -> Unit,
    onSelectPair: (ForexPair) -> Unit,
    onRefresh: () -> Unit,
    news: List<ForexNewsItem> = emptyList(),
    persianNews: List<ForexNewsItem> = emptyList()
) {
    val pages = ForexPairs.ALL_PAGES
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val pageTitles = if (lang == Language.PERSIAN) listOf("جفت‌ارزهای اصلی", "کراس‌ها", "اگزوتیک") else listOf("Major Pairs", "Crosses", "Exotics")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = if (lang == Language.PERSIAN) "فارکس" else "Forex",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
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

        // Error banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF7F1D1D))
                    .padding(12.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }


        // News Ticker (live from Finnhub)
        NewsTicker(news = news, isRtl = false)
        Spacer(modifier = Modifier.height(4.dp))
        NewsTicker(news = persianNews, isRtl = true)
        // Page title
        Text(
            text = pageTitles.getOrElse(pagerState.currentPage) { "" },
            color = RigourCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Horizontal pager with 10 pairs per page
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            val pagePairs = pages[pageIndex]
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(pagePairs) { pair ->
                    ForexPairCard(
                        pair = pair,
                        
                                lang = lang,quote = quotes[pair.symbol.uppercase()],
                        onClick = { onSelectPair(pair) }
                    )
                }
            }
        }

        // Page indicator dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                val active = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 10.dp else 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) RigourCyan else Color.Gray.copy(alpha = 0.4f))
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(i) }
                        }
                )
            }
        }
    }
}

@Composable
private fun ForexPairCard(
    pair: ForexPair,
    quote: ForexQuote?,
    lang: Language,
    onClick: () -> Unit
) {
    val isPositive = (quote?.changePercent ?: 0.0) >= 0.0
    val changeColor = if (isPositive) RigourBullish else Color(0xFFEF4444)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RigourSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pair.displayName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (lang == Language.PERSIAN) { if (pair.category == "Major") "اصلی" else if (pair.category == "Cross") "کراس" else "اگزوتیک" } else pair.category,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (quote != null) formatForexPrice(quote.price) else "—",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (quote != null) {
                        val sign = if (isPositive) "+" else ""
                        "$sign${"%.2f".format(quote.changePercent)}%"
                    } else "—",
                    color = changeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

