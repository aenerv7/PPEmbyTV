package magi.aenerv7.ppembytv.data.model

enum class AppThemePreset(
    val storageValue: String,
    val displayName: String,
    val description: String,
) {
    DARK("dark", "深色", "黑色系背景，白色系文字，适合夜间和影院环境。"),
    LIGHT("light", "亮色", "白色系背景，黑色系文字，适合白天和高亮环境。");

    companion object {
        fun fromStorageValue(value: String?): AppThemePreset =
            entries.firstOrNull { it.storageValue == value } ?: DARK
    }
}
