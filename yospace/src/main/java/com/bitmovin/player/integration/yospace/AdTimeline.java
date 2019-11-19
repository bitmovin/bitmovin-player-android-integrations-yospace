package com.bitmovin.player.integration.yospace;

import com.yospace.android.hls.analytic.advert.AdSystem;
import com.yospace.android.hls.analytic.advert.Advert;
import com.yospace.android.hls.analytic.advert.LinearCreative;
import com.yospace.android.hls.analytic.advert.NonLinearCreative;

import java.util.ArrayList;
import java.util.List;

public class AdTimeline {
    private List<AdBreak> adBreaks = new ArrayList<AdBreak>();

    public AdTimeline(List<com.yospace.android.hls.analytic.advert.AdBreak> adBreaks) {
        double count = 0;

        for (com.yospace.android.hls.analytic.advert.AdBreak adbreak : adBreaks) {
            AdBreak entry = new AdBreak("unknown", (adbreak.getStartMillis() - count) / 1000, adbreak.getDuration() / 1000.0, adbreak.getStartMillis() / 1000.0, (adbreak.getStartMillis() + adbreak.getDuration()) / 1000.0, 0);
            count += adbreak.getDuration();

            for (Advert advert : adbreak.getAdverts()) {
                String clickThroughUrl = YospaceUtil.getAdClickThroughUrl(advert);
                AdSystem adSystem = advert.getAdSystem();
                boolean isTruex = adSystem != null && adSystem.getAdSystemType().equals("trueX");
                boolean isLinear = !isTruex;
                double absoluteEnd = advert.getStartMillis() + advert.getDuration();
                int width = -1;
                int height = -1;
                String mediaFileUrl = "";
                String mimeType = "";

                if (isLinear) {
                    LinearCreative linearCreative = advert.getLinearCreative();
                    mediaFileUrl = linearCreative.getAssetUri();
                    if (linearCreative.getInteractiveUnit() != null) {
                        mimeType = linearCreative.getInteractiveUnit().getMIMEType();
                    }
                } else {
                    List<NonLinearCreative> nonLinearCreatives = advert.getNonLinearCreatives();
                    if (!nonLinearCreatives.isEmpty()) {
                        NonLinearCreative nonLinearCreative = nonLinearCreatives.get(0);
                        width = nonLinearCreative.getWidth();
                        height = nonLinearCreative.getHeight();
                    }
                }

                AdData adData = new AdData(-1, -1, -1, mimeType);

                Ad ad = new Ad(
                        advert.getIdentifier(),
                        entry.getRelativeStart() / 1000,
                        advert.getDuration() / 1000.0,
                        advert.getStartMillis() / 1000.0,
                        absoluteEnd / 1000.0,
                        advert.getSequence(),
                        clickThroughUrl,
                        mediaFileUrl,
                        isLinear,
                        advert.hasLinearInteractiveUnit(),
                        isTruex,
                        width,
                        height,
                        adData
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
        AdBreak currentAdBreak = null;
        Ad currentAd = null;

        for (AdBreak adBreak : adBreaks) {
            if (adBreak.getAbsoluteStart() < time && (adBreak.getAbsoluteStart() + adBreak.getDuration()) > time) {
                currentAdBreak = adBreak;
                break;
            }
        }

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
