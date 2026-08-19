package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktHistoryShow(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("year")
    val year: Int? = null,
    @SerializedName("ids")
    val ids: TraktIds? = null,
    @SerializedName("seasons")
    val seasons: List<TraktHistorySeason>? = null,
)
