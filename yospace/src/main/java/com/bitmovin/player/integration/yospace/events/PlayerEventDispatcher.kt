package com.bitmovin.player.integration.yospace.events

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import com.bitmovin.player.api.event.PlayerEvent
import kotlin.reflect.KClass

/**
 * Routes player event registrations either to the [AdjustedTimeChangedDispatcher] (for
 * [PlayerEvent.TimeChanged], which is adjusted for Yospace ad time) or straight to the underlying
 * [Player]. Keeps the routing logic out of [com.bitmovin.player.integration.yospace.BitmovinYospacePlayer]
 * so it can delegate registration wholesale.
 */
internal class PlayerEventDispatcher(
    private val player: Player,
    currentTime: () -> Double
) {
    private val adjustedTimeChanged = AdjustedTimeChangedDispatcher(player, currentTime)

    fun <E : Event> on(eventClass: KClass<E>, action: (E) -> Unit) {
        if (eventClass == PlayerEvent.TimeChanged::class) {
            adjustedTimeChanged.on(eventClass, action)
        } else {
            player.on(eventClass, action)
        }
    }

    fun <E : Event> next(eventClass: KClass<E>, action: (E) -> Unit) {
        if (eventClass == PlayerEvent.TimeChanged::class) {
            adjustedTimeChanged.next(eventClass, action)
        } else {
            player.next(eventClass, action)
        }
    }

    fun <E : Event> off(eventClass: KClass<E>, action: (E) -> Unit) {
        if (eventClass == PlayerEvent.TimeChanged::class) {
            adjustedTimeChanged.off(eventClass, action)
        } else {
            player.off(eventClass, action)
        }
    }

    fun <E : Event> off(action: (E) -> Unit) {
        adjustedTimeChanged.off(action)
        player.off(action)
    }

    fun <E : Event> on(eventClass: Class<E>, eventListener: EventListener<in E>) {
        if (eventClass == PlayerEvent.TimeChanged::class.java) {
            adjustedTimeChanged.on(eventClass, eventListener)
        } else {
            player.on(eventClass, eventListener)
        }
    }

    fun <E : Event> next(eventClass: Class<E>, eventListener: EventListener<in E>) {
        if (eventClass == PlayerEvent.TimeChanged::class.java) {
            adjustedTimeChanged.next(eventClass, eventListener)
        } else {
            player.next(eventClass, eventListener)
        }
    }

    fun <E : Event> off(eventClass: Class<E>, eventListener: EventListener<in E>) {
        if (eventClass == PlayerEvent.TimeChanged::class.java) {
            adjustedTimeChanged.off(eventClass, eventListener)
        } else {
            player.off(eventClass, eventListener)
        }
    }

    fun <E : Event> off(eventListener: EventListener<in E>) {
        adjustedTimeChanged.off(eventListener)
        player.off(eventListener)
    }
}
