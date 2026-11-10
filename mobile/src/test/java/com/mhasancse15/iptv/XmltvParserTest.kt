package com.mhasancse15.iptv

import com.mhasancse15.iptv.data.parser.XmltvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvParserTest {
    @Test
    fun parsesChannelAndProgrammeWithTimezoneOffset() {
        val (channels, programmes) = XmltvParser.parse(
            """
                <tv>
                  <channel id="news.bd">
                    <display-name>BD News</display-name>
                    <icon src="https://example.com/news.png"/>
                  </channel>
                  <programme start="20250202090000 +0600" stop="20250202100000 +0600" channel="news.bd">
                    <title>Morning News</title>
                    <desc>Daily headlines</desc>
                  </programme>
                </tv>
            """.trimIndent()
        )

        assertEquals(1, channels.size)
        assertEquals("BD News", channels.single().displayName)
        assertEquals("https://example.com/news.png", channels.single().icon)
        assertEquals(1, programmes.size)
        assertEquals("Morning News", programmes.single().title)
        assertEquals("Daily headlines", programmes.single().description)
        assertEquals(60 * 60 * 1000L, programmes.single().stop - programmes.single().start)
        assertEquals(3L, programmes.single().start.let { it / (60 * 60 * 1000L) % 24 })
    }

    @Test
    fun ignoresProgrammeEntriesWithMalformedTimes() {
        val (_, programmes) = XmltvParser.parse(
            """<tv><programme start="bad" stop="also-bad" channel="x"><title>Bad</title></programme></tv>"""
        )
        assertTrue(programmes.isEmpty())
    }
}
