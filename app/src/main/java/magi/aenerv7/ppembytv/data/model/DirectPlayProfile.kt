package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class DirectPlayProfile(
    @SerializedName("Type") val type: String,
    @SerializedName("Container") val container: String,
    @SerializedName("VideoCodec") val videoCodec: String? = null,
    @SerializedName("AudioCodec") val audioCodec: String? = null,
)
