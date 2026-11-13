package com.example.data.repository

import android.util.Log
import com.example.data.model.DexQuote
import com.example.data.remote.DexRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * DexRepository - Manages DEX state and connects Network to UI.
 */
class DexRepository {
    private val remote = DexRemoteDataSource()

    private val _currentQuote = MutableStateFlow<DexQuote?>(null)
    val currentQuote = _currentQuote.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _chartData = MutableStateFlow<List<com.example.data.model.Candle>>(emptyList())
    val chartData = _chartData.asStateFlow()

    private val _isChartLoading = MutableStateFlow(false)
    val isChartLoading = _isChartLoading.asStateFlow()

    private val _newPools = MutableStateFlow<List<com.example.data.model.DexPool>>(emptyList())
    val newPools = _newPools.asStateFlow()

    private val _isPoolsLoading = MutableStateFlow(false)
    val isPoolsLoading = _isPoolsLoading.asStateFlow()


    suspend fun fetchQuote(
        sellTokenAddress: String,
        buyTokenAddress: String,
        sellAmount: String,
        walletAddress: String,
        sellTokenSymbol: String,
        sellTokenSymbol: String
    ) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        _currentQuote.value = null

        try {
            val quote = remote.getQuote(
                sellTokenAddress = sellTokenAddress,
                buyTokenAddress = buyTokenAddress,
                sellAmount = sellAmount,
                walletAddress = walletAddress,
                sellTokenSymbol = sellTokenSymbol,
                sellTokenSymbol = sellTokenSymbol
            )

            if (quote != null) {
                _currentQuote.value = quote
                Log.d("DexRepo", "Quote fetched successfully. You will receive: ${quote.buyAmount}")
            } else {
                _errorMessage.value = "خطا در دریافت قیمت. لطفا دوباره تلاش کنید."
            }
        } catch (t: Throwable) {
            _errorMessage.value = "خطا: ${t.message}"
            Log.e("DexRepo", "fetchQuote failed", t)
        } finally {
            _isLoading.value = false
        }
    }

    
    suspend fun fetchChart(network: String, poolAddress: String) = withContext(Dispatchers.IO) {
        _isChartLoading.value = true
        try {
            val candles = remote.getChart(network, poolAddress)
            if (candles != null) {
                _chartData.value = candles
            }
        } catch (t: Throwable) {
            Log.e("DexRepo", "fetchChart failed", t)
        } finally {
            _isChartLoading.value = false
        }
    }

    suspend fun fetchNewPools(network: String) = withContext(Dispatchers.IO) {
        _isPoolsLoading.value = true
        try {
            val pools = remote.getTrendingTokens()
            if (pools != null) {
                _newPools.value = pools
            }
        } catch (t: Throwable) {
            Log.e("DexRepo", "fetchNewPools failed", t)
        } finally {
            _isPoolsLoading.value = false
        }
    }

    fun clearState() {
        _currentQuote.value = null
        _errorMessage.value = null
        _isLoading.value = false
    }
}
