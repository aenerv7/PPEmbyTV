package magi.aenerv7.ppembytv.data.model

enum class SystemNetworkSpeedPosition(
    val storageValue: String,
    val label: String,
) {
    TOP_CENTER("top_center", "顶部居中"),
    TOP_RIGHT("top_right", "顶部右侧");

    companion object {
        fun fromStorageValue(value: String?): SystemNetworkSpeedPosition? =
            entries.firstOrNull { it.storageValue == value }
    }
}
