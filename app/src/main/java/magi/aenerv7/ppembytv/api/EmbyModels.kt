package magi.aenerv7.ppembytv.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 认证 ----------

@Serializable
data class AuthenticationResult(
    val User: UserDto? = null,
    @SerialName("AccessToken") val accessToken: String = "",
    @SerialName("ServerId") val serverId: String = "",
)

@Serializable
data class UserDto(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String = "",
)

@Serializable
data class SystemInfo(
    @SerialName("ServerName") val serverName: String? = null,
    @SerialName("Version") val version: String? = null,
)

// ---------- 查询结果 ----------

@Serializable
data class QueryResult<T>(
    @SerialName("Items") val items: List<T> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0,
)

@Serializable
data class LibraryQueryResult(
    @SerialName("Items") val items: List<BaseItemDto> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0,
)

// ---------- 媒体条目 ----------

@Serializable
data class BaseItemDto(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("OriginalTitle") val originalTitle: String? = null,
    @SerialName("Type") val type: String = "",
    @SerialName("Overview") val overview: String? = null,
    @SerialName("Taglines") val taglines: List<String>? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("PremiereDate") val premiereDate: String? = null,
    @SerialName("EndDate") val endDate: String? = null,
    @SerialName("CommunityRating") val communityRating: Double? = null,
    @SerialName("CriticRating") val criticRating: Double? = null,
    @SerialName("OfficialRating") val officialRating: String? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("Genres") val genres: List<String>? = null,
    @SerialName("ImageTags") val imageTags: Map<String, String>? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String>? = null,
    @SerialName("ParentBackdropItemId") val parentBackdropItemId: String? = null,
    @SerialName("ParentBackdropImageTags") val parentBackdropImageTags: List<String>? = null,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("SeasonName") val seasonName: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("ParentId") val parentId: String? = null,
    @SerialName("SeriesId") val seriesId: String? = null,
    @SerialName("SeasonId") val seasonId: String? = null,
    @SerialName("ChildCount") val childCount: Int? = null,
    @SerialName("UserData") val userData: UserDataDto? = null,
    @SerialName("People") val people: List<PersonInfo>? = null,
    @SerialName("MediaSources") val mediaSources: List<MediaSourceInfo>? = null,
    @SerialName("Container") val container: String? = null,
    @SerialName("LocationType") val locationType: String? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("SeasonCount") val seasonCount: Int? = null,
    @SerialName("EpisodeCount") val episodeCount: Int? = null,
    @SerialName("Studios") val studios: List<NameValuePair>? = null,
    @SerialName("Tags") val tags: List<String>? = null,
    @SerialName("ProviderIds") val providerIds: Map<String, String>? = null,
    @SerialName("DateCreated") val dateCreated: String? = null,
    @SerialName("ProductionLocations") val productionLocations: List<String>? = null,
    @SerialName("ChannelId") val channelId: String? = null,
    @SerialName("IsFolder") val isFolder: Boolean = false,
    @SerialName("DisplayPreferencesId") val displayPreferencesId: String? = null,
    @SerialName("CollectionType") val collectionType: String? = null,
) {
    val primaryImageTag: String? get() = imageTags?.get("Primary")
    val backdropImageTag: String? get() = imageTags?.get("Backdrop")
    val thumbImageTag: String? get() = imageTags?.get("Thumb")
}

@Serializable
data class UserDataDto(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @SerialName("PlayCount") val playCount: Int = 0,
    @SerialName("IsFavorite") val isFavorite: Boolean = false,
    @SerialName("Played") val played: Boolean = false,
    @SerialName("PlayedPercentage") val playedPercentage: Double? = null,
    @SerialName("UnplayedItemCount") val unplayedItemCount: Int? = null,
)

@Serializable
data class PersonInfo(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("Type") val type: String? = null,
    @SerialName("Role") val role: String? = null,
)

@Serializable
data class NameValuePair(
    @SerialName("Name") val name: String = "",
    @SerialName("Value") val value: String? = null,
)

// ---------- 播放信息 ----------

@Serializable
data class PlaybackInfoResponse(
    @SerialName("MediaSources") val mediaSources: List<MediaSourceInfo> = emptyList(),
    @SerialName("PlaySessionId") val playSessionId: String = "",
)

@Serializable
data class MediaSourceInfo(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String? = null,
    @SerialName("Container") val container: String = "",
    @SerialName("MediaStreams") val mediaStreams: List<MediaStreamInfo> = emptyList(),
    @SerialName("DirectStreamUrl") val directStreamUrl: String? = null,
    @SerialName("TranscodingUrl") val transcodingUrl: String? = null,
    @SerialName("SupportsDirectPlay") val supportsDirectPlay: Boolean = false,
    @SerialName("SupportsDirectStream") val supportsDirectStream: Boolean = false,
    @SerialName("SupportsTranscoding") val supportsTranscoding: Boolean = false,
    @SerialName("Path") val path: String? = null,
    @SerialName("Bitrate") val bitrate: Long? = null,
    @SerialName("Size") val size: Long? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("DefaultAudioStreamIndex") val defaultAudioStreamIndex: Int? = null,
)

