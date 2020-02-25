package com.bitmovin.player.integration.yospacesample;

import android.util.Log;

import com.bitmovin.player.integration.yospace.BitmovinYospacePlayer;
import com.bitmovin.player.integration.yospace.BitmovinYospacePlayerPolicy;

public class BitmovinYospacePolicy implements BitmovinYospacePlayerPolicy {
    private BitmovinYospacePlayer bitmovinYospacePlayer;

    public BitmovinYospacePolicy(BitmovinYospacePlayer bitmovinYospacePlayer) {
        this.bitmovinYospacePlayer = bitmovinYospacePlayer;
    }

    @Override
    public boolean canSeek() {
        Log.d("BitmovinYospacePolicy", "canSeek");
        return !bitmovinYospacePlayer.isAd();
    }

    @Override
    public long canSeekTo(long seekTarget) {
        Log.d("BitmovinYospacePolicy", "canSeekTo");
        return seekTarget;
    }

    @Override
    public int canSkip() {
        Log.d("BitmovinYospacePolicy", "canSkip");
        return 0;
    }

    @Override
    public boolean canPause() {
        Log.d("BitmovinYospacePolicy", "canPause");
        return true;
    }

    @Override
    public boolean canMute() {
        Log.d("BitmovinYospacePolicy", "canMute");
        return true;
    }
}
