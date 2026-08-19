package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktHistoryMovie(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("year")
    val year: Int? = null,
    @SerializedName("ids")
    val ids: TraktIds,
    @SerializedName("watched_at")
    val watchedAt: String? = null,
)
