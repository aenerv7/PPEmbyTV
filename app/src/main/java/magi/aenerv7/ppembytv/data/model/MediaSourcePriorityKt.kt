package magi.aenerv7.ppembytv.data.model

import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.Locale

/**
 * Core "which media source to play / how to order versions" logic.
 *
 * Ported from the obfuscated `MediaSourcePriorityKt`:
 *  - version picker merging and priority sorting,
 *  - dynamic-range / video-type label building,
 *  - best-source selection for aggregate result sorting,
 *  - comparison helpers used by the aggregate-results screen.
 */

// ---------------------------------------------------------------------------
// Dynamic range / video type labels
// ---------------------------------------------------------------------------

/** Resolves a short dynamic-range label ("DV", "DV(8)", "HDR10+", "HDR10", "HDR", "SDR"). */
fun buildDynamicRangeLabel(mediaSource: MediaSource?): String? {
    val videoStream = mediaSource?.mediaStreams?.firstOrNull { it.type == "Video" }
    val doviProfileText = listOfNotNull(
        videoStream?.extendedVideoSubType,
        videoStream?.extendedVideoSubTypeDescription,
    ).joinToString(" ").lowercase(Locale.ROOT)
    val doviProfile = parseDolbyVisionProfile(doviProfileText)
    val rangeText = listOfNotNull(
        mediaSource?.videoType,
        videoStream?.videoRangeType,
        videoStream?.videoRange,
        videoStream?.extendedVideoType,
        videoStream?.extendedVideoSubType,
        videoStream?.extendedVideoSubTypeDescription,
        videoStream?.displayTitle,
        videoStream?.title,
        videoStream?.colorTransfer,
        videoStream?.colorSpace,
        videoStream?.colorPrimaries,
    ).joinToString(" ").lowercase(Locale.ROOT)
    val haystack = ((mediaSource?.name ?: "") + " " + (mediaSource?.path ?: "") + " " + rangeText).lowercase(Locale.ROOT)
    if (haystack.contains("dolbyvision") || haystack.contains("dolby vision") || haystack.contains("dovi")) {
        return if (doviProfile != null) "DV($doviProfile)" else "DV"
    }
    if (isHdr10PlusText(haystack)) {
        return "HDR10+"
    }
    if (isHdr10Text(haystack)) {
        return "HDR10"
    }
    if (haystack.contains("hdr")) {
        return "HDR"
    }
    return if (mediaSource != null) "SDR" else null
}

/** Normalizes a dynamic-range label into a video-type label ("DOVI…", "HDR", "SDR", …). */
fun buildVideoTypeLabel(mediaSource: MediaSource?): String? {
    val dynamicRangeLabel = buildDynamicRangeLabel(mediaSource) ?: return null
    if (dynamicRangeLabel.startsWith("DV")) {
        return "DOVI" + dynamicRangeLabel.drop(2)
    }
    if (dynamicRangeLabel.equals("HDR", true)) {
        return "HDR"
    }
    if (dynamicRangeLabel.equals("SDR", true)) {
        return "SDR"
    }
    return dynamicRangeLabel.uppercase(Locale.ROOT)
}

private fun isHdr10PlusText(text: String): Boolean {
    val normalized = text.replace("-", "").replace("_", "").replace(" ", "")
    return normalized.contains("hdr10plus") || normalized.contains("hdr10+")
}

private fun isHdr10Text(text: String): Boolean {
    val normalized = text.replace("-", "").replace("_", "").replace(" ", "")
    return normalized.contains("hdr10") || normalized.contains("smpte2084") || normalized.contains("st2084")
}

/** Extracts a Dolby Vision profile ("P5", "P8.1", …) from a text fragment. */
private fun parseDolbyVisionProfile(text: String?): String? {
    if (text != null && text.isNotBlank()) {
        val lower = text.lowercase(Locale.ROOT)
        val regex = Regex("dovi\\s*profile\\s*(\\d+)")
        val fallbackRegex = Regex("profile\\s*(\\d+(?:\\.\\d+)?)")
        val match = regex.find(lower) ?: fallbackRegex.find(lower)
        var result = match?.groupValues?.getOrNull(1)?.trim()
        if (result == null) {
            result = ""
        }
        if (result.isNotBlank()) {
            if (result.contains(".")) {
                return "P$result"
            }
            val digits = StringBuilder()
            for (c in result) {
                if (c.isDigit()) {
                    digits.append(c)
                }
            }
            val digitString = digits.toString()
            if (digitString.length >= 2) {
                return "P" + digitString.dropLast(1) + "." + digitString.takeLast(1)
            }
        }
    }
    return null
}

