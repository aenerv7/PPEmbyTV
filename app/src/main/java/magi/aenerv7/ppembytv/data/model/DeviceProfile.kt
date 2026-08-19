package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class DeviceProfile(
    @SerializedName("MaxStreamingBitrate") val maxStreamingBitrate: Long = 40_000_000,
    @SerializedName("MaxStaticBitrate") val maxStaticBitrate: Long = 40_000_000,
    @SerializedName("MusicStreamingTranscodingBitrate") val musicStreamingTranscodingBitrate: Long = 40_000_000,
    @SerializedName("DirectPlayProfiles") val directPlayProfiles: List<DirectPlayProfile>,
    @SerializedName("TranscodingProfiles") val transcodingProfiles: List<TranscodingProfile>,
    @SerializedName("CodecProfiles") val codecProfiles: List<CodecProfile>,
    @SerializedName("SubtitleProfiles") val subtitleProfiles: List<SubtitleProfile>,
    @SerializedName("ResponseProfiles") val responseProfiles: List<ResponseProfile> = emptyList(),
    @SerializedName("ContainerProfiles") val containerProfiles: List<Any> = emptyList(),
)
