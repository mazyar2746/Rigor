package com.example.data.wallet

import android.util.Log
import org.web3j.crypto.Keys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WalletSimulator - A mock implementation of wallet connection.
 * 
 * این کلاس فعلاً برای شبیه‌سازی اتصال کیف پول طراحی شده.
 * وقتی تحریم‌ها حل شد، می‌تونیم این رو با WalletConnect واقعی جایگزین کنیم.
 */
class WalletSimulator {

    private val _connectedAddress = MutableStateFlow<String?>(null)
    val connectedAddress: StateFlow<String?> = _connectedAddress.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * شبیه‌سازی اتصال کیف پول.
     * یه آدرس ولید اتریومی تولید می‌کنه و به عنوان "کیف پول متصل" ذخیره می‌کنه.
     */
    fun simulateConnect() {
        try {
            val keyPair = Keys.createEcKeyPair()
            val address = "0x" + Keys.getAddress(keyPair.publicKey)
            _connectedAddress.value = address
            _isConnected.value = true
            Log.d("WalletSimulator", "Simulated wallet connected: $address")
        } catch (e: Exception) {
            Log.e("WalletSimulator", "Failed to simulate connect: ${e.message}")
        }
    }

    fun disconnect() {
        _connectedAddress.value = null
        _isConnected.value = false
        Log.d("WalletSimulator", "Wallet disconnected")
    }
}
