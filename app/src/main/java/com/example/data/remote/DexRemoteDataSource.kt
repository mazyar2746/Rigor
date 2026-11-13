package com.example.data.remote

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.DexPool
import com.example.data.model.DexQuote
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * DexRemoteDataSource - Connects to our Cloudflare Worker.
 * Handles 0x swaps, DexPaprika charts, and DexScreener new pools.
 */
class DexRemoteDataSource {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://rigor-v339.h-nasiri3247.workers.dev/api/dex"

    // --- 1. Get Swap Quote (0x API) ---
    suspend fun getQuote(
        sellTokenAddress: String,
        buyTokenAddress: String,
        sellAmount: String,
        walletAddress: String
    ): DexQuote? = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/quote?chainId=56" +
                    "&sellToken=$sellTokenAddress" +
                    "&buyToken=$buyTokenAddress" +
                    "&sellAmount=" + try { java.math.BigDecimal(sellAmount).multiply(java.math.BigDecimal("1000000000000000000")).toBigInteger().toString() } catch (e: Exception) { "0" } +
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

    // --- 2. Get Chart Data (DexPaprika) ---
    suspend fun getChart(network: String, pool: String, interval: String = "1h"): List<Candle>? = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/chart?network=$network&pool=$pool&interval=$interval&limit=100"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                
                // Check if body is empty
                if (body.isBlank() || !body.startsWith("{")) return@withContext null
                
                val json = gson.fromJson(body, JsonObject::class.java)
                val dataArray = json.getAsJsonArray("data") ?: return@withContext null
                val candles = mutableListOf<Candle>()
                
                for (i in 0 until dataArray.size()) {
                    val obj = dataArray[i].asJsonObject
                    
                    // Safely parse values from DexPaprika response
                    val open = obj.get("open")?.asDouble ?: 0.0
                    val high = obj.get("high")?.asDouble ?: 0.0
                    val low = obj.get("low")?.asDouble ?: 0.0
                    val close = obj.get("close")?.asDouble ?: 0.0
                    val volume = obj.get("volume")?.asDouble ?: 0.0
                    
                    // Generate a sequential timestamp to ensure the chart draws properly
                    val fakeTimestamp = System.currentTimeMillis() - ((dataArray.size() - i) * 3600000L)

                    candles.add(Candle(
                        timestamp = fakeTimestamp,
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = volume
                    ))
                }
                return@withContext candles
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "getChart failed: ${t.message}")
            return@withContext null
        }
    }

    // --- 3. Get New Pools / Tokens (DexScreener) ---
    suspend fun getNewPools(network: String = "bsc"): List<DexPool>? = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/new-pools?network=$network"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val jsonArray = gson.fromJson(body, com.google.gson.JsonArray::class.java) ?: return@withContext null
                
                val pools = mutableListOf<DexPool>()
                for (i in 0 until minOf(jsonArray.size(), 15)) {
                    val obj = jsonArray[i].asJsonObject
                    pools.add(DexPool(
                        address = obj.get("tokenAddress")?.asString ?: "",
                        name = obj.get("description")?.asString ?: "Unknown Token",
                        network = obj.get("chainId")?.asString ?: "bsc",
                        priceUsd = obj.get("priceUsd")?.asString?.toDoubleOrNull() ?: 0.0,
                        volume24h = obj.get("volume24h")?.asString?.toDoubleOrNull() ?: 0.0,
                        imageUrl = obj.get("icon")?.asString
                    ))
                }
                return@withContext pools
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "getNewPools failed: ${t.message}")
            return@withContext null
        }
    }
}
