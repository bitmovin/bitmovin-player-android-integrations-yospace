package com.bitmovin.player.integration.yospace.config

import android.content.Context
import com.bitmovin.player.integration.yospace.YospaceLiveInitialisationType
import com.bitmovin.analytics.BitmovinAnalyticsConfig

data class YospaceConfiguration(
        val context: Context,
        val userAgent: String? = null,
        val readTimeout: Int = 25_000,
        val connectTimeout: Int = 25_000,
        val requestTimeout: Int = 25_000,
        val liveInitialisationType: YospaceLiveInitialisationType = YospaceLiveInitialisationType.DIRECT,
        val isDebug: Boolean = false,
        val analyticsConfig: BitmovinAnalyticsConfig? = null
)
