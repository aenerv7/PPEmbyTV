package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktWatchedSeason(
    @SerializedName("number")
    val number: Int? = null,
    @SerializedName("aired")
    val aired: Int? = null,
    @SerializedName("completed")
    val completed: Int? = null,
    @SerializedName("episodes")
    val episodes: List<TraktWatchedEpisode>? = null,
)
