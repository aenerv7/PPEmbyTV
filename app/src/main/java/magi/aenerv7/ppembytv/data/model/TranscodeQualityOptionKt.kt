package magi.aenerv7.ppembytv.data.model

/**
 * Builds the transcode-quality presets offered for [mediaSource] (optionally
 * appending the "自动" / "关闭转码" pseudo-options).
 *
 * 反编译缺失，已按片段重建：jadx 对该函数标注 "Code decompiled incorrectly"（多处
 * "Removed duplicated region"），此处按反编译片段逐条恢复选项表与控制流（含未达分支）。
 */
fun buildTranscodeQualityOptions(
    mediaSource: MediaSource?,
    includeAuto: Boolean = true,
): List<TranscodeQualityOption> {
    val videoStream = mediaSource?.mediaStreams?.firstOrNull { it.type == "Video" }
    val streamWidth = videoStream?.width?.takeIf { it > 0 }
    val sourceWidth = streamWidth ?: 4096
    val options = ArrayList<TranscodeQualityOption>()
    if (sourceWidth < 3400) {
        options += listOf(
            TranscodeQualityOption("4K - 200 Mbps", 200_000_000L, maxHeight = 2160),
            TranscodeQualityOption("4K - 160 Mbps", 160_000_000L, maxHeight = 2160),
            TranscodeQualityOption("4K - 140 Mbps", 140_000_000L, maxHeight = 2160),
            TranscodeQualityOption("4K - 120 Mbps", 120_000_000L, maxHeight = 2160),
            TranscodeQualityOption("4K - 100 Mbps", 100_000_000L, maxHeight = 2160),
            TranscodeQualityOption("4K - 80 Mbps", 80_000_000L, maxHeight = 2160),
            TranscodeQualityOption("4K - 60 Mbps", 60_000_001L, maxHeight = 2160),
            TranscodeQualityOption("4K - 40 Mbps", 40_000_001L, maxHeight = 2160),
        )
    }
    if (sourceWidth < 1440) {
        options += listOf(
            TranscodeQualityOption("1080p - 60 Mbps", 60_000_000L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 50 Mbps", 50_000_000L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 40 Mbps", 40_000_000L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 30 Mbps", 30_000_000L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 25 Mbps", 25_000_000L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 20 Mbps", 20_000_000L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 15 Mbps", 15_000_000L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 12 Mbps", 12_000_000L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 10 Mbps", 10_000_001L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 8 Mbps", 8_000_001L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 6 Mbps", 6_000_001L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 5 Mbps", 5_000_001L, maxHeight = 1080),
            TranscodeQualityOption("1080p - 4 Mbps", 4_000_002L, maxHeight = 1080),
        )
    } else if (sourceWidth >= 1200) {
        options += listOf(
            TranscodeQualityOption("720p - 10 Mbps", 10_000_000L, maxHeight = 720),
            TranscodeQualityOption("720p - 8 Mbps", 8_000_000L, maxHeight = 720),
            TranscodeQualityOption("720p - 6 Mbps", 6_000_000L, maxHeight = 720),
            TranscodeQualityOption("720p - 5 Mbps", 5_000_000L, maxHeight = 720),
        )
    } else if (sourceWidth >= 620) {
        options += listOf(
            TranscodeQualityOption("480p - 4 Mbps", 4_000_001L, maxHeight = 480),
            TranscodeQualityOption("480p - 3 Mbps", 3_000_001L, maxHeight = 480),
            TranscodeQualityOption("480p - 2.5 Mbps", 2_500_000L, maxHeight = 480),
            TranscodeQualityOption("480p - 2 Mbps", 2_000_001L, maxHeight = 480),
            TranscodeQualityOption("480p - 1.5 Mbps", 1_500_001L, maxHeight = 480),
        )
        if (sourceWidth >= 1260) {
            options += listOf(
                TranscodeQualityOption("720p - 4 Mbps", 4_000_000L, maxHeight = 720),
                TranscodeQualityOption("720p - 3 Mbps", 3_000_000L, maxHeight = 720),
                TranscodeQualityOption("720p - 2 Mbps", 2_000_000L, maxHeight = 720),
                TranscodeQualityOption("720p - 1.5 Mbps", 1_500_000L, maxHeight = 720),
                TranscodeQualityOption("720p - 1 Mbps", 1_000_001L, maxHeight = 720),
            )
        }
        options += listOf(
            TranscodeQualityOption("480p - 1 Mbps", 1_000_000L, maxHeight = 480),
            TranscodeQualityOption("480p - 720 kbps", 720_000L, maxHeight = 480),
            TranscodeQualityOption("480p - 420 kbps", 420_000L, maxHeight = 480),
            TranscodeQualityOption("360p", 400_000L, maxHeight = 360),
            TranscodeQualityOption("240p", 320_000L, maxHeight = 240),
            TranscodeQualityOption("144p", 192_000L, maxHeight = 144),
        )
        if (includeAuto) {
            options += TranscodeQualityOption("自动", 0L, isAuto = true)
            options += TranscodeQualityOption("关闭转码", 0L, disablesTranscoding = true)
        }
        return options
    }
    options += listOf(
        TranscodeQualityOption("480p - 1 Mbps", 1_000_000L, maxHeight = 480),
        TranscodeQualityOption("480p - 720 kbps", 720_000L, maxHeight = 480),
        TranscodeQualityOption("480p - 420 kbps", 420_000L, maxHeight = 480),
        TranscodeQualityOption("360p", 400_000L, maxHeight = 360),
        TranscodeQualityOption("240p", 320_000L, maxHeight = 240),
        TranscodeQualityOption("144p", 192_000L, maxHeight = 144),
    )
    if (includeAuto) {
        options += TranscodeQualityOption("自动", 0L, isAuto = true)
        options += TranscodeQualityOption("关闭转码", 0L, disablesTranscoding = true)
    }
    return options
}
