package magi.aenerv7.ppembytv.data.model

enum class SubtitleLanguagePriorityType(
    val storageValue: String,
    val label: String,
    val defaultPriority: Int,
) {
    SIMPLIFIED_CHINESE("simplified_chinese", "简体中文", 1),
    SIMPLIFIED_CHINESE_BILINGUAL("simplified_chinese_bilingual", "简体中文双语", 2),
    CHINESE("chinese", "中文", 3),
    CHINESE_BILINGUAL("chinese_bilingual", "中文双语", 4),
    TRADITIONAL_CHINESE("traditional_chinese", "繁体中文", 5),
    TRADITIONAL_CHINESE_BILINGUAL("traditional_chinese_bilingual", "繁体中文双语", 6),
    ENGLISH("english", "英文", 7),
    JAPANESE("japanese", "日文", 7),
    KOREAN("korean", "韩文", 7),
    OTHER("other", "其他", 8);

    companion object {
        fun fromStorageValue(value: String?): SubtitleLanguagePriorityType? =
            entries.firstOrNull { it.storageValue == value }
    }
}
