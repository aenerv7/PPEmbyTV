package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktMovie(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("year")
    val year: Int? = null,
    @SerializedName("ids")
    val ids: TraktIds? = null,
)
