package magi.aenerv7.ppembytv.data

import com.google.gson.annotations.SerializedName

data class OnlineSubtitleConfig(
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("assrtApiToken") val assrtApiToken: String = "",
    @SerializedName("assrtApiProtocol") val assrtApiProtocol: AssrtApiProtocol = AssrtApiProtocol.HTTPS,
) {
    /** 内置的默认 API Key，当用户未配置自己的 Key 时使用。 */
    val effectiveApiToken: String
        get() = if (assrtApiToken.isBlank()) "G1jDEk5mvd5s8eRlDnLLQbpWaHwmzoU9" else assrtApiToken
}
