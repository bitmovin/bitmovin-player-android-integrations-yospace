package com.bitmovin.player.integration.yospace.util

import com.yospace.android.hls.analytic.advert.InteractiveUnit

fun InteractiveUnit.notifyAdStarted() = onTrackingEvent("createView")

fun InteractiveUnit.notifyAdStopped() = onTrackingEvent("vpaidstopped")

fun InteractiveUnit.notifyAdSkipped() = onTrackingEvent("skip")

fun InteractiveUnit.notifyAdImpression() = onTrackingEvent("start")

fun InteractiveUnit.notifyAdVideoStart() = onTrackingEvent("createView")

fun InteractiveUnit.notifyAdVideoFirstQuartile() = onTrackingEvent("firstQuartile")

fun InteractiveUnit.notifyAdVideoMidpoint() = onTrackingEvent("midpoint")

fun InteractiveUnit.notifyAdVideoThirdQuartile() = onTrackingEvent("thirdQuartile")

fun InteractiveUnit.notifyAdVideoComplete() = onTrackingEvent("complete")

fun InteractiveUnit.notifyAdUserAcceptInvitation() = onTrackingEvent("acceptInvitationLinear")

fun InteractiveUnit.notifyAdUserMinimize() = onTrackingEvent("collapse")

fun InteractiveUnit.notifyAdUserClose() = onTrackingEvent("closeLinear")

fun InteractiveUnit.notifyAdPaused() = onTrackingEvent("pause")

fun InteractiveUnit.notifyAdVolumeMuted() = onTrackingEvent("mute")

fun InteractiveUnit.notifyAdVolumeUnmuted() = onTrackingEvent("unmute")
