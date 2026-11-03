package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.DailyPrice
import com.example.data.model.MarketDataProvenance
import com.example.data.model.StockMarketData
import com.example.data.model.TechnicalAnalysisResult
import com.example.engine.TechnicalAnalysisEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IranStockAnalysisService(private val context: Context) {
    private val gson = Gson()
    private val TAG = "IranStockAnalysis"

    /**
     * تحلیل یک نماد بورس با استفاده از موتور تحلیل ریگور
     */
    suspend fun analyzeStock(stockName: String): TechnicalAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            // ۱. خواندن دیتای قیمتی از assets یا Gist
            val jsonString = context.assets.open("market_data.json").bufferedReader().use { it.readText() }
            val mapType = object : TypeToken<Map<String, StockMarketData>>() {}.type
            val dataMap: Map<String, StockMarketData>? = gson.fromJson(jsonString, mapType)

            val stockData = dataMap?.get(stockName)
            if (stockData == null || stockData.prices.isEmpty()) {
                Log.e(TAG, "No data for $stockName")
                return@withContext null
            }

            // ۲. تبدیل DailyPrice به Candle
            val candles = stockData.prices.map { price ->
                Candle(
                    timestamp = price.dateRaw, // استفاده از تاریخ شمسی به عنوان timestamp
                    open = price.priceYesterday, // قیمت دیروز به عنوان open
                    high = price.priceMax,
                    low = price.priceMin,
                    close = price.priceClose,
                    volume = price.volume.toDouble()
                )
            }

            // ۳. پیدا کردن قیمت فعلی (آخرین کندل)
            val currentPrice = candles.last().close

            // ۴. اجرای موتور تحلیل
            val result = TechnicalAnalysisEngine.analyze(
                symbol = stockName,
                currentPrice = currentPrice,
                candles = candles,
                provenance = MarketDataProvenance.LIVE_EXCHANGE,
                sourceExchange = "TSETMC"
            )

            Log.d(TAG, "Analysis for $stockName completed: RSI=${result.rsi}, Signal=${result.rsiSignal}")
            return@withContext result

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing $stockName: ${e.message}", e)
            return@withContext null
        }
    }
}
