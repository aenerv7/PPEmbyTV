package magi.aenerv7.ppembytv.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 对应原 App 中 QrNetworkAddressResolver（uh2.a）的能力：
 * 计算用于二维码展示的局域网地址 "http://<ip>:<port>"。
 */
internal fun resolveServerUrl(context: Context, port: Int): String? {
    val entries = listActiveIpv4Entries(context)
    if (entries.isEmpty()) {
        Log.w("QrNetworkAddressResolver", "未检测到可用于二维码展示的 IPv4 地址")
        return null
    }
    val chosen = entries.firstOrNull { it.isDefault } ?: entries.first()
    Log.d("QrNetworkAddressResolver", "二维码地址使用自动选择网卡: ${chosen.name} -> ${chosen.address}")
    return "http://${chosen.address}:$port"
}

private class Ipv4Entry(val name: String, val address: String, val isDefault: Boolean)

private fun listActiveIpv4Entries(context: Context): List<Ipv4Entry> {
    // 优先读取系统活动网络（ConnectivityManager）的 IPv4 地址
    val activeAddresses = readActiveNetworkIpv4(context)
    val entries = mutableListOf<Ipv4Entry>()
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return entries
        while (interfaces.hasMoreElements()) {
            val nif = interfaces.nextElement()
            if (!nif.isUp || nif.isLoopback) continue
            val addresses = nif.inetAddresses ?: continue
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (addr is Inet4Address) {
                    val host = addr.hostAddress ?: continue
                    entries.add(
                        Ipv4Entry(
                            name = nif.name,
                            address = host,
                            isDefault = host in activeAddresses,
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        Log.e("QrNetworkAddressResolver", "读取本机 IPv4 地址失败", e)
    }
    return entries
}

private fun readActiveNetworkIpv4(context: Context): List<String> {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val linkProperties: LinkProperties? = connectivityManager?.getLinkProperties(connectivityManager.activeNetwork)
        val addresses = mutableListOf<String>()
        linkProperties?.linkAddresses?.forEach { linkAddress ->
            val address = linkAddress.address
            if (address is Inet4Address) {
                address.hostAddress?.let { addresses.add(it) }
            }
        }
        addresses
    } catch (e: Exception) {
        Log.e("QrNetworkAddressResolver", "读取活动网络 IPv4 失败", e)
        emptyList()
    }
}
