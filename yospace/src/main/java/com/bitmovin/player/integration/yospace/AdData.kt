package com.bitmovin.player.integration.yospace

class AdData(
    override val mimeType: String,
    override val bitrate: Int = -1,
    override val minBitrate: Int = -1,
    override val maxBitrate: Int = -1
) : com.bitmovin.player.model.advertising.AdData
