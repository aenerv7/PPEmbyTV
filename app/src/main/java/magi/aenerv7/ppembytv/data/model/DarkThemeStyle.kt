package magi.aenerv7.ppembytv.data.model

enum class DarkThemeStyle(
    val storageValue: String,
    val displayName: String,
) {
    PURE_BLACK("pure_black", "纯黑"),
    EMBER_ORANGE("ember_orange", "余烬暗橙"),
    OBSIDIAN_VIOLET("obsidian_violet", "曜石暗紫"),
    MIDNIGHT_BLUE("midnight_blue", "子夜深蓝"),
    PINE_GREEN("pine_green", "松影暗绿"),
    GRAPHITE("graphite", "石墨深灰"),
    NOIR_MAGENTA("noir_magenta", "夜幕品红");

    companion object {
        val DEFAULT: DarkThemeStyle = EMBER_ORANGE

        fun fromStorageValue(value: String?): DarkThemeStyle =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
