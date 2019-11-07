package com.bitmovin.player.integration.yospace;

import com.bitmovin.player.api.event.data.AdBreakFinishedEvent;

public class YospaceAdBreakFinishedEvent extends AdBreakFinishedEvent {
    private boolean isTrueX;

    public YospaceAdBreakFinishedEvent(boolean isTrueX) {
        super();
        this.isTrueX = isTrueX;
    }

    public boolean isTrueX() {
        return isTrueX;
    }
}
