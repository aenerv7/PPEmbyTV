package magi.aenerv7.ppembytv.server

import com.google.gson.annotations.SerializedName

/** Emby /emby/System/Ext/ServerDomains 接口的响应。 */
data class ExtServerDomainsResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("data") val data: List<ExtServerDomainItem>? = null,
)
