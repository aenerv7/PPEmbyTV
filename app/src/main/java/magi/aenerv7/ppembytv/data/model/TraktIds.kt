package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktIds(
    @SerializedName("trakt")
    val trakt: Int? = null,
    @SerializedName("slug")
    val slug: String? = null,
    @SerializedName("imdb")
    val imdb: String? = null,
    @SerializedName("tmdb")
    val tmdb: Int? = null,
    @SerializedName("tvdb")
    val tvdb: Int? = null,
)
