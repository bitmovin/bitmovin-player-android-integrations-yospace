package com.bitmovin.player.integration.yospace.analytics

import com.bitmovin.player.integration.yospace.advertising.AdBreakPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

private sealed class Call {
    data class AdBreakStart(val adBreak: SsaiAdBreakInfo) : Call()
    data class AdStart(val ad: SsaiAdInfo) : Call()
    data class Quartile(val quartile: SsaiQuartile) : Call()
    object AdBreakEnd : Call()
}

private class FakeSsaiAdTracker : SsaiAdTracker {
    val calls = mutableListOf<Call>()

    override fun adBreakStart(adBreak: SsaiAdBreakInfo) {
        calls += Call.AdBreakStart(adBreak)
    }

    override fun adStart(ad: SsaiAdInfo) {
        calls += Call.AdStart(ad)
    }

    override fun adQuartileFinished(quartile: SsaiQuartile) {
        calls += Call.Quartile(quartile)
    }

    override fun adBreakEnd() {
        calls += Call.AdBreakEnd
    }
}

private fun adInfo(adId: String = "ad-1", isSlate: Boolean = false) =
    SsaiAdInfo(adId = adId, adSystem = "Yospace", isSlate = isSlate, durationMs = 15_000)

class YospaceSsaiTrackerTest {

    @Test
    fun `reports ad break and ad in order`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 2, slates = 1)
        tracker.onAdStart(adInfo(), joinedMidAd = false)
        tracker.onAdBreakEnd()

        assertEquals(
            listOf(
                Call.AdBreakStart(SsaiAdBreakInfo(AdBreakPosition.MIDROLL, 2, 1)),
                Call.AdStart(adInfo()),
                Call.AdBreakEnd
            ),
            fake.calls
        )
    }

    @Test
    fun `ignores a second ad break start while a break is active`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.PREROLL, paidAds = 1, slates = 0)
        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 5, slates = 0)

        assertEquals(1, fake.calls.count { it is Call.AdBreakStart })
    }

    @Test
    fun `starts an ad break when an ad starts outside one`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdStart(adInfo(), joinedMidAd = true)

        assertEquals(
            listOf(
                Call.AdBreakStart(SsaiAdBreakInfo(AdBreakPosition.UNKNOWN, null, null)),
                Call.AdStart(adInfo())
            ),
            fake.calls
        )
    }

    @ParameterizedTest
    @CsvSource("FIRST", "MIDPOINT", "THIRD", "COMPLETED")
    fun `reports quartiles of an ad that was watched from the start`(quartileName: String) {
        val quartile = SsaiQuartile.valueOf(quartileName)
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 1, slates = 0)
        tracker.onAdStart(adInfo(), joinedMidAd = false)
        tracker.onQuartileFinished(quartile)

        assertTrue(fake.calls.contains(Call.Quartile(quartile)))
    }

    @Test
    fun `does not report quartiles of an ad that was joined mid-roll`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 1, slates = 0)
        tracker.onAdStart(adInfo(), joinedMidAd = true)
        tracker.onQuartileFinished(SsaiQuartile.THIRD)

        assertTrue(fake.calls.none { it is Call.Quartile })
    }

    @Test
    fun `reports quartiles again for the next ad after a mid-roll join`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 2, slates = 0)
        tracker.onAdStart(adInfo("ad-1"), joinedMidAd = true)
        tracker.onQuartileFinished(SsaiQuartile.THIRD)
        tracker.onAdStart(adInfo("ad-2"), joinedMidAd = false)
        tracker.onQuartileFinished(SsaiQuartile.FIRST)

        assertEquals(listOf(Call.Quartile(SsaiQuartile.FIRST)), fake.calls.filterIsInstance<Call.Quartile>())
    }

    @Test
    fun `reports each quartile of an ad once`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 1, slates = 0)
        tracker.onAdStart(adInfo(), joinedMidAd = false)
        tracker.onQuartileFinished(SsaiQuartile.FIRST)
        tracker.onQuartileFinished(SsaiQuartile.FIRST)

        assertEquals(1, fake.calls.count { it is Call.Quartile })
    }

    @Test
    fun `does not report quartiles outside an ad`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onQuartileFinished(SsaiQuartile.FIRST)

        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun `ends an ad break that is still open on reset`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 1, slates = 0)
        tracker.onAdStart(adInfo(), joinedMidAd = false)
        tracker.reset()

        assertEquals(Call.AdBreakEnd, fake.calls.last())
    }

    @Test
    fun `does not end an ad break when none is open`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 1, slates = 0)
        tracker.onAdBreakEnd()
        tracker.reset()

        assertEquals(1, fake.calls.count { it is Call.AdBreakEnd })
    }

    @Test
    fun `reports an ad break again after the previous one ended`() {
        val fake = FakeSsaiAdTracker()
        val tracker = YospaceSsaiTracker(fake)

        tracker.onAdBreakStart(AdBreakPosition.PREROLL, paidAds = 1, slates = 0)
        tracker.onAdBreakEnd()
        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 1, slates = 0)

        assertEquals(2, fake.calls.count { it is Call.AdBreakStart })
    }

    @Test
    fun `does nothing when analytics is not configured`() {
        val tracker = YospaceSsaiTracker(null)

        // Would throw if the absent tracker were dereferenced
        tracker.onAdBreakStart(AdBreakPosition.MIDROLL, paidAds = 1, slates = 0)
        tracker.onAdStart(adInfo(), joinedMidAd = false)
        tracker.onQuartileFinished(SsaiQuartile.FIRST)
        tracker.onAdBreakEnd()
        tracker.reset()
    }
}
