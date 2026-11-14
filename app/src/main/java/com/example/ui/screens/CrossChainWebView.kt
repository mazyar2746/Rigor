package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.RigourBackground
import com.example.ui.theme.RigourCyan
import com.example.ui.theme.RigourSurface

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossChainWebView(
    url: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(RigourBackground)) {
        // نوار بالا با لوگوی Rigor
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "RIGOR · Cross-Chain Swap",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RigourCyan)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RigourSurface)
        )

        // پیغام امنیتی کوچیک
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RigourCyan.copy(alpha = 0.1f))
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            Text(
                "این بخش با همکاری Rango Exchange ارائه می‌شه",
                color = RigourCyan,
                fontSize = 11.sp
            )
        }

        // خود WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
