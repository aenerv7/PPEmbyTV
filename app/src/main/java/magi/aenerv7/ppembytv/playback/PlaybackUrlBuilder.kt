package magi.aenerv7.ppembytv.playback

import magi.aenerv7.ppembytv.api.MediaSourceInfo
import magi.aenerv7.ppembytv.api.MediaStreamInfo
import magi.aenerv7.ppembytv.data.ServerConfig

/**
 * 播放 URL 构建（直连 / 转码 / 字幕），对应参考项目的播放地址拼接逻辑。
 */
object PlaybackUrlBuilder {

    /**
     * 直连播放 URL：
     *   {base}Videos/{itemId}/stream.{container}?api_key={token}&MediaSourceId={id}&Static=true
     */
    fun directStreamUrl(server: ServerConfig, itemId: String, source: MediaSourceInfo): String {
        val container = source.container.ifBlank { "mp4" }
        val path = "Videos/$itemId/stream.$container"
        return appendQuery(server.getFullUrl() + path, mapOf(
            "api_key" to server.accessToken,
            "MediaSourceId" to source.id,
            "Static" to "true",
        ))
    }

    /**
     * HLS 转码 URL（使用服务器下发的 TranscodingUrl 或自行拼接 master.m3u8）
     *
     * 自行拼接时带上 PlaySessionId（HLS 分片按会话鉴权，缺失会返回 400），
     * 并用 MaxWidth/MaxHeight 把转码输出限制在 1080p 内，
     * 保证直连解码失败回退转码时，弱解码能力设备也能正常播放。
     */
    fun transcodeUrl(server: ServerConfig, itemId: String, source: MediaSourceInfo, playSessionId: String? = null): String? {
        val transcodingUrl = source.transcodingUrl
        if (!transcodingUrl.isNullOrBlank()) {
            return if (transcodingUrl.startsWith("http")) transcodingUrl
            else appendQuery(server.getFullUrl() + transcodingUrl.trimStart('/'), mapOf(
                "api_key" to server.accessToken,
            ))
        }
        if (!source.supportsTranscoding) return null
        val path = "Videos/$itemId/master.m3u8"
        val params = mutableMapOf(
            "api_key" to server.accessToken,
            "MediaSourceId" to source.id,
            "VideoCodec" to "h264",
            "AudioCodec" to "aac",
            "TranscodingMaxAudioChannels" to "2",
            "RequireAvc" to "true",
            "SubtitleCodec" to "srt",
            "MaxVideoBitrate" to "12000000",
            "MaxAudioBitrate" to "384000",
            "MaxWidth" to "1920",
            "MaxHeight" to "1080",
        )
        if (!playSessionId.isNullOrBlank()) {
            params["PlaySessionId"] = playSessionId
        }
        return appendQuery(server.getFullUrl() + path, params)
    }

    /** 外挂/提取字幕 URL */
    fun subtitleUrl(
        server: ServerConfig,
        itemId: String,
        source: MediaSourceInfo,
        stream: MediaStreamInfo,
    ): String? {
        if (stream.isExternal) {
            // 外部字幕直接使用 DeliveryUrl（服务器已生成带鉴权的下载地址）
            val delivery = stream.deliveryUrl
            if (!delivery.isNullOrBlank()) {
                return if (delivery.startsWith("http")) delivery
                else appendQuery(server.getFullUrl() + delivery.trimStart('/'), mapOf("api_key" to server.accessToken))
            }
            return null
        }
        val format = when (stream.codec.lowercase()) {
            "srt", "subrip" -> "srt"
            "ass", "ssa" -> "ass"
            "vtt", "webvtt" -> "vtt"
            "pgs" -> "pgs"
            "dvdsub", "dvd" -> "vobsub"
            else -> "srt"
        }
        val path = "Videos/$itemId/${source.id}/Subtitles/${stream.index}/stream.$format"
        return appendQuery(server.getFullUrl() + path, mapOf("api_key" to server.accessToken))
    }

    /** 图片 URL（海报/背景/缩略图），带 api_key 鉴权 */
    fun imageUrl(
        server: ServerConfig,
        itemId: String,
        type: String,
        tag: String?,
        maxWidth: Int = 500,
    ): String? {
        if (tag.isNullOrBlank()) return null
        val path = "Items/$itemId/Images/$type"
        return appendQuery(server.getFullUrl() + path, mapOf(
            "maxWidth" to maxWidth.toString(),
            "tag" to tag,
            "quality" to "90",
            "api_key" to server.accessToken,
        ))
    }

    fun backdropUrl(server: ServerConfig, itemId: String, tag: String?, maxWidth: Int = 1920): String? =
        imageUrl(server, itemId, "Backdrop", tag, maxWidth)

    fun thumbUrl(server: ServerConfig, itemId: String, tag: String?, maxWidth: Int = 600): String? =
        imageUrl(server, itemId, "Thumb", tag, maxWidth)

    private fun appendQuery(url: String, params: Map<String, String>): String {
        val sb = StringBuilder(url)
        // 首个参数用 ?，后续参数必须用 &（此前该分隔符只在循环外计算一次，
        // 导致直连/图片 URL 变成 ?a=1?b=2?c=3，服务器返回 500 → 封面全挂、播放 Source error）
        var separator = if (url.contains("?")) "&" else "?"
        for ((k, v) in params) {
            if (v.isBlank()) continue
            sb.append(separator).append(k).append("=").append(android.net.Uri.encode(v))
            separator = "&"
        }
        return sb.toString()
    }
}
