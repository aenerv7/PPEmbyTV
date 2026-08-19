package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class AuthenticationResult(
    @SerializedName("User") val user: EmbyUser,
    @SerializedName("AccessToken") val accessToken: String,
    @SerializedName("ServerId") val serverId: String,
)
