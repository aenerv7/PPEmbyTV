package magi.aenerv7.ppembytv.data.model

data class NormalizedServerEndpoint(
    val protocol: String,
    val host: String,
    val port: Int,
    val path: String,
)
