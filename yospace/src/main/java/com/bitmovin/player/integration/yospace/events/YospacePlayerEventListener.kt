package com.bitmovin.player.integration.yospace.events

/**
 * Java-friendly listener for [YospacePlayerEvent]s. Use with the `Class<E>` overloads of
 * [BitmovinYospacePlayer.on], [BitmovinYospacePlayer.next] and [BitmovinYospacePlayer.off].
 */
fun interface YospacePlayerEventListener<E : YospacePlayerEvent> {
    fun onEvent(event: E)
}
