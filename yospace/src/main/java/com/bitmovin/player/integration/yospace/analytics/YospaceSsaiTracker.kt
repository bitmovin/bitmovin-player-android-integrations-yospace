package com.bitmovin.player.integration.yospace.analytics

import com.bitmovin.player.integration.yospace.advertising.AdBreakPosition

/**
 * Maps Yospace advert callbacks onto the Bitmovin Analytics SSAI API.
 *
 * Yospace inserts ads server-side, so the player emits no client-side ad events and analytics has
 * to be told about ad boundaries explicitly.
 *
 * Called from both the Yospace callback thread and the main thread, so all state is guarded.
 */
internal class YospaceSsaiTracker(private val tracker: SsaiAdTracker?) {

    private val lock = Any()
    private var isAdBreakActive = false
    private var isAdActive = false

    /**
     * Set by [reset] and cleared by [onSessionStart]. Callbacks already in flight when a session is
     * torn down find the tracker closed and are dropped, so they cannot reopen an ad break that is
     * never ended.
     */
    private var isClosed = true

    /**
     * Quartiles of an ad that was already playing when playback joined do not reflect what the
     * viewer saw, so they are not reported.
     */
    private var areQuartilesSuppressed = false
    private val reportedQuartiles = mutableSetOf<SsaiQuartile>()

    /**
     * Opens the tracker for a new Yospace session. Callbacks are ignored until this is called, so
     * that callbacks left over from a previous session cannot report against the new one.
     */
    fun onSessionStart() {
        synchronized(lock) {
            clearState()
            isClosed = false
        }
    }

    fun onAdBreakStart(position: AdBreakPosition, paidAds: Int?, slates: Int?) {
        val tracker = tracker ?: return
        synchronized(lock) {
            if (isClosed) return
            if (isAdBreakActive) return
            isAdBreakActive = true
            tracker.adBreakStart(SsaiAdBreakInfo(position, paidAds, slates))
        }
    }

    /**
     * @param joinedMidAd whether playback joined this ad after it had already started.
     */
    fun onAdStart(ad: SsaiAdInfo, joinedMidAd: Boolean) {
        val tracker = tracker ?: return
        synchronized(lock) {
            if (isClosed) return
            // Yospace reports adverts without a preceding break start when joining mid-break, and
            // the analytics API drops ads that are not inside a break.
            var joinedMidBreak = false
            if (!isAdBreakActive) {
                isAdBreakActive = true
                joinedMidBreak = true
                tracker.adBreakStart(SsaiAdBreakInfo(AdBreakPosition.UNKNOWN, null, null))
            }

            isAdActive = true
            // A missing break start means playback joined this advert while it was already running.
            areQuartilesSuppressed = joinedMidAd || joinedMidBreak
            reportedQuartiles.clear()

            // There is no ad-stop call; starting the next ad ends the previous one.
            tracker.adStart(ad)
        }
    }

    fun onQuartileFinished(quartile: SsaiQuartile) {
        val tracker = tracker ?: return
        synchronized(lock) {
            if (isClosed) return
            if (!isAdActive || areQuartilesSuppressed) return
            if (!reportedQuartiles.add(quartile)) return
            tracker.adQuartileFinished(quartile)
        }
    }

    fun onAdBreakEnd() {
        val tracker = tracker ?: return
        synchronized(lock) {
            if (isClosed) return
            if (!isAdBreakActive) return
            clearState()
            tracker.adBreakEnd()
        }
    }

    /**
     * Ends an ad break that is still open and closes the tracker until the next session starts.
     * Without this, analytics keeps attributing content playback to an ad.
     */
    fun reset() {
        synchronized(lock) {
            val wasAdBreakActive = isAdBreakActive
            clearState()
            isClosed = true
            if (wasAdBreakActive) tracker?.adBreakEnd()
        }
    }

    private fun clearState() {
        isAdBreakActive = false
        isAdActive = false
        areQuartilesSuppressed = false
        reportedQuartiles.clear()
    }
}
