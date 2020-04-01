package com.bitmovin.player.integration.yospace

import com.truex.adrenderer.TruexAdRendererConstants

enum class TruexSlotType(val type: String) {
    PREROLL(TruexAdRendererConstants.PREROLL),
    MIDROLL(TruexAdRendererConstants.MIDROLL)
}
