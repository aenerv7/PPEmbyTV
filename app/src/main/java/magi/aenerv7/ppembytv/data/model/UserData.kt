package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @SerializedName("PlayedPercentage") val playedPercentage: Double? = null,
    @SerializedName("PlayCount") val playCount: Int = 0,
    @SerializedName("IsFavorite") val isFavorite: Boolean = false,
    @SerializedName("Played") val played: Boolean = false,
    @SerializedName("UnplayedItemCount") val unplayedItemCount: Int? = null,
    @SerializedName("LastPlayedDate") val lastPlayedDate: String? = null,
)
