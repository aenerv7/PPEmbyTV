package magi.aenerv7.ppembytv.data.model

data class ServerPingState(
    val status: ServerPingStatus,
    val latencyMs: Long? = null,
    val httpStatusCode: Int? = null,
) {
    companion object {
        val Idle: ServerPingState = ServerPingState(ServerPingStatus.IDLE)
        val Measuring: ServerPingState = ServerPingState(ServerPingStatus.MEASURING)
        val Unreachable: ServerPingState = ServerPingState(ServerPingStatus.UNREACHABLE)

        fun fromLatency(latencyMs: Long): ServerPingState {
            val coerced = latencyMs.coerceAtLeast(0L)
            val status = when {
                coerced <= 100 -> ServerPingStatus.GOOD
                coerced <= 200 -> ServerPingStatus.NORMAL
                coerced <= 300 -> ServerPingStatus.WARNING
                coerced <= 500 -> ServerPingStatus.HIGH
                else -> ServerPingStatus.BAD
            }
            return ServerPingState(status, coerced)
        }

        fun fromHttpStatusCode(statusCode: Int): ServerPingState =
            ServerPingState(ServerPingStatus.HTTP_ERROR, httpStatusCode = statusCode)
    }
}
