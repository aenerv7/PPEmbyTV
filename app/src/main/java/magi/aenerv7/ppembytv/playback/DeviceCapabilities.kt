package magi.aenerv7.ppembytv.playback

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import magi.aenerv7.ppembytv.api.DeviceProfile
import magi.aenerv7.ppembytv.api.DirectPlayProfile

/**
 * 根据设备真实编解码能力构建 DeviceProfile：
 * 仅把设备确实具备解码器的编码标记为支持直连（缺失解码器的编码交给服务器转码）。
 * 解码器存在但具体文件超出其能力（如 4K HEVC 超分辨率）的情况，
 * 由播放器在解码失败时自动回退转码处理（见 PlayerScreen）。
 */
object DeviceCapabilities {

    private val VIDEO_CODECS = mapOf(
        "video/avc" to "h264",
        "video/hevc" to "hevc",
        "video/x-vnd.on2.vp8" to "vp8",
        "video/x-vnd.on2.vp9" to "vp9",
        "video/av01" to "av1",
        "video/mpeg2" to "mpeg2video",
        "video/mp4v-es" to "mpeg4",
    )

    private val AUDIO_CODECS = listOf(
        "audio/mp4a-latm" to "aac",
        "audio/mpeg" to "mp3",
        "audio/ac3" to "ac3",
        "audio/eac3" to "eac3",
        "audio/flac" to "flac",
        "audio/opus" to "opus",
        "audio/vorbis" to "vorbis",
        "audio/vnd.dts" to "dts",
        "audio/vnd.dts.hd" to "truehd",
    )

    private val CONTAINERS_VIDEO = "mp4,mkv,m4v,mov,ts,m2ts,webm,avi,flv,3gp,ogv"
    private val CONTAINERS_AUDIO = "mp4,mkv,m4v,mov"

    // 从大到小探测解码器支持的最大常见分辨率
    private val SIZES = listOf(
        3840 to 2160,
        2560 to 1440,
        1920 to 1080,
        1600 to 900,
        1280 to 720,
        854 to 480,
    )

    @Volatile
    private var cached: DeviceProfile? = null

    /** 惰性构建并缓存设备档案（MediaCodecList 是静态 API，无需 Context） */
    fun profile(): DeviceProfile {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val built = build()
            cached = built
            return built
        }
    }

    private fun build(): DeviceProfile {
        val decoders = scanDecoderMimes()

        val videoProfiles = mutableListOf<DirectPlayProfile>()
        for ((mime, codecName) in VIDEO_CODECS) {
            if (mime !in decoders) continue
            val (w, h) = maxDecodeSize(mime)
            if (w <= 0) continue
            videoProfiles += DirectPlayProfile(
                container = CONTAINERS_VIDEO,
                type = "Video",
                audioCodec = null,
                videoCodec = codecName,
                protocol = null,
                maxWidth = w,
                maxHeight = h,
            )
        }

        val audioCodecs = AUDIO_CODECS
            .filter { (mime, _) -> mime in decoders }
            .map { it.second }
            .joinToString(",")

        return DeviceProfile(
            maxStreamingBitrate = 120_000_000,
            maxStaticBitrate = 120_000_000,
            directPlayProfiles = videoProfiles + DirectPlayProfile(
                container = CONTAINERS_AUDIO,
                type = "Audio",
                audioCodec = audioCodecs.ifEmpty { null },
                videoCodec = null,
                protocol = null,
                maxWidth = null,
                maxHeight = null,
            ),
            transcodingProfiles = DeviceProfile.defaultTranscodingProfiles,
        )
    }

    private fun scanDecoderMimes(): Set<String> {
        val result = mutableSetOf<String>()
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecs.codecInfos) {
            if (info.isEncoder) continue
            for (type in info.supportedTypes) result.add(type)
        }
        return result
    }

    /** 该解码器能支持的最大常见分辨率；无解码器/不支持时返回 (-1,-1) */
    private fun maxDecodeSize(mime: String): Pair<Int, Int> {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecs.codecInfos) {
            if (info.isEncoder || mime !in info.supportedTypes) continue
            val caps = try {
                info.getCapabilitiesForType(mime)
            } catch (e: Exception) {
                continue
            }
            val vc = caps.videoCapabilities ?: continue
            for ((w, h) in SIZES) {
                if (vc.isSizeSupported(w, h)) return w to h
            }
            return -1 to -1
        }
        return -1 to -1
    }
}
