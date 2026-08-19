package magi.aenerv7.ppembytv.data.model

/**
 * A single video-version sorting rule: how to order candidate versions and what
 * the "preferred" values are.
 */
data class VideoPriorityRule(
    val sortType: VideoPrioritySortType = VideoPrioritySortType.NONE,
    val qualityPriorities: Map<VideoQualityStandard, Int>? = null,
    val valueSortDirection: VideoValueSortDirection? = null,
) {
    /** Whether this rule actually defines an ordering. */
    fun hasOrder(): Boolean = when (sortType) {
        VideoPrioritySortType.QUALITY -> qualityPriorities != null
        VideoPrioritySortType.BITRATE,
        VideoPrioritySortType.FILE_SIZE,
        VideoPrioritySortType.RESOLUTION -> valueSortDirection != null
        VideoPrioritySortType.NONE -> false
    }

    /** Short text describing the ordered values, or "空" when nothing is configured. */
    fun buildOrderText(): String = when (sortType) {
        VideoPrioritySortType.QUALITY ->
            qualityPriorities?.let { VideoVersionPrioritySettings.buildQualityPriorityText(it) } ?: "空"
        VideoPrioritySortType.BITRATE,
        VideoPrioritySortType.FILE_SIZE,
        VideoPrioritySortType.RESOLUTION ->
            valueSortDirection?.label ?: "空"
        VideoPrioritySortType.NONE -> ""
    }

    /** Preview text, e.g. "画质标准 DOVI/HDR/SDR"; null when the rule is inactive. */
    fun buildPreviewText(): String? {
        if (!hasOrder()) return null
        val orderText = buildOrderText()
        return when (sortType) {
            VideoPrioritySortType.QUALITY -> "画质标准 " + orderText
            VideoPrioritySortType.BITRATE -> "码率 " + orderText
            VideoPrioritySortType.FILE_SIZE -> "文件大小 " + orderText
            VideoPrioritySortType.RESOLUTION -> "分辨率 " + orderText
            VideoPrioritySortType.NONE -> null
        }
    }

    /** Clamps priorities / drops the value direction so the rule only keeps the sortable part. */
    fun normalized(): VideoPriorityRule {
        if (sortType != VideoPrioritySortType.QUALITY) {
            return copy(qualityPriorities = null)
        }
        return copy(
            qualityPriorities = qualityPriorities?.let { VideoVersionPrioritySettings.normalizeQualityPriorities(it) },
            valueSortDirection = null,
        )
    }
}
