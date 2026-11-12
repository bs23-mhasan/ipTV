package com.mhasancse15.iptv.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

const val APP_INFO_URL =
    "https://raw.githubusercontent.com/bs23-mhasan/ipTV/master/tv/appinfo.json"

class AppUpdateRepository(private val httpClient: OkHttpClient) {
    suspend fun fetchLatest(): AppUpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(APP_INFO_URL).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Update check failed (${response.code})")
            }
            val content = response.body?.string()?.takeIf(String::isNotBlank)
                ?: throw IOException("Update manifest was empty")
            AppUpdateInfo.parse(content)
        }
    }
}
