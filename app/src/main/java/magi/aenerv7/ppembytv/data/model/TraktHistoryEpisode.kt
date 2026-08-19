package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktHistoryEpisode(
    @SerializedName("number")
    val number: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("ids")
    val ids: TraktIds? = null,
    @SerializedName("watched_at")
    val watchedAt: String? = null,
)
