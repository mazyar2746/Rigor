package com.example.data.remote

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.DexPool
import com.example.data.model.DexQuote
import com.example.data.model.DexTokenInfo
import com.example.data.model.TokenDecimals
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class DexRemoteDataSource {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://rigor-v339.h-nasiri3247.workers.dev/api/dex"

    suspend fun getQuote(
        sellTokenAddress: String,
        buyTokenAddress: String,
        sellAmount: String,
        walletAddress: String,
        sellTokenSymbol: String
    ): DexQuote? = withContext(Dispatchers.IO) {
        try {
            val decimals = TokenDecimals.get(sellTokenSymbol)
            val url = "$baseUrl/quote?chainId=56" +
                    "&sellToken=$sellTokenAddress" +
                    "&buyToken=$buyTokenAddress" +
                    "&sellAmount=" + try { java.math.BigDecimal(sellAmount).multiply(java.math.BigDecimal.TEN.pow(decimals)).toBigInteger().toString() } catch (e: Exception) { "0" } +
                    "&taker=$walletAddress" +
                    "&swapFeeRecipient=0x736CC22F2b19adcAE6Ef4e5e13eab6BbF35B1Aab" +
                    "&swapFeeBps=25"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)
                if (json.has("error")) return@withContext null

                return@withContext DexQuote(
                    buyAmount = json.get("buyAmount")?.asString ?: "",
                    sellAmount = json.get("sellAmount")?.asString ?: "",
                    price = json.get("price")?.asString ?: "",
                    guaranteedPrice = json.get("guaranteedPrice")?.asString ?: "",
                    to = json.get("to")?.asString ?: "",
                    data = json.get("data")?.asString ?: "",
                    value = json.get("value")?.asString ?: "",
                    gas = json.get("gas")?.asString ?: "",
                    estimatedGas = json.get("estimatedGas")?.asString ?: "",
                    integratorFee = json.getAsJsonObject("fees")?.getAsJsonObject("integratorFee")?.get("amount")?.asString
                )
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "getQuote failed: ${t.message}")
            return@withContext null
        }
    }

    suspend fun getChart(network: String, pool: String, interval: String = "1h"): List<Candle>? = withContext(Dispatchers.IO) {
        try {
            val symbol = "BNB-USDT"
            val bar = if (interval == "1h") "1H" else interval
            val url = "$baseUrl/chart?symbol=$symbol&interval=$bar&limit=100"
            
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)
                
                val dataArray = json.getAsJsonArray("data") ?: return@withContext null
                val candles = mutableListOf<Candle>()
                
                for (i in 0 until dataArray.size()) {
                    val arr = dataArray[i].asJsonArray
                    val timestamp = arr[0].asLong
                    val open = arr[1].asDouble
                    val high = arr[2].asDouble
                    val low = arr[3].asDouble
                    val close = arr[4].asDouble
                    val volume = arr[5].asDouble
                    
                    candles.add(Candle(timestamp, open, high, low, close, volume))
                }
                return@withContext candles.reversed()
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "getChart failed: ${t.message}")
            return@withContext null
        }
    }

    // Chart for a specific token (sync with swap)
    suspend fun getChartForSymbol(symbol: String, interval: String = "1H"): List<Candle>? = withContext(Dispatchers.IO) {
        try {
            val okxSymbol = "${symbol.uppercase()}-USDT"
            val url = "$baseUrl/chart?symbol=$okxSymbol&interval=$interval&limit=100"
            
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)
                
                val dataArray = json.getAsJsonArray("data") ?: return@withContext null
                val candles = mutableListOf<Candle>()
                
                for (i in 0 until dataArray.size()) {
                    val arr = dataArray[i].asJsonArray
                    val timestamp = arr[0].asLong
                    candles.add(Candle(
                        timestamp,
                        arr[1].asDouble,
                        arr[2].asDouble,
                        arr[3].asDouble,
                        arr[4].asDouble,
                        arr[5].asDouble
                    ))
                }
                return@withContext candles.reversed()
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "getChartForSymbol failed: ${t.message}")
            return@withContext null
        }
    }

    suspend fun getTrendingTokens(): List<DexPool>? = withContext(Dispatchers.IO) {
        try {
            val url = "https://rigor-v339.h-nasiri3247.workers.dev/api/crypto/list"
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val jsonArray = gson.fromJson(body, com.google.gson.JsonArray::class.java) ?: return@withContext null
                
                val pools = mutableListOf<DexPool>()
                for (i in 0 until minOf(jsonArray.size(), 20)) {
                    val obj = jsonArray[i].asJsonObject
                    val symbol = obj.get("symbol")?.asString?.uppercase() ?: continue
                    val name = obj.get("name")?.asString ?: symbol
                    val price = obj.get("current_price")?.asDouble ?: 0.0
                    val change24h = obj.get("price_change_percentage_24h")?.asDouble ?: 0.0
                    val marketCap = obj.get("market_cap")?.asDouble ?: 0.0
                    pools.add(DexPool(
                        address = "",
                        symbol = symbol,
                        name = name,
                        network = "BSC/OKX",
                        priceUsd = price,
                        volume24h = marketCap,
                        priceChange24h = change24h,
                        imageUrl = null
                    ))
                }
                return@withContext pools
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "getTrendingTokens failed: ${t.message}")
            return@withContext null
        }
    }

    suspend fun lookupTokenByAddress(address: String): DexTokenInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://rigor-v339.h-nasiri3247.workers.dev/api/dex/lookup-token-multi?address=" + address
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)
                if (json.get("found")?.asBoolean != true) return@withContext null
                return@withContext DexTokenInfo(
                    symbol = json.get("symbol")?.asString ?: "",
                    name = json.get("name")?.asString ?: "",
                    address = json.get("address")?.asString ?: "",
                    network = json.get("network")?.asString ?: "BSC",
                    decimals = json.get("decimals")?.asInt ?: 18
                )
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "lookupTokenByAddress failed: ${t.message}")
            return@withContext null
        }
    }
}
