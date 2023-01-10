package com.bitmovin.player.integration.yospace.config

import android.view.ViewGroup

data class TruexConfig(
    val viewGroup: ViewGroup,
    val userId: String? = null,
    val vastConfigUrl: String? = null
)
