package com.mhasancse15.iptv.data.parser

import com.mhasancse15.iptv.domain.model.Channel
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

object M3uParser {

    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(m3uContent))
        var line: String? = reader.readLine()

        var tvgId = ""
        var tvgName = ""
        var logoUrl = ""
        var groupTitle = "General"
        var channelName = ""

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse attributes
                tvgId = extractAttribute(trimmed, "tvg-id")
                tvgName = extractAttribute(trimmed, "tvg-name")
                logoUrl = extractAttribute(trimmed, "tvg-logo")
                groupTitle = extractAttribute(trimmed, "group-title").ifEmpty { "General" }

                // Extract display name (after the last comma)
                val commaIndex = trimmed.lastIndexOf(',')
                if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    channelName = trimmed.substring(commaIndex + 1).trim()
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This is the stream URL
                val streamUrl = trimmed
                if (channelName.isEmpty()) {
                    channelName = "Channel ${channels.size + 1}"
                }

                val stableId = tvgId.ifEmpty {
                    // Fallback to normalized name + URL hash if no tvg-id
                    if (channelName.isNotEmpty()) channelName.lowercase().replace("\\s+".toRegex(), "_")
                    else UUID.nameUUIDFromBytes(streamUrl.toByteArray()).toString()
                }

                // Determine country based on group or name if possible, default to Bangladesh
                val country = determineCountry(groupTitle, channelName)

                channels.add(
                    Channel(
                        id = stableId,
                        name = channelName,
                        streamUrl = streamUrl,
                        logoUrl = logoUrl,
                        category = groupTitle,
                        country = country,
                        tvgId = tvgId,
                        tvgName = tvgName.ifEmpty { channelName }
                    )
                )

                // Reset for next channel
                tvgId = ""
                tvgName = ""
                logoUrl = ""
                groupTitle = "General"
                channelName = ""
            }
            line = reader.readLine()
        }
        return channels
    }

    private fun extractAttribute(extinf: String, attr: String): String {
        val pattern = "$attr=\"([^\"]*)\"".toRegex()
        val match = pattern.find(extinf)
        if (match != null) {
            return match.groupValues[1]
        }
        // Try without quotes
        val patternNoQuotes = "$attr=([^\\s,]*)".toRegex()
        val matchNoQuotes = patternNoQuotes.find(extinf)
        return matchNoQuotes?.groupValues?.get(1) ?: ""
    }

    private fun determineCountry(group: String, name: String): String {
        val lowerGroup = group.lowercase()
        val lowerName = name.lowercase()
        return when {
            lowerGroup.contains("india") || lowerName.contains("india") || lowerGroup.contains("hindi") || lowerGroup.contains("bengali india") -> "India"
            lowerGroup.contains("bd") || lowerGroup.contains("bangladesh") || lowerName.contains("bd") -> "Bangladesh"
            lowerGroup.contains("sports") || lowerGroup.contains("news") || lowerGroup.contains("movies") -> "Bangladesh" // Default major group to BD for BDIX
            else -> "International"
        }
    }
}
