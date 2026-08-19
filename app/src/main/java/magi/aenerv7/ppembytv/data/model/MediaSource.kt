package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName
import java.util.Locale

data class MediaSource(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Path") val path: String,
    @SerializedName("Container") val container: String,
    @SerializedName("Size") val size: Long? = null,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerializedName("Bitrate") val bitrate: Int? = null,
    @SerializedName("VideoType") val videoType: String? = null,
    @SerializedName("SupportsDirectStream") val supportsDirectStream: Boolean = false,
    @SerializedName("SupportsTranscoding") val supportsTranscoding: Boolean = false,
    @SerializedName("MediaStreams") val mediaStreams: List<MediaStream>? = null,
    @SerializedName("DirectStreamUrl") val directStreamUrl: String? = null,
    @SerializedName("TranscodingUrl") val transcodingUrl: String? = null,
    @SerializedName("LiveStreamId") val liveStreamId: String? = null,
    @SerializedName("RequiresClosing") val requiresClosing: Boolean = false,
) {
    val displayName: String
        get() {
            val name = this.name
            if (name != null && name.isNotBlank()) {
                return name
            }
            val videoStream = mediaStreams?.find { it.type == "Video" }
            val resolutionLabel = buildVideoResolutionLabel(this, ResolutionLabelStyle.UPPERCASE) ?: ""
            var codec = ""
            videoStream?.codec?.let { codec = it.uppercase(Locale.ROOT) }
            val sb = StringBuilder()
            if (resolutionLabel.isNotEmpty()) {
                sb.append(resolutionLabel)
            }
            if (codec.isNotEmpty()) {
                if (sb.isNotEmpty()) {
                    sb.append(" - ")
                }
                sb.append(codec)
            }
            if (sb.isEmpty()) {
                sb.append("默认版本")
            }
            return sb.toString()
        }
}
