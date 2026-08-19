package magi.aenerv7.ppembytv.data.api

import com.google.gson.annotations.SerializedName

data class IqiyiSuggestResponse(
    val code: String? = null,
    val data: List<IqiyiSuggestItem> = emptyList(),
    @SerializedName("show_query_count")
    val showQueryCount: Int = 0,
)
