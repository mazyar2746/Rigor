package com.example.data.remote

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.DexPool
import com.example.data.model.DexQuote
import com.example.data.model.DexTokenInfo
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class DexRemoteDataSource {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://rigor-v339.h-nasiri3247.workers.dev/api/dex"

    // RPC URL ها برای هر شبکه
    private val rpcUrls = mapOf(
        56 to "https://bsc-dataseed.binance.org/",
        1 to "https://eth.llamarpc.com",
        137 to "https://polygon-rpc.com",
        42161 to "https://arb1.arbitrum.io/rpc"
    )

    // ============================================================
    // Helper: Get Web3j instance for specific chain
    // ============================================================
    private fun getWeb3(chainId: Int): Web3j {
        val rpcUrl = rpcUrls[chainId] ?: rpcUrls[56]!!
        return Web3j.build(HttpService(rpcUrl))
    }

    // ============================================================
    // Read decimals from token contract (works for any chain)
    // ============================================================
    suspend fun getTokenDecimals(tokenAddress: String, chainId: Int = 56): Int = withContext(Dispatchers.IO) {
        try {
            val web3 = getWeb3(chainId)
            val function = Function(
                "decimals",
                emptyList<Type<*>>(),
                listOf(TypeReference.create(Uint8::class.java))
            )
            val encodedFunction = FunctionEncoder.encode(function)
            val transaction = Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000",
                tokenAddress,
                encodedFunction
            )
            val result = web3.ethCall(transaction, DefaultBlockParameterName.LATEST).send()
            
            if (result.hasError()) {
                Log.w("DexRemote", "Contract call error: ${result.error.message}")
                return@withContext 18
            }
            
            val decoded = FunctionReturnDecoder.decode(
                result.value,
                function.outputParameters
            )
            
            if (decoded.isNotEmpty()) {
                return@withContext (decoded[0].value as BigInteger).toInt()
            }
            return@withContext 18
        } catch (t: Throwable) {
            Log.w("DexRemote", "getTokenDecimals failed for $tokenAddress: ${t.message}")
            return@withContext 18
        }
    }

    // ============================================================
    // Get Swap Quote (0x API)
    // ============================================================
    suspend fun getQuote(
        sellTokenAddress: String,
        buyTokenAddress: String,
        sellAmount: String,
        walletAddress: String,
        sellTokenSymbol: String,
        chainId: Int = 56
    ): DexQuote? = withContext(Dispatchers.IO) {
        try {
            // اول decimals رو از خود قرارداد می‌خونیم
            val decimals = getTokenDecimals(sellTokenAddress, chainId)
            Log.d("DexRemote", "Decimals for $sellTokenSymbol: $decimals")

            val sellAmountWei = try {
                BigDecimal(sellAmount).multiply(BigDecimal.TEN.pow(decimals)).toBigInteger().toString()
            } catch (e: Exception) { "0" }

            val url = "$baseUrl/quote?chainId=$chainId" +
                    "&sellToken=$sellTokenAddress" +
                    "&buyToken=$buyTokenAddress" +
                    "&sellAmount=$sellAmountWei" +
                    "&taker=$walletAddress" +
                    "&swapFeeRecipient=0x736CC22F2b19adcAE6Ef4e5e13eab6BbF35B1Aab" +
                    "&swapFeeBps=25"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = gson.fromJson(body, JsonObject::class.java)
                if (json.has("error")) return@withContext null
                if (json.has("liquidityAvailable") && !json.get("liquidityAvailable").asBoolean) return@withContext null

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

    // ============================================================
    // Get Chart (OKX API)
    // ============================================================
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
                    candles.add(Candle(
                        timestamp = arr[0].asLong,
                        open = arr[1].asDouble,
                        high = arr[2].asDouble,
                        low = arr[3].asDouble,
                        close = arr[4].asDouble,
                        volume = arr[5].asDouble
                    ))
                }
                return@withContext candles.reversed()
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "getChartForSymbol failed: ${t.message}")
            return@withContext null
        }
    }

    // ============================================================
    // Get Trending Tokens
    // ============================================================
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
                    pools.add(DexPool(
                        address = "",
                        symbol = symbol,
                        name = name,
                        network = "BSC/OKX",
                        priceUsd = price,
                        volume24h = 0.0,
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

    // ============================================================
    // Lookup Token by Address
    // ============================================================
    suspend fun lookupTokenByAddress(address: String): DexTokenInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/lookup-token?address=$address"
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
                    chainId = json.get("chainId")?.asInt ?: 56,
                    decimals = json.get("decimals")?.asInt ?: 18
                )
            }
        } catch (t: Throwable) {
            Log.e("DexRemote", "lookupTokenByAddress failed: ${t.message}")
            return@withContext null
        }
    }
}
