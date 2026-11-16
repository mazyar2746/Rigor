package com.example.data.wallet

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.util.concurrent.TimeUnit

/**
 * WalletManager - In-App Wallet (Non-Custodial)
 * 
 * این کلاس یه کیف پول داخلی برای اپ می‌سازه که:
 * - کلید خصوصی رو فقط تو گوشی کاربر (SharedPreferences) ذخیره می‌کنه.
 * - هیچ ارتباطی با WalletConnect یا سرویس‌های تحریم‌شده نداره.
 * - برای همه شبکه‌های EVM کار می‌کنه.
 */
class WalletManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "rigor_wallet_prefs", Context.MODE_PRIVATE
    )

    private val _walletAddress = MutableStateFlow<String?>(null)
    val walletAddress: StateFlow<String?> = _walletAddress.asStateFlow()

    private val _isWalletCreated = MutableStateFlow(false)
    val isWalletCreated: StateFlow<Boolean> = _isWalletCreated.asStateFlow()

    private val _walletBalance = MutableStateFlow("0.0")
    val walletBalance: StateFlow<String> = _walletBalance.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // RPC URLs
    private val rpcUrls = mapOf(
        56 to "https://bsc-dataseed.binance.org/",
        1 to "https://eth.llamarpc.com",
        137 to "https://polygon-rpc.com",
        42161 to "https://arb1.arbitrum.io/rpc"
    )

    init {
        loadExistingWallet()
    }

    private fun loadExistingWallet() {
        try {
            val existingAddress = prefs.getString("wallet_address", null)
            if (existingAddress != null) {
                _walletAddress.value = existingAddress
                _isWalletCreated.value = true
                Log.d("WalletManager", "Existing wallet loaded: $existingAddress")
            }
        } catch (e: Exception) {
            Log.e("WalletManager", "Failed to load existing wallet: ${e.message}")
        }
    }

    suspend fun createNewWallet(): String? = withContext(Dispatchers.IO) {
        try {
            val ecKeyPair = Keys.createEcKeyPair()
            val credentials = Credentials.create(ecKeyPair)
            val address = "0x" + credentials.address
            
            prefs.edit()
                .putString("wallet_address", address)
                .putString("wallet_private_key", ecKeyPair.privateKey.toString(16))
                .apply()
            
            _walletAddress.value = address
            _isWalletCreated.value = true
            
            Log.d("WalletManager", "New wallet created: $address")
            return@withContext address
        } catch (e: Exception) {
            _errorMessage.value = "خطا در ساخت کیف پول: ${e.message}"
            Log.e("WalletManager", "Failed to create wallet: ${e.message}")
            return@withContext null
        }
    }

    suspend fun getBalance(chainId: Int = 56): String = withContext(Dispatchers.IO) {
        try {
            val address = _walletAddress.value ?: return@withContext "0.0"
            val rpcUrl = rpcUrls[chainId] ?: rpcUrls[56]!!
            
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            
            val requestBody = """
                {"jsonrpc":"2.0","method":"eth_getBalance","params":["$address","latest"],"id":1}
            """.trimIndent()
            
            val request = Request.Builder()
                .url(rpcUrl)
                .post(okhttp3.RequestBody.create(
                    "application/json".toMediaType(),
                    requestBody
                ))
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "0.0"
                val body = response.body?.string() ?: return@withContext "0.0"
                val json = Gson().fromJson(body, JsonObject::class.java)
                val balanceHex = json.get("result")?.asString ?: "0x0"
                val balanceWei = balanceHex.removePrefix("0x").toBigInteger(16)
                val balanceEther = java.math.BigDecimal(balanceWei).divide(java.math.BigDecimal.TEN.pow(18))
                val formatted = String.format("%.6f", balanceEther)
                _walletBalance.value = formatted
                Log.d("WalletManager", "Balance for $address on chain $chainId: $formatted")
                return@withContext formatted
            }
        } catch (e: Exception) {
            Log.e("WalletManager", "Failed to get balance: ${e.message}")
            return@withContext "0.0"
        }
    }

    fun deleteWallet() {
        prefs.edit().clear().apply()
        _walletAddress.value = null
        _isWalletCreated.value = false
        _walletBalance.value = "0.0"
        Log.d("WalletManager", "Wallet deleted")
    }

    fun hasWallet(): Boolean = _isWalletCreated.value
}
