package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktShowWatchedProgress(
    @SerializedName("aired")
    val aired: Int? = null,
    @SerializedName("completed")
    val completed: Int? = null,
    @SerializedName("last_watched_at")
    val lastWatchedAt: String? = null,
    @SerializedName("seasons")
    val seasons: List<TraktWatchedSeason>? = null,
)
