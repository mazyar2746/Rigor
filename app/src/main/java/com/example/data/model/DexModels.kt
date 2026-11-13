package com.example.data.model

/**
 * DEX Token Model - represents a token available for swap.
 */
data class DexToken(
    val symbol: String,
    val name: String,
    val address: String,
    val priceUsd: Double,
    val imageUrl: String? = null
)

/**
 * DEX Quote Model - returned from 0x API via Cloudflare proxy.
 */
data class DexQuote(
    val buyAmount: String,
    val sellAmount: String,
    val price: String,
    val guaranteedPrice: String,
    val to: String,
    val data: String,
    val value: String,
    val gas: String,
    val estimatedGas: String,
    val integratorFee: String? = null
)

/**
 * Represents a newly created liquidity pool on a DEX.
 * Returned from DexScreener/GeckoTerminal via our proxy.
 */
data class DexPool(
    val address: String,
    val name: String,
    val network: String,
    val priceUsd: Double,
    val volume24h: Double,
    val imageUrl: String? = null
)

/**
 * Common tokens on BSC for quick swapping.
 */
object DexTokens {
    val BSC_TOKENS = listOf(
        DexToken("BNB", "Binance Coin", "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", 0.0),
        DexToken("USDT", "Tether USD", "0x55d398326f99059fF775485246999027B3197955", 1.0),
        DexToken("BUSD", "Binance USD", "0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56", 1.0),
        DexToken("WBNB", "Wrapped BNB", "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", 0.0),
        DexToken("CAKE", "PancakeSwap", "0x0E09FaBB73Bd3Ade0a17ECC321fD13a19e81cE82", 0.0),
        DexToken("XRP", "Ripple", "0x1D2F0da169ceB9fC7B3144628dB156f3F6c60dBE", 0.0),
        DexToken("ADA", "Cardano", "0x3EE2200Efb3400fAbB9AacF31297cBdD1d435D47", 0.0),
        DexToken("DOGE", "Dogecoin", "0xbA2aE424d960c26247Dd6c32edC70B295c744C43", 0.0)
    )
}
