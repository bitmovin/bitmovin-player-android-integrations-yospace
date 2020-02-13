package com.bitmovin.player.integration.yospace

class AdData(
    override val mimeType: String,
    override val bitrate: Int,
    override val minBitrate: Int = 0,
    override val maxBitrate: Int = 0
) : com.bitmovin.player.model.advertising.AdData
