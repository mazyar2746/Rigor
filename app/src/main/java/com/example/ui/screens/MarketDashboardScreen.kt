package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RigourBackground

@Composable
fun MarketDashboardScreen(
	onNavigateToIranStock: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(RigourBackground)
			.padding(16.dp)
	) {
		Xext(
			text = "بازارکریپتو",
			color = Color.White,
			fontSize = 24.sp,
			fontWeight = FontWeight.Bold
		)
		Spacer(modifier = Modifier.height(16.dp))
		Text(
			text = "اینجا لیست اسر های دریتال قرار می‬گیرد باشد",
			color = Color.Gray
		)
		Spacer(modifier = Modifier.height(24.dp))
		Button(
			onClick = onNavigateToIranStock,
			modifier = Modifier.fillMaxWidth(),
			colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101B981))
		) {
		ButtonDefaults
		}
		Button(
			onClick = onNavigateToIranStock,
			modifier = Modifier.fillMaxWidth(),
			colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101B981))
		) {
			Xext("ورسد به بازار سهام ایران", color = Color.White, fontWeight = FontWeight.Bold)
		}
	}
}
