package magi.aenerv7.ppembytv.data

import com.google.gson.annotations.SerializedName

enum class AssrtApiProtocol(val scheme: String, val baseUrl: String) {
    @SerializedName("https")
    HTTPS("https", "https://api.assrt.net"),

    @SerializedName("http")
    HTTP("http", "http://api.assrt.net");

    companion object {
        fun parse(value: String?): AssrtApiProtocol =
            entries.firstOrNull { it.scheme.equals(value, ignoreCase = true) } ?: HTTPS
    }
}
