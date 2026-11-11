package com.example.data.remote

import android.util.Log
import com.example.config.RigorConfig
import com.example.data.model.Candle
import com.example.data.model.ForexQuote
import com.example.data.model.ForexNewsItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * ForexRemoteDataSource
 *
 * Talks to Twelve Data API (https://twelvedata.com).
 * Free plan: 800 requests/day, 8 requests/minute.
 *
 * We implement a small in-memory rate limiter (min 8s between calls)
 * and a per-symbol quote cache (30s TTL) to stay under the limit.
 */
class ForexRemoteDataSource(
    private val apiKey: String = RigorConfig.TWELVE_DATA_API_KEY
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(RigorConfig.HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(RigorConfig.HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // --- Simple in-memory caches ---
    private data class CachedQuote(val quote: ForexQuote, val timestamp: Long)
    private data class CachedCandles(val candles: List<Candle>, val timestamp: Long)

    private val quoteCache = mutableMapOf<String, CachedQuote>()
    private val candleCache = mutableMapOf<String, CachedCandles>()

    private val quoteTtlMs = 30_000L                 // 30s for quotes
    private val candleTtlMs = RigorConfig.CANDLE_CACHE_TTL_MS  // 30 min for candles

    @Volatile
    private var lastRequestAt: Long = 0L
    private val minIntervalMs = 8_500L               // 8.5s to stay safely under 8/min

    private suspend fun throttle() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestAt
        if (elapsed < minIntervalMs) {
            val wait = minIntervalMs - elapsed
            Log.d(TAG, "Throttling forex API: waiting ${wait}ms")
            kotlinx.coroutines.delay(wait)
        }
        lastRequestAt = System.currentTimeMillis()
    }

    /**
     * Fetch a live quote for a pair (e.g. "EUR/USD").
     * Returns null on failure (network, rate limit, bad key, etc.)
     */
    suspend fun fetchQuote(symbol: String): ForexQuote? = withContext(Dispatchers.IO) {
        val normalized = symbol.uppercase()

        // Cache hit?
        quoteCache[normalized]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < quoteTtlMs) {
                Log.d(TAG, "Quote cache hit: $normalized")
                return@withContext cached.quote
            }
        }

        try {
            throttle()
            val url = "${RigorConfig.TWELVE_DATA_BASE_URL}quote?symbol=$normalized&apikey=$apiKey"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Quote HTTP ${response.code} for $normalized")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)

                // Twelve Data returns {code: ..., message: ..., status: "error"} on failure
                if (json.has("status") && json.get("status").asString == "error") {
                    val msg = json.get("message")?.asString ?: "unknown"
                    Log.w(TAG, "Quote API error for $normalized: $msg")
                    return@withContext null
                }

                val price = json.get("close")?.asString?.toDoubleOrNull()
                    ?: json.get("price")?.asString?.toDoubleOrNull()
                    ?: return@withContext null

                val change = json.get("change")?.asString?.toDoubleOrNull() ?: 0.0
                val changePct = json.get("percent_change")?.asString?.toDoubleOrNull() ?: 0.0
                val high = json.get("high")?.asString?.toDoubleOrNull() ?: price
                val low = json.get("low")?.asString?.toDoubleOrNull() ?: price

                val quote = ForexQuote(
                    symbol = normalized,
                    price = price,
                    change = change,
                    changePercent = changePct,
                    dayHigh = high,
                    dayLow = low
                )

                quoteCache[normalized] = CachedQuote(quote, System.currentTimeMillis())
                return@withContext quote
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Quote fetch failed for $normalized: ${t.message}")
            return@withContext null
        }
    }

    /**
     * Fetch candles for a pair. Timeframe examples: "1h", "4h", "1day".
     * Returns null on failure.
     */
    suspend fun fetchCandles(
        symbol: String,
        interval: String = "1h",
        outputSize: Int = 120
    ): List<Candle>? = withContext(Dispatchers.IO) {
        val normalized = symbol.uppercase()
        val cacheKey = "$normalized|$interval|$outputSize"

        candleCache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestamp < candleTtlMs) {
                Log.d(TAG, "Candle cache hit: $cacheKey")
                return@withContext cached.candles
            }
        }

        try {
            throttle()
            val url = "${RigorConfig.TWELVE_DATA_BASE_URL}time_series" +
                "?symbol=$normalized" +
                "&interval=$interval" +
                "&outputsize=$outputSize" +
                "&apikey=$apiKey"

            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Candles HTTP ${response.code} for $cacheKey")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)

                if (json.has("status") && json.get("status").asString == "error") {
                    val msg = json.get("message")?.asString ?: "unknown"
                    Log.w(TAG, "Candles API error for $cacheKey: $msg")
                    return@withContext null
                }

                val values = json.getAsJsonArray("values") ?: return@withContext null
                if (values.size() == 0) return@withContext null

                val result = ArrayList<Candle>(values.size())
                for (i in 0 until values.size()) {
                    val item = values[i].asJsonObject
                    val open = item.get("open")?.asString?.toDoubleOrNull() ?: continue
                    val high = item.get("high")?.asString?.toDoubleOrNull() ?: continue
                    val low = item.get("low")?.asString?.toDoubleOrNull() ?: continue
                    val close = item.get("close")?.asString?.toDoubleOrNull() ?: continue
                    val volume = item.get("volume")?.asString?.toDoubleOrNull() ?: 0.0
                    val dt = item.get("datetime")?.asString

                    val ts = parseTimestamp(dt)
                    result.add(Candle(timestamp = ts, open = open, high = high, low = low, close = close, volume = volume))
                }

                // Twelve Data returns newest first; reverse to oldest->newest for charts.
                result.reverse()

                candleCache[cacheKey] = CachedCandles(result, System.currentTimeMillis())
                return@withContext result
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Candles fetch failed for $cacheKey: ${t.message}")
            return@withContext null
        }
    }

    private fun parseTimestamp(dt: String?): Long {
        if (dt.isNullOrBlank()) return System.currentTimeMillis()
        // Format: "2024-11-10 14:00:00" or "2024-11-10"
        return try {
            val pattern = if (dt.contains(":")) "yyyy-MM-dd HH:mm:ss" else "yyyy-MM-dd"
            val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            fmt.parse(dt)?.time ?: System.currentTimeMillis()
        } catch (t: Throwable) {
            System.currentTimeMillis()
        }
    }

    /** Optional: batch quote fetch for multiple symbols (uses /quote?symbol=A,B,C). */
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

                // Single symbol returns object, multiple returns object of objects
                if (symbols.size == 1) {
                    val sym = symbols.first().uppercase()
                    val price = json.get("close")?.asString?.toDoubleOrNull() ?: return@withContext result
                    result[sym] = ForexQuote(
                        sym, price,
                        json.get("change")?.asString?.toDoubleOrNull() ?: 0.0,
                        json.get("percent_change")?.asString?.toDoubleOrNull() ?: 0.0,
                        json.get("high")?.asString?.toDoubleOrNull() ?: price,
                        json.get("low")?.asString?.toDoubleOrNull() ?: price
                    )
                } else {
                    for (sym in symbols) {
                        val key = sym.uppercase()
                        val obj = json.getAsJsonObject(key) ?: continue
                        if (obj.has("status") && obj.get("status").asString == "error") continue
                        val price = obj.get("close")?.asString?.toDoubleOrNull() ?: continue
                        result[key] = ForexQuote(
                            key, price,
                            obj.get("change")?.asString?.toDoubleOrNull() ?: 0.0,
                            obj.get("percent_change")?.asString?.toDoubleOrNull() ?: 0.0,
                            obj.get("high")?.asString?.toDoubleOrNull() ?: price,
                            obj.get("low")?.asString?.toDoubleOrNull() ?: price
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Batch quote fetch failed: ${t.message}")
        }

        return@withContext result
    }

    /**
     * Fetch quotes for multiple symbols using Finnhub (60 req/min free tier).
     * Finnhub symbol format: OANDA:EUR_USD, OANDA:GBP_USD, etc.
     * Falls back to Twelve Data if Finnhub fails.
     */
    suspend fun fetchQuotesFromFinnhub(symbols: List<String>): Map<String, ForexQuote> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ForexQuote>()
        if (symbols.isEmpty()) return@withContext result

        for (sym in symbols) {
            try {
                val finnhubSymbol = "OANDA:" + sym.uppercase().replace("/", "_")
                val url = "https://finnhub.io/api/v1/quote?symbol=$finnhubSymbol&token=${RigorConfig.FINNHUB_API_KEY}"
                val request = Request.Builder().url(url).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Finnhub HTTP ${response.code} for $finnhubSymbol")
                        return@use
                    }
                    val body = response.body?.string() ?: return@use
                    val json = gson.fromJson(body, JsonObject::class.java)

                    val price = json.get("c")?.asDouble ?: 0.0
                    if (price <= 0.0) {
                        Log.w(TAG, "Finnhub no price for $finnhubSymbol")
                        return@use
                    }

                    val change = json.get("d")?.asDouble ?: 0.0
                    val changePct = json.get("dp")?.asDouble ?: 0.0
                    val high = json.get("h")?.asDouble ?: price
                    val low = json.get("l")?.asDouble ?: price

                    val key = sym.uppercase()
                    val quote = ForexQuote(
                        symbol = key,
                        price = price,
                        change = change,
                        changePercent = changePct,
                        dayHigh = high,
                        dayLow = low
                    )
                    result[key] = quote
                    quoteCache[key] = CachedQuote(quote, System.currentTimeMillis())
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Finnhub fetch failed for $sym: ${t.message}")
            }
        }

        Log.d(TAG, "Finnhub loaded ${result.size}/${symbols.size} quotes")
        return@withContext result
    }

    /**
     * Fetch live forex news from Finnhub (category=forex).
     * Returns top N headlines. Used for the News Ticker on Forex screen.
     */
    suspend fun fetchForexNews(limit: Int = 15): List<ForexNewsItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<ForexNewsItem>()
        try {
            val url = "https://finnhub.io/api/v1/news?category=forex&token=${RigorConfig.FINNHUB_API_KEY}"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Finnhub news HTTP ${response.code}")
                    return@withContext items
                }
                val body = response.body?.string() ?: return@withContext items
                val arr = gson.fromJson(body, com.google.gson.JsonArray::class.java) ?: return@withContext items

                var count = 0
                for (elem in arr) {
                    if (count >= limit) break
                    val obj = elem.asJsonObject
                    val headline = obj.get("headline")?.asString ?: continue
                    val id = System.currentTimeMillis()
                    val summary = obj.get("summary")?.asString ?: ""
                    val source = obj.get("source")?.asString ?: ""
                    val newsUrl = obj.get("url")?.asString ?: ""
                    val datetime = obj.get("datetime")?.asLong ?: 0L
                    val imageUrl = obj.get("image")?.asString

                    items.add(
                        ForexNewsItem(
                            id = id,
                            headline = headline,
                            summary = summary,
                            source = source,
                            url = newsUrl,
                            datetime = datetime,
                            imageUrl = imageUrl
                        )
                    )
                    count++
                }
            }
            Log.d(TAG, "Loaded ${items.size} forex news items")
        } catch (t: Throwable) {
            Log.w(TAG, "fetchForexNews failed: ${t.message}")
        }
        return@withContext items
    }

    /**
     * Fetch quotes using open.er-api.com (free, no limits, no API key).
     * Converts all pairs into their quote values based on USD.
     */
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

                fun getRate(base: String, quote: String): Double {
                    if (base == "USD") return rates.get(quote)?.asDouble ?: 0.0
                    if (quote == "USD") return 1.0 / (rates.get(base)?.asDouble ?: 1.0)
                    val b = rates.get(base)?.asDouble ?: 1.0
                    val q = rates.get(quote)?.asDouble ?: 1.0
                    return q / b
                }

                for (pair in ForexPairs.ALL) {
                    val parts = pair.symbol.split("/")
                    if (parts.size == 2) {
                        val base = parts[0]
                        val quote = parts[1]
                        val price = getRate(base, quote)
                        if (price > 0.0) {
                            val key = pair.symbol.uppercase()
                            result[key] = ForexQuote(
                                symbol = key,
                                price = price,
                                change = 0.0,
                                changePercent = 0.0,
                                dayHigh = price,
                                dayLow = price
                            )
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

    /**
     * Fetch quotes using open.er-api.com (free, no limits, no API key).
     * Converts all pairs into their quote values based on USD.
     */
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

                fun getRate(base: String, quote: String): Double {
                    if (base == "USD") return rates.get(quote)?.asDouble ?: 0.0
                    if (quote == "USD") return 1.0 / (rates.get(base)?.asDouble ?: 1.0)
                    val b = rates.get(base)?.asDouble ?: 1.0
                    val q = rates.get(quote)?.asDouble ?: 1.0
                    return q / b
                }

                for (pair in ForexPairs.ALL) {
                    val parts = pair.symbol.split("/")
                    if (parts.size == 2) {
                        val base = parts[0]
                        val quote = parts[1]
                        val price = getRate(base, quote)
                        if (price > 0.0) {
                            val key = pair.symbol.uppercase()
                            result[key] = ForexQuote(
                                symbol = key,
                                price = price,
                                change = 0.0,
                                changePercent = 0.0,
                                dayHigh = price,
                                dayLow = price
                            )
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

    companion object {
        private const val TAG = "ForexRemote"
    }
}
