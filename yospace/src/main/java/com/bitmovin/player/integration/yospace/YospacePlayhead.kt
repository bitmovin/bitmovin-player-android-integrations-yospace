package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.Player
import com.yospace.admanagement.TimedMetadata
import kotlin.math.roundToLong

/**
 * Yospace expects milliseconds from the fixed timeline origin where the first media segment
 * was available to this session. This anchors ad-break matching and analytics for DVR live.
 *
 * Returns a [Long] because live wall-clock times in milliseconds exceed [Int.MAX_VALUE].
 */
internal fun Player.yospacePlayheadMs(): Long =
    ((currentTime + playbackTimeOffsetToRelativeTime) * 1000)
        .roundToLong()
        .coerceAtLeast(0)

/**
 * Yospace times live quartiles from the playhead difference between metadata events.
 */
internal fun Player.createYospaceTimedMetadata(
    ymid: String?,
    yseq: String?,
    ytyp: String?,
    ydur: String?,
): TimedMetadata? = when {
    ymid != null && yseq != null && ytyp != null && ydur != null ->
        TimedMetadata.createFromMetadata(ymid, yseq, ytyp, ydur, yospacePlayheadMs())
    else -> null
}
