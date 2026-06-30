package com.bitmovin.player.integration.yospace

import kotlin.reflect.KClass

internal class DefaultYospaceApi(
    private val yospaceEventEmitter: YospaceEventEmitter
) : YospaceApi {
    private val javaListenerActions = mutableMapOf<JavaListenerKey, MutableList<(YospacePlayerEvent) -> Unit>>()

    override fun <E : YospacePlayerEvent> on(eventClass: KClass<E>, action: (E) -> Unit) =
        yospaceEventEmitter.on(eventClass, action)

    override fun <E : YospacePlayerEvent> next(eventClass: KClass<E>, action: (E) -> Unit) =
        yospaceEventEmitter.next(eventClass, action)

    override fun <E : YospacePlayerEvent> off(eventClass: KClass<E>, action: (E) -> Unit) =
        yospaceEventEmitter.off(eventClass, action)

    override fun <E : YospacePlayerEvent> off(action: (E) -> Unit) =
        yospaceEventEmitter.off(action)

    override fun <E : YospacePlayerEvent> on(
        eventClass: Class<E>,
        listener: YospacePlayerEventListener<E>
    ) {
        val action: (YospacePlayerEvent) -> Unit = action@{
            val event = eventClass.cast(it) ?: return@action
            listener.onEvent(event)
        }

        addJavaListenerAction(eventClass.kotlin, listener, action)
        addEmitterAction(eventClass.kotlin, action)
    }

    override fun <E : YospacePlayerEvent> next(
        eventClass: Class<E>,
        listener: YospacePlayerEventListener<E>
    ) {
        lateinit var action: (YospacePlayerEvent) -> Unit
        action = action@{
            removeJavaListenerAction(eventClass.kotlin, listener, action)
            removeEmitterAction(eventClass.kotlin, action)
            val event = eventClass.cast(it) ?: return@action
            listener.onEvent(event)
        }

        addJavaListenerAction(eventClass.kotlin, listener, action)
        addEmitterAction(eventClass.kotlin, action)
    }

    override fun <E : YospacePlayerEvent> off(
        eventClass: Class<E>,
        listener: YospacePlayerEventListener<E>
    ) {
        val action = removeLastJavaListenerAction(eventClass.kotlin, listener) ?: return
        removeEmitterAction(eventClass.kotlin, action)
    }

    private fun addJavaListenerAction(
        eventClass: KClass<out YospacePlayerEvent>,
        listener: YospacePlayerEventListener<out YospacePlayerEvent>,
        action: (YospacePlayerEvent) -> Unit
    ) {
        synchronized(javaListenerActions) {
            javaListenerActions.getOrPut(JavaListenerKey(eventClass, listener)) { mutableListOf() }.add(action)
        }
    }

    private fun removeLastJavaListenerAction(
        eventClass: KClass<out YospacePlayerEvent>,
        listener: YospacePlayerEventListener<out YospacePlayerEvent>
    ): ((YospacePlayerEvent) -> Unit)? =
        synchronized(javaListenerActions) {
            val key = JavaListenerKey(eventClass, listener)
            val actions = javaListenerActions[key] ?: return@synchronized null
            val action = actions.removeAt(actions.lastIndex)
            if (actions.isEmpty()) {
                javaListenerActions.remove(key)
            }
            action
        }

    private fun removeJavaListenerAction(
        eventClass: KClass<out YospacePlayerEvent>,
        listener: YospacePlayerEventListener<out YospacePlayerEvent>,
        action: (YospacePlayerEvent) -> Unit
    ) {
        synchronized(javaListenerActions) {
            val key = JavaListenerKey(eventClass, listener)
            val actions = javaListenerActions[key] ?: return@synchronized
            actions.remove(action)
            if (actions.isEmpty()) {
                javaListenerActions.remove(key)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : YospacePlayerEvent> addEmitterAction(
        eventClass: KClass<E>,
        action: (YospacePlayerEvent) -> Unit
    ) = yospaceEventEmitter.on(eventClass, action as (E) -> Unit)

    @Suppress("UNCHECKED_CAST")
    private fun <E : YospacePlayerEvent> removeEmitterAction(
        eventClass: KClass<E>,
        action: (YospacePlayerEvent) -> Unit
    ) = yospaceEventEmitter.off(eventClass, action as (E) -> Unit)

    private class JavaListenerKey(
        private val eventClass: KClass<out YospacePlayerEvent>,
        private val listener: YospacePlayerEventListener<out YospacePlayerEvent>
    ) {
        override fun equals(other: Any?) =
            other is JavaListenerKey && eventClass == other.eventClass && listener === other.listener

        override fun hashCode() = 31 * eventClass.hashCode() + System.identityHashCode(listener)
    }
}
