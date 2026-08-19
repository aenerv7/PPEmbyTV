package magi.aenerv7.ppembytv.data.model

enum class PlayerFrameRateMatchingMode(
    val storageValue: String,
    val label: String,
    val description: String,
) {
    SEAMLESS_ONLY("seamless_only", "自动模式", "原来的无感切换，仅在系统支持时才匹配刷新率。"),
    ALWAYS("always", "积极匹配", "优先按视频帧率切换显示模式，部分电视可能会黑屏 1-2 秒。");

    companion object {
        fun fromStorageValue(value: String?): PlayerFrameRateMatchingMode =
            if (value == ALWAYS.storageValue) ALWAYS else SEAMLESS_ONLY
    }
}
