package magi.aenerv7.ppembytv.data.model

import java.util.Locale

/** Resolves the display label of a source's video resolution. */
fun buildVideoResolutionLabel(
    source: MediaSource?,
    labelStyle: ResolutionLabelStyle = ResolutionLabelStyle.UPPERCASE,
): String? {
    val mediaStream = source?.mediaStreams?.firstOrNull { it.type == "Video" }
    val width = mediaStream?.width ?: 0
    val height = mediaStream?.height ?: 0
    val resolved = resolveVideoResolution(width, height)
    if (resolved != null) return resolved.label(labelStyle)
    if (height <= 0) {
        if (width > 0) return "SD"
        val parsed = parseVideoResolution(source?.name, source?.path)
        if (parsed != null) return parsed.label(labelStyle)
        return null
    }
    return if (labelStyle == ResolutionLabelStyle.UPPERCASE) "${height}P" else "${height}p"
}

/** Parses a resolution from the combined name/path text, e.g. "2160p", "4k", "fhd". */
fun parseVideoResolution(name: String?, path: String?): VideoResolution? {
    val lower = ((name ?: "") + " " + (path ?: "")).lowercase(Locale.ROOT)
    val containsHd = Regex("(^|[^a-z0-9])hd([^a-z0-9]|$)").containsMatchIn(lower)
    return when {
        lower.contains("2160p") || lower.contains("4k") || lower.contains("uhd") -> VideoResolution.UHD_4K
        lower.contains("1440p") || lower.contains("2k") || lower.contains("qhd") -> VideoResolution.QHD_2K
        lower.contains("1080p") || lower.contains("1080i") || lower.contains("fhd") -> VideoResolution.FULL_HD_1080
        lower.contains("720p") || lower.contains("720i") || containsHd -> VideoResolution.HD_720
        lower.contains("480p") || lower.contains("480i") || lower.contains("sd") -> VideoResolution.SD_480
        else -> null
    }
}

/** Maps a width/height pair to the closest known resolution class, or null when too small. */
fun resolveVideoResolution(width: Int?, height: Int?): VideoResolution? {
    val w = width ?: 0
    val h = height ?: 0
    val max = maxOf(w, h)
    val min = minOf(w, h)
    return when {
        max >= 3800 || min >= 2160 -> VideoResolution.UHD_4K
        max >= 2500 || min >= 1440 -> VideoResolution.QHD_2K
        max >= 1900 || min >= 1080 -> VideoResolution.FULL_HD_1080
        max >= 1200 || min >= 720 -> VideoResolution.HD_720
        max >= 800 || min >= 480 -> VideoResolution.SD_480
        else -> null
    }
}

/** Numeric sort key derived from a resolution label, e.g. "1080p" -> 1080. */
fun resolveVideoResolutionLabelSortValue(label: String?): Int {
    val lower = label?.trim()?.lowercase(Locale.ROOT) ?: ""
    return when (lower) {
        "sd" -> 360
        "4k" -> 2160
        "2k" -> 1440
        else -> Regex("(\\d{3,4})p").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }
}

/** Numeric sort key for a source's video resolution (higher = better). */
fun resolveVideoResolutionSortValue(source: MediaSource?): Int {
    val mediaStream = source?.mediaStreams?.firstOrNull { it.type == "Video" }
    val width = mediaStream?.width ?: 0
    val height = mediaStream?.height ?: 0
    val resolved = resolveVideoResolution(width, height)
    if (resolved != null) return resolved.sortValue
    if (height > 0) return height
    if (width > 0) return 360
    val parsed = parseVideoResolution(source?.name, source?.path)
    return parsed?.sortValue ?: 0
}
