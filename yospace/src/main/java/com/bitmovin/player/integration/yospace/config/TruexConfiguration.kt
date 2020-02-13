package com.bitmovin.player.integration.yospace.config

import android.view.ViewGroup

data class TruexConfiguration(
    var viewGroup: ViewGroup,
    val userId: String?,
    val vastConfigUrl: String?
)
