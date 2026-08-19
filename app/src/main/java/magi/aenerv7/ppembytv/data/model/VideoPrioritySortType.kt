package magi.aenerv7.ppembytv.data.model

enum class VideoPrioritySortType(val label: String) {
    NONE("空"),
    QUALITY("画质标准"),
    BITRATE("码率大小"),
    FILE_SIZE("文件大小"),
    RESOLUTION("分辨率大小"),
}
