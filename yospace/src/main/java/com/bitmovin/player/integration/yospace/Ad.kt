package com.bitmovin.player.integration.yospace

data class Ad(
        val identifier: String,
        val relativeStart: Double,
        val duration: Double,
        val absoluteStart: Double,
        val absoluteEnd: Double,
        val sequence: Int,
        val clickThroughUrl: String,
        val hasInteractiveUnit: Boolean,
        val isTrueX: Boolean
)
