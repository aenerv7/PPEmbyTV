package magi.aenerv7.ppembytv.data.model

/**
 * Subtitle-version priority settings: sort-type order plus per-language and
 * per-format priority tables.
 */
data class SubtitleVersionPrioritySettings(
    val sortTypes: List<SubtitlePrioritySortType> = SubtitleVersionPrioritySettings.DEFAULT_SORT_TYPES,
    val languagePriorities: Map<SubtitleLanguagePriorityType, Int> = SubtitleVersionPrioritySettings.DEFAULT_LANGUAGE_PRIORITIES,
    val formatPriorities: Map<SubtitleFormatPriorityType, Int> = SubtitleVersionPrioritySettings.DEFAULT_FORMAT_PRIORITIES,
) {
    companion object {
        const val MAX_FORMAT_PRIORITY = 7
        const val MAX_LANGUAGE_PRIORITY = 10

        val DEFAULT_SORT_TYPES: List<SubtitlePrioritySortType> = listOf(
            SubtitlePrioritySortType.LANGUAGE,
            SubtitlePrioritySortType.FORMAT,
        )

        val DEFAULT_LANGUAGE_PRIORITIES: Map<SubtitleLanguagePriorityType, Int> =
            SubtitleLanguagePriorityType.entries.associateWith { it.defaultPriority }

        val DEFAULT_FORMAT_PRIORITIES: Map<SubtitleFormatPriorityType, Int> =
            SubtitleFormatPriorityType.entries.associateWith { it.defaultPriority }

        val DEFAULT: SubtitleVersionPrioritySettings = SubtitleVersionPrioritySettings()

        private fun cyclePriority(value: Int, maxPriority: Int): Int {
            val coerced = value.coerceIn(1, maxPriority)
            return if (coerced >= maxPriority) 1 else coerced + 1
        }

        fun cycleFormatPriority(value: Int): Int = cyclePriority(value, MAX_FORMAT_PRIORITY)

        fun cycleLanguagePriority(value: Int): Int = cyclePriority(value, MAX_LANGUAGE_PRIORITY)

        /** Deduplicates [sortTypes], fills missing entries from the defaults, and truncates. */
        fun normalizeSortTypes(sortTypes: List<SubtitlePrioritySortType>): List<SubtitlePrioritySortType> {
            val result = mutableListOf<SubtitlePrioritySortType>()
            for (sortType in sortTypes) {
                if (!result.contains(sortType)) result.add(sortType)
            }
            for (sortType in SubtitlePrioritySortType.entries) {
                if (result.size < DEFAULT_SORT_TYPES.size && !result.contains(sortType)) {
                    result.add(sortType)
                }
            }
            val taken = result.take(DEFAULT_SORT_TYPES.size)
            return if (taken.isEmpty()) DEFAULT_SORT_TYPES else taken
        }

        /** Clamps every known format's priority into 1..7, keeping default ordering. */
        fun normalizeFormatPriorities(priorities: Map<SubtitleFormatPriorityType, Int>): Map<SubtitleFormatPriorityType, Int> =
            DEFAULT_FORMAT_PRIORITIES.mapValues { (format, default) ->
                priorities[format]?.coerceIn(1, MAX_FORMAT_PRIORITY) ?: default
            }

        /** Clamps every known language's priority into 1..10, keeping default ordering. */
        fun normalizeLanguagePriorities(priorities: Map<SubtitleLanguagePriorityType, Int>): Map<SubtitleLanguagePriorityType, Int> =
            DEFAULT_LANGUAGE_PRIORITIES.mapValues { (language, default) ->
                priorities[language]?.coerceIn(1, MAX_LANGUAGE_PRIORITY) ?: default
            }

        /** e.g. "SRT/VTT -> ASS/SSA" for format priority groups 1..7. */
        fun buildFormatPriorityText(settings: SubtitleVersionPrioritySettings): String {
            val normalized = settings.normalizedFormatPriorities()
            val groups = (1..MAX_FORMAT_PRIORITY).mapNotNull { priority ->
                SubtitleFormatPriorityType.entries
                    .filter { normalized[it] == priority }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("/") { it.label }
            }
            return groups.joinToString(" -> ")
        }

        /** e.g. "简体中文/英文 -> 日文" for language priority groups 1..10. */
        fun buildLanguagePriorityText(settings: SubtitleVersionPrioritySettings): String {
            val normalized = settings.normalizedLanguagePriorities()
            val groups = (1..MAX_LANGUAGE_PRIORITY).mapNotNull { priority ->
                SubtitleLanguagePriorityType.entries
                    .filter { normalized[it] == priority }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("/") { it.label }
            }
            return groups.joinToString(" -> ")
        }
    }

    private fun normalizedFormatPriorities(): Map<SubtitleFormatPriorityType, Int> =
        Companion.normalizeFormatPriorities(formatPriorities)

    private fun normalizedLanguagePriorities(): Map<SubtitleLanguagePriorityType, Int> =
        Companion.normalizeLanguagePriorities(languagePriorities)

    /** Normalized copy: sort types deduped, both priority tables clamped. */
    fun normalized(): SubtitleVersionPrioritySettings = copy(
        sortTypes = Companion.normalizeSortTypes(sortTypes),
        languagePriorities = Companion.normalizeLanguagePriorities(languagePriorities),
        formatPriorities = Companion.normalizeFormatPriorities(formatPriorities),
    )

    fun formatPriorityFor(formatType: SubtitleFormatPriorityType): Int =
        normalizedFormatPriorities()[formatType] ?: formatType.defaultPriority

    fun languagePriorityFor(languageType: SubtitleLanguagePriorityType): Int =
        normalizedLanguagePriorities()[languageType] ?: languageType.defaultPriority

    fun hasFormatPriority(): Boolean = normalizedFormatPriorities().values.toSet().size > 1

    fun hasLanguagePriority(): Boolean = normalizedLanguagePriorities().values.toSet().size > 1

    fun buildPreviewText(): String {
        val normalized = normalized()
        val parts = normalized.sortTypes.mapNotNull { sortType ->
            when (sortType) {
                SubtitlePrioritySortType.LANGUAGE ->
                    if (normalized.hasLanguagePriority()) "语言 " + Companion.buildLanguagePriorityText(normalized) else null
                SubtitlePrioritySortType.FORMAT ->
                    if (normalized.hasFormatPriority()) "格式 " + Companion.buildFormatPriorityText(normalized) else null
            }
        }
        return if (parts.isEmpty()) {
            "效果：当前不区分字幕语言或格式，列表保持原始顺序。"
        } else {
            "效果：按 " + parts.joinToString("，再按 ") + " 排序；同档内保持原始顺序。"
        }
    }
}
