package com.bitmovin.player.integration.yospace;

public class Ad {
    private double relativeStart = 0;
    private double duration = 0;
    private double absoluteStart = 0;
    private double absoluteEnd = 0;
    private String identifier = "unknown";
    private boolean hasInteractiveUnit = false;

    public Ad(String identifier, double relativeStart, double duration, double absoluteStart, double absoluteEnd, boolean hasInteractiveUnit) {
        this.relativeStart = relativeStart;
        this.duration = duration;
        this.absoluteStart = absoluteStart;
        this.absoluteEnd = absoluteEnd;
        this.identifier = identifier;
        this.hasInteractiveUnit = hasInteractiveUnit;
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

    public String getIdentifier() {
        return identifier;
    }

    public boolean isHasInteractiveUnit() {
        return hasInteractiveUnit;
    }
}
