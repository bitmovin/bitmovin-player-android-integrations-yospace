package com.bitmovin.player.integrations.bitmovinyospacemodule;

public class TimelineEntry {
    private double relativeStart = 0;
    private double duration = 0;
    private double absoluteStart = 0;
    private double absoluteEnd = 0;
    public double getRelativeStart() {
        return relativeStart;
    }

    public void setRelativeStart(double relativeStart) {
        this.relativeStart = relativeStart;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getAbsoluteStart() {
        return absoluteStart;
    }

    public void setAbsoluteStart(double absoluteStart) {
        this.absoluteStart = absoluteStart;
    }

    public double getAbsoluteEnd() {
        return absoluteEnd;
    }

    public void setAbsoluteEnd(double absoluteEnd) {
        this.absoluteEnd = absoluteEnd;
    }
}
