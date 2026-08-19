package magi.aenerv7.ppembytv.data.model

enum class AudioPrioritySortType(
    val storageValue: String,
    val label: String,
) {
    LANGUAGE("language", "语言类型"),
    FORMAT("format", "音频格式");

    companion object {
        fun fromStorageValue(value: String?): AudioPrioritySortType? =
            entries.firstOrNull { it.storageValue == value }
    }
}
