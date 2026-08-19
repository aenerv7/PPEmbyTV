package magi.aenerv7.ppembytv.data.api

import com.google.gson.annotations.SerializedName
import magi.aenerv7.ppembytv.data.model.MediaSource

data class PlaybackInfoResponse(
    @SerializedName("MediaSources") val mediaSources: List<MediaSource>? = null,
    @SerializedName("PlaySessionId") val playSessionId: String? = null,
)
