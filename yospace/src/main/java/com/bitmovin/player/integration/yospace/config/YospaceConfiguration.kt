package com.bitmovin.player.integration.yospace.config

data class YospaceConfiguration(
        val userAgent: String? = null,
        val readTimeout: Int = 25_000,
        val connectTimeout: Int = 25_000,
        val requestTimeout: Int = 25_000,
        val isDebug: Boolean = false
)