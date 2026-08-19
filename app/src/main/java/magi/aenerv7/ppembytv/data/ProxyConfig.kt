package magi.aenerv7.ppembytv.data

data class ProxyConfig(
    val enabled: Boolean = false,
    val protocol: ProxyProtocol = ProxyProtocol.HTTP,
    val host: String = "192.168.5.235",
    val port: Int = 7890,
    val username: String = "",
    val password: String = "",
    val bypassLan: Boolean = true,
) {
    val hasCredentials: Boolean
        get() = username.isNotBlank()
}
