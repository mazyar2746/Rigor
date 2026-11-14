package com.example.data.model

data class DexToken(
    val symbol: String,
    val name: String,
    val address: String,
    val priceUsd: Double,
    val imageUrl: String? = null
)

data class DexTokenInfo(
    val symbol: String,
    val name: String,
    val address: String,
    val network: String,
    val decimals: Int
)

data class DexQuote(
    val buyAmount: String,
    val sellAmount: String,
    val price: String,
    val guaranteedPrice: String,
    val to: String,
    val data: String,
    val value: String,
    val gas: String,
    val estimatedGas: String,
    val integratorFee: String? = null
)

data class DexPool(
    val address: String,
    val symbol: String,
    val name: String,
    val network: String,
    val priceUsd: Double,
    val volume24h: Double,
    val priceChange24h: Double,
    val imageUrl: String? = null
)

object DexTokens {
    val BSC_TOKENS = listOf(
        DexToken("BNB", "Binance Coin", "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", 0.0),
        DexToken("USDT", "Tether USD", "0x55d398326f99059fF775485246999027B3197955", 1.0),
        DexToken("BUSD", "Binance USD", "0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56", 1.0),
        DexToken("WBNB", "Wrapped BNB", "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", 0.0),
        DexToken("CAKE", "PancakeSwap", "0x0E09FaBB73Bd3Ade0a17ECC321fD13a19e81cE82", 0.0),
        DexToken("XRP", "Ripple", "0x1D2F0da169ceB9fC7B3144628dB156f3F6c60dBE", 0.0),
        DexToken("ADA", "Cardano", "0x3EE2200Efb3400fAbB9AacF31297cBdD1d435D47", 0.0),
        DexToken("DOGE", "Dogecoin", "0xbA2aE424d960c26247Dd6c32edC70B295c744C43", 0.0),
        DexToken("SHIB", "Shiba Inu", "0x2859e4544C4bB03966803b044A93563Bd2D0DD4D", 0.0),
        DexToken("PEPE", "Pepe", "0x25d887Ce7a35172C62FeBFD67a1856F20FaEbB00", 0.0),
        DexToken("TON", "Toncoin", "0x76A797A59Ba2C17726896976B7B3747BfD1d220f", 0.0),
        DexToken("LINK", "Chainlink", "0xF8A0BF9cF54Bb92F17374d9e9A321E6a111a51bD", 0.0),
        DexToken("DOT", "Polkadot", "0x7083609fCE4d1d8Dc0C979AAb8c869Ea2C873402", 0.0),
        DexToken("MATIC", "Polygon", "0xCC42724C6683B7E57334c4E856f4c9965ED682bD", 0.0),
        DexToken("LTC", "Litecoin", "0x4338665CBB7B2485A8855A139b75D5e34AB0DB94", 0.0),
        DexToken("TRX", "TRON", "0x85EAC5Ac2F758618dFa09bD094f0Ee5C0CF1743A", 0.0),
        DexToken("AVAX", "Avalanche", "0x1CE0c2827e2eF14D5C4f29a091d735A204794041", 0.0),
        DexToken("ATOM", "Cosmos", "0x0Eb3a705fc54725037CC9e008bDede697f62F335", 0.0),
        DexToken("UNI", "Uniswap", "0xBf5140A22578168FD562DCcF235E5D43A02ce9B1", 0.0),
        DexToken("FIL", "Filecoin", "0x0D8Ce2A99Bb6e3B7Db580eD848240e4a0F9aE153", 0.0),
        DexToken("INJ", "Injective", "0xa2B726B1145A4773F68593CF171187d8EBe4d495", 0.0),
        DexToken("RUNE", "THORChain", "0xfA9343C3897324496A05fC75abeD6bAC29f8A40f", 0.0),
        DexToken("APE", "ApeCoin", "0xC762043E211571eB34f1ef377e5e8e76914962f9", 0.0),
        DexToken("TWT", "Trust Wallet", "0x4B0F1812e5Df2A09796481Ff14017e6005508003", 0.0),
        DexToken("FTM", "Fantom", "0xAD29AbB318791D579433D831ed122aFeAf29dcfe", 0.0),
        DexToken("SAND", "The Sandbox", "0x67b725d7e342d7B611fa85e859Df9697D9378B2e", 0.0),
        DexToken("MANA", "Decentraland", "0x26433c8127d9b4e9B71Eaa15111DF99Ea2EeB2f8", 0.0),
        DexToken("GALA", "Gala", "0x7dDEE176F665cD201F93eEDE625770E2fD911990", 0.0),
        DexToken("XLM", "Stellar", "0x43C934A845205F0b514417d757d7235B8f71f1B9", 0.0),
        DexToken("EOS", "EOS", "0x56b6fB708fC5733eD2C7CfaF522aBc2D2B3D3C2D", 0.0),
        DexToken("BCH", "Bitcoin Cash", "0x8fF795a6F4D97E7887C79beA79aba5cc76444aDf", 0.0),
        DexToken("ALGO", "Algorand", "0x7B5c6f6bD9B7A410b7c56E5f4D7bE6c43Dd4d2A1", 0.0),
        DexToken("VET", "VeChain", "0x6FDcdfef7c496407cCb0cEC90f9C5Aaa1Cc8D888", 0.0),
        DexToken("NEAR", "NEAR Protocol", "0x1Fa4a73a3F0133f0025378af00236f3aBDEE5D63", 0.0),
        DexToken("EGLD", "MultiversX", "0xbF7c81FFF98BbE61B40Ed186e4AfD6DDd01337fe", 0.0),
        DexToken("AXS", "Axie Infinity", "0x715D400F88C167884bbCc41C5FeA407ed4D2f8A0", 0.0),
        DexToken("HOT", "Holo", "0xC1357d32F19aB3FB0ce82d7865fDa68e153Ae3D9", 0.0),
        DexToken("ZIL", "Zilliqa", "0xb86AbCb37C3A4B64f74f59301AFF131a1BEcC787", 0.0),
        DexToken("1INCH", "1inch", "0x111111111117dC0aa78b770fA6A738034120C302", 0.0),
        DexToken("COMP", "Compound", "0x52CE071Bd9b1C4B00A0b92D298c512478CaD67e8", 0.0)
    )
}

object TokenDecimals {
    private val map = mapOf(
        "USDT" to 18, "BUSD" to 18, "USDC" to 18,
        "BTC" to 8, "DOGE" to 8, 
        "SHIB" to 18, "ETH" to 18, "BNB" to 18, "CAKE" to 18, "XRP" to 18, "ADA" to 18, "TRX" to 6
    )
    fun get(symbol: String): Int = map[symbol.uppercase()] ?: 18
}
