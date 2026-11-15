package com.example.config

object RigorConfig {
    // === Twelve Data (Forex) ===
    const val TWELVE_DATA_API_KEY = "03f1e4b52ff8458faad388ad52a5fcb6"
    const val TWELVE_DATA_BASE_URL = "https://api.twelvedata.com/"

    // === Finnhub (Forex News) ===
    const val FINNHUB_API_KEY = "dbq4ar9r01qr0r9rbe4g"

    // === CoinMarketCap (Crypto) ===
    const val CMC_PRO_API_KEY = "YOUR_CMC_API_KEY"
    const val CMC_PRO_LISTINGS_URL = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest"

    // === CoinGecko ===
    const val COINGECKO_MARKETS_URL = "https://api.coingecko.com/api/v3/coins/markets"

    // === Binance US ===
    const val BINANCE_US_TICKER_24HR_URL = "https://api.binance.us/api/v3/ticker/24hr"
    const val BINANCE_US_KLINES_URL = "https://api.binance.us/api/v3/klines"

    // === MEXC ===
    const val MEXC_TICKER_24HR_URL = "https://api.mexc.com/api/v3/ticker/24hr"
    const val MEXC_KLINES_URL = "https://api.mexc.com/api/v3/klines"

    // === KuCoin ===
    const val KUCOIN_CANDLES_URL = "https://api.kucoin.com/api/v1/market/candles"

    // === WalletConnect ===
    const val WALLETCONNECT_PROJECT_ID = "WALLETCONNECT_PROJECT_ID_PLACEHOLDER"

    // === App Identity ===
    const val OFFICIAL_SUPPORT_EMAIL = "support@rigor.app"

    // === Timeouts ===
    const val HTTP_CONNECT_TIMEOUT_SECONDS = 30L
    const val HTTP_READ_TIMEOUT_SECONDS = 30L
}
