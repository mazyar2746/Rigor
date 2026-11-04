package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onNavigateToCrypto: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToForex: () -> Unit,
    onNavigateToDex: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(1500),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(1500),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .alpha(alpha)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "RIGOR",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00BCD4),
                letterSpacing = 8.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "A comprehensive analytical and trading platform for financial markets.\nRigor is the best of the best.",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontStyle = FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "انتخاب بازار",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MarketCard(
                    title = "کریپتو",
                    icon = "₿",
                    gradient = listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
                    onClick = onNavigateToCrypto,
                    modifier = Modifier.weight(1f)
                )
                MarketCard(
                    title = "بورس ایران",
                    icon = "📈",
                    gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                    onClick = onNavigateToStock,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MarketCard(
                    title = "فارکس",
                    icon = "💱",
                    gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                    onClick = onNavigateToForex,
                    modifier = Modifier.weight(1f),
                    disabled = true
                )
                MarketCard(
                    title = "دکس",
                    icon = "🔗",
                    gradient = listOf(Color(0xFF06B6D4), Color(0xFF0891B2)),
                    onClick = onNavigateToDex,
                    modifier = Modifier.weight(1f),
                    disabled = true
                )
            }
        }
    }
}

@Composable
fun MarketCard(
    title: String,
    icon: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false
) {
    Card(
        modifier = modifier.height(100.dp),
        onClick = onClick,
        enabled = !disabled,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (disabled) Color(0xFF334155) else Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.linearGradient(gradient),
                    shape = RoundedCornerShape(16.dp),
                    alpha = if (disabled) 0.5f else 1f
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (disabled) {
                    Text(
                        text = "به‌زودی",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
