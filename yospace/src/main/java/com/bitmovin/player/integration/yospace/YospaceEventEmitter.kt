package com.bitmovin.player.integration.yospace

import com.yospace.admanagement.Event as YoEvent
import com.yospace.admanagement.EventListener as YoEventListener
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/*
* Fires Yospace integration events to registered observers.
*/
class YospaceEventEmitter {
    private val eventActions =
        ConcurrentHashMap<KClass<out YospacePlayerEvent>, MutableList<(YospacePlayerEvent) -> Unit>>()
    private val yoEventListeners = ConcurrentHashMap<Class<*>, MutableList<YoEventListener<*>>>()

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
        eventActions[eventClass]?.remove(action as (YospacePlayerEvent) -> Unit)
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <E : YospacePlayerEvent> off(action: (E) -> Unit) {
        eventActions.values.forEach { it.remove(action as (YospacePlayerEvent) -> Unit) }
    }

    @Synchronized
    fun emit(event: YospacePlayerEvent) {
        BitLog.d("Emitting ${event::class.simpleName}")
        eventActions[event::class]?.toList()?.forEach { it(event) }
    }

    @Synchronized
    fun on(listener: YoEventListener<*>) {
        val listenerClass: Class<*>? = listenerClass(listener)
        listenerClass?.let {
            yoEventListeners[it]?.add(listener) ?: yoEventListeners.put(it, mutableListOf(listener))
        }
    }

    @Synchronized
    fun off(listener: YoEventListener<*>) {
        val listenerClass = listenerClass(listener)
        listenerClass?.let {
            yoEventListeners[it]?.remove(listener)
        }
    }

    @Synchronized
    // This method is defined to support Yospace SDK custom events
    fun emit(event: CustomEvent) {
        when (event) {
            is TruexAdFreeEvent -> {
                BitLog.d("Emitting TruexAdFreeEvent")
                val listeners = yoEventListeners[OnTruexAdFreeListener::class.java]
                listeners?.forEach {
                    (it as OnTruexAdFreeListener).handle(YoEvent(event))
                }
            }
            is YospaceAdStartedEvent -> {
                BitLog.d("Emitting YospaceAdStartedEvent")
                val listeners = yoEventListeners[YospaceAdStartedListener::class.java]
                listeners?.forEach {
                    (it as YospaceAdStartedListener).handle(YoEvent(event))
                }
            }
            else -> {
                BitLog.d("Emitting Unknown Custom Event: $event")
            }
        }
    }

    private fun listenerClass(listener: YoEventListener<*>): Class<*>? = when (listener) {
        is OnTruexAdFreeListener -> OnTruexAdFreeListener::class.java
        is YospaceAdStartedListener -> YospaceAdStartedListener::class.java
        else -> {
            BitLog.d("Adding undefined listener: $listener")
            null
        }
    }

    public interface OnTruexAdFreeListener : YoEventListener<TruexAdFreeEvent>
    public interface YospaceAdStartedListener : YoEventListener<CustomEvent>
}
