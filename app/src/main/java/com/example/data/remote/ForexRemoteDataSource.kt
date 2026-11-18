package com.example.data.remote

import android.util.Log
import com.example.config.RigorConfig
import com.example.data.model.Candle
import com.example.data.model.ForexPairs
import com.example.data.model.ForexQuote
import com.example.data.model.ForexNewsItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * ForexRemoteDataSource
 * Handles fetching data from Twelve Data, Finnhub, open.er-api.com, and RSS feeds.
 */
class ForexRemoteDataSource(
    private val apiKey: String = RigorConfig.TWELVE_DATA_API_KEY
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(RigorConfig.HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(RigorConfig.HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private data class CachedQuote(val quote: ForexQuote, val timestamp: Long)
    private data class CachedCandles(val candles: List<Candle>, val timestamp: Long)

    private val quoteCache = mutableMapOf<String, CachedQuote>()
    private val candleCache = mutableMapOf<String, CachedCandles>()
    private val quoteTtlMs = 30_000L
    private val candleTtlMs = 60_000L
    
    private var lastRequestTime = 0L
    private val minRequestIntervalMs = 8_000L // 8 seconds throttle

    private suspend fun throttle() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < minRequestIntervalMs) {
            kotlinx.coroutines.delay(minRequestIntervalMs - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    // --- Twelve Data (for single quote and candles) ---

    suspend fun fetchQuote(symbol: String): ForexQuote? = withContext(Dispatchers.IO) {
        val normalized = symbol.uppercase()
        quoteCache[normalized]?.let {
            if (System.currentTimeMillis() - it.timestamp < quoteTtlMs) return@withContext it.quote
        }
        try {
            throttle()
            val url = "${RigorConfig.TWELVE_DATA_BASE_URL}quote?symbol=$normalized&apikey=$apiKey"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)
                if (json.has("status") && json.get("status").asString == "error") return@withContext null
                val price = json.get("close")?.asString?.toDoubleOrNull() ?: return@withContext null
                val quote = ForexQuote(
                    symbol = normalized,
                    price = price,
                    change = json.get("change")?.asString?.toDoubleOrNull() ?: 0.0,
                    changePercent = json.get("percent_change")?.asString?.toDoubleOrNull() ?: 0.0,
                    dayHigh = json.get("high")?.asString?.toDoubleOrNull() ?: price,
                    dayLow = json.get("low")?.asString?.toDoubleOrNull() ?: price
                )
                quoteCache[normalized] = CachedQuote(quote, System.currentTimeMillis())
                return@withContext quote
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Quote fetch failed for $normalized: ${t.message}")
            return@withContext null
        }
    }

    suspend fun fetchQuotes(symbols: List<String>): Map<String, ForexQuote> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ForexQuote>()
        if (symbols.isEmpty()) return@withContext result
        try {
            throttle()
            val joined = symbols.joinToString(",") { it.uppercase() }
            val url = "${RigorConfig.TWELVE_DATA_BASE_URL}quote?symbol=$joined&apikey=$apiKey"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext result
                val body = response.body?.string() ?: return@withContext result
                val json = gson.fromJson(body, JsonObject::class.java)
                if (symbols.size == 1) {
                    val sym = symbols.first().uppercase()
                    val price = json.get("close")?.asString?.toDoubleOrNull() ?: return@withContext result
                    result[sym] = ForexQuote(sym, price, json.get("change")?.asString?.toDoubleOrNull() ?: 0.0, json.get("percent_change")?.asString?.toDoubleOrNull() ?: 0.0, json.get("high")?.asString?.toDoubleOrNull() ?: price, json.get("low")?.asString?.toDoubleOrNull() ?: price)
                } else {
                    for (sym in symbols) {
                        val key = sym.uppercase()
                        val obj = json.getAsJsonObject(key) ?: continue
                        if (obj.has("status") && obj.get("status").asString == "error") continue
                        val price = obj.get("close")?.asString?.toDoubleOrNull() ?: continue
                        result[key] = ForexQuote(key, price, obj.get("change")?.asString?.toDoubleOrNull() ?: 0.0, obj.get("percent_change")?.asString?.toDoubleOrNull() ?: 0.0, obj.get("high")?.asString?.toDoubleOrNull() ?: price, obj.get("low")?.asString?.toDoubleOrNull() ?: price)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Batch quotes failed: ${t.message}")
        }
        return@withContext result
    }

    suspend fun fetchCandles(symbol: String, interval: String = "1h", outputSize: Int = 120): List<Candle>? = withContext(Dispatchers.IO) {
        val normalized = symbol.uppercase()
        val cacheKey = "$normalized|$interval|$outputSize"
        candleCache[cacheKey]?.let {
            if (System.currentTimeMillis() - it.timestamp < candleTtlMs) return@withContext it.candles
        }
        try {
            throttle()
            val url = "${RigorConfig.TWELVE_DATA_BASE_URL}time_series?symbol=$normalized&interval=$interval&outputsize=$outputSize&apikey=$apiKey"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)
                val values = json.getAsJsonArray("values") ?: return@withContext null
                val candles = mutableListOf<Candle>()
                for (i in values.size() - 1 downTo 0) {
                    val v = values[i].asJsonObject
                    val close = v.get("close").asString.toDoubleOrNull() ?: continue
                    candles.add(Candle(
                        timestamp = 0L,
                        open = v.get("open").asString.toDoubleOrNull() ?: close,
                        high = v.get("high").asString.toDoubleOrNull() ?: close,
                        low = v.get("low").asString.toDoubleOrNull() ?: close,
                        close = close,
                        volume = v.get("volume")?.asString?.toDoubleOrNull() ?: 0.0
                    ))
                }
                candleCache[cacheKey] = CachedCandles(candles, System.currentTimeMillis())
                return@withContext candles
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Candles failed for $normalized: ${t.message}")
            return@withContext null
        }
    }

    // --- open.er-api.com (Unlimited batch quotes) ---

    suspend fun fetchQuotesFromExchangeRate(): Map<String, ForexQuote> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ForexQuote>()
        try {
            val url = "https://open.er-api.com/v6/latest/USD"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext result
                val body = response.body?.string() ?: return@withContext result
                val json = gson.fromJson(body, JsonObject::class.java)
                val rates = json.getAsJsonObject("rates") ?: return@withContext result

                fun getRate(base: String, quote: String): Double? {
                    if (base == "USD") return rates.get(quote)?.asDouble
                    if (quote == "USD") {
                        val b = rates.get(base)?.asDouble ?: return null
                        return 1.0 / b
                    }
                    val b = rates.get(base)?.asDouble ?: return null
                    val q = rates.get(quote)?.asDouble ?: return null
                    return q / b
                }

                for (pair in ForexPairs.ALL) {
                    if (pair.category == "Metal" || pair.category == "Commodity") continue
                    val parts = pair.symbol.split("/")
                    if (parts.size == 2) {
                        val price = getRate(parts[0], parts[1]) ?: 0.0
                        if (price > 0.0) {
                            val key = pair.symbol.uppercase()
                            result[key] = ForexQuote(key, price, 0.0, 0.0, price, price)
                        }
                    }
                }
            }
            Log.d(TAG, "ExchangeRate loaded ${result.size} quotes")
        } catch (t: Throwable) {
            Log.w(TAG, "fetchQuotesFromExchangeRate failed: ${t.message}")
        }
        return@withContext result
    }

    // --- Finnhub (kept for compatibility) ---

    suspend fun fetchQuotesFromFinnhub(symbols: List<String>): Map<String, ForexQuote> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ForexQuote>()
        for (sym in symbols) {
            try {
                val finnhubSymbol = "OANDA:" + sym.uppercase().replace("/", "_")
                val url = "https://finnhub.io/api/v1/quote?symbol=$finnhubSymbol&token=${RigorConfig.FINNHUB_API_KEY}"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val price = json.get("c")?.asDouble ?: 0.0
                    if (price > 0.0) {
                        val key = sym.uppercase()
                        result[key] = ForexQuote(key, price, json.get("d")?.asDouble ?: 0.0, json.get("dp")?.asDouble ?: 0.0, json.get("h")?.asDouble ?: price, json.get("l")?.asDouble ?: price)
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Finnhub fetch failed for $sym: ${t.message}")
            }
        }
        return@withContext result
    }

    // --- ForexLive News via RSS2JSON (Free, no API key) ---

    suspend fun fetchForexNews(limit: Int = 15): List<ForexNewsItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<ForexNewsItem>()
        try {
            val url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Fwww.forexlive.com%2Ffeed%2Fnews"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext items
                val body = response.body?.string() ?: return@withContext items
                val json = JsonParser.parseString(body).asJsonObject
                val itemsArray = json.getAsJsonArray("items") ?: return@withContext items

                for (elem in itemsArray) {
                    if (items.size >= limit) break
                    val obj = elem.asJsonObject
                    val headline = obj.get("title")?.asString ?: continue
                    items.add(
                        ForexNewsItem(
                            id = System.currentTimeMillis(),
                            headline = headline,
                            summary = obj.get("description")?.asString ?: "",
                            source = "ForexLive",
                            url = obj.get("link")?.asString ?: "",
                            datetime = System.currentTimeMillis(),
                            imageUrl = null
                        )
                    )
                }
            }
            Log.d(TAG, "Loaded ${items.size} forex news items")
        } catch (t: Throwable) {
            Log.e(TAG, "fetchForexNews failed: ${t.message}", t)
        }
        return@withContext items
    }

    suspend fun fetchPersianNews(limit: Int = 15): List<ForexNewsItem> = withContext(Dispatchers.IO) {
            val items = mutableListOf<ForexNewsItem>()
            try {
                val url = "https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Fdonya-e-eqtesad.com%2Ffeeds%2F"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext items
                    val body = response.body?.string() ?: return@withContext items
                    val json = JsonParser.parseString(body).asJsonObject
                    val itemsArray = json.getAsJsonArray("items") ?: return@withContext items
                    for (elem in itemsArray) {
                        if (items.size >= limit) break
                        val obj = elem.asJsonObject
                        val headline = obj.get("title")?.asString ?: continue
                        items.add(ForexNewsItem(
                            id = System.currentTimeMillis(),
                            headline = headline,
                            summary = obj.get("description")?.asString ?: "",
                            source = "دنیای اقتصاد",
                            url = obj.get("link")?.asString ?: "",
                            datetime = System.currentTimeMillis(),
                            language = "fa"
                        ))
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "fetchPersianNews failed: ${t.message}", t)
            }
            return@withContext items
        }

    companion object {
        private const val TAG = "ForexRemote"
    }
}

    