// ---------------------------------------------------------------------------
// Version picker
// ---------------------------------------------------------------------------

/**
 * Merges two version lists (by media-source id) and returns them sorted by
 * priority: [list2] entries win for shared ids (filled from [list] where a
 * field is missing), [list]-only ids are appended.
 */
fun buildVersionPickerMediaSources(
    list: List<MediaSource>?,
    list2: List<MediaSource>?,
    videoVersionPrioritySettings: VideoVersionPrioritySettings,
): List<MediaSource> {
    val primary = list ?: emptyList()
    val secondary = list2 ?: emptyList()
    if (primary.isEmpty()) {
        return sortMediaSourcesByPriority(secondary, videoVersionPrioritySettings)
    }
    if (secondary.isEmpty()) {
        return sortMediaSourcesByPriority(primary, videoVersionPrioritySettings)
    }
    val byId = LinkedHashMap<String, MediaSource>(primary.size.coerceAtLeast(16))
    for (source in primary) {
        byId[source.id] = source
    }
    val merged = ArrayList<MediaSource>(secondary.size)
    for (source in secondary) {
        merged.add(mergeVersionPickerMediaSource(source, byId[source.id]))
    }
    val extras = ArrayList<MediaSource>()
    for (source in primary) {
        if (secondary.isNotEmpty()) {
            for (other in secondary) {
                if (other.id == source.id) {
                    break
                }
            }
        }
        extras.add(source)
    }
    return sortMediaSourcesByPriority(merged + extras, videoVersionPrioritySettings)
}

/**
 * Merges [mediaSource] (the secondary entry) with [mediaSource2] (the primary
 * counterpart, nullable): blank/absent fields fall back to the counterpart.
 *
 * 反编译缺失，已按片段重建：jadx 标注 "Code restructure failed: missing block"，copy()
 * 的位掩码寄存器丢失，此处按其余字段一致的合并方向（优先本条目、缺失回退）重建。
 */
private fun mergeVersionPickerMediaSource(mediaSource: MediaSource, mediaSource2: MediaSource?): MediaSource {
    if (mediaSource2 == null) {
        return mediaSource
    }
    val fallbackName = mediaSource2.name?.takeIf { !it.isBlank() }
    val name = mediaSource.name ?: fallbackName
    val path = mediaSource.path.takeIf { it.isNotBlank() } ?: mediaSource2.path
    val container = mediaSource.container.takeIf { it.isNotBlank() } ?: mediaSource2.container
    val size = mediaSource.size ?: mediaSource2.size
    val runTimeTicks = mediaSource.runTimeTicks ?: mediaSource2.runTimeTicks
    val bitrate = mediaSource.bitrate ?: mediaSource2.bitrate
    val videoType = mediaSource.videoType ?: mediaSource2.videoType
    val supportsDirectStream = mediaSource.supportsDirectStream || mediaSource2.supportsDirectStream
    val supportsTranscoding = mediaSource.supportsTranscoding || mediaSource2.supportsTranscoding
    val mediaStreams = mediaSource.mediaStreams?.takeIf { it.isNotEmpty() } ?: mediaSource2.mediaStreams
    val directStreamUrl = mediaSource.directStreamUrl ?: mediaSource2.directStreamUrl
    val transcodingUrl = mediaSource.transcodingUrl ?: mediaSource2.transcodingUrl
    val liveStreamId = mediaSource.liveStreamId ?: mediaSource2.liveStreamId
    return mediaSource.copy(
        id = mediaSource.id,
        name = name,
        path = path,
        container = container,
        size = size,
        runTimeTicks = runTimeTicks,
        bitrate = bitrate,
        videoType = videoType,
        supportsDirectStream = supportsDirectStream,
        supportsTranscoding = supportsTranscoding,
        mediaStreams = mediaStreams,
        directStreamUrl = directStreamUrl,
        transcodingUrl = transcodingUrl,
        liveStreamId = liveStreamId,
        requiresClosing = mediaSource.requiresClosing || mediaSource2.requiresClosing,
    )
}

/**
 * Sorts [list] by the configured priority rules (deduplicated by id, stable
 * tie-break by original order); returns the list unchanged when there is
 * nothing to order.
 */
