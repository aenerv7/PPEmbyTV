package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktSyncHistoryNotFound(
    @SerializedName("movies")
    val movies: List<TraktHistoryMovie>? = null,
    @SerializedName("shows")
    val shows: List<TraktHistoryShow>? = null,
    @SerializedName("seasons")
    val seasons: List<TraktHistorySeason>? = null,
    @SerializedName("episodes")
    val episodes: List<TraktHistoryEpisode>? = null,
)
