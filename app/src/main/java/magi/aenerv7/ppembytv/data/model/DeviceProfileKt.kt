package magi.aenerv7.ppembytv.data.model

fun createAndroidTvDeviceProfile(
    maxStreamingBitrate: Long = 40_000_000,
    maxVideoWidth: Int? = null,
    maxVideoHeight: Int? = null,
): DeviceProfile {
    val profile = DeviceProfile(
        maxStreamingBitrate = maxStreamingBitrate,
        maxStaticBitrate = maxStreamingBitrate,
        musicStreamingTranscodingBitrate = maxStreamingBitrate,
        directPlayProfiles = listOf(
            DirectPlayProfile(
                type = "Video",
                container = "mov,mp4,mkv,webm,avi,flv,m4v,ts",
                videoCodec = "h264,hevc,hev1,vp9,mpeg4,mpeg2video",
                audioCodec = "aac,mp3,ac3,eac3,flac,opus,dts,dca,truehd,wav",
            )
        ),
        transcodingProfiles = listOf(
            TranscodingProfile(
                type = "Video",
                container = "ts",
                videoCodec = "h264,hevc",
                audioCodec = "aac,mp3,ac3,eac3",
                protocol = "hls",
            )
        ),
        codecProfiles = listOf(
            CodecProfile(
                type = "Video",
                codec = "h264",
                applyConditions = listOf(
                    ProfileCondition("NotEquals", "IsAnamorphic", "true"),
                    ProfileCondition("EqualsAny", "VideoProfile", "high|main|baseline|constrained baseline"),
                    ProfileCondition("LessThanEqual", "VideoLevel", "80"),
                    ProfileCondition("NotEquals", "IsInterlaced", "true"),
                ),
            ),
            CodecProfile(
                type = "Video",
                codec = "hevc",
                applyConditions = listOf(
                    ProfileCondition("NotEquals", "IsAnamorphic", "true"),
                    ProfileCondition("EqualsAny", "VideoProfile", "high|main|main 10"),
                    ProfileCondition("LessThanEqual", "VideoLevel", "175"),
                    ProfileCondition("NotEquals", "IsInterlaced", "true"),
                ),
            ),
        ),
        subtitleProfiles = listOf(
            SubtitleProfile("ass", "Embed"),
            SubtitleProfile("ssa", "Embed"),
            SubtitleProfile("subrip", "Embed"),
            SubtitleProfile("sub", "Embed"),
            SubtitleProfile("pgssub", "Embed"),
            SubtitleProfile("subrip", "External"),
            SubtitleProfile("sub", "External"),
            SubtitleProfile("ass", "External"),
            SubtitleProfile("ssa", "External"),
            SubtitleProfile("vtt", "External"),
        ),
        responseProfiles = listOf(
            ResponseProfile("Video", "m4v", "video/mp4"),
        ),
    )

    val width = maxVideoWidth?.takeIf { it > 0 }
    val height = maxVideoHeight?.takeIf { it > 0 }
    if (width == null && height == null) {
        return profile
    }

    val updatedCodecProfiles = profile.codecProfiles.map { codecProfile ->
        if (codecProfile.type == "Video") {
            var conditions = codecProfile.applyConditions
            if (width != null) {
                conditions = upsertLessThanEqualProfileCondition(conditions, "Width", width.toString())
            }
            if (height != null) {
                conditions = upsertLessThanEqualProfileCondition(conditions, "Height", height.toString())
            }
            codecProfile.copy(applyConditions = conditions)
        } else {
            codecProfile
        }
    }
    return profile.copy(codecProfiles = updatedCodecProfiles)
}

private fun upsertLessThanEqualProfileCondition(
    conditions: List<ProfileCondition>,
    property: String,
    value: String,
): List<ProfileCondition> {
    val result = mutableListOf<ProfileCondition>()
    var found = false
    for (condition in conditions) {
        if (condition.property == property) {
            result.add(condition.copy(condition = "LessThanEqual", value = value))
            found = true
        } else {
            result.add(condition)
        }
    }
    if (!found) {
        result.add(ProfileCondition("LessThanEqual", property, value))
    }
    return result
}
