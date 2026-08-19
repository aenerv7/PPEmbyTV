package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TranscodingProfile(
    @SerializedName("Type") val type: String,
    @SerializedName("Container") val container: String,
    @SerializedName("VideoCodec") val videoCodec: String,
    @SerializedName("AudioCodec") val audioCodec: String,
    @SerializedName("Protocol") val protocol: String,
    @SerializedName("Context") val context: String = "Streaming",
    @SerializedName("MaxAudioChannels") val maxAudioChannels: String = "6",
    @SerializedName("MinSegments") val minSegments: Int = 2,
    @SerializedName("BreakOnNonKeyFrames") val breakOnNonKeyFrames: Boolean = true,
)
