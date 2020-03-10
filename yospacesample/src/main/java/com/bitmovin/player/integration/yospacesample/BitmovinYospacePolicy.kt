package com.bitmovin.player.integration.yospacesample

import com.bitmovin.player.integration.yospace.BitmovinYospacePlayer
import com.bitmovin.player.integration.yospace.BitmovinYospacePlayerPolicy

class BitmovinYospacePolicy(private val player: BitmovinYospacePlayer) : BitmovinYospacePlayerPolicy {

    override fun canSeek(): Boolean = !player.isAd

    override fun canSeekTo(seekTarget: Long): Long = seekTarget

    override fun canSkip(): Int = 0

    override fun canPause(): Boolean = true

    override fun canMute(): Boolean = true
}