fun sortMediaSourcesByPriority(
    list: List<MediaSource>,
    videoVersionPrioritySettings: VideoVersionPrioritySettings,
): List<MediaSource> {
    if (list.size <= 1) {
        return list
    }
    val seen = HashSet<String>(list.size)
    val deduped = ArrayList<MediaSource>(list.size)
    for (source in list) {
        if (seen.add(source.id)) {
            deduped.add(source)
        }
    }
    if (deduped.size > 1) {
        val orderedRules = videoVersionPrioritySettings.normalized().rules.filter { it.hasOrder() }
        if (orderedRules.isNotEmpty()) {
            val indexed = ArrayList(deduped.mapIndexed { index, source -> index to source })
            val sorted = indexed.sortedWith { (index1, source1), (index2, source2) ->
                for (rule in orderedRules) {
                    val result = compareByRule(source1, source2, rule)
                    if (result != 0) {
                        return@sortedWith result
                    }
                }
                index1.compareTo(index2)
            }
            return sorted.map { it.second }
        }
    }
    return deduped
}

// ---------------------------------------------------------------------------
// Priority comparisons
// ---------------------------------------------------------------------------

/** Returns the best source per the priority settings (first of the sorted list, or the first). */
fun selectBestMediaSourceByPriority(
    list: List<MediaSource>,
    videoVersionPrioritySettings: VideoVersionPrioritySettings,
    preferHigherBitrate: Boolean = false,
): MediaSource? {
    if (list.isEmpty()) {
        return null
    }
    val orderedRules = videoVersionPrioritySettings.normalized().rules.filter { it.hasOrder() }
    if (orderedRules.isNotEmpty()) {
        return sortMediaSourcesByPriority(list, videoVersionPrioritySettings).firstOrNull()
    }
    if (!preferHigherBitrate) {
        return list.firstOrNull()
    }
    return list.maxWithOrNull(
        compareBy<MediaSource> { it.bitrate ?: 0 }
            .thenBy { it.size ?: 0L }
            .thenBy { resolveVideoResolutionSortValue(it) },
    )
}

/** Compares two single rules for a pair of sources; first non-zero rule result wins. */
private fun compareByRule(mediaSource: MediaSource, mediaSource2: MediaSource, rule: VideoPriorityRule): Int =
    when (rule.sortType) {
        VideoPrioritySortType.QUALITY ->
            resolveFormatPriority(mediaSource, rule).compareTo(resolveFormatPriority(mediaSource2, rule))
        VideoPrioritySortType.BITRATE -> {
            val direction = rule.valueSortDirection ?: return 0
            compareByDirection(mediaSource.bitrate?.toLong(), mediaSource2.bitrate?.toLong(), direction)
        }
        VideoPrioritySortType.FILE_SIZE -> {
            val direction = rule.valueSortDirection ?: return 0
            compareByDirection(mediaSource.size, mediaSource2.size, direction)
        }
        VideoPrioritySortType.RESOLUTION -> {
            val direction = rule.valueSortDirection ?: return 0
            compareByDirection(
                resolveKnownVideoResolutionSortValue(mediaSource),
                resolveKnownVideoResolutionSortValue(mediaSource2),
                direction,
            )
        }
        VideoPrioritySortType.NONE -> 0
    }

/** Nulls-last ordered comparison in the requested direction. */
private fun compareByDirection(a: Long?, b: Long?, direction: VideoValueSortDirection): Int = when (direction) {
    VideoValueSortDirection.DESCENDING -> compareNullableLongDesc(a, b)
    VideoValueSortDirection.ASCENDING -> compareNullableLongAsc(a, b)
}

private fun compareNullableLongDesc(a: Long?, b: Long?): Int {
    if (a == null && b == null) {
        return 0
    }
    if (a == null) {
        return 1
    }
    if (b == null) {
        return -1
    }
    return b.compareTo(a)
}

private fun compareNullableLongAsc(a: Long?, b: Long?): Int {
    if (a == null && b == null) {
        return 0
    }
    if (a == null) {
        return 1
    }
    if (b == null) {
        return -1
    }
    return a.compareTo(b)
}

