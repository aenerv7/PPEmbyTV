package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class BackupRouteConfig(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("alias") val alias: String,
    @SerializedName("protocol") val protocol: String,
    @SerializedName("host") val host: String,
    @SerializedName("port") val port: Int,
    @SerializedName("path") val path: String = "",
    @SerializedName("directOnly") val directOnly: Boolean = false,
) {
    val displayAddress: String
        get() = "$protocol://$host:$port${normalizeServerPath(path)}"

    val fullUrl: String
        get() = "$protocol://$host:$port${normalizeServerPath(path)}/"
}
