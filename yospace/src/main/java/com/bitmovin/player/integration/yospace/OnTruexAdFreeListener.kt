package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.event.listener.EventListener

interface OnTruexAdFreeListener : EventListener<TruexAdFreeEvent> {
    fun onTruexAdFree(event: TruexAdFreeEvent)
}
