package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Candle
import com.example.data.model.DexPool
import com.example.data.model.DexQuote
import com.example.data.model.DexTokenInfo
import com.example.data.repository.DexRepository
import com.example.data.wallet.WalletManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RigourViewModel(application: Application) : AndroidViewModel(application) {
    private val dexRepository = DexRepository()
    private val walletManager = WalletManager(application)

    // ============================================================
    // DEX State
    // ============================================================
    val dexQuote: StateFlow<DexQuote?> = dexRepository.currentQuote
    val isDexLoading: StateFlow<Boolean> = dexRepository.isLoading
    val dexErrorMessage: StateFlow<String?> = dexRepository.errorMessage
    val dexChartData: StateFlow<List<Candle>> = dexRepository.chartData
    val isDexChartLoading: StateFlow<Boolean> = dexRepository.isChartLoading
    val dexNewPools: StateFlow<List<DexPool>> = dexRepository.newPools
    val isDexPoolsLoading: StateFlow<Boolean> = dexRepository.isPoolsLoading

    // ============================================================
    // Wallet State
    // ============================================================
    val walletConnectedAddress: StateFlow<String?> = walletManager.walletAddress
    val isWalletCreated: StateFlow<Boolean> = walletManager.isWalletCreated
    val walletBalance: StateFlow<String> = walletManager.walletBalance
    val walletErrorMessage: StateFlow<String?> = walletManager.errorMessage

    // ============================================================
    // DEX Functions
    // ============================================================
    fun getDexQuote(
        sellTokenAddress: String,
        buyTokenAddress: String,
        sellAmount: String,
        walletAddress: String,
        sellTokenSymbol: String,
        chainId: Int = 56,
        sellTokenDecimals: Int = 18
    ) {
        viewModelScope.launch {
            dexRepository.fetchQuote(
                sellTokenAddress, buyTokenAddress, sellAmount,
                walletAddress, sellTokenSymbol, chainId, sellTokenDecimals
            )
        }
    }

    fun getDexChartForSymbol(symbol: String) {
        viewModelScope.launch {
            dexRepository.fetchChartForSymbol(symbol)
        }
    }

    fun getDexNewPools(network: String) {
        viewModelScope.launch {
            dexRepository.fetchNewPools(network)
        }
    }

    fun lookupDexToken(address: String, onResult: (DexTokenInfo?) -> Unit) {
        viewModelScope.launch {
            val token = dexRepository.lookupTokenByAddress(address)
            onResult(token)
        }
    }

    fun clearDexState() {
        dexRepository.clearState()
    }

    // ============================================================
    // Wallet Functions
    // ============================================================
    fun createWallet() {
        viewModelScope.launch {
            val address = walletManager.createNewWallet()
            if (address != null) {
                // بعد از ساخت، موجودی رو هم بگیر
                walletManager.getBalance(56)
            }
        }
    }

    fun refreshWalletBalance(chainId: Int = 56) {
        viewModelScope.launch {
            walletManager.getBalance(chainId)
        }
    }

    fun deleteWallet() {
        walletManager.deleteWallet()
    }
}
