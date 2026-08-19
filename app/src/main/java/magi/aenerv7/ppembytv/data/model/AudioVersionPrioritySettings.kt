package magi.aenerv7.ppembytv.data.model

/**
 * Audio-version priority settings: preferred language, per-codec priority (1..6)
 * and the sort-type order used to pick the audio track.
 */
data class AudioVersionPrioritySettings(
    val sortTypes: List<AudioPrioritySortType> = AudioVersionPrioritySettings.DEFAULT_SORT_TYPES,
    val preferredLanguage: AudioLanguagePreference = AudioLanguagePreference.DEFAULT,
    val aacPriority: Int = 1,
    val ac3Priority: Int = 1,
    val eac3Priority: Int = 1,
    val dtsPriority: Int = 1,
    val truehdPriority: Int = 1,
    val flacPriority: Int = 1,
) {
    companion object {
        const val MAX_PRIORITY = 6

        val DEFAULT_SORT_TYPES: List<AudioPrioritySortType> = listOf(
            AudioPrioritySortType.LANGUAGE,
            AudioPrioritySortType.FORMAT,
        )

        val DEFAULT: AudioVersionPrioritySettings = AudioVersionPrioritySettings()

        /** Cycles a priority through 1..6 (6 wraps back to 1). */
        fun cyclePriority(value: Int): Int {
            val coerced = value.coerceIn(1, MAX_PRIORITY)
            return if (coerced >= MAX_PRIORITY) 1 else coerced + 1
        }

        /** Deduplicates [sortTypes], fills missing entries from the defaults, and truncates. */
        fun normalizeSortTypes(sortTypes: List<AudioPrioritySortType>): List<AudioPrioritySortType> {
            val result = mutableListOf<AudioPrioritySortType>()
            for (sortType in sortTypes) {
                if (!result.contains(sortType)) result.add(sortType)
            }
            for (sortType in AudioPrioritySortType.entries) {
                if (result.size < DEFAULT_SORT_TYPES.size && !result.contains(sortType)) {
                    result.add(sortType)
                }
            }
            val taken = result.take(DEFAULT_SORT_TYPES.size)
            return if (taken.isEmpty()) DEFAULT_SORT_TYPES else taken
        }

        /** True when the six codec priorities are not all identical after normalization. */
        fun hasDistinctFormatPriorities(settings: AudioVersionPrioritySettings): Boolean {
            val normalized = settings.normalized()
            return setOf(
                normalized.aacPriority,
                normalized.ac3Priority,
                normalized.eac3Priority,
                normalized.dtsPriority,
                normalized.truehdPriority,
                normalized.flacPriority,
            ).size > 1
        }

        /** "AAC/AC3/..." for the codecs whose priority equals [priority]. */
        private fun buildFormatGroupText(settings: AudioVersionPrioritySettings, priority: Int): String {
            return buildList {
                if (settings.aacPriority == priority) add("AAC")
                if (settings.ac3Priority == priority) add("AC3")
                if (settings.eac3Priority == priority) add("EAC3")
                if (settings.dtsPriority == priority) add("DTS")
                if (settings.truehdPriority == priority) add("TrueHD")
                if (settings.flacPriority == priority) add("FLAC")
            }.joinToString("、")
        }

        /** e.g. "【AAC/AC3】 -> 【DTS】 -> 【TrueHD/FLAC】" for priority groups 1..6. */
        fun buildFormatPriorityText(settings: AudioVersionPrioritySettings): String {
            val normalized = settings.normalized()
            val groups = (1..MAX_PRIORITY).mapNotNull { priority ->
                buildFormatGroupText(normalized, priority).takeIf { it.isNotEmpty() }?.let { "【$it】" }
            }
            return groups.joinToString(" -> ")
        }
    }

    /** Clamps codec priorities into 1..6 and normalizes the sort-type list. */
    fun normalized(): AudioVersionPrioritySettings = copy(
        sortTypes = Companion.normalizeSortTypes(sortTypes),
        aacPriority = aacPriority.coerceIn(1, MAX_PRIORITY),
        ac3Priority = ac3Priority.coerceIn(1, MAX_PRIORITY),
        eac3Priority = eac3Priority.coerceIn(1, MAX_PRIORITY),
        dtsPriority = dtsPriority.coerceIn(1, MAX_PRIORITY),
        truehdPriority = truehdPriority.coerceIn(1, MAX_PRIORITY),
        flacPriority = flacPriority.coerceIn(1, MAX_PRIORITY),
    )

    /**
     * Human-readable summary of the current audio-picking strategy.
     *
     * Note: the decompiled body of this method was partially lost to a jadx
     * "Code decompiled incorrectly" error; the behavior was reconstructed from the
     * surviving bytecode fragments (the four/five preview strings below match the
     * original switch exactly).
     */
    fun buildPreviewText(): String {
        val normalized = normalized()
        val languageLabel = normalized.preferredLanguage
            .takeUnless { it == AudioLanguagePreference.DEFAULT }
            ?.label
        val formatPriorityText = Companion.buildFormatPriorityText(normalized)
        val hasDistinctFormats = Companion.hasDistinctFormatPriorities(normalized)
        val hasLanguage = languageLabel != null
        if (!hasLanguage && !hasDistinctFormats) {
            return "效果：当前不区分音频语言或格式，列表保持原始顺序。"
        }
        val relevant = normalized.sortTypes.filter { sortType ->
            when (sortType) {
                AudioPrioritySortType.LANGUAGE -> hasLanguage
                AudioPrioritySortType.FORMAT -> hasDistinctFormats
            }
        }
        val first = relevant.getOrNull(0)
        val second = relevant.getOrNull(1)
        return when {
            first == AudioPrioritySortType.LANGUAGE && second == AudioPrioritySortType.FORMAT ->
                "效果：先优先选择${languageLabel}音轨；同语言内再按 ${formatPriorityText} 分组；未命中时保持当前默认音轨。"
            first == AudioPrioritySortType.FORMAT && second == AudioPrioritySortType.LANGUAGE ->
                "效果：先按 ${formatPriorityText} 分组；每组内再优先选择${languageLabel}音轨；未命中语言时继续按格式顺序选择。"
            first == AudioPrioritySortType.LANGUAGE ->
                "效果：优先选择${languageLabel}音轨；若存在多个${languageLabel}音轨，则保持原始顺序；未命中时保持当前默认音轨。"
            first == AudioPrioritySortType.FORMAT ->
                "效果：先按 ${formatPriorityText} 分组；每组内保持原始顺序。"
            else -> "效果：当前不区分音频语言或格式，列表保持原始顺序。"
        }
    }
}
