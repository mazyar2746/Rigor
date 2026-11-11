package com.example.ui.screens

/**
 * Shared formatting helper for Forex screens.
 */
fun formatForexPrice(price: Double): String {
    return when {
        price >= 100.0 -> "%.2f".format(price)
        price >= 10.0 -> "%.3f".format(price)
        price >= 1.0 -> "%.4f".format(price)
        else -> "%.5f".format(price)
    }
}