@Serializable
data class MediaStreamInfo(
    @SerialName("Index") val index: Int = 0,
    @SerialName("Type") val type: String = "",       // Video | Audio | Subtitle
    @SerialName("Codec") val codec: String = "",
    @SerialName("Language") val language: String? = null,
    @SerialName("DisplayTitle") val displayTitle: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("IsDefault") val isDefault: Boolean = false,
    @SerialName("IsForced") val isForced: Boolean = false,
    @SerialName("IsExternal") val isExternal: Boolean = false,
    @SerialName("IsTextSubtitleStream") val isTextSubtitleStream: Boolean = false,
    @SerialName("Width") val width: Int = 0,
    @SerialName("Height") val height: Int = 0,
    @SerialName("BitRate") val bitRate: Long? = null,
    @SerialName("Channels") val channels: Int? = null,
    @SerialName("Profile") val profile: String? = null,
    @SerialName("DeliveryUrl") val deliveryUrl: String? = null,
    @SerialName("DisplayLanguage") val displayLanguage: String? = null,
)

// ---------- 播放信息请求体（DeviceProfile 精简版） ----------

@Serializable
data class PlaybackInfoRequest(
    @SerialName("DeviceProfile") val deviceProfile: DeviceProfile = magi.aenerv7.ppembytv.playback.DeviceCapabilities.profile(),
    @SerialName("StartTimeTicks") val startTimeTicks: Long = 0L,
    @SerialName("IsPlayback") val isPlayback: Boolean = false,
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Long? = null,
)

@Serializable
data class DeviceProfile(
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Long = 120_000_000,
    @SerialName("MaxStaticBitrate") val maxStaticBitrate: Long = 120_000_000,
    @SerialName("DirectPlayProfiles") val directPlayProfiles: List<DirectPlayProfile> = emptyList(),
    @SerialName("TranscodingProfiles") val transcodingProfiles: List<TranscodingProfile> = defaultTranscodingProfiles,
) {
    companion object {
        val defaultTranscodingProfiles = listOf(
            TranscodingProfile("mp4", "h264", "aac", "Video", "Streaming", 2, false),
            TranscodingProfile("hls", "h264", "aac", "Video", "Streaming", 2, true),
        )
    }
}

@Serializable
data class DirectPlayProfile(
    @SerialName("Container") val container: String,
    @SerialName("Type") val type: String,
    @SerialName("AudioCodec") val audioCodec: String?,
    @SerialName("VideoCodec") val videoCodec: String?,
    @SerialName("Protocol") val protocol: String?,
    @SerialName("MaxWidth") val maxWidth: Int? = null,
    @SerialName("MaxHeight") val maxHeight: Int? = null,
)

@Serializable
data class TranscodingProfile(
    @SerialName("Container") val container: String,
    @SerialName("VideoCodec") val videoCodec: String,
    @SerialName("AudioCodec") val audioCodec: String,
    @SerialName("Type") val type: String,
    @SerialName("Context") val context: String,
    @SerialName("MaxAudioChannels") val maxAudioChannels: Long,
    @SerialName("CopyTimestamps") val copyTimestamps: Boolean,
)

// ---------- 播放进度上报 ----------

@Serializable
data class PlaybackProgressInfo(
    @SerialName("ItemId") val itemId: String = "",
    @SerialName("MediaSourceId") val mediaSourceId: String = "",
    @SerialName("PositionTicks") val positionTicks: Long = 0L,
    @SerialName("IsPaused") val isPaused: Boolean = false,
    @SerialName("PlayMethod") val playMethod: String = "DirectStream",
    @SerialName("PlaySessionId") val playSessionId: String = "",
    @SerialName("CanSeek") val canSeek: Boolean = true,
    @SerialName("EventName") val eventName: String = "timeupdate",
    @SerialName("IsMuted") val isMuted: Boolean = false,
    @SerialName("PlaybackRate") val playbackRate: Double = 1.0,
    @SerialName("RepeatMode") val repeatMode: String = "RepeatNone",
    @SerialName("PlaylistIndex") val playlistIndex: Int = 0,
    @SerialName("PlaylistLength") val playlistLength: Int = 1,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
)

// ---------- 搜索提示 ----------

@Serializable
data class SearchHint(
    @SerialName("ItemId") val itemId: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("Overview") val overview: String? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
    @SerialName("Series") val series: String? = null,
    @SerialName("Album") val album: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
)

@Serializable
data class SearchHintResult(
    @SerialName("SearchHints") val searchHints: List<SearchHint> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0,
)
