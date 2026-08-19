package magi.aenerv7.ppembytv.data.model

import java.net.URI

private fun normalizeServerProtocol(protocol: String?): String =
    if (protocol?.equals("https", true) == true) "https" else "http"

private fun defaultPortForProtocol(protocol: String?): Int =
    if (protocol?.equals("https", true) == true) 443 else 80

private fun normalizeStoredServerPath(path: String?): String =
    (path ?: "").trim().trim('/')

fun normalizeServerPath(path: String?): String {
    val p = normalizeStoredServerPath(path)
    return if (p.isEmpty()) "" else "/$p"
}

fun normalizeBackupRouteConfig(route: BackupRouteConfig): BackupRouteConfig {
    val endpoint = normalizeServerEndpoint(route.protocol, route.host, route.port, route.path)
    return route.copy(
        protocol = endpoint.protocol,
        host = endpoint.host,
        port = endpoint.port,
        path = endpoint.path,
    )
}

private fun normalizeServerEndpoint(
    protocol: String?,
    host: String?,
    port: Int,
    path: String?,
): NormalizedServerEndpoint {
    var proto = normalizeServerProtocol(protocol)
    val trimmedHost = (host ?: "").trim().trimEnd('/')
    var normalizedPath = normalizeStoredServerPath(path)
    val hasScheme = trimmedHost.startsWith("http://") || trimmedHost.startsWith("https://")

    if (trimmedHost.isEmpty()) {
        return NormalizedServerEndpoint(proto, "", port, normalizedPath)
    }

    val url = if (hasScheme) trimmedHost else "$proto://$trimmedHost"
    val uri = try {
        URI(url)
    } catch (e: Exception) {
        null
    }
    uri?.host?.let { parsedHost ->
        if (parsedHost.isNotBlank()) {
            val parsedPath = normalizeStoredServerPath(uri.path)
            val protoFromScheme = normalizeServerProtocol(uri.scheme)
            val parsedHostTrimmed = parsedHost.trim().trimEnd('/')
            var portOut = port
            val parsedPort = uri.port
            if (parsedPort in 1..65535) {
                portOut = parsedPort
            } else if (hasScheme) {
                portOut = defaultPortForProtocol(protoFromScheme)
            }
            if (parsedPath.isNotEmpty()) {
                normalizedPath = parsedPath
            }
            return NormalizedServerEndpoint(protoFromScheme, parsedHostTrimmed, portOut, normalizedPath)
        }
    }

    if (trimmedHost.startsWith("https://")) {
        proto = "https"
    } else if (trimmedHost.startsWith("http://")) {
        proto = "http"
    }
    val withoutScheme = Regex("^https?://", RegexOption.IGNORE_CASE).replace(trimmedHost, "")
    val hostPortPart = withoutScheme.substringBefore('/')
    val hostPartTrimmed = hostPortPart.trim().trimEnd('/')
    val afterPath = withoutScheme.substringAfter('/', "")
    if (afterPath.isNotBlank()) {
        normalizedPath = afterPath
    }

    val colonCount = hostPartTrimmed.count { it == ':' }
    if (colonCount == 1) {
        val hostOnly = hostPartTrimmed.substringBefore(':').trim().trimEnd('/')
        val portStr = hostPartTrimmed.substringAfter(':')
        val parsedPort = portStr.toIntOrNull()
        if (hostOnly.isNotBlank() && parsedPort != null) {
            return NormalizedServerEndpoint(proto, hostOnly, parsedPort, normalizeStoredServerPath(normalizedPath))
        }
    }

    var portOut = port
    if (hasScheme) {
        portOut = defaultPortForProtocol(proto)
    }
    return NormalizedServerEndpoint(proto, hostPartTrimmed, portOut, normalizeStoredServerPath(normalizedPath))
}
