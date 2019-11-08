package com.bitmovin.player.integration.yospace;

public class Ad {
    private String identifier;
    private double relativeStart;
    private double duration;
    private double absoluteStart;
    private double absoluteEnd;
    private int sequence;
    private String clickThroughUrl;
    private boolean hasInteractiveUnit;
    private boolean isTruex;

    public Ad(String identifier, double relativeStart, double duration, double absoluteStart, double absoluteEnd, int sequence, String clickThroughUrl, boolean hasInteractiveUnit, boolean isTruex) {
        this.identifier = identifier;
        this.relativeStart = relativeStart;
        this.duration = duration;
        this.absoluteStart = absoluteStart;
        this.absoluteEnd = absoluteEnd;
        this.sequence = sequence;
        this.clickThroughUrl = clickThroughUrl;
        this.hasInteractiveUnit = hasInteractiveUnit;
        this.isTruex = isTruex;
    }

    public String getIdentifier() {
        return identifier;
    }

    public double getRelativeStart() {
        return relativeStart;
    }

    public double getDuration() {
        return duration;
    }

    public double getAbsoluteStart() {
        return absoluteStart;
    }

    public double getAbsoluteEnd() {
        return absoluteEnd;
    }

    public int getSequence() {
        return sequence;
    }

    public String getClickThroughUrl() {
        return clickThroughUrl;
    }

    public boolean isHasInteractiveUnit() {
        return hasInteractiveUnit;
    }

    public boolean isTruex() {
        return isTruex;
    }
}
