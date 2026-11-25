package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RigourDatabase
import com.example.data.model.CoinSignalData
import com.example.data.model.CryptoCoin
import com.example.data.model.MarketDataProvenance
import com.example.data.model.SubscriptionPlan
import com.example.data.model.TechnicalAnalysisResult
import com.example.data.model.UserSubscription
import com.example.data.repository.CryptoRepository
import com.example.data.repository.ForexRepository
import com.example.data.repository.IranStockAnalysisService
import com.example.data.repository.FullAnalysisResult
import com.example.data.repository.DexRepository
import com.example.data.wallet.WalletManager
import com.example.data.wallet.WalletSimulator
import com.example.data.model.DexQuote
import com.example.data.model.CrossChainQuote
import com.example.data.model.Candle
import com.example.data.model.DexPool
import com.example.data.model.ForexPair
import com.example.data.model.ForexPairs
import com.example.data.model.ForexQuote
import com.example.data.model.ForexAnalysisResult
import com.example.data.model.ForexNewsItem
import com.example.data.repository.SubscriptionRepository
import com.example.i18n.Language
import com.example.i18n.StringsDictionary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers

sealed class Screen {
    object Welcome : Screen()
    object Hub : Screen()
    object Markets : Screen()
    object IranStockSearch : Screen()
    data class IranStockDetail(val stockName: String) : Screen()
    object Signals : Screen()
    data class CoinDetail(val coin: CryptoCoin) : Screen()
    object Subscription : Screen()
    object ContactUs : Screen()
    object Settings : Screen()
    object Forex : Screen()
    object Dex : Screen()
    data class ForexDetail(val symbol: String) : Screen()
}

class RigourViewModel(application: Application) : AndroidViewModel(application) {
    var lastMarketScreen: Screen = Screen.Markets
        private set


    private val database = RigourDatabase.getInstance(application)
    private val cryptoRepository = CryptoRepository(database.favoritesDao(), database.coinCacheDao(), context = application)
    private val forexRepository = ForexRepository()
    private val iranStockAnalysisService = IranStockAnalysisService(getApplication())
    private val dexRepository = DexRepository()
    private val walletManager = WalletManager(application)
    private val subscriptionRepository = SubscriptionRepository(database.subscriptionDao())

