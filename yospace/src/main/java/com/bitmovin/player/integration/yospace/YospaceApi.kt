package com.bitmovin.player.integration.yospace

import kotlin.reflect.KClass

/**
 * Yospace-specific event API exposed via [BitmovinYospacePlayer.yospace]. Consumers subscribe to
 * integration-owned ad events here instead of through `Player.on<PlayerEvent...>`, which keeps the
 * non-serializable integration payloads away from the Player UI.
 *
 * ```kotlin
 * player.yospace.on<YospacePlayerEvent.AdBreakStarted> { event ->
 *     val adBreak = event.adBreak
 * }
 * ```
 */
interface YospaceApi {
    /**
     * Subscribes [action] to Yospace integration events of type [eventClass]. Use the reified
     * [on] extension for Kotlin call sites.
     */
    fun <E : YospacePlayerEvent> on(eventClass: KClass<E>, action: (E) -> Unit)

    /**
     * Subscribes [action] to the next Yospace integration event of type [eventClass], then
     * unsubscribes automatically.
     */
    fun <E : YospacePlayerEvent> next(eventClass: KClass<E>, action: (E) -> Unit)

    /**
     * Unsubscribes [action] from Yospace integration events of type [eventClass].
     */
    fun <E : YospacePlayerEvent> off(eventClass: KClass<E>, action: (E) -> Unit)

    /**
     * Unsubscribes [action] from all Yospace integration events.
     */
    fun <E : YospacePlayerEvent> off(action: (E) -> Unit)

    /**
     * Java-friendly overload of [on] taking a [Class] and a [YospacePlayerEventListener].
     */
    fun <E : YospacePlayerEvent> on(eventClass: Class<E>, listener: YospacePlayerEventListener<E>)

    /**
     * Java-friendly overload of [next] taking a [Class] and a [YospacePlayerEventListener].
     */
    fun <E : YospacePlayerEvent> next(eventClass: Class<E>, listener: YospacePlayerEventListener<E>)

    /**
     * Java-friendly overload of [off] taking a [Class] and a [YospacePlayerEventListener].
     */
    fun <E : YospacePlayerEvent> off(eventClass: Class<E>, listener: YospacePlayerEventListener<E>)
}

/**
 * Reified [YospaceApi.on] so consumers can write
 * `player.yospace.on<YospacePlayerEvent.AdBreakStarted> { ... }`.
 */
inline fun <reified E : YospacePlayerEvent> YospaceApi.on(noinline action: (E) -> Unit) =
    on(E::class, action)

/**
 * Reified [YospaceApi.next].
 */
inline fun <reified E : YospacePlayerEvent> YospaceApi.next(noinline action: (E) -> Unit) =
    next(E::class, action)