/** Compares two sources through the ordered rules; nulls sort last. */
private fun compareSourcesByRules(a: MediaSource?, b: MediaSource?, rules: List<VideoPriorityRule>): Int {
    if (a == null && b == null) {
        return 0
    }
    if (a == null) {
        return 1
    }
    if (b == null) {
        return -1
    }
    for (rule in rules) {
        val result = compareByRule(a, b, rule)
        if (result != 0) {
            return result
        }
    }
    return 0
}

/** Priority value of a source's format family for a QUALITY rule (lower = better). */
private fun resolveFormatPriority(mediaSource: MediaSource, rule: VideoPriorityRule): Int {
    val standard = when (resolveVersionVideoFormat(mediaSource)) {
        VersionVideoFormat.DOVI -> VideoQualityStandard.DOVI
        VersionVideoFormat.HDR -> VideoQualityStandard.HDR
        VersionVideoFormat.SDR -> VideoQualityStandard.SDR
    }
    val qualityPriorities = rule.qualityPriorities ?: emptyMap()
    return VideoVersionPrioritySettings.normalizeQualityPriorities(qualityPriorities)[standard] ?: Int.MAX_VALUE
}

private fun resolveVersionVideoFormat(mediaSource: MediaSource): VersionVideoFormat {
    val label = buildVideoTypeLabel(mediaSource) ?: return VersionVideoFormat.SDR
    return when {
        label.startsWith("DOVI") -> VersionVideoFormat.DOVI
        label.startsWith("HDR") -> VersionVideoFormat.HDR
        else -> VersionVideoFormat.SDR
    }
}

/** Resolution sort value as a Long, or null when unknown/zero. */
private fun resolveKnownVideoResolutionSortValue(source: MediaSource?): Long? {
    val value = resolveVideoResolutionSortValue(source)
    return if (value <= 0) null else value.toLong()
}

// ---------------------------------------------------------------------------
// Aggregate-result comparisons (multi-source items / other-server matches)
// ---------------------------------------------------------------------------

/**
 * Compares two media items by their best source under [aggregateResultSortMode];
 * returns 0 for SERVER_ORDER or when a VIDEO_PRIORITY sort has no ordered rules.
 */
fun compareMediaItemsByAggregateResultSort(
    list: List<MediaItem>,
    list2: List<MediaItem>,
    aggregateResultSortMode: AggregateResultSortMode,
    videoVersionPrioritySettings: VideoVersionPrioritySettings = VideoVersionPrioritySettings.DEFAULT,
): Int {
    if (aggregateResultSortMode != AggregateResultSortMode.SERVER_ORDER) {
        val orderedRules = videoVersionPrioritySettings.normalized().rules.filter { it.hasOrder() }
        if (aggregateResultSortMode != AggregateResultSortMode.VIDEO_PRIORITY || orderedRules.isNotEmpty()) {
            val sourcesA = list.flatMap { it.mediaSources ?: emptyList() }
            val sourcesB = list2.flatMap { it.mediaSources ?: emptyList() }
            val bestA = selectBestMediaSourceForAggregateSort(sourcesA, aggregateResultSortMode, videoVersionPrioritySettings)
            val bestB = selectBestMediaSourceForAggregateSort(sourcesB, aggregateResultSortMode, videoVersionPrioritySettings)
            return when (aggregateResultSortMode) {
                AggregateResultSortMode.BITRATE_DESC -> {
                    val bitrateCompare = compareNullableLongDesc(bestA?.bitrate?.toLong(), bestB?.bitrate?.toLong())
                    if (bitrateCompare != 0) {
                        bitrateCompare
                    } else {
                        compareNullableLongDesc(
                            resolveKnownVideoResolutionSortValue(bestA),
                            resolveKnownVideoResolutionSortValue(bestB),
                        )
                    }
                }
                AggregateResultSortMode.RESOLUTION_DESC -> {
                    val resolutionCompare = compareNullableLongDesc(
                        resolveKnownVideoResolutionSortValue(bestA),
                        resolveKnownVideoResolutionSortValue(bestB),
                    )
                    if (resolutionCompare != 0) {
                        resolutionCompare
                    } else {
                        compareNullableLongDesc(bestA?.bitrate?.toLong(), bestB?.bitrate?.toLong())
                    }
                }
                AggregateResultSortMode.VIDEO_PRIORITY -> compareSourcesByRules(bestA, bestB, orderedRules)
                AggregateResultSortMode.SERVER_ORDER -> 0
            }
        }
    }
    return 0
}

