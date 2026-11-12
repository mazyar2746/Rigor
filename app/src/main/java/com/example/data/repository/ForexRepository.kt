package com.example.data.repository

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.ForexAnalysisResult
import com.example.data.model.ForexPairs
import com.example.data.model.ForexQuote
import com.example.data.model.ForexNewsItem
import com.example.data.remote.ForexRemoteDataSource
import com.example.engine.TechnicalAnalysisEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * ForexRepository
 *
 * Orchestrates forex data: quotes, candles, and technical analysis.
 * Uses ForexRemoteDataSource for network + TechnicalAnalysisEngine for TA.
 *
 * State exposed to ViewModel:
 *  - quotes: Map<symbol, ForexQuote>
 *  - analyses: Map<symbol, ForexAnalysisResult>
 *  - isLoading / errorMessage
 */
class ForexRepository(
    private val remote: ForexRemoteDataSource = ForexRemoteDataSource()
) {

    private val _quotes = MutableStateFlow<Map<String, ForexQuote>>(emptyMap())
    val quotes = _quotes.asStateFlow()

    private val _analyses = MutableStateFlow<Map<String, ForexAnalysisResult>>(emptyMap())
    val analyses = _analyses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated = _lastUpdated.asStateFlow()

    // --- Forex News ---
    private val _news = MutableStateFlow<List<ForexNewsItem>>(emptyList())
    val news = _news.asStateFlow()
    private val _persianNews = MutableStateFlow<List<ForexNewsItem>>(emptyList())
    val persianNews = _persianNews.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading = _isNewsLoading.asStateFlow()

    // TechnicalAnalysisEngine is a Kotlin object, use directly

    /**
     * Load quotes for a page of symbols (max ~10 per call).
     * Uses batch endpoint to save request quota.
     */
    suspend fun loadQuotesForSymbols(symbols: List<String>) = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val batch = remote.fetchQuotesFromExchangeRate()
            if (batch.isNotEmpty()) {
                _quotes.value = _quotes.value + batch
                _lastUpdated.value = System.currentTimeMillis()
                Log.d(TAG, "Loaded ${batch.size}/${symbols.size} quotes")
            } else {
                _errorMessage.value = "خطا در دریافت قیمت‌ها. اتصال اینترنت را بررسی کنید."
                Log.w(TAG, "Batch returned empty for ${symbols.size} symbols")
            }
        } catch (t: Throwable) {
            _errorMessage.value = "خطا: ${t.message}"
            Log.w(TAG, "loadQuotesForSymbols failed: ${t.message}")
        } finally {
            _isLoading.value = false
        }
    }

    /** Load a single quote and merge into cache. */
    suspend fun loadQuote(symbol: String) = withContext(Dispatchers.IO) {
        try {
            val q = remote.fetchQuote(symbol) ?: return@withContext
            _quotes.value = _quotes.value + (symbol.uppercase() to q)
            _lastUpdated.value = System.currentTimeMillis()
        } catch (t: Throwable) {
            Log.w(TAG, "loadQuote($symbol) failed: ${t.message}")
        }
    }

    /**
     * Load candles + run technical analysis for one pair.
     * Returns the analysis result (also stored in analyses map).
     */
    suspend fun analyzePair(
        symbol: String,
        interval: String = "1h",
        outputSize: Int = 120
    ): ForexAnalysisResult? = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val candles = remote.fetchCandles(symbol, interval, outputSize)
            if (candles.isNullOrEmpty() || candles.size < 30) {
                _errorMessage.value = "داده کافی برای تحلیل $symbol موجود نیست."
                Log.w(TAG, "Not enough candles for $symbol: ${candles?.size ?: 0}")
                return@withContext null
            }

            // Ensure a fresh quote exists
            val currentQuote = _quotes.value[symbol.uppercase()] ?: remote.fetchQuote(symbol)
            val currentPrice = currentQuote?.price ?: candles.last().close

            // Use existing TechnicalAnalysisEngine — same API used for crypto
            val taResult = TechnicalAnalysisEngine.analyze(
                symbol = symbol,
                candles = candles,
                currentPrice = currentPrice
            )

            val analysis = ForexAnalysisResult(
                symbol = symbol,
                currentPrice = currentPrice,
                rsi = taResult.rsi,
                rsiSignal = taResult.rsiSignal,
                rsiDescription = taResult.rsiDescription,
                macdLine = taResult.macdLine,
                macdSignalLine = taResult.macdSignalLine,
                macdHistogram = taResult.macdHistogram,
                macdSignal = taResult.macdSignal,
                macdDescription = taResult.macdDescription,
                bbUpper = taResult.bbUpper,
                bbMiddle = taResult.bbMiddle,
                bbLower = taResult.bbLower,
                bbSignal = taResult.bbSignal,
                bbDescription = taResult.bbDescription,
                overallSignal = taResult.overallSignal,
                confidencePercent = taResult.confidencePercent,
                shortTermTarget = taResult.shortTermTarget,
                mediumTermTarget = taResult.mediumTermTarget,
                stopLoss = taResult.stopLoss,
                resistance2 = taResult.resistance2,
                resistance1 = taResult.resistance1,
                pivotPoint = taResult.pivotPoint,
                support1 = taResult.support1,
                support2 = taResult.support2,
                candles = candles,
                provenance = taResult.provenance,
                dataSourceNotice = taResult.dataSourceNotice
            )

            _analyses.value = _analyses.value + (symbol.uppercase() to analysis)
            _lastUpdated.value = System.currentTimeMillis()
            return@withContext analysis
        } catch (t: Throwable) {
            _errorMessage.value = "خطا در تحلیل $symbol: ${t.message}"
            Log.w(TAG, "analyzePair($symbol) failed: ${t.message}")
            return@withContext null
        } finally {
            _isLoading.value = false
        }
    }

    /** Get cached candles for a pair, if present in analyses. */
    fun cachedCandles(symbol: String): List<Candle>? =
        _analyses.value[symbol.uppercase()]?.candles

    /** Get cached quote. */
    fun cachedQuote(symbol: String): ForexQuote? =
        _quotes.value[symbol.uppercase()]

    /** Get cached analysis. */
    fun cachedAnalysis(symbol: String): ForexAnalysisResult? =
        _analyses.value[symbol.uppercase()]

    /** Load first page quotes (10 majors). Called on screen open. */
    suspend fun loadFirstPage() = withContext(Dispatchers.IO) {
        val firstPage = ForexPairs.ALL_PAGES.firstOrNull() ?: return@withContext
        loadQuotesForSymbols(firstPage.map { it.symbol })
    }

    /** Clear transient error message. */
    fun clearError() {
        _errorMessage.value = null
    }

    /** Load live forex news from Finnhub. Called once when Forex screen opens. */
    suspend fun loadForexNews(limit: Int = 15) = withContext(Dispatchers.IO) {
        if (_news.value.isNotEmpty()) return@withContext
        _isNewsLoading.value = true
        try {
            val items = remote.fetchForexNews(limit)
            _persianNews.value = remote.fetchPersianNews(limit)
            if (items.isNotEmpty()) {
                _news.value = items
                Log.d(TAG, "Loaded ${items.size} news items")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "loadForexNews failed: ${t.message}")
        } finally {
            _isNewsLoading.value = false
        }
    }

    companion object {
        private const val TAG = "ForexRepo"
    }
}
