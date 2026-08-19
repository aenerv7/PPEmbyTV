package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class CodecProfile(
    @SerializedName("Type") val type: String,
    @SerializedName("Codec") val codec: String,
    @SerializedName("ApplyConditions") val applyConditions: List<ProfileCondition>,
)
