package com.bitmovin.player.integration.yospace

import com.yospace.android.hls.analytic.Session
import com.yospace.android.hls.analytic.advert.AdBreak
import com.yospace.android.hls.analytic.policy.PolicyHandler

class YospacePlayerPolicy(var playerPolicy: BitmovinYospacePlayerPolicy?) : PolicyHandler {
    //TODO fix this class with proper values once we have a VOD test stream
    private var playbackMode: Session.PlaybackMode? = null

    override fun canStart(l: Long, list: List<AdBreak>): Boolean = true

    override fun canStop(l: Long, list: List<AdBreak>): Boolean = true

    override fun canPause(l: Long, list: List<AdBreak>): Boolean = playerPolicy?.canPause() ?: true

    override fun canRewind(l: Long, list: List<AdBreak>): Boolean = true

    override fun canSkip(l: Long, list: List<AdBreak>, l1: Long): Int = playerPolicy?.canSkip() ?: 0

    override fun canSeek(l: Long, list: List<AdBreak>): Boolean = playerPolicy?.canSeek() ?: true

    override fun willSeekTo(l: Long, list: List<AdBreak>): Long = playerPolicy?.canSeekTo(l) ?: l

    override fun canMute(l: Long, list: List<AdBreak>): Boolean = playerPolicy?.canMute() ?: true

    override fun canGoFullScreen(l: Long, list: List<AdBreak>): Boolean = true

    override fun canExitFullScreen(l: Long, list: List<AdBreak>): Boolean = true

    override fun canExpandCreative(l: Long, list: List<AdBreak>): Boolean = true

    override fun canCollapseCreative(l: Long, list: List<AdBreak>): Boolean = true

    override fun canClickThrough(s: String, l: Long, list: List<AdBreak>): Boolean = true

    override fun setPlaybackMode(playbackMode: Session.PlaybackMode) {
        this.playbackMode = playbackMode
    }
}
