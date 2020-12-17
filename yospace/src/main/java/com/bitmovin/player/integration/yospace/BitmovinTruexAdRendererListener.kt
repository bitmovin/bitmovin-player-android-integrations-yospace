package com.bitmovin.player.integration.yospace

interface BitmovinTruexAdRendererListener {
    fun onAdCompleted()
    fun onAdFree()
    fun onSessionAdFree()
}
