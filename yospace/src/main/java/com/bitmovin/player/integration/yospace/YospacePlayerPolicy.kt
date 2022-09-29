package com.bitmovin.player.integration.yospace

import com.yospace.admanagement.PlaybackPolicyHandler
import com.yospace.admanagement.Session

class YospacePlayerPolicy(var playerPolicy: BitmovinYospacePlayerPolicy?) : PlaybackPolicyHandler {
    private var playbackMode: Session.PlaybackMode? = null

    override fun canStop(
        playhead: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ): Boolean = true

    override fun canPause(
        playhead: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ): Boolean = playerPolicy?.canPause() ?: true

    override fun canSkip(
        playhead: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?,
        duration: Long
    ): Int = playerPolicy?.canSkip() ?: 0

    override fun willSeekTo(
        position: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ): Long = playerPolicy?.canSeekTo(position) ?: position

    override fun canChangeVolume(
        mute: Boolean,
        playhead: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ): Boolean = playerPolicy?.canMute() ?: true

    override fun canResize(
        fullScreen: Boolean,
        playhead: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ): Boolean = true

    override fun canResizeCreative(
        expand: Boolean,
        playhead: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ): Boolean = true

    override fun canClickThrough(
        url: String?,
        playhead: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ): Boolean = true

    override fun setPlaybackMode(mode: Session.PlaybackMode?) {
        this.playbackMode = playbackMode
    }

    override fun didSkip(
        from: Long,
        to: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ) {
        BitLog.d("Did skip from :$from to :$to")
    }

    override fun didSeek(
        from: Long,
        to: Long,
        timeline: MutableList<com.yospace.admanagement.AdBreak>?
    ) {
        BitLog.d("Did seek from :$from to :$to")
    }
}
