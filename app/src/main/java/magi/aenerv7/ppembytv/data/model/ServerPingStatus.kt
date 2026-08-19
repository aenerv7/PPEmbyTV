package magi.aenerv7.ppembytv.data.model

enum class ServerPingStatus {
    IDLE,
    MEASURING,
    GOOD,
    NORMAL,
    WARNING,
    HIGH,
    BAD,
    HTTP_ERROR,
    UNREACHABLE,
}
