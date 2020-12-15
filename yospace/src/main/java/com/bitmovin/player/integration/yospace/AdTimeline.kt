package com.bitmovin.player.integration.yospace

class AdTimeline(val adBreaks: List<AdBreak>) {

    override fun toString() = "${adBreaks.size} ad breaks: ${adBreaks.joinToString { "[${it.relativeStart} - ${it.duration}]" }}"

    /**
     * Returns the current ad break the player is in
     *
     * @param time - absolute time of the player
     * @return current ad break
     */
    fun currentAdBreak(time: Double) = adBreaks
        .find { it.absoluteStart <= time && it.absoluteStart + it.duration >= time }

    /**
     * Returns the next ad break in the timeline
     *
     * @param time - absolute time of the player
     * @return next ad break
     */
    fun nextAdBreak(time: Double) = adBreaks
        .filter { it.relativeStart > time }
        .minBy { it.relativeStart }

    /**
     * Returns the previous ad break in the timeline
     *
     * @param time - absolute time of the player
     * @return previous ad break
     */
    fun previousAdBreak(time: Double) = adBreaks
        .filter { it.relativeStart < time }
        .maxBy { it.relativeStart }

    /**
     * Converts the time from absolute to relative
     *
     * @param time - current absolute time
     * @return
     */
    fun absoluteToRelative(time: Double): Double {
        val currentAdBreak = currentAdBreak(time)
        val passedAdBreakDurations = totalPassedAdBreakDurations(time)
        return currentAdBreak?.let { it.absoluteStart - passedAdBreakDurations }
            ?: time - passedAdBreakDurations
    }

    /**
     * Converts the time from relative into absolute
     *
     * @param time - current relative time
     * @return
     */
    fun relativeToAbsolute(time: Double): Double = time + adBreaks
        .filter { it.relativeStart < time }
        .sumByDouble { it.duration }

    /**
     * Returns the sum of all of the ad break durations
     *
     * @return total ad break durations
     */
    fun totalAdBreakDurations(): Double = adBreaks.sumByDouble { it.duration }

    /**
     * Returns the sum of all of the ad break durations that have been watched
     *
     * @param time - current absolute time
     * @return total ad break durations
     */
    fun totalPassedAdBreakDurations(time: Double) = adBreaks
        .filter { it.absoluteEnd < time }
        .sumByDouble { it.duration }

}
