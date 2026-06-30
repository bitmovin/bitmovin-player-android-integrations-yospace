package com.bitmovin.player.integration.yospace

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class YospaceEventEmitterTest {
    private val emitter = YospaceEventEmitter()

    private fun adBreak() = AdBreak(
        id = "ad-break",
        absoluteStart = 1.0,
        relativeStart = 1.0,
        duration = 1.0,
        absoluteEnd = 2.0
    )

    @Test
    fun `on emits Yospace player events`() {
        var adBreakStartCount = 0

        emitter.on(YospacePlayerEvent.AdBreakStarted::class) {
            adBreakStartCount += 1
        }

        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))

        assertEquals(1, adBreakStartCount)
    }

    @Test
    fun `on delivers the integration ad break payload`() {
        val adBreak = adBreak()
        var emittedAdBreak: AdBreak? = null

        emitter.on(YospacePlayerEvent.AdBreakStarted::class) {
            emittedAdBreak = it.adBreak
        }

        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak))

        assertSame(adBreak, emittedAdBreak)
    }

    @Test
    fun `on only delivers events of the subscribed type`() {
        var adBreakStartCount = 0

        emitter.on(YospacePlayerEvent.AdBreakStarted::class) {
            adBreakStartCount += 1
        }

        emitter.emit(YospacePlayerEvent.AdBreakFinished(adBreak()))

        assertEquals(0, adBreakStartCount)
    }

    @Test
    fun `off removes a Yospace player event listener`() {
        var adBreakStartCount = 0
        val listener: (YospacePlayerEvent.AdBreakStarted) -> Unit = {
            adBreakStartCount += 1
        }

        emitter.on(YospacePlayerEvent.AdBreakStarted::class, listener)
        emitter.off(YospacePlayerEvent.AdBreakStarted::class, listener)
        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))

        assertEquals(0, adBreakStartCount)
    }

    @Test
    fun `off without an event class removes the listener from all events`() {
        var count = 0
        val listener: (YospacePlayerEvent) -> Unit = {
            count += 1
        }

        emitter.on(YospacePlayerEvent.AdBreakStarted::class, listener)
        emitter.on(YospacePlayerEvent.AdBreakFinished::class, listener)
        emitter.off(listener)
        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))
        emitter.emit(YospacePlayerEvent.AdBreakFinished(adBreak()))

        assertEquals(0, count)
    }

    @Test
    fun `next emits a Yospace player event once`() {
        var adBreakStartCount = 0

        emitter.next(YospacePlayerEvent.AdBreakStarted::class) {
            adBreakStartCount += 1
        }

        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))
        emitter.emit(YospacePlayerEvent.AdBreakStarted(adBreak()))

        assertEquals(1, adBreakStartCount)
    }

    @Test
    fun `emit delivers ad started payload`() {
        val ad = Ad(
            id = "ad",
            creativeId = null,
            sequence = 0,
            absoluteStart = 0.0,
            relativeStart = 0.0,
            duration = 5.0,
            absoluteEnd = 5.0,
            system = null,
            title = null,
            advertiser = null,
            hasInteractiveUnit = false,
            isFiller = false,
            lineage = null,
            extensions = emptyList(),
            isLinear = true
        )
        var emittedAd: com.bitmovin.player.api.advertising.Ad? = null

        emitter.on(YospacePlayerEvent.AdStarted::class) {
            emittedAd = it.ad
        }

        emitter.emit(YospacePlayerEvent.AdStarted(ad = ad))

        assertSame(ad, emittedAd)
    }

    @Test
    fun `emit delivers ad clicked payload`() {
        var clickThroughUrl: String? = null

        emitter.on(YospacePlayerEvent.AdClicked::class) {
            clickThroughUrl = it.clickThroughUrl
        }

        emitter.emit(YospacePlayerEvent.AdClicked("https://example.com"))

        assertEquals("https://example.com", clickThroughUrl)
    }

    @Test
    fun `emit delivers error payload`() {
        var error: YospacePlayerEvent.Error? = null

        emitter.on(YospacePlayerEvent.Error::class) {
            error = it
        }

        emitter.emit(
            YospacePlayerEvent.Error(
                code = YospaceErrorCode.InvalidYospaceSource,
                message = "Invalid YoSpace source."
            )
        )

        assertEquals(YospaceErrorCode.InvalidYospaceSource, error?.code)
        assertEquals("Invalid YoSpace source.", error?.message)
    }

    @Test
    fun `emit delivers warning payload`() {
        var warning: YospacePlayerEvent.Warning? = null

        emitter.on(YospacePlayerEvent.Warning::class) {
            warning = it
        }

        emitter.emit(
            YospacePlayerEvent.Warning(
                code = YospaceWarningCode.UnsupportedAPI,
                message = "Unsupported API."
            )
        )

        assertEquals(YospaceWarningCode.UnsupportedAPI, warning?.code)
        assertEquals("Unsupported API.", warning?.message)
    }

    @Test
    fun `ad clickThroughUrlOpened invokes callback`() {
        var clickCount = 0
        val ad = Ad(
            id = "ad",
            creativeId = null,
            sequence = 0,
            absoluteStart = 0.0,
            relativeStart = 0.0,
            duration = 5.0,
            absoluteEnd = 5.0,
            system = null,
            title = null,
            advertiser = null,
            hasInteractiveUnit = false,
            isFiller = false,
            lineage = null,
            extensions = emptyList(),
            isLinear = true
        )
        ad.onClickThroughUrlOpened = { clickCount += 1 }

        ad.clickThroughUrlOpened()

        assertEquals(1, clickCount)
    }
}
