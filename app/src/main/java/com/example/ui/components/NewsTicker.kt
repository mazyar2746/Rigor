package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ForexNewsItem
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurface

/**
 * NewsTicker - A true continuous scrolling marquee (reel).
 * No dots, no pagination. Just a continuous smooth scroll.
 */
@Composable
fun NewsTicker(
    news: List<ForexNewsItem>,
    modifier: Modifier = Modifier
) {
    if (news.isEmpty()) return

    // Combine all headlines into a single long string with separators
    val combinedNews = news.joinToString("   ★   ") { it.headline }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RigourSurface)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // News icon
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(RigourCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Newspaper,
                contentDescription = "News",
                tint = RigourCyan,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // The actual scrolling text
        Text(
            text = combinedNews,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 500,
                    velocity = 60.dp // سرعت حرکت نوار
                )
        )
    }
}
