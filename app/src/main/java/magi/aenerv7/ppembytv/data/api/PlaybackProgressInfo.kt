package magi.aenerv7.ppembytv.data.api

import com.google.gson.annotations.SerializedName

data class PlaybackProgressInfo(
    @SerializedName("ItemId") val itemId: String,
    @SerializedName("PositionTicks") val positionTicks: Long,
    @SerializedName("IsPaused") val isPaused: Boolean = false,
    @SerializedName("PlayMethod") val playMethod: String = "DirectStream",
    @SerializedName("CanSeek") val canSeek: Boolean = true,
    @SerializedName("MediaSourceId") val mediaSourceId: String,
    @SerializedName("PlaySessionId") val playSessionId: String,
    @SerializedName("EventName") val eventName: String? = null,
    @SerializedName("IsMuted") val isMuted: Boolean = false,
    @SerializedName("PlaybackRate") val playbackRate: Int = 1,
    @SerializedName("RepeatMode") val repeatMode: String = "RepeatNone",
    @SerializedName("PlaylistIndex") val playlistIndex: Int = 0,
    @SerializedName("PlaylistLength") val playlistLength: Int = 1,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerializedName("NowPlayingQueue") val nowPlayingQueue: List<QueueItem>? = null,
)
