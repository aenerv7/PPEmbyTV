package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktRefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("client_secret")
    val clientSecret: String,
    @SerializedName("redirect_uri")
    val redirectUri: String = "urn:ietf:wg:oauth:2.0:oob",
    @SerializedName("grant_type")
    val grantType: String = "refresh_token",
)
