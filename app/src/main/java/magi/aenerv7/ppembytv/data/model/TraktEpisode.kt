package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktEpisode(
    @SerializedName("season")
    val season: Int? = null,
    @SerializedName("number")
    val number: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("ids")
    val ids: TraktIds? = null,
)
