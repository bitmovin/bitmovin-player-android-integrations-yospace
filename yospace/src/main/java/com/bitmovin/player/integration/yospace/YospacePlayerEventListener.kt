package com.bitmovin.player.integration.yospace

/**
 * Java-friendly listener for [YospacePlayerEvent]s. Use with the `Class<E>` overloads of
 * [YospaceApi.on], [YospaceApi.next] and [YospaceApi.off] via [BitmovinYospacePlayer.yospace].
 */
fun interface YospacePlayerEventListener<E : YospacePlayerEvent> {
    fun onEvent(event: E)
}
