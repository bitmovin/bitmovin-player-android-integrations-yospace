package com.bitmovin.player.integration.yospace

interface BitmovinTruexRendererListener {
    fun onAdFinished(isAdFree: Boolean)
    fun onAdError()
}
