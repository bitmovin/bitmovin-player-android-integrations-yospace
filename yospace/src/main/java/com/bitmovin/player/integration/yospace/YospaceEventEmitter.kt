package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.event.data.*
import com.bitmovin.player.api.event.listener.*
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

class YospaceEventEmitter {

    private val eventListeners = ConcurrentHashMap<Class<*>, MutableList<EventListener<*>>>()

    @Synchronized
    fun addEventListener(listener: EventListener<*>) {
        getListenerClass(listener)?.let { listenerClass ->
            val listeners = eventListeners[listenerClass]
            if (listeners == null) {
                eventListeners[listenerClass] = ArrayList()
            }
            eventListeners[listenerClass]!!.add(listener)
        }
    }

    @Synchronized
    fun removeEventListener(listener: EventListener<*>) {
        getListenerClass(listener)?.let { listenerClass ->
            val listeners = eventListeners[listenerClass]
            listeners?.remove(listener)
        }
    }

    @Synchronized
    fun emit(event: BitmovinPlayerEvent) {
        when (event) {
            is AdBreakStartedEvent -> {
                eventListeners[OnAdBreakStartedListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnAdBreakStartedListener).onAdBreakStarted(event)
                    }
                }
            }
            is AdBreakFinishedEvent -> {
                eventListeners[OnAdBreakFinishedListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnAdBreakFinishedListener).onAdBreakFinished(event)
                    }
                }
            }
            is AdStartedEvent -> {
                eventListeners[OnAdStartedListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnAdStartedListener).onAdStarted(event)
                    }
                }
            }
            is AdFinishedEvent -> {
                eventListeners[OnAdFinishedListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnAdFinishedListener).onAdFinished(event)
                    }
                }
            }
            is AdClickedEvent -> {
                eventListeners[OnAdClickedListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnAdClickedListener).onAdClicked(event)
                    }
                }
            }
            is AdErrorEvent -> {
                eventListeners[OnAdErrorListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnAdErrorListener).onAdError(event)
                    }
                }
            }
            is AdSkippedEvent -> {
                eventListeners[OnAdSkippedListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnAdSkippedListener).onAdSkipped(event)
                    }
                }
            }
            is ErrorEvent -> {
                eventListeners[OnErrorListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnErrorListener).onError(event)
                    }
                }
            }
            is WarningEvent -> {
                eventListeners[OnWarningListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnWarningListener).onWarning(event)
                    }
                }
            }
            is TimeChangedEvent -> {
                eventListeners[OnTimeChangedListener::class.java]?.let { eventListeners ->
                    eventListeners.forEach { listener ->
                        (listener as OnTimeChangedListener).onTimeChanged(event)
                    }
                }
            }
        }
    }

    private fun getListenerClass(listener: EventListener<*>): Class<*>? = when (listener) {
        is OnAdBreakStartedListener -> OnAdBreakStartedListener::class.java
        is OnAdBreakFinishedListener -> OnAdBreakFinishedListener::class.java
        is OnAdClickedListener -> OnAdClickedListener::class.java
        is OnAdErrorListener -> OnAdErrorListener::class.java
        is OnAdFinishedListener -> OnAdFinishedListener::class.java
        is OnAdStartedListener -> OnAdStartedListener::class.java
        is OnAdSkippedListener -> OnAdSkippedListener::class.java
        is OnErrorListener -> OnErrorListener::class.java
        is OnWarningListener -> OnWarningListener::class.java
        is OnTimeChangedListener -> OnTimeChangedListener::class.java
        else -> null
    }
}
