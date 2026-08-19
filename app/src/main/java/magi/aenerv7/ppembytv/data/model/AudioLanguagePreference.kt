package magi.aenerv7.ppembytv.data.model

enum class AudioLanguagePreference(
    val storageValue: String,
    val label: String,
) {
    DEFAULT("default", "默认"),
    CHINESE("chinese", "中文"),
    ENGLISH("english", "英语"),
    KOREAN("korean", "韩语"),
    JAPANESE("japanese", "日语"),
    CANTONESE("cantonese", "粤语"),
    FRENCH("french", "法语"),
    GERMAN("german", "德语"),
    SPANISH("spanish", "西班牙语"),
    RUSSIAN("russian", "俄语"),
    ITALIAN("italian", "意大利语"),
    THAI("thai", "泰语");

    companion object {
        fun fromStorageValue(value: String?): AudioLanguagePreference =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
