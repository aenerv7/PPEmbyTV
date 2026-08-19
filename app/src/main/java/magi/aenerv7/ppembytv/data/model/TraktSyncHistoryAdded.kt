package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktSyncHistoryAdded(
    @SerializedName("movies")
    val movies: Int = 0,
    @SerializedName("episodes")
    val episodes: Int = 0,
    @SerializedName("shows")
    val shows: Int = 0,
    @SerializedName("seasons")
    val seasons: Int = 0,
)
