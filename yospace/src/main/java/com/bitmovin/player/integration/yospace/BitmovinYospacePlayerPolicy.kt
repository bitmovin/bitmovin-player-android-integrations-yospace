package com.bitmovin.player.integration.yospace

interface BitmovinYospacePlayerPolicy {

    /**
     * @return boolean returning true if the user is allowed to seek at the current point in the stream
     */
    fun canSeek(): Boolean

    /**
     * @param seekTarget seekTarget requested by the user
     * @return position the player should seek to
     */
    fun canSeekTo(seekTarget: Long): Long

    /**
     * @return integer giving the time in seconds when the user can skip
     */
    fun canSkip(): Int

    /**
     * @return true if the player can pause
     */
    fun canPause(): Boolean

    /**
     * @return boolean if the player is allowed to mute
     */
    fun canMute(): Boolean

}
