package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.deficiency.ErrorCode
import com.bitmovin.player.api.deficiency.WarningCode

/**
 * 6000 - 6999: Yospace-related error codes
 * - 6001: Invalid Yospace Source
 * - 6002: Session No Analytics
 * - 6003: Session Not Initialised
 */
enum class YospaceErrorCode(override val value: Int) : ErrorCode {
    InvalidYospaceSourceE(6001),
    SessionNoAnalytics(6002),
    SessionNotInitialised(6003),
    UnsupportedAPI(6004);

    companion object {
        private val map by lazy { YospaceErrorCode.values().associateBy(YospaceErrorCode::value) }

        @JvmStatic
        fun fromValue(code: Int): YospaceErrorCode? = map[code]
    }
}

/**
 * 6000 - 6999: Yospace-related error codes
 * - 6004: Unsupported API
 */
enum class YospaceWarningCode(override val value: Int) : WarningCode {
    UnsupportedAPI(6004);

    companion object {
        private val map by lazy { YospaceWarningCode.values().associateBy(YospaceWarningCode::value) }

        @JvmStatic
        fun fromValue(code: Int): YospaceWarningCode? = map[code]
    }
}