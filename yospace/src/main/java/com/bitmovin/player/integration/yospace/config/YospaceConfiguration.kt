package com.bitmovin.player.integration.yospace.config

import com.bitmovin.player.integration.yospace.YospaceLiveInitialisationType

data class YospaceConfiguration(
    val userAgent: String,
    val readTimeout: Int,
    val connectTimeout: Int,
    val requestTimeout: Int,
    val liveInitialisationType: YospaceLiveInitialisationType,
    val isDebug: Boolean
)
