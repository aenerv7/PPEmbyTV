package magi.aenerv7.ppembytv.server

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

object LocalNetwork {

    /** 获取局域网 IPv4 地址（优先 WiFi） */
    fun getLocalIpAddress(context: Context): String? {
        try {
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo = wifi?.connectionInfo
            val wifiIp = connectionInfo?.ipAddress
            if (wifiIp != null && wifiIp != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    (wifiIp and 0xff),
                    (wifiIp shr 8) and 0xff,
                    (wifiIp shr 16) and 0xff,
                    (wifiIp shr 24) and 0xff
                )
            }
        } catch (_: Exception) {
        }

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") ||
                            ip.startsWith("172.") || ip.startsWith("100.")
                        ) {
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }
}
