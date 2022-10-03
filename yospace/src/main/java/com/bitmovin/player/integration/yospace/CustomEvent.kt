package com.bitmovin.player.integration.yospace

/**
 * Includes all possible custom events the [Player] or [Source] can emit.
 */
open class CustomEvent {
    /**
     * The time at which the event was emitted in milliseconds since the Unix Epoch.
     */
    var timestamp: Long = System.currentTimeMillis()
}