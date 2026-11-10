package com.mhasancse15.iptv.data.parser

import com.mhasancse15.iptv.domain.model.Channel
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

object M3uParser {

    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var tvgId = ""
        var tvgName = ""
        var logoUrl = ""
        var category = ""
        var country = ""
        var channelName = ""

        BufferedReader(StringReader(m3uContent)).useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    tvgId = extractAttribute(line, "tvg-id")
                    tvgName = extractAttribute(line, "tvg-name")
                    logoUrl = extractAttribute(line, "tvg-logo")
                    category = extractAttribute(line, "group-title")
                    country = extractAttribute(line, "tvg-country")
                        .ifEmpty { extractAttribute(line, "country") }
                    val commaIndex = displayNameSeparator(line)
                    channelName = if (commaIndex >= 0) line.substring(commaIndex + 1).trim() else ""
                } else if (line.isNotEmpty() && !line.startsWith("#")) {
                    val streamUrl = line
                    if (channelName.isNotBlank() && isSupportedUrl(streamUrl)) {
                        val stableId = tvgId.ifBlank {
                            UUID.nameUUIDFromBytes("$channelName\n$streamUrl".toByteArray(Charsets.UTF_8)).toString()
                        }
                        channels += Channel(
                            id = stableId,
                            name = channelName,
                            streamUrl = streamUrl,
                            logoUrl = logoUrl,
                            category = category,
                            country = country,
                            tvgId = tvgId,
                            tvgName = tvgName
                        )
                    }
                    tvgId = ""
                    tvgName = ""
                    logoUrl = ""
                    category = ""
                    country = ""
                    channelName = ""
                }
            }
        }
        return channels.distinctBy { it.id }
    }

    private fun displayNameSeparator(line: String): Int {
        var insideQuotes = false
        line.forEachIndexed { index, char ->
            when (char) {
                '"' -> insideQuotes = !insideQuotes
                ',' -> if (!insideQuotes) return index
            }
        }
        return -1
    }

    private fun extractAttribute(extinf: String, attribute: String): String {
        val pattern = Regex("""(?:^|\s)$attribute\s*=\s*(?:"([^"]*)"|([^,\s]*))""", RegexOption.IGNORE_CASE)
        val match = pattern.find(extinf) ?: return ""
        return match.groups[1]?.value ?: match.groups[2]?.value.orEmpty()
    }

    private fun isSupportedUrl(value: String): Boolean =
        try {
            val uri = java.net.URI(value)
            uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
        } catch (_: java.net.URISyntaxException) {
            false
        }
}
