package com.mhasancse15.iptv.data.parser

import com.mhasancse15.iptv.domain.model.EpgChannel
import com.mhasancse15.iptv.domain.model.EpgProgramme
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object XmltvParser {

    fun parse(xmlContent: String): Pair<List<EpgChannel>, List<EpgProgramme>> {
        val channels = mutableListOf<EpgChannel>()
        val programmes = mutableListOf<EpgProgramme>()
        val parser = KXmlParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(xmlContent.reader())
        }

        var channelId = ""
        var channelName = ""
        var channelIcon = ""
        var currentProgramme: EpgProgramme? = null
        var title = ""
        var description = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "channel" -> {
                        channelId = parser.getAttributeValue(null, "id").orEmpty()
                        channelName = parser.getAttributeValue(null, "display-name").orEmpty()
                        channelIcon = parser.getAttributeValue(null, "icon").orEmpty()
                    }
                    "programme" -> {
                        currentProgramme = EpgProgramme(
                            channelId = parser.getAttributeValue(null, "channel").orEmpty(),
                            start = parseXmltvTime(parser.getAttributeValue(null, "start").orEmpty()),
                            stop = parseXmltvTime(parser.getAttributeValue(null, "stop").orEmpty()),
                            title = "",
                            description = ""
                        )
                        title = ""
                        description = ""
                    }
                    "display-name" -> if (channelId.isNotEmpty()) {
                        channelName = parser.nextText().trim()
                    }
                    "icon" -> if (channelId.isNotEmpty()) {
                        channelIcon = parser.getAttributeValue(null, "src").orEmpty()
                    }
                    "title" -> if (currentProgramme != null) title = parser.nextText().trim()
                    "desc" -> if (currentProgramme != null) description = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "channel" -> {
                        if (channelId.isNotBlank()) channels += EpgChannel(channelId, channelName, channelIcon)
                        channelId = ""
                        channelName = ""
                        channelIcon = ""
                    }
                    "programme" -> {
                        currentProgramme?.let {
                            if (it.start > 0L && it.stop > it.start && title.isNotBlank()) {
                                programmes += it.copy(title = title, description = description)
                            }
                        }
                        currentProgramme = null
                    }
                }
            }
            eventType = parser.next()
        }
        return channels to programmes
    }

    private fun parseXmltvTime(value: String): Long {
        val normalized = value.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return 0L
        val format = when {
            Regex("""^\d{14}\s+[+-]\d{4}$""").matches(normalized) -> "yyyyMMddHHmmss Z"
            Regex("""^\d{12}\s+[+-]\d{4}$""").matches(normalized) -> "yyyyMMddHHmm Z"
            Regex("""^\d{14}$""").matches(normalized) -> "yyyyMMddHHmmss"
            else -> return 0L
        }
        return try {
            SimpleDateFormat(format, Locale.ROOT).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalized)?.time ?: 0L
        } catch (_: IllegalArgumentException) {
            0L
        } catch (_: java.text.ParseException) {
            0L
        }
    }
}
