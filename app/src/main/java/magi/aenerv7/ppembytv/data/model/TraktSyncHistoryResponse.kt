package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktSyncHistoryResponse(
    @SerializedName("added")
    val added: TraktSyncHistoryAdded? = null,
    @SerializedName("existing")
    val existing: TraktSyncHistoryAdded? = null,
    @SerializedName("not_found")
    val notFound: TraktSyncHistoryNotFound? = null,
)
