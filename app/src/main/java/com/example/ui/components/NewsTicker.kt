package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ForexNewsItem
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurface
import kotlinx.coroutines.delay

/**
 * NewsTicker - A marquee-style scrolling news banner for Forex screen.
 * Displays live headlines from Finnhub news API.
 */
@Composable
fun NewsTicker(
    news: List<ForexNewsItem>,
    modifier: Modifier = Modifier
) {
    if (news.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-scroll news every 5 seconds
    LaunchedEffect(news.size) {
        if (news.size > 1) {
            while (true) {
                delay(5000)
                currentIndex = (currentIndex + 1) % news.size
            }
        }
    }

    val currentNews = news.getOrNull(currentIndex) ?: return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RigourSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // News icon with cyan background
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(RigourCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Newspaper,
                contentDescription = "News",
                tint = RigourCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Scrolling headline
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentNews.headline,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (currentNews.source.isNotBlank()) {
                Text(
                    text = currentNews.source,
                    color = RigourCyan.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        // Indicator dots (which news is showing)
        if (news.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(minOf(news.size, 5)) { idx ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (idx == currentIndex % 5) RigourCyan
                                else Color.Gray.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}
