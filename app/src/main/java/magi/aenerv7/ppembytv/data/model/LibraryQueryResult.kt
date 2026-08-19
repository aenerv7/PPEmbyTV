package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class LibraryQueryResult(
    @SerializedName("Items") val items: List<Library>,
    @SerializedName("TotalRecordCount") val totalRecordCount: Int,
)
