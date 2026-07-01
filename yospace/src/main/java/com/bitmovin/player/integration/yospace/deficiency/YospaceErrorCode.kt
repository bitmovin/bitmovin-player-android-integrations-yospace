package com.bitmovin.player.integration.yospace.deficiency

import com.bitmovin.player.api.deficiency.ErrorCode

/**
 * 6000 - 6999: Yospace-related error codes
 * - 6001: Invalid Yospace Source
 * - 6002: Session No Analytics
 * - 6003: Session Not Initialised
 */
enum class YospaceErrorCode(override val value: Int) : ErrorCode {
    InvalidYospaceSource(6001),
    SessionNoAnalytics(6002),
    SessionNotInitialised(6003);

    companion object {
        private val map by lazy { YospaceErrorCode.values().associateBy(YospaceErrorCode::value) }

        @JvmStatic
        fun fromValue(code: Int): YospaceErrorCode? = map[code]
    }
}
