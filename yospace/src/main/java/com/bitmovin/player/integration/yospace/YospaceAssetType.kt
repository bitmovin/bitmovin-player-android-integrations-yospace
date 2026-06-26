package com.bitmovin.player.integration.yospace

enum class YospaceAssetType {
    @Deprecated(
        message = "Use LINEAR_START_OVER for DVR live playback.",
        replaceWith = ReplaceWith(
            "YospaceAssetType.LINEAR_START_OVER",
            "com.bitmovin.player.integration.yospace.YospaceAssetType"
        )
    )
    LINEAR,
    VOD,
    LINEAR_START_OVER
}