    // App Navigation
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Hub)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // i18n & RTL State
    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    // Crypto Market Data & Pagination
    val coins: StateFlow<List<CryptoCoin>> = cryptoRepository.coins
    val isMarketLoading: StateFlow<Boolean> = cryptoRepository.isLoading
    val isLoadingMore: StateFlow<Boolean> = cryptoRepository.isLoadingMore
    val hasMore: StateFlow<Boolean> = cryptoRepository.hasMore
    val lastUpdated: StateFlow<Long> = cryptoRepository.lastUpdated
    val favoriteSymbols: StateFlow<List<String>> = cryptoRepository.favoriteSymbols
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCoins: StateFlow<List<CryptoCoin>> = combine(
        coins,
        _searchQuery,
        _onlyFavorites,
        favoriteSymbols
    ) { coinList, query, onlyFavs, favs ->
        var list = coinList
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.symbol.lowercase().contains(q)
            }
        }
        if (onlyFavs) {
            list = list.filter { favs.contains(it.symbol) }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Coin & Technical Analysis
    private val _selectedCoin = MutableStateFlow<CryptoCoin?>(null)
    val selectedCoin: StateFlow<CryptoCoin?> = _selectedCoin.asStateFlow()

    private val _currentTimeframe = MutableStateFlow("1h")
    val currentTimeframe: StateFlow<String> = _currentTimeframe.asStateFlow()

    private val _analysisResult = MutableStateFlow<TechnicalAnalysisResult?>(null)
    val analysisResult: StateFlow<TechnicalAnalysisResult?> = _analysisResult.asStateFlow()

    private val _isAnalysisLoading = MutableStateFlow(false)
    val isAnalysisLoading: StateFlow<Boolean> = _isAnalysisLoading.asStateFlow()

    // Dedicated AI Trading Signals Stream
    private val _signals = MutableStateFlow<List<CoinSignalData>>(emptyList())
    val signals: StateFlow<List<CoinSignalData>> = _signals.asStateFlow()

    private val _isSignalsLoading = MutableStateFlow(false)
    val isSignalsLoading: StateFlow<Boolean> = _isSignalsLoading.asStateFlow()

    // Subscription & Paywall
    val subscription: StateFlow<UserSubscription> = subscriptionRepository.currentSubscriptionFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserSubscription(false, null, 0L, "")
        )

    private val _isVerifyingTx = MutableStateFlow(false)
    val isVerifyingTx: StateFlow<Boolean> = _isVerifyingTx.asStateFlow()

    private val _verificationMessage = MutableStateFlow<String?>(null)
    val verificationMessage: StateFlow<String?> = _verificationMessage.asStateFlow()

    private val _isVerificationSuccess = MutableStateFlow(false)
    val isVerificationSuccess: StateFlow<Boolean> = _isVerificationSuccess.asStateFlow()

    // API Keys & Settings
    private val _cmcApiKey = MutableStateFlow("")
    val cmcApiKey: StateFlow<String> = _cmcApiKey.asStateFlow()

    private val _bscApiKey = MutableStateFlow("")
    val bscApiKey: StateFlow<String> = _bscApiKey.asStateFlow()

    private val _isSandboxMode = MutableStateFlow(false)
    val isSandboxMode: StateFlow<Boolean> = _isSandboxMode.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            cryptoRepository.initializeFromCache()
            cryptoRepository.refreshCoins(_cmcApiKey.value)
            computeSignals()
        }
        startPeriodicRefresh()
    }

    fun loadMoreCoins() {
        viewModelScope.launch {
            cryptoRepository.loadNextBatch(_cmcApiKey.value)
        }
    }

    fun computeSignals() {
        viewModelScope.launch {
            _isSignalsLoading.value = true
            try {
                val currentCoins = coins.value.take(15)
                val deferredList = currentCoins.map { coin ->
                    async(Dispatchers.IO) {
                        try {
                            val analysis = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                                cryptoRepository.getTechnicalAnalysis(coin, "1h")
                            }
                            if (analysis != null && (analysis.provenance == MarketDataProvenance.LIVE_EXCHANGE || analysis.provenance == MarketDataProvenance.CACHED_EXCHANGE)) {
                                CoinSignalData(coin, analysis)
                            } else null
                        } catch (e: Exception) {
                            android.util.Log.w("RigourVM", "Failed: ${coin.symbol}")
                            null
                        }
                    }
                }
                _signals.value = deferredList.awaitAll().filterNotNull()
            } finally {
                _isSignalsLoading.value = false
            }
        }
    }


    val forexSignals = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.data.model.ForexAnalysisResult>>(emptyList())
    val iranStockSignals = kotlinx.coroutines.flow.MutableStateFlow<List<FullAnalysisResult>>(emptyList())
    fun computeForexSignals() {
        viewModelScope.launch {
            _isSignalsLoading.value = true
            try {
                val l = mutableListOf<com.example.data.model.ForexAnalysisResult>()
                for (p in com.example.data.model.ForexPairs.ALL.take(5)) {
                    try {
                        forexRepository.analyzePair(p.symbol, "1h")?.let { l.add(it) }
                        kotlinx.coroutines.delay(2500)
                    } catch (e: Exception) {
                        android.util.Log.e("RigourVM", "Forex API Error: ${e.message}", e)
                    }
                }
                forexSignals.value = l
            } finally {
                _isSignalsLoading.value = false
            }
        }
    }
    fun computeIranStockSignals() {
        viewModelScope.launch {
            _isSignalsLoading.value = true
            try {
                val l = mutableListOf<FullAnalysisResult>()
                val s = listOf("فولاد", "خودرو", "شپنا", "وبملت", "فملی")
                for (x in s) {
                    try {
                        iranStockAnalysisService.analyzeStock(x)?.let { l.add(it) }
                        kotlinx.coroutines.delay(2500)
                    } catch (e: Exception) {
                        android.util.Log.e("RigourVM", "Stock API Error: ${e.message}", e)
                    }
                }
                iranStockSignals.value = l
            } finally {
                _isSignalsLoading.value = false
            }
        }
    }
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Signals && _signals.value.isEmpty()) {
            computeSignals()
        }
    }

    fun selectCoin(coin: CryptoCoin) {
        _selectedCoin.value = coin
        _currentScreen.value = Screen.CoinDetail(coin)
        loadTechnicalAnalysis(coin, _currentTimeframe.value)
    }

    fun setTimeframe(timeframe: String) {
        _currentTimeframe.value = timeframe
        _selectedCoin.value?.let { coin ->
            loadTechnicalAnalysis(coin, timeframe)
        }
    }

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesOnly() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun toggleFavorite(symbol: String) {
        viewModelScope.launch {
            cryptoRepository.toggleFavorite(symbol)
        }
    }

    fun setCmcApiKey(key: String) {
        _cmcApiKey.value = key
        loadCoins()
    }

    fun setBscApiKey(key: String) {
        _bscApiKey.value = key
    }

    fun setSandboxMode(enabled: Boolean) {
        _isSandboxMode.value = enabled
    }

    fun loadCoins() {
        viewModelScope.launch {
            cryptoRepository.refreshCoins(_cmcApiKey.value)
        }
    }

    fun loadTechnicalAnalysis(coin: CryptoCoin, timeframe: String) {
        viewModelScope.launch {
            _isAnalysisLoading.value = true
            try {
                val result = cryptoRepository.getTechnicalAnalysis(coin, timeframe)
                _analysisResult.value = result
            } catch (e: Exception) {
                // Return explicitly marked unavailable result, NOT disguised signals
                _analysisResult.value = com.example.engine.TechnicalAnalysisEngine.createUnavailableResult(
                    symbol = coin.symbol,
                    currentPrice = coin.priceUsd,
                    candles = emptyList(),
                    provenance = MarketDataProvenance.UNAVAILABLE,
                    reason = "Market data unavailable: ${e.message}"
                )
            } finally {
                _isAnalysisLoading.value = false
            }
        }
    }

    fun verifySubscription(txHash: String, plan: SubscriptionPlan) {
        viewModelScope.launch {
            _isVerifyingTx.value = true
            _verificationMessage.value = null
            _isVerificationSuccess.value = false

            val result = subscriptionRepository.verifyAndActivate(
                txHash = txHash,
                plan = plan,
                bscApiKey = _bscApiKey.value,
                isSandbox = _isSandboxMode.value
            )

            _isVerifyingTx.value = false
            if (result.isSuccess) {
                _isVerificationSuccess.value = true
                _verificationMessage.value = StringsDictionary.get("verification_success", _currentLanguage.value)
            } else {
                _isVerificationSuccess.value = false
                _verificationMessage.value = result.exceptionOrNull()?.message
                    ?: StringsDictionary.get("verification_failed", _currentLanguage.value)
            }
        }
    }

    fun activateInstantSandboxAccess(plan: SubscriptionPlan) {
        val testHash = "0x" + System.currentTimeMillis().toString(16).padStart(64, 'a')
        viewModelScope.launch {
            _isVerifyingTx.value = true
            val result = subscriptionRepository.verifyAndActivate(
                txHash = testHash,
                plan = plan,
                isSandbox = true
            )
            _isVerifyingTx.value = false
            if (result.isSuccess) {
                _isVerificationSuccess.value = true
                _verificationMessage.value = "Sandbox access granted for ${plan.durationDays} days!"
            }
        }
    }

    fun activateBazaarSubscription(purchaseToken: String) {
        viewModelScope.launch {
            _isVerifyingTx.value = true
            val result = subscriptionRepository.activateBazaarSubscription(purchaseToken)
            _isVerifyingTx.value = false
            if (result.isSuccess) {
                _isVerificationSuccess.value = true
                _verificationMessage.value = StringsDictionary.get("bazaar_purchase_success", _currentLanguage.value)
            } else {
                _isVerificationSuccess.value = false
                _verificationMessage.value = "Failed to activate Cafe Bazaar subscription"
            }
        }
    }

    fun onBazaarPurchaseCanceled() {
        _verificationMessage.value = "Cafe Bazaar purchase was canceled."
        _isVerificationSuccess.value = false
    }

    fun onBazaarPurchaseError(error: String) {
        _verificationMessage.value = "Cafe Bazaar error: $error"
        _isVerificationSuccess.value = false
    }

    fun clearVerificationStatus() {
        _verificationMessage.value = null
        _isVerificationSuccess.value = false
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(60_000) // 60 seconds data refresh
                cryptoRepository.refreshCoins(_cmcApiKey.value)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    // ============================================================
    // FOREX STATE & FUNCTIONS
    // ============================================================

    // Live quotes from ForexRepository
    val forexQuotes: StateFlow<Map<String, ForexQuote>> = forexRepository.quotes
    val forexAnalyses: StateFlow<Map<String, ForexAnalysisResult>> = forexRepository.analyses
    val isForexLoading: StateFlow<Boolean> = forexRepository.isLoading
    val forexErrorMessage: StateFlow<String?> = forexRepository.errorMessage
    val forexLastUpdated: StateFlow<Long> = forexRepository.lastUpdated
    val forexNews: StateFlow<List<ForexNewsItem>> = forexRepository.news
    val forexPersianNews: StateFlow<List<ForexNewsItem>> = forexRepository.persianNews
    val isForexNewsLoading: StateFlow<Boolean> = forexRepository.isNewsLoading

    // Currently selected forex pair (for detail screen)
    private val _selectedForexPair = MutableStateFlow<ForexPair?>(null)
    val selectedForexPair: StateFlow<ForexPair?> = _selectedForexPair.asStateFlow()

    // --- Navigation: Forex ---

    fun navigateToForex() {
        _currentScreen.value = Screen.Forex
        // Load first page (10 majors) automatically
        viewModelScope.launch {
            forexRepository.loadFirstPage()
            forexRepository.loadForexNews()
        }
    }

    fun selectForexPair(pair: ForexPair) {
        _selectedForexPair.value = pair
        _currentScreen.value = Screen.ForexDetail(pair.symbol)
        // Load candles + analysis for this pair
        viewModelScope.launch {
            forexRepository.analyzePair(pair.symbol, "1h", 120)
        }
    }

    fun refreshForexQuotes(pairSymbols: List<String>) {
        viewModelScope.launch {
            forexRepository.loadQuotesForSymbols(pairSymbols)
        }
    }

    fun refreshForexDetail() {
        _selectedForexPair.value?.let { pair ->
            viewModelScope.launch {
                forexRepository.analyzePair(pair.symbol, "1h", 120)
            }
        }
    }

    fun getCachedForexAnalysis(symbol: String): ForexAnalysisResult? {
        return forexRepository.cachedAnalysis(symbol)
    }

    fun getCachedForexCandles(symbol: String) = forexRepository.cachedCandles(symbol)

    fun clearForexError() {
        forexRepository.clearError()
    }

    // ============================================================
    // DEX STATE & FUNCTIONS
    // ============================================================

    val dexQuote: StateFlow<DexQuote?> = dexRepository.currentQuote
    val isDexLoading: StateFlow<Boolean> = dexRepository.isLoading
    val dexErrorMessage: StateFlow<String?> = dexRepository.errorMessage

    val dexChartData: StateFlow<List<Candle>> = dexRepository.chartData
    val isDexChartLoading: StateFlow<Boolean> = dexRepository.isChartLoading
    val dexNewPools: StateFlow<List<DexPool>> = dexRepository.newPools
    val isDexPoolsLoading: StateFlow<Boolean> = dexRepository.isPoolsLoading

    fun getDexQuote(sellTokenAddress: String, buyTokenAddress: String, sellAmount: String, walletAddress: String, sellTokenSymbol: String, chainId: Int = 56, sellTokenDecimals: Int = 18) {
        viewModelScope.launch {
            dexRepository.fetchQuote(sellTokenAddress, buyTokenAddress, sellAmount, walletAddress, sellTokenSymbol, chainId, sellTokenDecimals)
        }
    }

    fun clearDexState() {
        dexRepository.clearState()
    }

    fun getDexChart(network: String, poolAddress: String) {
        viewModelScope.launch {
            dexRepository.fetchChart(network, poolAddress)
        }
    }

    fun getDexNewPools(network: String) {
        viewModelScope.launch {
            dexRepository.fetchNewPools(network)
        }
    }

    fun getDexChartForSymbol(symbol: String) {
        viewModelScope.launch {
            dexRepository.fetchChartForSymbol(symbol)
        }
    }

    fun lookupDexToken(address: String, onResult: (com.example.data.model.DexTokenInfo?) -> Unit) {
        viewModelScope.launch {
            val token = dexRepository.lookupTokenByAddress(address)
            onResult(token)
        }
    }

    val crossChainQuote: StateFlow<CrossChainQuote?> = dexRepository.crossChainQuote

    fun getCrossChainQuote(originChain: Int, destinationChain: Int, sellToken: String, buyToken: String, sellAmount: String, originAddress: String) {
        viewModelScope.launch {
            dexRepository.fetchCrossChainQuote(originChain, destinationChain, sellToken, buyToken, sellAmount, originAddress)
        }
    }


    // ============================================================
    // WALLET (SIMULATED)
    // ============================================================
    val walletConnectedAddress: StateFlow<String?> = walletManager.walletAddress
    val isWalletCreated: StateFlow<Boolean> = walletManager.isWalletCreated
    val walletBalance: StateFlow<String> = walletManager.walletBalance

    val swapResult = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun executeSwap(quote: com.example.data.model.DexQuote) {
        viewModelScope.launch {
            swapResult.value = "در حال ارسال تراکنش..."
            val txHash = walletManager.signAndSendTransaction(
                to = quote.to,
                data = quote.data,
                value = quote.value,
                gasLimit = if (quote.gas.isEmpty()) quote.estimatedGas else quote.gas
            )
            swapResult.value = if (txHash != null) "موفق! هش: $txHash" else "خطا در ارسال تراکنش"
        }
    }

    fun createWallet() {
        viewModelScope.launch {
            val address = walletManager.createNewWallet()
            if (address != null) {
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
