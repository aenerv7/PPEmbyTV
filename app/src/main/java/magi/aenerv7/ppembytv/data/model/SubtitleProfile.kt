package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class SubtitleProfile(
    @SerializedName("Format") val format: String,
    @SerializedName("Method") val method: String,
)
