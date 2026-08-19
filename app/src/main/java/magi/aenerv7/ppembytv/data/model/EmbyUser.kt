package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class EmbyUser(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("ServerId") val serverId: String,
    @SerializedName("HasPassword") val hasPassword: Boolean,
    @SerializedName("HasConfiguredPassword") val hasConfiguredPassword: Boolean,
    @SerializedName("HasConfiguredEasyPassword") val hasConfiguredEasyPassword: Boolean,
    @SerializedName("EnableAutoLogin") val enableAutoLogin: Boolean? = null,
    @SerializedName("LastLoginDate") val lastLoginDate: String? = null,
    @SerializedName("LastActivityDate") val lastActivityDate: String? = null,
)
