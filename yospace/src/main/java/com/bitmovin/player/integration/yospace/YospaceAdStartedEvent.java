package com.bitmovin.player.integration.yospace;

import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.bitmovin.player.config.advertising.AdSourceType;

public class YospaceAdStartedEvent extends AdStartedEvent {
    public YospaceAdStartedEvent(AdSourceType clientType, String clickThroughUrl, int indexInQueue, double duration, double timeOffset, String position, double skipOffset) {
        super(clientType, clickThroughUrl, indexInQueue, duration / 1000, timeOffset / 1000, position, skipOffset / 1000);
    }
}
