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
     * The session callbacks are currently accepted for, or `null` while no session is active.
     *
     * Removing the analytic observer does not cancel a callback already in flight, and a delayed
     * one can arrive after the next session has started. Callbacks therefore carry the session they
     * belong to and are dropped unless it is still this one, so they can neither reopen an ad break
     * that is never ended nor mutate a later session's state.
     */
    private var activeSession: Any? = null

    /**
     * Quartiles of an ad that was already playing when playback joined do not reflect what the
     * viewer saw, so they are not reported.
     */
    private var areQuartilesSuppressed = false
    private val reportedQuartiles = mutableSetOf<SsaiQuartile>()

    /**
     * Opens the tracker for [session]. Callbacks are ignored until this is called, and callbacks of
     * any earlier session are ignored from here on.
     */
    fun onSessionStart(session: Any) {
        synchronized(lock) {
            clearState()
            activeSession = session
        }
    }

    fun onAdBreakStart(session: Any, position: AdBreakPosition, paidAds: Int?, slates: Int?) {
        val tracker = tracker ?: return
        synchronized(lock) {
            if (session !== activeSession) return
            if (isAdBreakActive) return
            isAdBreakActive = true
            tracker.adBreakStart(SsaiAdBreakInfo(position, paidAds, slates))
        }
    }

    /**
     * @param joinedMidAd whether playback joined this ad after it had already started.
     */
    fun onAdStart(session: Any, ad: SsaiAdInfo, joinedMidAd: Boolean) {
        val tracker = tracker ?: return
        synchronized(lock) {
            if (session !== activeSession) return
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

    fun onQuartileFinished(session: Any, quartile: SsaiQuartile) {
        val tracker = tracker ?: return
        synchronized(lock) {
            if (session !== activeSession) return
            if (!isAdActive || areQuartilesSuppressed) return
            if (!reportedQuartiles.add(quartile)) return
            tracker.adQuartileFinished(quartile)
        }
    }

    fun onAdBreakEnd(session: Any) {
        val tracker = tracker ?: return
        synchronized(lock) {
            if (session !== activeSession) return
            if (!isAdBreakActive) return
            clearState()
            tracker.adBreakEnd()
        }
    }

    /**
     * Ends an ad break that is still open and detaches from the current session, so its callbacks
     * are ignored from here on. Without this, analytics keeps attributing content playback to an ad.
     */
    fun reset() {
        synchronized(lock) {
            val wasAdBreakActive = isAdBreakActive
            clearState()
            activeSession = null
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
