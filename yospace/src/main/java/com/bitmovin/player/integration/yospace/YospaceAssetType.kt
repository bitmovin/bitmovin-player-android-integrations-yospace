package com.bitmovin.player.integration.yospace

enum class YospaceAssetType {
    @Deprecated(
        message = "Use LINEAR_START_OVER for DVR live playback.",
        replaceWith = ReplaceWith("LINEAR_START_OVER")
    )
    LINEAR,
    VOD,
    LINEAR_START_OVER
}
