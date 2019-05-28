package com.bitmovin.player.integration.yospace;

public interface BitmovinYospacePlayerPolicy {

    /**
     * @return boolean returning true if the user is allowed to seek at the current point in the stream
     */
    boolean canSeek();

    /**
     * @param seekTarget seekTarget requested by the user
     * @return position the player should seek to
     */
    long canSeekTo(long seekTarget);

    /**
     * @return integer giving the time in seconds when the user can skip
     */
    int canSkip();

    /**
     * @return true if the player can pause
     */
    boolean canPause();

    /**
     * @return boolean if the player is allowed to mute
     */
    boolean canMute();

}
