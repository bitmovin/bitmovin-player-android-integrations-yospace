package com.bitmovin.player.integration.yospace

interface TruexAdRendererEventListener {
    fun onSkipTruexAd()
    fun onSkipAdBreak()
    fun onSessionAdFree()
}
