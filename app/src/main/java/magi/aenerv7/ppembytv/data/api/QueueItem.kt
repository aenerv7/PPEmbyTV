package magi.aenerv7.ppembytv.data.api

import com.google.gson.annotations.SerializedName

data class QueueItem(
    @SerializedName("Id") val id: String,
    @SerializedName("PlaylistItemId") val playlistItemId: String,
)
