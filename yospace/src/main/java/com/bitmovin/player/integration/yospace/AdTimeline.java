package com.bitmovin.player.integration.yospace;

import com.yospace.android.hls.analytic.advert.Advert;

import java.util.ArrayList;
import java.util.List;

public class AdTimeline {
    private List<AdBreak> adBreaks = new ArrayList<>();

    public AdTimeline(List<com.yospace.android.hls.analytic.advert.AdBreak> adBreaks) {
        double relativeOffset = 0;

        for (com.yospace.android.hls.analytic.advert.AdBreak adBreak : adBreaks) {
            AdBreak entry = new AdBreak(
                    "unknown",
                    (adBreak.getStartMillis() - relativeOffset) / 1000,
                    adBreak.getDuration() / 1000.0,
                    adBreak.getStartMillis() / 1000.0,
                    (adBreak.getStartMillis() + adBreak.getDuration()) / 1000.0,
                    0
            );

            relativeOffset += adBreak.getDuration();

            for (Advert advert : adBreak.getAdverts()) {
                boolean isTruex = YospaceUtil.isAdTruex(advert);
                String mimeType = YospaceUtil.getAdMimeType(advert);
                AdData adData = new AdData(mimeType);

                Ad ad = new Ad(
                        advert.getIdentifier(),
                        entry.getRelativeStart() / 1000,
                        advert.getDuration() / 1000.0,
                        advert.getStartMillis() / 1000.0,
                        (advert.getStartMillis() + advert.getDuration()) / 1000.0,
                        advert.getSequence(),
                        advert.hasLinearInteractiveUnit(),
                        isTruex,
                        !isTruex,
                        YospaceUtil.getAdClickThroughUrl(advert),
                        adData,
                        0,
                        0,
                        null
                );

                entry.appendAd(ad);
            }

            this.adBreaks.add(entry);
        }
    }

    @Override
    public String toString() {
        String str = adBreaks.size() + " ad breaks ";
        for (AdBreak entry : adBreaks) {
            str += " [" + entry.getRelativeStart() + " - " + entry.getDuration() + "] ";
        }
        return str;
    }

    /**
     * Returns the current ad break the player is in
     *
     * @param time - absolute time of the player
     * @return current ad break
     */
    public AdBreak currentAdBreak(double time) {
        AdBreak currentAdBreak = null;

        for (AdBreak adBreak : adBreaks) {
            if (adBreak.getAbsoluteStart() < time && (adBreak.getAbsoluteStart() + adBreak.getDuration()) > time) {
                currentAdBreak = adBreak;
                break;
            }
        }
        return currentAdBreak;
    }

    /**
     * Returns the current ad the player is in
     *
     * @param time - current absolute time
     * @return
     */
    public Ad currentAd(double time) {
        AdBreak currentAdBreak = currentAdBreak(time);
        Ad currentAd = null;

        if (currentAdBreak == null) {
            return null;
        }

        List<com.bitmovin.player.model.advertising.Ad> ads = currentAdBreak.getAds();

        for (com.bitmovin.player.model.advertising.Ad ad : ads) {
            Ad ysAd = ((Ad) ad);
            if (ysAd.getAbsoluteStart() < time && (ysAd.getAbsoluteEnd() > time)) {
                currentAd = ysAd;
                break;
            }
        }

        return currentAd;
    }

    /**
     * Converts the time from absolute to relative
     *
     * @param time - current absolute time
     * @return
     */
    public double absoluteToRelative(double time) {
        double passedAdBreakDurations = 0;
        AdBreak currentAdBreak = currentAdBreak(time);

        for (AdBreak entry : adBreaks) {
            if (entry.getAbsoluteEnd() < time) {
                passedAdBreakDurations += entry.getDuration();
            }
        }

        if (currentAdBreak != null) {
            return (currentAdBreak.getAbsoluteStart() - passedAdBreakDurations);
        }

        return (time - passedAdBreakDurations);
    }

    /**
     * Converts the time from relative into absolute
     *
     * @param time - current relative time
     * @return
     */
    public double relativeToAbsolute(double time) {
        double passedAdBreakDurations = 0;

        for (AdBreak entry : adBreaks) {
            if (entry.getRelativeStart() < time) {
                passedAdBreakDurations += entry.getDuration();
            }
        }

        return (time + passedAdBreakDurations);
    }

    /**
     * Returns the sum of all of the ad break durations
     *
     * @return total ad break duraitons
     */
    public double totalAdBreakDurations() {
        double breakDurations = 0;

        for (AdBreak entry : adBreaks) {
            breakDurations += entry.getDuration();
        }

        return breakDurations;
    }

    /**
     * Returns the current progress through the ad (if we are in an ad). Otherwise returns the time passed in
     *
     * @param time - current absolute time
     * @return progress through the ad
     */
    public double adTime(double time) {
        Ad currentAd = this.currentAd(time);
        if (currentAd != null) {
            return time - currentAd.getAbsoluteStart();
        } else {
            return time;
        }
    }

    /**
     * Returns a list of all ad breaks
     *
     * @return
     */
    public List<AdBreak> getAdBreaks() {
        return adBreaks;
    }
}
