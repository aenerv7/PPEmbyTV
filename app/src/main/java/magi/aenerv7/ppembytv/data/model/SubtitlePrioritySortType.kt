package magi.aenerv7.ppembytv.data.model

enum class SubtitlePrioritySortType(
    val storageValue: String,
    val label: String,
) {
    LANGUAGE("language", "语言类型"),
    FORMAT("format", "字幕格式");

    companion object {
        fun fromStorageValue(value: String?): SubtitlePrioritySortType? =
            entries.firstOrNull { it.storageValue == value }
    }
}
