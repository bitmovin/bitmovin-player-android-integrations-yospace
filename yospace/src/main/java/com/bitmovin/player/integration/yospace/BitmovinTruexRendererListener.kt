package com.bitmovin.player.integration.yospace

interface BitmovinTruexRendererListener {
    fun onTruexAdFree()
    fun onTruexAdCompleted()
    fun onTruexAdError()
    fun onTruexNoAds()
}
