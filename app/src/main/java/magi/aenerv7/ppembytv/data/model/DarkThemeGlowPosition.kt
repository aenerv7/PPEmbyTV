package magi.aenerv7.ppembytv.data.model

enum class DarkThemeGlowPosition(
    val storageValue: String,
    val displayName: String,
) {
    LEFT_TOP("left_top", "左上"),
    CENTER_TOP("center_top", "中上"),
    RIGHT_TOP("right_top", "右上");

    companion object {
        val DEFAULT: DarkThemeGlowPosition = LEFT_TOP

        fun fromStorageValue(value: String?): DarkThemeGlowPosition =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
