package magi.aenerv7.ppembytv.dlna

/**
 * DLNA renderer configuration.
 *
 * Ported from `com.dh.myembyapp.dlna.DlnaConfig`.
 */
data class DlnaConfig(
    val enabled: Boolean = false,
    val deviceName: String = "皮皮 TV",
    val autoPlay: Boolean = false,
    val useProxyByDefault: Boolean = false,
    val trustAllCerts: Boolean = false,
)
