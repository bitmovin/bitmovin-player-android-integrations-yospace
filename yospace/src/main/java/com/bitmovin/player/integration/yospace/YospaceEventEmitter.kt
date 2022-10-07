package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.PlayerEvent.*
import com.yospace.admanagement.Event as YoEvent
import com.yospace.admanagement.EventListener as YoEventListener
import java.util.concurrent.ConcurrentHashMap

class YospaceEventEmitter {
    private val eventListeners = ConcurrentHashMap<Class<*>, MutableList<EventListener<*>>>()
    private val yoEventListeners = ConcurrentHashMap<Class<*>, MutableList<YoEventListener<*>>>()

    @Synchronized
    fun addEventListener(listener: EventListener<*>) {
        val listenerClass: Class<*>? = listenerClass(listener)
        listenerClass?.let {
            eventListeners[it]?.add(listener) ?: eventListeners.put(it, mutableListOf(listener))
        }
    }

    @Synchronized
    fun addEventListener(listener: YoEventListener<*>) {
        val listenerClass: Class<*>? = listenerClass(listener)
        listenerClass?.let {
            yoEventListeners[it]?.add(listener) ?: yoEventListeners.put(it, mutableListOf(listener))
        }
    }

    @Synchronized
    fun removeEventListener(listener: EventListener<*>) {
        val listenerClass = listenerClass(listener)
        listenerClass?.let {
            eventListeners[it]?.remove(listener)
        }
    }

    @Synchronized
    fun removeEventListener(listener: YoEventListener<*>) {
        val listenerClass = listenerClass(listener)
        listenerClass?.let {
            yoEventListeners[it]?.remove(listener)
        }
    }

    @Synchronized
    fun emit(event: Event) {
        when (event) {
            is AdBreakStarted -> {
                BitLog.d("Emitting AdBreakStartedEvent")
                val listeners = eventListeners[OnAdBreakStartedListener::class.java]
                listeners?.forEach {
                    (it as OnAdBreakStartedListener).onEvent(event)
                }
            }
            is AdBreakFinished -> {
                BitLog.d("Emitting AdBreakFinishedEvent")
                val listeners = eventListeners[OnAdBreakFinishedListener::class.java]
                listeners?.forEach {
                    (it as OnAdBreakFinishedListener).onEvent(event)
                }
            }
            is AdStarted -> {
                BitLog.d("Emitting AdStartedEvent")
                val listeners = eventListeners[OnAdStartedListener::class.java]
                listeners?.forEach {
                    (it as OnAdStartedListener).onEvent(event)
                }
            }
            is AdFinished -> {
                BitLog.d("Emitting AdFinishedEvent")
                val listeners = eventListeners[OnAdFinishedListener::class.java]
                listeners?.forEach {
                    (it as OnAdFinishedListener).onEvent(event)
                }
            }
            is AdClicked -> {
                val listeners = eventListeners[OnAdClickedListener::class.java]
                listeners?.forEach {
                    (it as OnAdClickedListener).onEvent(event)
                }
            }
            is AdError -> {
                val listeners = eventListeners[OnAdErrorListener::class.java]
                listeners?.forEach {
                    (it as OnAdErrorListener).onEvent(event)
                }
            }
            is AdSkipped -> {
                val listeners = eventListeners[OnAdSkippedListener::class.java]
                listeners?.forEach {
                    (it as OnAdSkippedListener).onEvent(event)
                }
            }
            is AdQuartile -> {
                val listeners = eventListeners[OnAdQuartileListener::class.java]
                listeners?.forEach {
                    (it as OnAdQuartileListener).onEvent(event)
                }
            }
            is Error -> {
                val listeners = eventListeners[OnErrorListener::class.java]
                listeners?.forEach {
                    (it as OnErrorListener).onEvent(event)
                }
            }
            is Warning -> {
                val listeners = eventListeners[OnWarningListener::class.java]
                listeners?.forEach {
                    (it as OnWarningListener).onEvent(event)
                }
            }
            is TimeChanged -> {
                val listeners = eventListeners[OnTimeChangedListener::class.java]
                listeners?.forEach {
                    (it as OnTimeChangedListener).onEvent(event)
                }
            }
            else -> {
                BitLog.d("Emitting Unknown Event: $event")
            }
        }
    }

    @Synchronized
    // This method is defined to support custom events
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

    private fun listenerClass(listener: EventListener<*>): Class<*>? = when (listener) {
        is OnAdBreakStartedListener -> OnAdBreakStartedListener::class.java
        is OnAdBreakFinishedListener -> OnAdBreakFinishedListener::class.java
        is OnAdClickedListener -> OnAdClickedListener::class.java
        is OnAdErrorListener -> OnAdErrorListener::class.java
        is OnAdFinishedListener -> OnAdFinishedListener::class.java
        is OnAdStartedListener -> OnAdStartedListener::class.java
        is OnAdSkippedListener -> OnAdSkippedListener::class.java
        is OnAdQuartileListener -> OnAdQuartileListener::class.java
        is OnErrorListener -> OnErrorListener::class.java
        is OnWarningListener -> OnWarningListener::class.java
        is OnTimeChangedListener -> OnTimeChangedListener::class.java
        else -> {
            BitLog.d("Adding undefined listener: $listener")
            null
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

    // define interfaces with their legacy name
    public interface OnAdBreakStartedListener : EventListener<PlayerEvent.AdBreakStarted>
    public interface OnAdBreakFinishedListener : EventListener<PlayerEvent.AdBreakFinished>
    public interface OnAdClickedListener : EventListener<PlayerEvent.AdClicked>
    public interface OnAdErrorListener : EventListener<PlayerEvent.AdError>
    public interface OnAdFinishedListener : EventListener<PlayerEvent.AdFinished>
    public interface OnAdStartedListener : EventListener<PlayerEvent.AdStarted>
    public interface OnAdSkippedListener : EventListener<PlayerEvent.AdSkipped>
    public interface OnAdQuartileListener : EventListener<PlayerEvent.AdQuartile>
    public interface OnErrorListener : EventListener<PlayerEvent.Error>
    public interface OnWarningListener : EventListener<PlayerEvent.Warning>
    public interface OnTimeChangedListener : EventListener<PlayerEvent.TimeChanged>
    public interface OnTruexAdFreeListener : YoEventListener<TruexAdFreeEvent>
    public interface YospaceAdStartedListener : YoEventListener<CustomEvent>
}
