package com.bitmovin.player.integration.yospace

import kotlin.reflect.KClass

/**
 * Subscribes [action] to Yospace integration events of type [eventClass].
 */
fun <E : YospacePlayerEvent> BitmovinYospacePlayer.on(eventClass: KClass<E>, action: (E) -> Unit) =
    onYospacePlayerEvent(eventClass, action)

/**
 * Subscribes [action] to the next Yospace integration event of type [eventClass], then unsubscribes
 * automatically.
 */
fun <E : YospacePlayerEvent> BitmovinYospacePlayer.next(eventClass: KClass<E>, action: (E) -> Unit) =
    nextYospacePlayerEvent(eventClass, action)

/**
 * Unsubscribes [action] from Yospace integration events of type [eventClass].
 */
fun <E : YospacePlayerEvent> BitmovinYospacePlayer.off(eventClass: KClass<E>, action: (E) -> Unit) =
    offYospacePlayerEvent(eventClass, action)

/**
 * Reified [on] so consumers can write
 * `player.on<YospacePlayerEvent.AdBreakStarted> { ... }`.
 */
inline fun <reified E : YospacePlayerEvent> BitmovinYospacePlayer.on(noinline action: (E) -> Unit) =
    on(E::class, action)

/**
 * Reified [next].
 */
inline fun <reified E : YospacePlayerEvent> BitmovinYospacePlayer.next(noinline action: (E) -> Unit) =
    next(E::class, action)
