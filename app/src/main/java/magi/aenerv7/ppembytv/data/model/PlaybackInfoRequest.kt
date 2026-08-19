package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class PlaybackInfoRequest(
    @SerializedName("DeviceProfile") val deviceProfile: DeviceProfile,
    @SerializedName("MaxStreamingBitrate") val maxStreamingBitrate: Long? = null,
    @SerializedName("MaxWidth") val maxWidth: Int? = null,
    @SerializedName("MaxHeight") val maxHeight: Int? = null,
    @SerializedName("StartTimeTicks") val startTimeTicks: Long? = null,
    @SerializedName("AudioStreamIndex") val audioStreamIndex: Int? = null,
    @SerializedName("SubtitleStreamIndex") val subtitleStreamIndex: Int? = null,
    @SerializedName("MediaSourceId") val mediaSourceId: String? = null,
    @SerializedName("MaxAudioChannels") val maxAudioChannels: Int? = null,
    @SerializedName("EnableDirectPlay") val enableDirectPlay: Boolean? = null,
    @SerializedName("EnableDirectStream") val enableDirectStream: Boolean? = null,
    @SerializedName("EnableTranscoding") val enableTranscoding: Boolean? = null,
)
