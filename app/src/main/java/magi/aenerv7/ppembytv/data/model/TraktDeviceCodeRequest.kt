package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktDeviceCodeRequest(
    @SerializedName("client_id")
    val clientId: String,
)
