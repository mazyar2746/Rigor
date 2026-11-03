package com.example.engine

import com.example.data.model.SignalType
import com.example.data.model.TechnicalAnalysisResult

/**
 * این کلاس خروجی خام موتور تحلیل را به یک تحلیل منطقی، فارسی و قابل فهم برای کاربر تبدیل می‌کند.
 */
object SignalInterpreter {

    fun getFinalSignal(result: TechnicalAnalysisResult): SignalType {
        // ۱. اگر RSI و MACD هم‌جهت باشند، سیگنال قوی می‌شود.
        if (result.rsiSignal == SignalType.BUY && result.macdSignal == SignalType.BUY) {
            return SignalType.STRONG_BUY
        }
        if (result.rsiSignal == SignalType.SELL && result.macdSignal == SignalType.SELL) {
            return SignalType.STRONG_SELL
        }
        // ۲. اگر یکی خرید بده و دیگری خنثی باشد، سیگنال خرید معمولی می‌شود.
        if (result.rsiSignal == SignalType.BUY || result.macdSignal == SignalType.BUY) {
            return SignalType.BUY
        }
        if (result.rsiSignal == SignalType.SELL || result.macdSignal == SignalType.SELL) {
            return SignalType.SELL
        }
        // ۳. در غیر این صورت خنثی است.
        return SignalType.NEUTRAL
    }

    fun getReasoningText(result: TechnicalAnalysisResult): String {
        val reasons = mutableListOf<String>()

        // توضیح RSI
        when {
            result.rsi < 30 -> reasons.add("شاخص قدرت نسبی (RSI) در ناحیه اشباع فروش (زیر ۳۰) قرار دارد که نشان‌دهنده احتمال بالای برگشت قیمت است.")
            result.rsi > 70 -> reasons.add("شاخص قدرت نسبی (RSI) در ناحیه اشباع خرید (بالای ۷۰) است و احتمال اصلاح قیمت وجود دارد.")
            else -> reasons.add("شاخص RSI در محدوده خنثی قرار دارد و فشار خرید یا فروش خاصی دیده نمی‌شود.")
        }

        // توضیح MACD
        if (result.macdHistogram > 0) {
            reasons.add("هیستوگرام MACD مثبت است، یعنی مومنتوم (شتاب) صعودی در بازار حاکم است.")
        } else {
            reasons.add("هیستوگرام MACD منفی است و فشار فروش در کوتاه‌مدت وجود دارد.")
        }

        // توضیح بولینگر (اگر قیمت به باندها نزدیک باشد)
        val price = result.currentPrice
        if (price <= result.bbLower) {
            reasons.add("قیمت به باند پایین بولینگر برخورد کرده است که معمولاً یک سطح حمایتی قوی محسوب می‌شود.")
        } else if (price >= result.bbUpper) {
            reasons.add("قیمت به باند بالای بولینگر رسیده است که نشانه احتمال اشباع خرید و اصلاح است.")
        }

        // جمع‌بندی
        val finalSignal = getFinalSignal(result)
        val conclusion = when (finalSignal) {
            SignalType.STRONG_BUY -> "با توجه به هم‌جهتی سیگنال‌ها، این نماد در وضعیت بسیار مناسبی برای خرید قرار دارد."
            SignalType.BUY -> "شرایط کلی برای خرید مساعد است، ولی بهتر است با حجم کمتر وارد شوید."
            SignalType.STRONG_SELL -> "با توجه به فشار فروش و سیگنال‌های نزولی، فروش یا عدم خرید توصیه می‌شود."
            SignalType.SELL -> "احتمال نزول قیمت بیشتر است، توصیه به احتیاط در خرید می‌شود."
            SignalType.NEUTRAL -> "سیگنال‌ها هم‌جهت نیستند. بهتر است تا مشخص شدن روند، صبر کنید."
        }

        return reasons.joinToString("\n\n") + "\n\n📌 **نتیجه‌گیری نهایی:** " + conclusion
    }
}
