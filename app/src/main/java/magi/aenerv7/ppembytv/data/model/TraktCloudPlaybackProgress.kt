package magi.aenerv7.ppembytv.data.model

data class TraktCloudPlaybackProgress(
    val progressPercent: Double,
    val positionTicks: Long,
    val pausedAt: String?,
    val playbackId: Long?,
)
