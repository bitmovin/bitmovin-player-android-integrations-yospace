package com.bitmovin.player.integration.yospace

import com.truex.adrenderer.TruexAdRendererConstants

enum class SlotType(val type: String) {
    PREROLL(TruexAdRendererConstants.PREROLL),
    MIDROLL(TruexAdRendererConstants.MIDROLL)
}
