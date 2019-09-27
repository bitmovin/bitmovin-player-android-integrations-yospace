package com.bitmovin.player.integration.yospace;

import com.yospace.android.hls.analytic.Session;
import com.yospace.android.hls.analytic.advert.AdBreak;
import com.yospace.android.hls.analytic.policy.PolicyHandler;

import java.util.List;

public class YospacePlayerPolicy implements PolicyHandler {
    //TODO fix this class with proper values once we have a VOD test stream
    private Session.PlaybackMode mPlaybackMode;
    private BitmovinYospacePlayerPolicy playerPolicy;

    public YospacePlayerPolicy(BitmovinYospacePlayerPolicy playerPolicy) {
        this.playerPolicy = playerPolicy;
    }

    @Override
    public boolean canStart(long l, List<AdBreak> list) {
        return true;
    }

    @Override
    public boolean canStop(long l, List<AdBreak> list) {
        return true;
    }

    @Override
    public boolean canPause(long l, List<AdBreak> list) {
        return this.playerPolicy.canPause();
    }

    @Override
    public boolean canRewind(long l, List<AdBreak> list) {
        return true;
    }

    @Override
    public int canSkip(long l, List<AdBreak> list, long l1) {
        return this.playerPolicy.canSkip();
    }

    @Override
    public boolean canSeek(long l, List<AdBreak> list) {
        return this.playerPolicy.canSeek();
    }

    @Override
    public long willSeekTo(long l, List<AdBreak> list) {
        return this.playerPolicy.canSeekTo(l);
    }

    @Override
    public boolean canMute(long l, List<AdBreak> list) {
        return this.playerPolicy.canMute();
    }

    @Override
    public boolean canGoFullScreen(long l, List<AdBreak> list) {
        return true;
    }

    @Override
    public boolean canExitFullScreen(long l, List<AdBreak> list) {
        return true;
    }

    @Override
    public boolean canExpandCreative(long l, List<AdBreak> list) {
        return true;
    }

    @Override
    public boolean canCollapseCreative(long l, List<AdBreak> list) {
        return true;
    }

    @Override
    public boolean canClickThrough(String s, long l, List<AdBreak> list) {
        return true;
    }

    @Override
    public void setPlaybackMode(Session.PlaybackMode playbackMode) {
        mPlaybackMode = playbackMode;
    }

    public void setPlayerPolicy(BitmovinYospacePlayerPolicy playerPolicy) {
        this.playerPolicy = playerPolicy;
    }
}
