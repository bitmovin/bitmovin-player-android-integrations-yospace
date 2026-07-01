package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import com.bitmovin.player.api.event.PlayerEvent
import kotlin.reflect.KClass

internal class AdjustedTimeChangedDispatcher(
    private val player: Player,
    private val currentTime: () -> Double
) {
    private val kotlinActions =
        ListenerActionRegistry<EventActionKey, (PlayerEvent.TimeChanged) -> Unit>()
    private val javaActions =
        ListenerActionRegistry<JavaEventListenerKey, (PlayerEvent.TimeChanged) -> Unit>()

    fun <E : Event> on(eventClass: KClass<E>, action: (E) -> Unit) {
        val wrappedAction = adjustedAction(action)
        kotlinActions.add(EventActionKey(eventClass, action), wrappedAction)
        player.on(PlayerEvent.TimeChanged::class, wrappedAction)
    }

    fun <E : Event> next(eventClass: KClass<E>, action: (E) -> Unit) {
        lateinit var wrappedAction: (PlayerEvent.TimeChanged) -> Unit
        wrappedAction = {
            kotlinActions.remove(EventActionKey(eventClass, action), wrappedAction)
            player.off(PlayerEvent.TimeChanged::class, wrappedAction)
            emitAdjusted(action)
        }
        kotlinActions.add(EventActionKey(eventClass, action), wrappedAction)
        player.on(PlayerEvent.TimeChanged::class, wrappedAction)
    }

    fun <E : Event> off(eventClass: KClass<E>, action: (E) -> Unit) {
        kotlinActions.removeLast(EventActionKey(eventClass, action))?.let {
            player.off(PlayerEvent.TimeChanged::class, it)
        }
    }

    fun <E : Event> off(action: (E) -> Unit) {
        kotlinActions.removeAll { it.matchesAction(action) }.forEach {
            player.off(PlayerEvent.TimeChanged::class, it)
        }
    }

    fun <E : Event> on(eventClass: Class<E>, eventListener: EventListener<in E>) {
        val wrappedAction = adjustedAction(eventListener)
        javaActions.add(JavaEventListenerKey(eventClass, eventListener), wrappedAction)
        player.on(PlayerEvent.TimeChanged::class, wrappedAction)
    }

    fun <E : Event> next(eventClass: Class<E>, eventListener: EventListener<in E>) {
        lateinit var wrappedAction: (PlayerEvent.TimeChanged) -> Unit
        wrappedAction = {
            javaActions.remove(JavaEventListenerKey(eventClass, eventListener), wrappedAction)
            player.off(PlayerEvent.TimeChanged::class, wrappedAction)
            emitAdjusted(eventListener)
        }
        javaActions.add(JavaEventListenerKey(eventClass, eventListener), wrappedAction)
        player.on(PlayerEvent.TimeChanged::class, wrappedAction)
    }

    fun <E : Event> off(eventClass: Class<E>, eventListener: EventListener<in E>) {
        javaActions.removeLast(JavaEventListenerKey(eventClass, eventListener))?.let {
            player.off(PlayerEvent.TimeChanged::class, it)
        }
    }

    fun <E : Event> off(eventListener: EventListener<in E>) {
        javaActions.removeAll { it.matchesListener(eventListener) }.forEach {
            player.off(PlayerEvent.TimeChanged::class, it)
        }
    }

    private fun <E : Event> adjustedAction(action: (E) -> Unit): (PlayerEvent.TimeChanged) -> Unit =
        { emitAdjusted(action) }

    @Suppress("UNCHECKED_CAST")
    private fun <E : Event> emitAdjusted(action: (E) -> Unit) {
        action(PlayerEvent.TimeChanged(currentTime()) as E)
    }

    private fun <E : Event> adjustedAction(
        eventListener: EventListener<in E>
    ): (PlayerEvent.TimeChanged) -> Unit = {
        emitAdjusted(eventListener)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : Event> emitAdjusted(eventListener: EventListener<in E>) {
        eventListener.onEvent(PlayerEvent.TimeChanged(currentTime()) as E)
    }
}

private class EventActionKey(
    private val eventClass: KClass<out Event>,
    private val action: Any
) {
    fun matchesAction(action: Any) = this.action === action

    override fun equals(other: Any?) =
        other is EventActionKey && eventClass == other.eventClass && action === other.action

    override fun hashCode() = 31 * eventClass.hashCode() + System.identityHashCode(action)
}

private class JavaEventListenerKey(
    private val eventClass: Class<out Event>,
    private val eventListener: Any
) {
    fun matchesListener(eventListener: Any) = this.eventListener === eventListener

    override fun equals(other: Any?) =
        other is JavaEventListenerKey && eventClass == other.eventClass && eventListener === other.eventListener

    override fun hashCode() = 31 * eventClass.hashCode() + System.identityHashCode(eventListener)
}
