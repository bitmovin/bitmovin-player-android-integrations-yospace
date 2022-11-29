package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.ErrorEvent
import com.bitmovin.player.api.deficiency.WarningEvent
import com.bitmovin.player.api.source.Source

/**
 * Includes all possible custom events the [Player] or [Source] can emit.
 */
open class CustomEvent {
    /**
     * The time at which the event was emitted in milliseconds since the Unix Epoch.
     */
    var timestamp: Long = System.currentTimeMillis() / 1000
}
/**
 * Includes all possible events that the [Source] can emit.
 */
open class CustomSourceEvent : CustomEvent() {
    /**
     * Emitted when a source error occurred.
     */
    data class Error(
        /**
         * The error code used to identify the source error.
         */
        override val code: YospaceErrorCode,
        /**
         * The error message to explain the reason for the source error.
         */
        override val message: String,
        /**
         * Potential additional data.
         */
        override val data: Any? = null
    ) : ErrorEvent, CustomSourceEvent()

    /**
     * Emitted when a source warning occurred.
     */
    data class Warning(
        /**
         * The warning code used to identify the occurred source warning.
         */
        override val code: YospaceWarningCode,
        /**
         * The warning message to explain the reason for the source warning.
         */
        override val message: String
    ) : WarningEvent, CustomSourceEvent()

}