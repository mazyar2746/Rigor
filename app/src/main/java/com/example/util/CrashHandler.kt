package com.example.util

import android.content.Context
import android.util.Log

object CrashHandler {
    private const val TAG = "RigorCrashHandler"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "App crashed! Clearing cache to prevent future crashes.", throwable)
            try {
                // پاک کردن کش برای جلوگیری از کرش مجدد
                context.cacheDir.deleteRecursively()
                // پاک کردن فایل‌های کش داخلی که با نام cache_ شروع می‌شوند
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("cache_") || file.name.endsWith(".json")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
            }
            // ارجاع خطا به هندلر پیش‌فرض اندروید تا برنامه بسته شود
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
