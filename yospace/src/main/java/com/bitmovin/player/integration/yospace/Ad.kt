package com.bitmovin.player.integration.yospace

import com.bitmovin.player.model.advertising.AdData

data class Ad(
    override var id: String?,
    val relativeStart: Double,
    val duration: Double,
    val absoluteStart: Double,
    val absoluteEnd: Double,
    val sequence: Int,
    val isHasInteractiveUnit: Boolean,
    val isTruex: Boolean,
    override val isLinear: Boolean,
    override var clickThroughUrl: String?,
    override var data: AdData?,
    override var width: Int = 0,
    override var height: Int = 0,
    override var mediaFileUrl: String? = null
) : com.bitmovin.player.model.advertising.Ad
