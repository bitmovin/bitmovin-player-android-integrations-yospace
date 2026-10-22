package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

// Live DASH reports wall-clock time in seconds since the Unix epoch.
private const val LIVE_WALL_CLOCK_TIME = 1_790_862_365.83

private fun fakePlayer(currentTime: Double, playbackTimeOffsetToRelativeTime: Double = 0.0): Player =
    Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
        when (method.name) {
            "getCurrentTime" -> currentTime
            "getPlaybackTimeOffsetToRelativeTime" -> playbackTimeOffsetToRelativeTime
            else -> throw UnsupportedOperationException(method.name)
        }
    } as Player

class YospacePlayheadTest {

    @Test
    fun `live wall-clock time does not clamp to Int max value`() {
        assertEquals(1_790_862_365_830L, fakePlayer(LIVE_WALL_CLOCK_TIME).yospacePlayheadMs())
    }

    @Test
    fun `offset to relative time is applied`() {
        val player = fakePlayer(LIVE_WALL_CLOCK_TIME, playbackTimeOffsetToRelativeTime = -1_790_862_300.0)

        assertEquals(65_830L, player.yospacePlayheadMs())
    }

    @Test
    fun `negative playhead is clamped to zero`() {
        assertEquals(0L, fakePlayer(1.0, playbackTimeOffsetToRelativeTime = -2.0).yospacePlayheadMs())
    }

    @Test
    fun `live timed metadata keeps the time between metadata events`() {
        val start = fakePlayer(LIVE_WALL_CLOCK_TIME).createYospaceTimedMetadata("758692483", "3:8", "S", "0.00")
        val mid = fakePlayer(LIVE_WALL_CLOCK_TIME + 2.0).createYospaceTimedMetadata("758692483", "3:8", "M", "2.00")

        assertNotNull(start)
        assertNotNull(mid)
        assertEquals(1_790_862_365_830L, start!!.playhead)
        assertEquals(2_000L, mid!!.playhead - start.playhead)
    }

    @Test
    fun `timed metadata is not created when a field is missing`() {
        assertNull(fakePlayer(LIVE_WALL_CLOCK_TIME).createYospaceTimedMetadata("758692483", "3:8", "S", null))
    }
}
