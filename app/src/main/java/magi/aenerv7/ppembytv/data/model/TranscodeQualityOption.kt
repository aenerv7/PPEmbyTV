package magi.aenerv7.ppembytv.data.model

import kotlin.math.ceil

/**
 * A selectable transcode-quality preset: a display [name], a target [bitrate]
 * and optional max dimensions. When [maxWidth] is omitted it defaults to the
 * 16:9 width derived from [maxHeight] (e.g. 2160 -> 3840).
 */
data class TranscodeQualityOption(
    val name: String,
    val bitrate: Long,
    val maxHeight: Int? = null,
    val maxWidth: Int? = if (maxHeight != null) ceil(maxHeight * 16.0 / 9.0).toInt() else null,
    val isAuto: Boolean = false,
    val disablesTranscoding: Boolean = false,
)
