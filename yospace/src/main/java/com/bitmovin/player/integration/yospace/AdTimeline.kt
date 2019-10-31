package com.bitmovin.player.integration.yospace

import com.bitmovin.player.integration.yospace.util.toAdBreaks
import com.yospace.android.hls.analytic.advert.AdBreak as YospaceAdBreak

class AdTimeline(yospaceAdBreaks: List<YospaceAdBreak>) {

    val adBreaks: List<AdBreak> = yospaceAdBreaks.toAdBreaks()

    override fun toString(): String =
        "${adBreaks.size} Ad Breaks ${adBreaks.joinToString { adBreak -> "[${adBreak.relativeStart} - ${adBreak.duration}]" }}"

    /**
     * Returns the current ad break the player is in
     * @param time - absolute time of the player
     * @return current ad break
     */
    fun currentAdBreak(time: Double): AdBreak? =
        adBreaks.find { adBreak -> adBreak.absoluteStart < time && adBreak.absoluteStart + adBreak.duration > time }

    /**
     * Returns the current ad the player is in
     * @param time - current absolute time
     * @return
     */
    fun currentAd(time: Double): Ad? {
        val currentAdBreak: AdBreak = currentAdBreak(time) ?: return null
        return currentAdBreak.ads.find { ad -> ad.absoluteStart < time && ad.absoluteEnd > time }
    }

    /**
     * Converts the time from absolute to relative
     * @param time - current absolute time
     * @return
     */
    fun absoluteToRelative(time: Double): Double {
        val currentAdBreak = currentAdBreak(time)
        val passedAdBreakDurations = adBreaks.filter { adBreak -> adBreak.absoluteEnd < time }
            .sumByDouble { adBreak -> adBreak.duration }

        return if (currentAdBreak != null) {
            currentAdBreak.absoluteStart - passedAdBreakDurations
        } else {
            time - passedAdBreakDurations
        }
    }

    /**
     * Converts the time from relative into absolute
     * @param time - current relative time
     * @return
     */
    fun relativeToAbsolute(time: Double): Double =
        time + adBreaks.filter { adBreak -> adBreak.relativeStart < time }
            .sumByDouble { adBreak -> adBreak.duration }

    /**
     * Returns the sum of all of the ad break durations
     *
     * @return total ad break duraitons
     */
    fun totalAdBreakDurations(): Double = adBreaks.sumByDouble { adBreak -> adBreak.duration }

    /**
     * Returns the current progress through the ad (if we are in an ad). Otherwise returns the time passed in
     * @param time - current absolute time
     * @return progress through the ad
     */
    fun adTime(time: Double): Double {
        currentAd(time)?.let { ad -> return time - ad.absoluteStart }
        return time
    }
}
