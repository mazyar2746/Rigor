package com.example.data.model

/**
 * Rigor Forex Models.
 * Designed for Twelve Data API integration.
 * Uses the same Candle, SignalType, and MarketDataProvenance shared with crypto.
 */

/**
 * A single forex pair (e.g. EUR/USD).
 */
data class ForexPair(
    val symbol: String,          // e.g. "EUR/USD"
    val displayName: String,     // e.g. "EUR / USD"
    val baseCurrency: String,    // e.g. "EUR"
    val quoteCurrency: String,   // e.g. "USD"
    val category: String = "Major" // Major, Minor, Exotic
)

/**
 * Live quote for a forex pair (from /quote endpoint).
 */
data class ForexQuote(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Full analysis result for a forex pair.
 * Mirrors TechnicalAnalysisResult but keyed by forex symbol.
 */
data class ForexAnalysisResult(
    val symbol: String,
    val currentPrice: Double,
    val rsi: Double,
    val rsiSignal: SignalType,
    val rsiDescription: String,
    val macdLine: Double,
    val macdSignalLine: Double,
    val macdHistogram: Double,
    val macdSignal: SignalType,
    val macdDescription: String,
    val bbUpper: Double,
    val bbMiddle: Double,
    val bbLower: Double,
    val bbSignal: SignalType,
    val bbDescription: String,
    val overallSignal: SignalType,
    val confidencePercent: Int,
    val shortTermTarget: Double,
    val mediumTermTarget: Double,
    val stopLoss: Double,
    val resistance2: Double,
    val resistance1: Double,
    val pivotPoint: Double,
    val support1: Double,
    val support2: Double,
    val candles: List<Candle>,
    val provenance: MarketDataProvenance = MarketDataProvenance.LIVE_EXCHANGE,
    val dataSourceNotice: String? = null
)

/**
 * Group of forex pairs for one horizontal page (10 pairs per page).
 */
data class ForexPage(
    val pageIndex: Int,
    val pairs: List<ForexPair>
)

/**
 * Signal data for a forex pair, used in signals screen.
 */
data class ForexSignalData(
    val pair: ForexPair,
    val quote: ForexQuote,
    val analysis: ForexAnalysisResult,
    val isProSignal: Boolean = analysis.confidencePercent >= 75 ||
        analysis.overallSignal == SignalType.STRONG_BUY ||
        analysis.overallSignal == SignalType.STRONG_SELL
)

/**
 * Predefined list of forex pairs grouped by category.
 * 30 pairs = 3 pages x 10 pairs.
 */
object ForexPairs {

    val MAJORS: List<ForexPair> = listOf(
        ForexPair("EUR/USD", "EUR / USD", "EUR", "USD", "Major"),
        ForexPair("GBP/USD", "GBP / USD", "GBP", "USD", "Major"),
        ForexPair("USD/JPY", "USD / JPY", "USD", "JPY", "Major"),
        ForexPair("USD/CHF", "USD / CHF", "USD", "CHF", "Major"),
        ForexPair("AUD/USD", "AUD / USD", "AUD", "USD", "Major"),
        ForexPair("USD/CAD", "USD / CAD", "USD", "CAD", "Major"),
        ForexPair("NZD/USD", "NZD / USD", "NZD", "USD", "Major"),
        ForexPair("EUR/GBP", "EUR / GBP", "EUR", "GBP", "Cross"),
        ForexPair("EUR/JPY", "EUR / JPY", "EUR", "JPY", "Cross"),
        ForexPair("GBP/JPY", "GBP / JPY", "GBP", "JPY", "Cross")
    )

    val CROSSES: List<ForexPair> = listOf(
        ForexPair("AUD/CAD", "AUD / CAD", "AUD", "CAD", "Cross"),
        ForexPair("AUD/CHF", "AUD / CHF", "AUD", "CHF", "Cross"),
        ForexPair("AUD/JPY", "AUD / JPY", "AUD", "JPY", "Cross"),
        ForexPair("CAD/CHF", "CAD / CHF", "CAD", "CHF", "Cross"),
        ForexPair("CAD/JPY", "CAD / JPY", "CAD", "JPY", "Cross"),
        ForexPair("CHF/JPY", "CHF / JPY", "CHF", "JPY", "Cross"),
        ForexPair("EUR/AUD", "EUR / AUD", "EUR", "AUD", "Cross"),
        ForexPair("EUR/CAD", "EUR / CAD", "EUR", "CAD", "Cross"),
        ForexPair("EUR/CHF", "EUR / CHF", "EUR", "CHF", "Cross"),
        ForexPair("GBP/AUD", "GBP / AUD", "GBP", "AUD", "Cross")
    )

    val EXOTICS: List<ForexPair> = listOf(
        ForexPair("GBP/CAD", "GBP / CAD", "GBP", "CAD", "Cross"),
        ForexPair("GBP/CHF", "GBP / CHF", "GBP", "CHF", "Cross"),
        ForexPair("NZD/CAD", "NZD / CAD", "NZD", "CAD", "Cross"),
        ForexPair("NZD/CHF", "NZD / CHF", "NZD", "CHF", "Cross"),
        ForexPair("NZD/JPY", "NZD / JPY", "NZD", "JPY", "Cross"),
        ForexPair("USD/MXN", "USD / MXN", "USD", "MXN", "Exotic"),
        ForexPair("USD/ZAR", "USD / ZAR", "USD", "ZAR", "Exotic"),
        ForexPair("USD/TRY", "USD / TRY", "USD", "TRY", "Exotic"),
        ForexPair("EUR/TRY", "EUR / TRY", "EUR", "TRY", "Exotic"),
        ForexPair("USD/SEK", "USD / SEK", "USD", "SEK", "Exotic")
    )

    /** All pairs in order: pages of 10. */
    val ALL_PAGES: List<List<ForexPair>> = listOf(MAJORS, CROSSES, EXOTICS)

    /** Flat list of all pairs. */
    val ALL: List<ForexPair> = MAJORS + CROSSES + EXOTICS

    /** Find a pair by symbol. */
    fun findBySymbol(symbol: String): ForexPair? =
        ALL.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
}

/**
 * Forex News Item - fetched from Finnhub news API.
 * Used in the News Ticker on the Forex screen.
 */
data class ForexNewsItem(
    val id: Long,
    val headline: String,
    val summary: String,
    val source: String,
    val url: String,
    val datetime: Long,
    val imageUrl: String? = null,
    val language: String = "en"
)
