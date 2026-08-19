package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktWatchedEpisode(
    @SerializedName("number")
    val number: Int? = null,
    @SerializedName("completed")
    val completed: Boolean? = null,
    @SerializedName("last_watched_at")
    val lastWatchedAt: String? = null,
)
