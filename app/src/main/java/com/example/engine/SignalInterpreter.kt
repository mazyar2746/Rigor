package com.example.engine

import com.example.data.model.SignalType
import com.example.data.model.TechnicalAnalysisResult
import com.example.i18n.Language
import com.example.i18n.StringsDictionary

object SignalInterpreter {

    fun getFinalSignal(result: TechnicalAnalysisResult): SignalType {
        if (result.rsiSignal == SignalType.BUY && result.macdSignal == SignalType.BUY) return SignalType.STRONG_BUY
        if (result.rsiSignal == SignalType.SELL && result.macdSignal == SignalType.SELL) return SignalType.STRONG_SELL
        if (result.rsiSignal == SignalType.BUY || result.macdSignal == SignalType.BUY) return SignalType.BUY
        if (result.rsiSignal == SignalType.SELL || result.macdSignal == SignalType.SELL) return SignalType.SELL
        return SignalType.NEUTRAL
    }

    fun getSignalLabel(signal: SignalType, lang: Language): String {
        val key = when (signal) {
            SignalType.STRONG_BUY -> "iran_stock_strong_buy"
            SignalType.BUY -> "iran_stock_buy"
            SignalType.NEUTRAL -> "iran_stock_neutral"
            SignalType.SELL -> "iran_stock_sell"
            SignalType.STRONG_SELL -> "iran_stock_strong_sell"
        }
        return StringsDictionary.get(key, lang)
    }

    fun getReasoningText(result: TechnicalAnalysisResult, lang: Language): String {
        val isFa = lang == Language.PERSIAN // یا Language.FARSI، بسته به تعریف شما
        val reasons = mutableListOf<String>()

        if (isFa) {
            when {
                result.rsi < 30 -> reasons.add("شاخص قدرت نسبی (RSI) در ناحیه اشباع فروش (زیر ۳۰) قرار دارد که نشان‌دهنده احتمال بالای برگشت قیمت است.")
                result.rsi > 70 -> reasons.add("شاخص قدرت نسبی (RSI) در ناحیه اشباع خرید (بالای ۷۰) است و احتمال اصلاح قیمت وجود دارد.")
                else -> reasons.add("شاخص RSI در محدوده خنثی قرار دارد و فشار خرید یا فروش خاصی دیده نمی‌شود.")
            }
            if (result.macdHistogram > 0) reasons.add("هیستوگرام MACD مثبت است، یعنی مومنتوم (شتاب) صعودی در بازار حاکم است.")
            else reasons.add("هیستوگرام MACD منفی است و فشار فروش در کوتاه‌مدت وجود دارد.")

            val price = result.currentPrice
            if (price <= result.bbLower) reasons.add("قیمت به باند پایین بولینگر برخورد کرده است که معمولاً یک سطح حمایتی قوی محسوب می‌شود.")
            else if (price >= result.bbUpper) reasons.add("قیمت به باند بالای بولینگر رسیده است که نشانه احتمال اشباع خرید و اصلاح است.")

            val conclusion = when (getFinalSignal(result)) {
                SignalType.STRONG_BUY -> "با توجه به هم‌جهتی سیگنال‌ها، این نماد در وضعیت بسیار مناسبی برای خرید قرار دارد."
                SignalType.BUY -> "شرایط کلی برای خرید مساعد است، ولی بهتر است با حجم کمتر وارد شوید."
                SignalType.STRONG_SELL -> "با توجه به فشار فروش و سیگنال‌های نزولی، فروش یا عدم خرید توصیه می‌شود."
                SignalType.SELL -> "احتمال نزول قیمت بیشتر است، توصیه به احتیاط در خرید می‌شود."
                SignalType.NEUTRAL -> "سیگنال‌ها هم‌جهت نیستند. بهتر است تا مشخص شدن روند، صبر کنید."
            }
            return reasons.joinToString("\n\n") + "\n\n📌 نتیجه‌گیری نهایی: " + conclusion
        } else {
            // انگلیسی
            when {
                result.rsi < 30 -> reasons.add("RSI is in the oversold zone (below 30), indicating a high probability of a price bounce.")
                result.rsi > 70 -> reasons.add("RSI is in the overbought zone (above 70), suggesting a potential price correction.")
                else -> reasons.add("RSI is in the neutral zone, with no strong buying or selling pressure.")
            }
            if (result.macdHistogram > 0) reasons.add("MACD histogram is positive, indicating bullish momentum.")
            else reasons.add("MACD histogram is negative, indicating short-term selling pressure.")

            val price = result.currentPrice
            if (price <= result.bbLower) reasons.add("Price has touched the lower Bollinger Band, a strong support level.")
            else if (price >= result.bbUpper) reasons.add("Price has reached the upper Bollinger Band, signaling overbought conditions.")

            val conclusion = when (getFinalSignal(result)) {
                SignalType.STRONG_BUY -> "Given the aligned signals, this symbol is in a strong buying position."
                SignalType.BUY -> "Overall conditions favor buying, but consider a smaller position size."
                SignalType.STRONG_SELL -> "With strong bearish signals, selling or avoiding buying is recommended."
                SignalType.SELL -> "Further downside is likely. Exercise caution."
                SignalType.NEUTRAL -> "Signals are mixed. Wait for a clearer trend."
            }
            return reasons.joinToString("\n\n") + "\n\n📌 Final Conclusion: " + conclusion
        }
    }
}
