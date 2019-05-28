package com.bitmovin.player.integration.yospace;

public class DefaultBitmovinYospacePlayerPolicy implements BitmovinYospacePlayerPolicy {
    private BitmovinYospacePlayer bitmovinYospacePlayer;

    public DefaultBitmovinYospacePlayerPolicy(BitmovinYospacePlayer player) {
        this.bitmovinYospacePlayer = player;
    }

    @Override
    public boolean canSeek() {
        return !bitmovinYospacePlayer.isAd();
    }

    @Override
    public long canSeekTo(long seekTarget) {
        return seekTarget;
    }

    @Override
    public int canSkip() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canMute() {
        return true;
    }
}
