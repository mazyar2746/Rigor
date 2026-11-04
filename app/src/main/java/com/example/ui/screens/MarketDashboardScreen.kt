package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoCoin
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import com.example.ui.components.CoinIcon
import com.example.ui.theme.*

@Composable
fun MarketDashboardScreen(
    coins: List<CryptoCoin>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    favoriteSymbols: List<String>,
    onToggleFavoriteCoin: (String) -> Unit,
    onSelectCoin: (CryptoCoin) -> Unit,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToIranStock: () -> Unit,
    lang: Language
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RigourBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // دکمه ورود به بازار سهام ایران
        Button(
            onClick = onNavigateToIranStock,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "📈 ورود به بازار سهام ایران",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = StringsDictionary.get("search_placeholder", lang),
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RigourSurface,
                unfocusedContainerColor = RigourSurface,
                focusedBorderColor = RigourCyan,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !onlyFavorites,
                    onClick = { if (onlyFavorites) onToggleFavorites() },
                    label = { Text(StringsDictionary.get("filter_all", lang)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RigourCyan.copy(alpha = 0.2f),
                        selectedLabelColor = RigourCyan
                    )
                )
                FilterChip(
                    selected = onlyFavorites,
                    onClick = onToggleFavorites,
                    label = { Text("Watchlist") },
                    leadingIcon = if (onlyFavorites) {
                        { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = RigourCyan)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading && coins.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RigourCyan)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(coins) { coin ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCoin(coin) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RigourSurface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoinIcon(symbol = coin.symbol, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = coin.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(text = coin.symbol, color = Color.Gray, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "$${coin.price}", color = Color.White, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${coin.changePercent}%",
                                    color = if (coin.changePercent >= 0) RigourBullish else RigourBearish,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { onToggleFavoriteCoin(coin.symbol) }) {
                                Icon(
                                    imageVector = if (favoriteSymbols.contains(coin.symbol)) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (favoriteSymbols.contains(coin.symbol)) Color(0xFFEAB308) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
