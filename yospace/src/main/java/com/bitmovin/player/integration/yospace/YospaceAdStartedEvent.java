package com.bitmovin.player.integration.yospace;

import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.bitmovin.player.config.advertising.AdSourceType;
import com.bitmovin.player.model.advertising.Ad;

public class YospaceAdStartedEvent extends AdStartedEvent {
    private boolean isTruex;

    public YospaceAdStartedEvent(AdSourceType clientType, String clickThroughUrl, int indexInQueue, double duration, double timeOffset, String position, double skipOffset, boolean isTruex, Ad ad) {
        super(clientType, clickThroughUrl, indexInQueue, duration, timeOffset, position, skipOffset, ad);
        this.isTruex = isTruex;
    }

    public boolean isTruex() {
        return isTruex;
    }
}
