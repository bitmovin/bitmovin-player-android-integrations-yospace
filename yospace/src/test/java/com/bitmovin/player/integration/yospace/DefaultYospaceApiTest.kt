package com.bitmovin.player.integration.yospace

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultYospaceApiTest {
    private val emitter = YospaceEventEmitter()
    private val api = DefaultYospaceApi(emitter)

    private fun adBreak() = AdBreak(
        id = "ad-break",
        absoluteStart = 1.0,
        relativeStart = 1.0,
        duration = 1.0,
        absoluteEnd = 2.0
    )

    @Test
    fun `java off removes registered listener`() {
        var adBreakStartCount = 0
        val listener = YospacePlayerEventListener<YospacePlayerEvent.AdBreakStarted> {
            adBreakStartCount += 1
        }

        api.on(YospacePlayerEvent.AdBreakStarted::class.java, listener)
        api.off(YospacePlayerEvent.AdBreakStarted::class.java, listener)
        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))

        assertEquals(0, adBreakStartCount)
    }

    @Test
    fun `java off removes pending next listener`() {
        var adBreakStartCount = 0
        val listener = YospacePlayerEventListener<YospacePlayerEvent.AdBreakStarted> {
            adBreakStartCount += 1
        }

        api.next(YospacePlayerEvent.AdBreakStarted::class.java, listener)
        api.off(YospacePlayerEvent.AdBreakStarted::class.java, listener)
        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))

        assertEquals(0, adBreakStartCount)
    }

    @Test
    fun `java next emits once`() {
        var adBreakStartCount = 0

        api.next(YospacePlayerEvent.AdBreakStarted::class.java) {
            adBreakStartCount += 1
        }

        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))
        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))

        assertEquals(1, adBreakStartCount)
    }
}
