package magi.aenerv7.ppembytv.data.model

/**
 * Video-version priority settings: an ordered list of sorting rules plus the
 * shared quality-priority table used by QUALITY rules.
 */
data class VideoVersionPrioritySettings(
    val rules: List<VideoPriorityRule> = listOf(VideoPriorityRule()),
) {
    companion object {
        const val MAX_RULE_COUNT = 4

        val DEFAULT_QUALITY_PRIORITIES: Map<VideoQualityStandard, Int> = mapOf(
            VideoQualityStandard.DOVI to 1,
            VideoQualityStandard.HDR to 1,
            VideoQualityStandard.SDR to 1,
        )

        val DEFAULT: VideoVersionPrioritySettings = VideoVersionPrioritySettings()

        /** Cycles a quality priority through 1 -> 2 -> 3 -> 1. */
        fun cycleQualityPriority(value: Int): Int {
            val coerced = value.coerceIn(1, 3)
            return when (coerced) {
                1 -> 2
                2 -> 3
                else -> 1
            }
        }

        /**
         * Keeps only the three known standards (in declaration order), clamping any
         * user-provided priority into 1..3 and falling back to the default otherwise.
         */
        fun normalizeQualityPriorities(priorities: Map<VideoQualityStandard, Int>): Map<VideoQualityStandard, Int> =
            DEFAULT_QUALITY_PRIORITIES.mapValues { (standard, default) ->
                priorities[standard]?.coerceIn(1, 3) ?: default
            }

        /** e.g. "DOVI/HDR" for priority 1, "SDR" for priority 2 -> "DOVI/HDR -> SDR". */
        fun buildQualityPriorityText(priorities: Map<VideoQualityStandard, Int>): String {
            val normalized = normalizeQualityPriorities(priorities)
            val groups = (1..3).mapNotNull { priority ->
                VideoQualityStandard.entries
                    .filter { normalized[it] == priority }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("/") { it.label }
            }
            return groups.joinToString(" -> ")
        }
    }

    /** Rules clamped to [MAX_RULE_COUNT], each normalized; never empty. */
    fun normalized(): VideoVersionPrioritySettings {
        val source = if (rules.isEmpty()) listOf(VideoPriorityRule()) else rules
        val normalizedRules = source.take(MAX_RULE_COUNT).map { it.normalized() }
        val result = if (normalizedRules.isEmpty()) listOf(VideoPriorityRule()) else normalizedRules
        return copy(rules = result)
    }

    fun buildPreviewText(): String {
        val texts = normalized().rules.mapNotNull { it.buildPreviewText() }
        return if (texts.isEmpty()) {
            "效果：当前保持视频版本原始顺序。"
        } else {
            "效果：按 " + texts.joinToString("，再按 ") + " 排序。"
        }
    }
}
