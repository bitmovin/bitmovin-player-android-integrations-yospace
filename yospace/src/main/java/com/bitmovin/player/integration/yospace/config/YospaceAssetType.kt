package com.bitmovin.player.integration.yospace.config

enum class YospaceAssetType {
    @Deprecated(
        message = "Use LINEAR_START_OVER for DVR live playback.",
        replaceWith = ReplaceWith(
            "YospaceAssetType.LINEAR_START_OVER",
            "com.bitmovin.player.integration.yospace.config.YospaceAssetType"
        )
    )
    LINEAR,
    VOD,
    LINEAR_START_OVER
}
