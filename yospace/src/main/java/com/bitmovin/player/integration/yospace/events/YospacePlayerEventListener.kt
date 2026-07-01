package com.bitmovin.player.integration.yospace.events

/**
 * Java-friendly listener for [YospacePlayerEvent]s. Use with the `Class<E>` overloads of
 * [com.bitmovin.player.integration.yospace.BitmovinYospacePlayer.on],
 * [com.bitmovin.player.integration.yospace.BitmovinYospacePlayer.next] and
 * [com.bitmovin.player.integration.yospace.BitmovinYospacePlayer.off].
 */
fun interface YospacePlayerEventListener<E : YospacePlayerEvent> {
    fun onEvent(event: E)
}
