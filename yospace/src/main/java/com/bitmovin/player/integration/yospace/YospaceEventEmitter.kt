package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.event.data.*
import com.bitmovin.player.api.event.listener.*
import com.bitmovin.player.api.event.listener.EventListener
import java.util.concurrent.ConcurrentHashMap

/*
* Fires player events to player event observers
*/
class YospaceEventEmitter {
    private val eventListeners = ConcurrentHashMap<Class<*>, MutableList<EventListener<*>>>()

    @Synchronized
    fun addEventListener(listener: EventListener<*>) {
        val listenerClass: Class<*>? = listenerClass(listener)
        listenerClass?.let {
            eventListeners[it]?.add(listener) ?: eventListeners.put(it, mutableListOf(listener))
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
    fun emit(event: BitmovinPlayerEvent) {
        when (event) {
            is AdBreakStartedEvent -> {
                BitLog.d("Emitting AdBreakStartedEvent")
                val listeners = eventListeners[OnAdBreakStartedListener::class.java]
                listeners?.forEach {
                    (it as OnAdBreakStartedListener).onAdBreakStarted(event)
                }
            }
            is AdBreakFinishedEvent -> {
                BitLog.d("Emitting AdBreakFinishedEvent")
                val listeners = eventListeners[OnAdBreakFinishedListener::class.java]
                listeners?.forEach {
                    (it as OnAdBreakFinishedListener).onAdBreakFinished(event)
                }
            }
            is AdStartedEvent -> {
                BitLog.d("Emitting AdStartedEvent")
                val listeners = eventListeners[OnAdStartedListener::class.java]
                listeners?.forEach {
                    (it as OnAdStartedListener).onAdStarted(event)
                }
            }
            is AdFinishedEvent -> {
                BitLog.d("Emitting AdFinishedEvent")
                val listeners = eventListeners[OnAdFinishedListener::class.java]
                listeners?.forEach {
                    (it as OnAdFinishedListener).onAdFinished(event)
                }
            }
            is AdClickedEvent -> {
                val listeners = eventListeners[OnAdClickedListener::class.java]
                listeners?.forEach {
                    (it as OnAdClickedListener).onAdClicked(event)
                }
            }
            is AdErrorEvent -> {
                val listeners = eventListeners[OnAdErrorListener::class.java]
                listeners?.forEach {
                    (it as OnAdErrorListener).onAdError(event)
                }
            }
            is AdSkippedEvent -> {
                val listeners = eventListeners[OnAdSkippedListener::class.java]
                listeners?.forEach {
                    (it as OnAdSkippedListener).onAdSkipped(event)
                }
            }
            is AdQuartileEvent -> {
                val listeners = eventListeners[OnAdQuartileListener::class.java]
                listeners?.forEach {
                    (it as OnAdQuartileListener).onAdQuartile(event)
                }
            }
            is ErrorEvent -> {
                val listeners = eventListeners[OnErrorListener::class.java]
                listeners?.forEach {
                    (it as OnErrorListener).onError(event)
                }
            }
            is WarningEvent -> {
                val listeners = eventListeners[OnWarningListener::class.java]
                listeners?.forEach {
                    (it as OnWarningListener).onWarning(event)
                }
            }
            is TimeChangedEvent -> {
                val listeners = eventListeners[OnTimeChangedListener::class.java]
                listeners?.forEach {
                    (it as OnTimeChangedListener).onTimeChanged(event)
                }
            }
            is TruexAdFreeEvent -> {
                BitLog.d("Emitting TruexAdFreeEvent")
                val listeners = eventListeners[OnTruexAdFreeListener::class.java]
                listeners?.forEach {
                    (it as OnTruexAdFreeListener).onTruexAdFree(event)
                }
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
        is OnTruexAdFreeListener -> OnTruexAdFreeListener::class.java
        else -> null
    }
}
