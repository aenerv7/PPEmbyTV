package magi.aenerv7.ppembytv.data.api

import com.google.gson.annotations.SerializedName

data class UserDataRequest(
    @SerializedName("PlaybackPositionTicks") val playbackPositionTicks: Long,
    @SerializedName("PlayCount") val playCount: Int? = null,
    @SerializedName("IsFavorite") val isFavorite: Boolean? = null,
    @SerializedName("Played") val played: Boolean? = null,
    @SerializedName("LastPlayedDate") val lastPlayedDate: String? = null,
)
