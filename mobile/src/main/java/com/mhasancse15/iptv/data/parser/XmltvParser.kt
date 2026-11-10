package com.mhasancse15.iptv.data.parser

import com.mhasancse15.iptv.domain.model.EpgProgramme
import com.mhasancse15.iptv.domain.model.EpgChannel
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream

object XmltvParser {

    fun parse(xmlContent: String): Pair<List<EpgChannel>, List<EpgProgramme>> {
        val channels = mutableListOf<EpgChannel>()
        val programmes = mutableListOf<EpgProgramme>()

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")

        var eventType = parser.eventType
        var currentChannelId = ""
        var currentProgramme: EpgProgramme? = null
        var currentTitle: String = ""
        var currentDesc: String = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "channel" -> {
                            val id = parser.getAttributeValue(null, "id") ?: ""
                            val displayName = parser.getAttributeValue(null, "display-name") ?: ""
                            val icon = parser.getAttributeValue(null, "icon") ?: ""
                            channels.add(EpgChannel(id, displayName, icon))
                            currentChannelId = id
                        }
                        "programme" -> {
                            val id = parser.getAttributeValue(null, "channel") ?: ""
                            val startStr = parser.getAttributeValue(null, "start") ?: ""
                            val stopStr = parser.getAttributeValue(null, "stop") ?: ""
                            val start = parseXmltvTime(startStr)
                            val stop = parseXmltvTime(stopStr)
                            currentProgramme = EpgProgramme(
                                channelId = id,
                                start = start,
                                stop = stop,
                                title = "",
                                description = ""
                            )
                            currentTitle = ""
                            currentDesc = ""
                        }
                        "title" -> {
                            if (currentProgramme != null) {
                                currentTitle = parser.nextText()
                            }
                        }
                        "desc" -> {
                            if (currentProgramme != null) {
                                currentDesc = parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            if (currentProgramme != null) {
                                programmes.add(
                                    currentProgramme!!.copy(
                                        title = currentTitle,
                                        description = currentDesc
                                    )
                                )
                                currentProgramme = null
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return channels to programmes
    }

    private fun parseXmltvTime(timeStr: String): Long {
        if (timeStr.isEmpty()) return 0L
        try {
            // XMLTV format: 20250101120000 +0000
            val year = timeStr.substring(0, 4).toInt()
            val month = timeStr.substring(4, 6).toInt()
            val day = timeStr.substring(6, 8).toInt()
            val hour = timeStr.substring(8, 10).toInt()
            val minute = timeStr.substring(10, 12).toInt()
            val second = if (timeStr.length >= 14) timeStr.substring(12, 14).toInt() else 0

            val tzStr = if (timeStr.length >= 18) timeStr.substring(14) else "+0000"
            val tzHours = if (tzStr.length >= 3) {
                val sign = if (tzStr[0] == '-') -1 else 1
                val h = tzStr.substring(1, 3).toInt()
                val m = if (tzStr.length >= 5) tzStr.substring(3, 5).toInt() else 0
                sign * (h * 60 + m)
            } else 0

            // Convert to epoch millis
            val calendar = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC"))
            calendar.set(year, month - 1, day, hour, minute, second)
            val utcMillis = calendar.timeInMillis

            // Apply timezone offset
            return utcMillis - tzHours * 60 * 1000
        } catch (e: Exception) {
            return 0L
        }
    }
}