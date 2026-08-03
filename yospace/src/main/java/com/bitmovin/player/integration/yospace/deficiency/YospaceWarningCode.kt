package com.bitmovin.player.integration.yospace.deficiency

import com.bitmovin.player.api.deficiency.WarningCode

/**
 * 6000 - 6999: Yospace-related warning codes
 * - 6004: Unsupported API
 * - 6005: No Analytics
 * - 6006: Session initialization issue
 * - 6007: Session analytics issue
 * - 6008: Analytics configuration ignored
 */
enum class YospaceWarningCode(override val value: Int) : WarningCode {
    UnsupportedAPI(6004),
    NoAnalytics(6005),
    SessionInitializationIssue(6006),
    SessionAnalyticsIssue(6007),
    AnalyticsConfigIgnored(6008);

    companion object {
        private val map by lazy { YospaceWarningCode.values().associateBy(YospaceWarningCode::value) }

        @JvmStatic
        fun fromValue(code: Int): YospaceWarningCode? = map[code]
    }
}
