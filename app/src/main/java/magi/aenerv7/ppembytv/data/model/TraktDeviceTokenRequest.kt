package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktDeviceTokenRequest(
    @SerializedName("code")
    val code: String,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("client_secret")
    val clientSecret: String,
)