/**
 * Compares two media items by their best source under the priority rules
 * (falling back to bitrate when [preferHigherBitrate] is set); 0 when nothing
 * orders them.
 */
fun compareMediaItemsByBestSourcePriority(
    list: List<MediaItem>,
    list2: List<MediaItem>,
    videoVersionPrioritySettings: VideoVersionPrioritySettings,
    preferHigherBitrate: Boolean = false,
): Int {
    val orderedRules = videoVersionPrioritySettings.normalized().rules.filter { it.hasOrder() }
    if (orderedRules.isEmpty() && !preferHigherBitrate) {
        return 0
    }
    val sourcesA = list.flatMap { it.mediaSources ?: emptyList() }
    val sourcesB = list2.flatMap { it.mediaSources ?: emptyList() }
    val bestA = selectBestMediaSourceByPriority(sourcesA, videoVersionPrioritySettings, preferHigherBitrate)
    val bestB = selectBestMediaSourceByPriority(sourcesB, videoVersionPrioritySettings, preferHigherBitrate)
    if (orderedRules.isNotEmpty()) {
        return compareSourcesByRules(bestA, bestB, orderedRules)
    }
    return compareNullableLongDesc(bestA?.bitrate?.toLong(), bestB?.bitrate?.toLong())
}

/** Compares two other-server matches by their best source under [aggregateResultSortMode]. */
fun compareOtherServerMatchesByAggregateResultSort(
    otherServerResourceMatch: OtherServerResourceMatch,
    otherServerResourceMatch2: OtherServerResourceMatch,
    aggregateResultSortMode: AggregateResultSortMode,
    videoVersionPrioritySettings: VideoVersionPrioritySettings = VideoVersionPrioritySettings.DEFAULT,
): Int {
    val orderedRules = videoVersionPrioritySettings.normalized().rules.filter { it.hasOrder() }
    if ((aggregateResultSortMode == AggregateResultSortMode.VIDEO_PRIORITY && orderedRules.isEmpty()) ||
        aggregateResultSortMode == AggregateResultSortMode.SERVER_ORDER
    ) {
        return 0
    }
    return when (aggregateResultSortMode) {
        AggregateResultSortMode.BITRATE_DESC -> {
            val bitrateCompare = otherServerResourceMatch2.bitrateSortValue
                .compareTo(otherServerResourceMatch.bitrateSortValue)
            if (bitrateCompare != 0) {
                bitrateCompare
            } else {
                otherServerResourceMatch2.resolutionSortValue
                    .compareTo(otherServerResourceMatch.resolutionSortValue)
            }
        }
        AggregateResultSortMode.RESOLUTION_DESC -> {
            val resolutionCompare = otherServerResourceMatch2.resolutionSortValue
                .compareTo(otherServerResourceMatch.resolutionSortValue)
            if (resolutionCompare != 0) {
                resolutionCompare
            } else {
                otherServerResourceMatch2.bitrateSortValue
                    .compareTo(otherServerResourceMatch.bitrateSortValue)
            }
        }
        AggregateResultSortMode.VIDEO_PRIORITY ->
            compareSourcesByRules(otherServerResourceMatch.mediaSource, otherServerResourceMatch2.mediaSource, orderedRules)
        AggregateResultSortMode.SERVER_ORDER -> 0
    }
}

/** Best source of [list] under [aggregateResultSortMode] (max by the mode's value chain). */
private fun selectBestMediaSourceForAggregateSort(
    list: List<MediaSource>,
    aggregateResultSortMode: AggregateResultSortMode,
    videoVersionPrioritySettings: VideoVersionPrioritySettings,
): MediaSource? = when (aggregateResultSortMode) {
    AggregateResultSortMode.SERVER_ORDER -> list.firstOrNull()
    AggregateResultSortMode.BITRATE_DESC -> list.maxWithOrNull(
        compareBy<MediaSource> { it.bitrate ?: 0 }
            .thenBy { resolveVideoResolutionSortValue(it) }
            .thenBy { it.size ?: 0L },
    )
    AggregateResultSortMode.RESOLUTION_DESC -> list.maxWithOrNull(
        compareBy<MediaSource> { resolveVideoResolutionSortValue(it) }
            .thenBy { it.bitrate ?: 0 }
            .thenBy { it.size ?: 0L },
    )
    AggregateResultSortMode.VIDEO_PRIORITY -> selectBestMediaSourceByPriority(list, videoVersionPrioritySettings, false)
}
