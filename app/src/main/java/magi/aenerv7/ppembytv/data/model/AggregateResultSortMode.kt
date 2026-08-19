package magi.aenerv7.ppembytv.data.model

enum class AggregateResultSortMode {
    BITRATE_DESC,
    SERVER_ORDER,
    RESOLUTION_DESC,
    VIDEO_PRIORITY;

    val label: String
        get() = when (this) {
            BITRATE_DESC -> "码率从高到低"
            SERVER_ORDER -> "服务器顺序"
            RESOLUTION_DESC -> "分辨率从高到低"
            VIDEO_PRIORITY -> "跟随视频优先级"
        }

    val description: String
        get() = when (this) {
            BITRATE_DESC -> "优先展示码率更高的版本（默认）。主要对电影有效；剧集常无可用源数据时效果有限。"
            SERVER_ORDER -> "按服务器列表/返回顺序展示，不按画质重排。"
            RESOLUTION_DESC -> "优先展示分辨率更高的版本。主要对电影有效；剧集常无可用源数据时效果有限。"
            VIDEO_PRIORITY -> "按播放器「播放杂项」里设置的视频优先级顺序展示；未设置优先级时保持服务器顺序。"
        }

    companion object {
        val DEFAULT: AggregateResultSortMode = BITRATE_DESC

        fun fromStorage(raw: String?): AggregateResultSortMode {
            if (raw == null || raw.isBlank()) return DEFAULT
            return entries.firstOrNull { it.name == raw } ?: DEFAULT
        }
    }
}
