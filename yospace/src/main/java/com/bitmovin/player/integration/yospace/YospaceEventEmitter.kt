package com.bitmovin.player.integration.yospace

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/*
* Fires Yospace integration events to registered observers.
*/
class YospaceEventEmitter {
    private val eventActions =
        ConcurrentHashMap<KClass<out YospacePlayerEvent>, MutableList<(YospacePlayerEvent) -> Unit>>()

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <E : YospacePlayerEvent> on(eventClass: KClass<E>, action: (E) -> Unit) {
        eventActions[eventClass]?.add(action as (YospacePlayerEvent) -> Unit)
            ?: eventActions.put(eventClass, mutableListOf(action as (YospacePlayerEvent) -> Unit))
    }

    @Synchronized
    fun <E : YospacePlayerEvent> next(eventClass: KClass<E>, action: (E) -> Unit) {
        lateinit var wrappedAction: (E) -> Unit
        wrappedAction = {
            off(eventClass, wrappedAction)
            action(it)
        }

        on(eventClass, wrappedAction)
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <E : YospacePlayerEvent> off(eventClass: KClass<E>, action: (E) -> Unit) {
        eventActions[eventClass]?.let { actions ->
            actions.remove(action as (YospacePlayerEvent) -> Unit)
            if (actions.isEmpty()) {
                eventActions.remove(eventClass)
            }
        }
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <E : YospacePlayerEvent> off(action: (E) -> Unit) {
        eventActions.forEach { (eventClass, actions) ->
            actions.remove(action as (YospacePlayerEvent) -> Unit)
            if (actions.isEmpty()) {
                eventActions.remove(eventClass, actions)
            }
        }
    }

    fun emit(event: YospacePlayerEvent) {
        BitLog.d("Emitting ${event::class.simpleName}")
        val actions = synchronized(this) {
            eventActions[event::class]?.toList().orEmpty()
        }
        actions.forEach { it(event) }
    }
}
