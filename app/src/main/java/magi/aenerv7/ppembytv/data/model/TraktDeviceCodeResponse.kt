package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktDeviceCodeResponse(
    @SerializedName("device_code")
    val deviceCode: String,
    @SerializedName("user_code")
    val userCode: String,
    @SerializedName("verification_url")
    val verificationUrl: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("interval")
    val interval: Long,
) {
    val activationUrl: String
        get() = verificationUrl.trimEnd('/') + "/" + userCode
}
