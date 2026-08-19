package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktPlaybackProgressItem(
    @SerializedName("progress")
    val progress: Double = 0.0,
    @SerializedName("paused_at")
    val pausedAt: String? = null,
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("movie")
    val movie: TraktMovie? = null,
    @SerializedName("show")
    val show: TraktShow? = null,
    @SerializedName("episode")
    val episode: TraktEpisode? = null,
)
