package magi.aenerv7.ppembytv.server

import com.google.gson.annotations.SerializedName

/** Emby /emby/System/Ext/ServerDomains 接口返回的条目。 */
data class ExtServerDomainItem(
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null,
)
