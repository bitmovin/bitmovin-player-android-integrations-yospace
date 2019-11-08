package com.bitmovin.player.integration.yospace;

import com.bitmovin.player.api.event.listener.EventListener;

public interface OnTruexAdFreeListener extends EventListener<TruexAdFreeEvent> {
    void onTruexAdFree(TruexAdFreeEvent event);
}
