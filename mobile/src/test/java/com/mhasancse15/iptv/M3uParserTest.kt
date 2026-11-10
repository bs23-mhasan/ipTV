package com.mhasancse15.iptv

import com.mhasancse15.iptv.data.parser.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesChannelMetadataAndCommaInQuotedAttribute() {
        val channels = M3uParser.parse(
            """
                #EXTM3U
                #EXTINF:-1 tvg-id="news.bd" tvg-name="News, HD" tvg-logo="https://example.com/logo.png" tvg-country="Bangladesh" group-title="News, Live",News Channel
                https://example.com/live.m3u8
            """.trimIndent()
        )

        assertEquals(1, channels.size)
        val channel = channels.single()
        assertEquals("news.bd", channel.id)
        assertEquals("News Channel", channel.name)
        assertEquals("News, Live", channel.category)
        assertEquals("Bangladesh", channel.country)
        assertEquals("News, HD", channel.tvgName)
        assertEquals("https://example.com/live.m3u8", channel.streamUrl)
    }

    @Test
    fun fallbackIdentifierIsStableAndMetadataIsNotInvented() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Channel One
            https://example.com/one
        """.trimIndent()

        val first = M3uParser.parse(playlist).single()
        val second = M3uParser.parse(playlist).single()

        assertEquals(first.id, second.id)
        assertTrue(first.category.isEmpty())
        assertTrue(first.country.isEmpty())
    }

    @Test
    fun skipsEntriesWithoutValidHttpStreamUrls() {
        val channels = M3uParser.parse(
            """
                #EXTM3U
                #EXTINF:-1,Invalid
                not-a-url
                #EXTINF:-1,Valid
                https://example.com/valid
            """.trimIndent()
        )

        assertEquals(1, channels.size)
        assertEquals("Valid", channels.single().name)
        assertFalse(channels.single().streamUrl.isBlank())
    }
}
