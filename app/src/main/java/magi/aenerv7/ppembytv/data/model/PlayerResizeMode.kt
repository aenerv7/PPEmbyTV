package magi.aenerv7.ppembytv.data.model

enum class PlayerResizeMode(
    val storageValue: String,
    val label: String,
    val description: String,
    val media3ResizeMode: Int,
    val exoVideoScalingMode: Int,
    val useTextureView: Boolean = false,
    val useSurfaceCropTransform: Boolean = false,
) {
    DEFAULT("default", "默认", "保持原始比例，必要时显示黑边", 0, 1),
    STRETCH("stretch", "拉伸", "铺满整个屏幕，可能会拉伸变形", 3, 1),
    CROP("crop", "裁切", "保持比例铺满屏幕，超出的边缘会被裁掉", 0, 1, true, true);

    companion object {
        fun fromStorageValue(value: String?): PlayerResizeMode =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
