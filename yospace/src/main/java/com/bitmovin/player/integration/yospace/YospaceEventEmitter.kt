package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.event.data.*
import com.bitmovin.player.api.event.listener.*
import com.bitmovin.player.api.event.listener.EventListener

class YospaceEventEmitter {
    private val eventListeners = emptyMap<Class<*>, MutableList<EventListener<*>>>()

    @Synchronized
    fun addEventListener(listener: EventListener<*>) {
        val listenerClass = listenerClass(listener)
        listenerClass?.let {
            eventListeners[listenerClass].orEmpty().toMutableList().add(listener)
        }
    }

    @Synchronized
    fun removeEventListener(listener: EventListener<*>) {
        val listenerClass = listenerClass(listener)
        listenerClass?.let {
            eventListeners[listenerClass]?.remove(listener)
        }
    }

    @Synchronized
    fun emit(event: BitmovinPlayerEvent) {
        BitLog.d("Emitting ${event.javaClass.simpleName}")
        when (event) {
            is AdBreakStartedEvent -> {
                val listeners = eventListeners[OnAdBreakStartedListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnAdBreakStartedListener>().forEach {
                        it.onAdBreakStarted(event)
                    }
                }
            }
            is AdBreakFinishedEvent -> {
                val listeners = eventListeners[OnAdBreakFinishedListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnAdBreakFinishedListener>().forEach {
                        it.onAdBreakFinished(event)
                    }
                }
            }
            is AdStartedEvent -> {
                val listeners = eventListeners[OnAdStartedListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnAdStartedListener>().forEach {
                        it.onAdStarted(event)
                    }
                }
            }
            is AdFinishedEvent -> {
                val listeners = eventListeners[OnAdFinishedListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnAdFinishedListener>().forEach {
                        it.onAdFinished(event)
                    }
                }
            }
            is AdClickedEvent -> {
                val listeners = eventListeners[OnAdClickedListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnAdClickedListener>().forEach {
                        it.onAdClicked(event)
                    }
                }
            }
            is AdErrorEvent -> {
                val listeners = eventListeners[OnAdErrorListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnAdErrorListener>().forEach {
                        it.onAdError(event)
                    }
                }
            }
            is AdSkippedEvent -> {
                val listeners = eventListeners[OnAdSkippedListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnAdSkippedListener>().forEach {
                        it.onAdSkipped(event)
                    }
                }
            }
            is ErrorEvent -> {
                val listeners = eventListeners[OnErrorListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnErrorListener>().forEach {
                        it.onError(event)
                    }
                }
            }
            is WarningEvent -> {
                val listeners = eventListeners[OnWarningListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnWarningListener>().forEach {
                        it.onWarning(event)
                    }
                }
            }
            is TimeChangedEvent -> {
                val listeners = eventListeners[OnTimeChangedListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnTimeChangedListener>().forEach {
                        it.onTimeChanged(event)
                    }
                }
            }
            is TruexAdFreeEvent -> {
                val listeners = eventListeners[OnTruexAdFreeListener::class.java]
                listeners?.let {
                    listeners.filterIsInstance<OnTruexAdFreeListener>().forEach {
                        it.onTruexAdFree(event)
                    }
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
        is OnErrorListener -> OnErrorListener::class.java
        is OnWarningListener -> OnWarningListener::class.java
        is OnTimeChangedListener -> OnTimeChangedListener::class.java
        is OnTruexAdFreeListener -> OnTruexAdFreeListener::class.java
        else -> null
    }
}
