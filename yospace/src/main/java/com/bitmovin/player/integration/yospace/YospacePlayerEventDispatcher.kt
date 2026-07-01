package com.bitmovin.player.integration.yospace

import kotlin.reflect.KClass

internal class YospacePlayerEventDispatcher(
    private val emitter: YospaceEventEmitter
) {
    private val javaActions =
        ListenerActionRegistry<YospaceJavaListenerKey, (YospacePlayerEvent) -> Unit>()

    fun <E : YospacePlayerEvent> on(eventClass: KClass<E>, action: (E) -> Unit) =
        emitter.on(eventClass, action)

    fun <E : YospacePlayerEvent> next(eventClass: KClass<E>, action: (E) -> Unit) =
        emitter.next(eventClass, action)

    fun <E : YospacePlayerEvent> off(eventClass: KClass<E>, action: (E) -> Unit) =
        emitter.off(eventClass, action)

    fun <E : YospacePlayerEvent> on(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) {
        val action = eventAction(eventClass, listener)
        javaActions.add(YospaceJavaListenerKey(eventClass, listener), action)
        addEmitterAction(eventClass.kotlin, action)
    }

    fun <E : YospacePlayerEvent> next(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) {
        lateinit var action: (YospacePlayerEvent) -> Unit
        action = action@{
            val event = castEvent(eventClass, it) ?: return@action
            javaActions.remove(YospaceJavaListenerKey(eventClass, listener), action)
            removeEmitterAction(eventClass.kotlin, action)
            listener.onEvent(event)
        }

        javaActions.add(YospaceJavaListenerKey(eventClass, listener), action)
        addEmitterAction(eventClass.kotlin, action)
    }

    fun <E : YospacePlayerEvent> off(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) {
        val action = javaActions.removeLast(YospaceJavaListenerKey(eventClass, listener)) ?: return
        removeEmitterAction(eventClass.kotlin, action)
    }

    private fun <E : YospacePlayerEvent> eventAction(
        eventClass: Class<E>,
        listener: YospacePlayerEventListener<E>
    ): (YospacePlayerEvent) -> Unit = action@{
        val event = castEvent(eventClass, it) ?: return@action
        listener.onEvent(event)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : YospacePlayerEvent> castEvent(
        eventClass: Class<E>,
        event: YospacePlayerEvent
    ): E? = if (eventClass.isInstance(event)) event as E else null

    @Suppress("UNCHECKED_CAST")
    private fun <E : YospacePlayerEvent> addEmitterAction(
        eventClass: KClass<E>,
        action: (YospacePlayerEvent) -> Unit
    ) = emitter.on(eventClass, action as (E) -> Unit)

    @Suppress("UNCHECKED_CAST")
    private fun <E : YospacePlayerEvent> removeEmitterAction(
        eventClass: KClass<E>,
        action: (YospacePlayerEvent) -> Unit
    ) = emitter.off(eventClass, action as (E) -> Unit)
}

private class YospaceJavaListenerKey(
    private val eventClass: Class<out YospacePlayerEvent>,
    private val listener: YospacePlayerEventListener<out YospacePlayerEvent>
) {
    override fun equals(other: Any?) =
        other is YospaceJavaListenerKey && eventClass == other.eventClass && listener === other.listener

    override fun hashCode() = 31 * eventClass.hashCode() + System.identityHashCode(listener)
}
