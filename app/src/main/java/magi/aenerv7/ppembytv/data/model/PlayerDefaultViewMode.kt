package magi.aenerv7.ppembytv.data.model

enum class PlayerDefaultViewMode(
    val storageValue: String,
    val label: String,
    val description: String,
    val useTextureView: Boolean,
) {
    SURFACE("surface", "SurfaceView", "更省资源，保持现有默认行为，但部分厂商设备兼容性一般。", false),
    TEXTURE("texture", "TextureView", "兼容性通常更好，能绕开部分电视的 SurfaceView 合成问题。", true);

    companion object {
        fun fromStorageValue(value: String?): PlayerDefaultViewMode =
            if (value == TEXTURE.storageValue) TEXTURE else SURFACE
    }
}
