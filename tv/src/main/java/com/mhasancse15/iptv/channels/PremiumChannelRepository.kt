package com.mhasancse15.iptv.channels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.UUID

data class PremiumChannel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String,
    val category: String,
    val resolution: Int? = null
)

const val PREMIUM_CHANNELS_URL =
    "https://raw.githubusercontent.com/bs23-mhasan/ipTV/master/bangla_channels.json"

class PremiumChannelRepository(private val httpClient: OkHttpClient) {
    suspend fun fetchChannelsJson(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(PREMIUM_CHANNELS_URL).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Premium channel request failed (${response.code})")
            }
            response.body?.string()?.takeIf(String::isNotBlank)
                ?: throw IOException("Premium channel list was empty")
        }
    }

    companion object {
        fun parseChannels(json: String): List<PremiumChannel> {
            val root = try {
                Json.parseToJsonElement(json) as? JsonObject
                    ?: throw IOException("Premium channel JSON must be an object")
            } catch (error: SerializationException) {
                throw IOException("Premium channel JSON is invalid", error)
            }
            val channels = root["channels"] as? JsonArray
                ?: throw IOException("Premium channel JSON is missing its channels list")
            return channels.mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                fun string(name: String): String =
                    (item[name] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
                val name = string("name").trim()
                val url = string("url").trim()
                if (name.isBlank() || !url.isHttpUrl()) return@mapNotNull null

                val sourceId = string("id").trim()
                val id = "premium:" + sourceId.ifBlank {
                    UUID.nameUUIDFromBytes("$name\n$url".toByteArray(Charsets.UTF_8)).toString()
                }
                val resolutionValue = (item["resolution"] as? JsonPrimitive)?.content
                val resolution = resolutionValue?.toIntOrNull()
                    ?: resolutionValue?.let {
                        Regex("""(?<!\d)(\d{3,4})\s*p\b""", RegexOption.IGNORE_CASE)
                            .find(it)?.groupValues?.get(1)?.toIntOrNull()
                    }
                PremiumChannel(
                    id = id,
                    name = name,
                    url = url,
                    logo = string("logo"),
                    category = string("group"),
                    resolution = resolution
                )
            }.distinctBy { it.id }
        }

        private fun String.isHttpUrl(): Boolean {
            val uri = runCatching { URI(this) }.getOrNull()
            return (uri?.scheme.equals("https", ignoreCase = true) ||
                uri?.scheme.equals("http", ignoreCase = true)) &&
                !uri?.host.isNullOrBlank()
        }
    }
}
