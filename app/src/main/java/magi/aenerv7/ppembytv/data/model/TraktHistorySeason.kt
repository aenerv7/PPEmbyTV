package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktHistorySeason(
    @SerializedName("number")
    val number: Int,
    @SerializedName("episodes")
    val episodes: List<TraktHistoryEpisode>,
)
