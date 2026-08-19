package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class ResponseProfile(
    @SerializedName("Type") val type: String,
    @SerializedName("Container") val container: String,
    @SerializedName("MimeType") val mimeType: String,
)
