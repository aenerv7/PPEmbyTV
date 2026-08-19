package magi.aenerv7.ppembytv.data.model

enum class LightThemeBackgroundStyle(
    val storageValue: String,
    val displayName: String,
) {
    PLAIN_WHITE("plain_white", "素白"),
    MORNING_BLUE("morning_blue", "晨雾蔚蓝"),
    SAKURA_BLUSH("sakura_blush", "樱语柔粉"),
    IRIS_BREEZE("iris_breeze", "鸢尾晴岚"),
    SPRING_MINT("spring_mint", "春芽薄荷"),
    APRICOT_DAWN("apricot_dawn", "杏霞晨光"),
    CORAL_HAZE("coral_haze", "珊瑚轻霞");

    companion object {
        fun fromStorageValue(value: String?): LightThemeBackgroundStyle =
            entries.firstOrNull { it.storageValue == value } ?: PLAIN_WHITE
    }
}
