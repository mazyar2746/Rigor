package com.example.ui.components

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    // آدرس فایل version.json روی گیت‌هاب
    private const val VERSION_URL = "https://raw.githubusercontent.com/mazyar2746/Rigor/main/version.json"
    
    // نسخه فعلی برنامه (باید با build.gradle.kts همخوانی داشته باشد)
    private const val CURRENT_VERSION = "1.1.4"

    fun checkForUpdates(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(VERSION_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestVersion = json.getString("version")
                    val downloadUrl = json.getString("download_url")

                    if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                        showUpdateDialog(context, latestVersion, downloadUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // خطا در چک کردن آپدیت، برنامه کرش نکنه
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun showUpdateDialog(context: Context, latestVersion: String, downloadUrl: String) {
        CoroutineScope(Dispatchers.Main).launch {
            AlertDialog.Builder(context)
                .setTitle("نسخه جدید موجود است! 🚀")
                .setMessage("نسخه $latestVersion برنامه ریگور منتشر شد. لطفاً برای دریافت امکانات جدید، برنامه را آپدیت کنید.")
                .setPositiveButton("دانلود نسخه جدید") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    context.startActivity(intent)
                }
                .setNegativeButton("بعداً", null)
                .setCancelable(false)
                .show()
        }
    }
}
