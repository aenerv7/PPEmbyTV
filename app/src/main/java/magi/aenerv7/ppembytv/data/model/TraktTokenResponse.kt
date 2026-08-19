package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("token_type")
    val tokenType: String? = null,
    @SerializedName("scope")
    val scope: String? = null,
    @SerializedName("created_at")
    val createdAt: Long? = null,
)
