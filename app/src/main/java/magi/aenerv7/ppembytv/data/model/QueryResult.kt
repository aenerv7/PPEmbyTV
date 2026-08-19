package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class QueryResult(
    @SerializedName("Items") val items: List<MediaItem>,
    @SerializedName("TotalRecordCount") val totalRecordCount: Int,
)
