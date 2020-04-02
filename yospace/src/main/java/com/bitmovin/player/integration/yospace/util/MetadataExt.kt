package com.bitmovin.player.integration.yospace.util

import com.bitmovin.player.api.event.data.MetadataEvent
import com.bitmovin.player.integration.yospace.BitLog
import com.bitmovin.player.model.emsg.EventMessage
import com.bitmovin.player.model.id3.BinaryFrame
import com.yospace.hls.TimedMetadata

fun MetadataEvent.createTimedMetadata(): TimedMetadata? = when {
    type === "EMSG" -> convertEmsgToId3()
    type === "ID3" -> processId3()
    else -> null
}

private fun MetadataEvent.processId3(): TimedMetadata? {
    var ymid: String? = null
    var yseq: String? = null
    var ytyp: String? = null
    var ydur: String? = null
    var yprg: String? = null

    for (i in 0 until metadata.length()) {
        val entry = metadata.get(i)
        if (entry is BinaryFrame) {
            when (entry.id) {
                "YMID" -> ymid = String(entry.data)
                "YSEQ" -> yseq = String(entry.data)
                "YTYP" -> ytyp = String(entry.data)
                "YDUR" -> ydur = String(entry.data)
                "YPRG" -> yprg = String(entry.data)
            }
        }
    }

    return generateTimedMetadata(ymid, yseq, ytyp, ydur, yprg)
}

private fun MetadataEvent.convertEmsgToId3(): TimedMetadata? {
    var ymid: String? = null
    var yseq: String? = null
    var ytyp: String? = null
    var ydur: String? = null
    var yprg: String? = null

    for (i in 0 until metadata.length()) {
        val message = metadata.get(i) as EventMessage
        val data = String(message.messageData).split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (j in data.indices) {
            val entry = data[j].split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (entry.size > 1) {
                val key = entry[0]
                val value = entry[1]
                BitLog.d("Key: $key, value: $value")
                when (key) {
                    "YMID" -> ymid = value
                    "YSEQ" -> yseq = value
                    "YTYP" -> ytyp = value
                    "YDUR" -> ydur = value
                    "YPRG" -> yprg = value
                }
            }
        }
    }

    return generateTimedMetadata(ymid, yseq, ytyp, ydur, yprg)
}

private fun generateTimedMetadata(ymid: String?, yseq: String?, ytyp: String?, ydur: String?, yprg: String?): TimedMetadata? {
    if (ymid != null && yseq != null && ytyp != null && ydur != null) {
        return TimedMetadata.createFromMetadata(ymid, yseq, ytyp, ydur)
    } else if (yprg != null) {
        return TimedMetadata.createFromMetadata(yprg, 0.0f)
    }
    return null
}
