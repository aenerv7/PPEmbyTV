package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktSearchResult(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("score")
    val score: Double? = null,
    @SerializedName("movie")
    val movie: TraktMovie? = null,
    @SerializedName("show")
    val show: TraktShow? = null,
    @SerializedName("episode")
    val episode: TraktEpisode? = null,
)
