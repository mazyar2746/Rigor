package com.example.data.repository

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.DexPool
import com.example.data.model.DexQuote
import com.example.data.model.CrossChainQuote
import com.example.data.model.DexTokenInfo
import com.example.data.remote.DexRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DexRepository {
    private val remote = DexRemoteDataSource()

    private val _currentQuote = MutableStateFlow<DexQuote?>(null)
    val currentQuote = _currentQuote.asStateFlow()

    private val _crossChainQuote = MutableStateFlow<CrossChainQuote?>(null)
    val crossChainQuote = _crossChainQuote.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _chartData = MutableStateFlow<List<Candle>>(emptyList())
    val chartData = _chartData.asStateFlow()

    private val _isChartLoading = MutableStateFlow(false)
    val isChartLoading = _isChartLoading.asStateFlow()

    private val _newPools = MutableStateFlow<List<DexPool>>(emptyList())
    val newPools = _newPools.asStateFlow()

    private val _isPoolsLoading = MutableStateFlow(false)
    val isPoolsLoading = _isPoolsLoading.asStateFlow()

    suspend fun fetchQuote(
        sellTokenAddress: String,
        buyTokenAddress: String,
        sellAmount: String,
        walletAddress: String,
        sellTokenSymbol: String,
        chainId: Int = 56
    ) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        _currentQuote.value = null
        try {
            val quote = remote.getQuote(sellTokenAddress, buyTokenAddress, sellAmount, walletAddress, sellTokenSymbol, chainId)
            if (quote != null) _currentQuote.value = quote
            else _errorMessage.value = "خطا در دریافت قیمت."
        } catch (t: Throwable) {
            _errorMessage.value = "خطا: ${t.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun fetchChart(network: String, poolAddress: String) = withContext(Dispatchers.IO) {
        _isChartLoading.value = true
        try {
            val candles = remote.getChart(network, poolAddress)
            if (candles != null) _chartData.value = candles
        } finally {
            _isChartLoading.value = false
        }
    }

    suspend fun fetchChartForSymbol(symbol: String) = withContext(Dispatchers.IO) {
        _isChartLoading.value = true
        try {
            val candles = remote.getChartForSymbol(symbol)
            if (candles != null) _chartData.value = candles
        } finally {
            _isChartLoading.value = false
        }
    }

    suspend fun fetchNewPools(network: String) = withContext(Dispatchers.IO) {
        _isPoolsLoading.value = true
        try {
            val pools = remote.getTrendingTokens()
            if (pools != null) _newPools.value = pools
        } finally {
            _isPoolsLoading.value = false
        }
    }

    suspend fun lookupTokenByAddress(address: String): DexTokenInfo? = withContext(Dispatchers.IO) {
        return@withContext remote.lookupTokenByAddress(address)
    }

    fun clearState() {
        _currentQuote.value = null
        _crossChainQuote.value = null
        _errorMessage.value = null
        _isLoading.value = false
    }

    suspend fun fetchCrossChainQuote(
        originChain: Int,
        destinationChain: Int,
        sellToken: String,
        buyToken: String,
        sellAmount: String,
        originAddress: String
    ) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null
        _crossChainQuote.value = null
        try {
            val quote = remote.getCrossChainQuote(originChain, destinationChain, sellToken, buyToken, sellAmount, originAddress)
            if (quote != null) _crossChainQuote.value = quote
            else _errorMessage.value = "خطا در دریافت قیمت Cross-Chain"
        } finally {
            _isLoading.value = false
        }
    }
}
