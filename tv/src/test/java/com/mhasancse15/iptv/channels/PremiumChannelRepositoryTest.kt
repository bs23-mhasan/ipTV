package com.mhasancse15.iptv.channels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumChannelRepositoryTest {
    @Test
    fun parsesChannelFieldsAndPrefixesIds() {
        val channels = PremiumChannelRepository.parseChannels(
            """{"total":1,"channels":[{"name":"Channel A","id":"a.bd","country":"Bangladesh","url":"https://example.com/a.m3u8","logo":"https://example.com/a.png","group":"News"}]}"""
        )

        assertEquals(
            PremiumChannel(
                id = "premium:a.bd",
                name = "Channel A",
                url = "https://example.com/a.m3u8",
                logo = "https://example.com/a.png",
                category = "News"
            ),
            channels.single()
        )
    }

    @Test
    fun skipsEntriesWithoutValidNameOrHttpStreamUrl() {
        val channels = PremiumChannelRepository.parseChannels(
            """{"channels":[{"name":"No URL"},{"name":"Bad URL","url":"file:///channel"},{"name":"Valid","url":"https://example.com/live"}]}"""
        )

        assertEquals(1, channels.size)
        assertEquals("Valid", channels.single().name)
    }

    @Test
    fun parsesResolutionFromNumberOrQualityLabel() {
        val channels = PremiumChannelRepository.parseChannels(
            """{"channels":[{"name":"Movie HD","url":"https://example.com/a","resolution":1080},{"name":"Bangla HD","url":"https://example.com/b","resolution":"720p"}]}"""
        )

        assertEquals(listOf(1080, 720), channels.map { it.resolution })
    }

    @Test
    fun rejectsMissingChannelsArray() {
        val error = runCatching { PremiumChannelRepository.parseChannels("""{"total":0}""") }
            .exceptionOrNull()

        assertTrue(error is java.io.IOException)
    }
}
