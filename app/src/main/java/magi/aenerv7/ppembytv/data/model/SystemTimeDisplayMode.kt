package magi.aenerv7.ppembytv.data.model

enum class SystemTimeDisplayMode(
    val storageValue: String,
    val label: String,
    val showsInMenu: Boolean,
    val isPinned: Boolean,
) {
    OFF("OFF", "完全关闭", false, false),
    MENU_ONLY("menu_only", "关闭常驻", true, false),
    ALWAYS("always", "开启常驻", true, true);

    companion object {
        fun fromStorageValue(value: String?): SystemTimeDisplayMode? =
            entries.firstOrNull { it.storageValue == value }
    }
}
