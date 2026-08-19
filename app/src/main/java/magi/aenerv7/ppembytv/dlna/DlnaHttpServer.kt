package magi.aenerv7.ppembytv.dlna

import android.content.Intent
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Locale
import java.util.UUID
import kotlin.text.Regex
import kotlin.text.RegexOption

/**
 * NanoHTTPD server exposing the DLNA media renderer: device description, SCPD documents,
 * SOAP AVTransport / RenderingControl / ConnectionManager control points, event
 * subscription and CORS headers. All XML / SOAP / header strings are reproduced EXACTLY
 * from `com.dh.myembyapp.dlna.DlnaHttpServer`.
 */
class DlnaHttpServer(
    private val service: DlnaService,
    private val deviceUuid: String,
    private val deviceName: String,
    port: Int = 0,
) : NanoHTTPD("0.0.0.0", port) {

    private var currentUri: String = ""
    private var currentMetadata: String = ""
    private var currentPosition: Long = 0L
    private var currentDuration: Long = 0L
    private var transportState: String = "NO_MEDIA_PRESENT"

    private fun createSoapResponse(body: String): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/xml; charset=\"utf-8\"",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "    <s:Body>\n" +
                "        $body\n" +
                "    </s:Body>\n" +
                "</s:Envelope>"
        )
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun unescapeXml(text: String): String {
        return text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val patterns = listOf(
            "<$tag>(.+?)</$tag>",
            "<$tag[^>]*>(.+?)</$tag>",
            "<[^:]+:$tag>(.+?)</[^:]+:$tag>"
        )
        for (pattern in patterns) {
            val match = Regex(pattern, RegexOption.DOT_MATCHES_ALL).find(xml) ?: continue
            val value = match.groupValues.getOrNull(1)?.trim() ?: continue
            return unescapeXml(value)
        }
        return null
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        return String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
    }

    private fun getLocalIp(): String {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取本地 IP 失败", e)
        }
        return "127.0.0.1"
    }

    private fun handleAvTransportControl(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val postData = files["postData"] ?: ""
            Log.d(TAG, "========== AVTransport SOAP 请求 ==========")
            Log.d(TAG, "SOAP Body: $postData")
            val soapAction = session.headers["soapaction"] ?: ""
            val action = soapAction.replace("\"", "").substringAfterLast("#")
            Log.d(TAG, "SOAP Action: $action")
            val response = when (action) {
                "GetMediaInfo" -> handleGetMediaInfo()
                "GetTransportSettings" -> handleGetTransportSettings()
                "SetNextAVTransportURI" -> handleSetNextAvTransportUri(postData)
                "Play" -> handlePlay()
                "Seek" -> handleSeek(postData)
                "Stop" -> handleStop()
                "SetAVTransportURI" -> handleSetAvTransportUri(postData)
                "Pause" -> handlePause()
                "GetDeviceCapabilities" -> handleGetDeviceCapabilities()
                "GetPositionInfo" -> handleGetPositionInfo()
                "GetTransportInfo" -> handleGetTransportInfo()
                "GetCurrentTransportActions" -> handleGetCurrentTransportActions()
                else -> {
                    Log.w(TAG, "未知的 AVTransport Action: $action")
                    createSoapResponse("")
                }
            }
            Log.d(TAG, "========================================")
            response
        } catch (e: Exception) {
            Log.e(TAG, "处理 AVTransport 请求失败", e)
            e.printStackTrace()
            NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                NanoHTTPD.MIME_PLAINTEXT,
                "Error: ${e.message}"
            )
        }
    }

    private fun handleConnectionManager(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val postData = files["postData"] ?: ""
            val soapAction = session.headers["soapaction"] ?: ""
            val action = soapAction.replace("\"", "").substringAfterLast("#")
            Log.d(TAG, "========== ConnectionManager SOAP 请求 ==========")
            Log.d(TAG, "Action: $action")
            Log.d(TAG, "SOAP Body: $postData")
            val sinkProtocols = listOf(
                "http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_CIF15_AAC_520;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000",
                "http-get:*:video/mp4:*",
                "http-get:*:video/x-matroska:*",
                "http-get:*:video/x-msvideo:*",
                "http-get:*:video/avi:*",
                "http-get:*:video/webm:*",
                "http-get:*:video/mpeg:*",
                "http-get:*:video/x-ms-wmv:*",
                "http-get:*:video/x-flv:*",
                "http-get:*:video/3gpp:*",
                "http-get:*:audio/mpeg:*",
                "http-get:*:audio/mp4:*",
                "http-get:*:audio/x-ms-wma:*",
                "http-get:*:audio/flac:*",
                "http-get:*:audio/ogg:*",
                "http-get:*:image/jpeg:*",
                "http-get:*:image/png:*",
                "http-get:*:*:*"
            ).joinToString(",")
            when (action) {
                "GetCurrentConnectionIDs" -> {
                    createSoapResponse(
                        """<u:GetCurrentConnectionIDsResponse xmlns:u="urn:schemas-upnp-org:service:ConnectionManager:1">
    <ConnectionIDs>0</ConnectionIDs>
</u:GetCurrentConnectionIDsResponse>"""
                    )
                }
                "GetProtocolInfo" -> {
                    Log.d(TAG, "返回 GetProtocolInfo 响应")
                    createSoapResponse(
                        """<u:GetProtocolInfoResponse xmlns:u="urn:schemas-upnp-org:service:ConnectionManager:1">
    <Source></Source>
    <Sink>$sinkProtocols</Sink>
</u:GetProtocolInfoResponse>"""
                    )
                }
                "GetCurrentConnectionInfo" -> {
                    createSoapResponse(
                        """<u:GetCurrentConnectionInfoResponse xmlns:u="urn:schemas-upnp-org:service:ConnectionManager:1">
    <RcsID>0</RcsID>
    <AVTransportID>0</AVTransportID>
    <ProtocolInfo></ProtocolInfo>
    <PeerConnectionManager></PeerConnectionManager>
    <PeerConnectionID>-1</PeerConnectionID>
    <Direction>Input</Direction>
    <Status>OK</Status>
</u:GetCurrentConnectionInfoResponse>"""
                    )
                }
                else -> createSoapResponse("")
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理 ConnectionManager 请求失败", e)
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error")
        }
    }

    private fun handleEventSubscription(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val method = session.method.name
        Log.d(TAG, "事件订阅请求: method=$method, uri=${session.uri}")
        return when (method) {
            "SUBSCRIBE" -> {
                val sid = "uuid:" + UUID.randomUUID()
                NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/xml", "").apply {
                    addHeader("SID", sid)
                    addHeader("TIMEOUT", "Second-1800")
                }
            }
            "UNSUBSCRIBE" -> NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/xml", "")
            else -> NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, NanoHTTPD.MIME_PLAINTEXT, "Method Not Allowed")
        }
    }

    private fun handleGetCurrentTransportActions(): NanoHTTPD.Response {
        val actions = when (transportState) {
            "NO_MEDIA_PRESENT" -> "Play"
            "STOPPED" -> "Play,Seek"
            "PAUSED_PLAYBACK" -> "Play,Stop,Seek"
            "PLAYING" -> "Pause,Stop,Seek"
            else -> "Play,Pause,Stop,Seek"
        }
        return createSoapResponse(
            """<u:GetCurrentTransportActionsResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
    <Actions>$actions</Actions>
</u:GetCurrentTransportActionsResponse>"""
        )
    }

    private fun handleGetDeviceCapabilities(): NanoHTTPD.Response {
        return createSoapResponse(
            """<u:GetDeviceCapabilitiesResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
    <PlayMedia>NETWORK,NONE</PlayMedia>
    <RecMedia>NOT_IMPLEMENTED</RecMedia>
    <RecQualityModes>NOT_IMPLEMENTED</RecQualityModes>
</u:GetDeviceCapabilitiesResponse>"""
        )
    }

    private fun handleGetMediaInfo(): NanoHTTPD.Response {
        val duration = formatTime(currentDuration)
        val uri = escapeXml(currentUri)
        val metadata = escapeXml(currentMetadata)
        return createSoapResponse(
            """<u:GetMediaInfoResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
    <NrTracks>1</NrTracks>
    <MediaDuration>$duration</MediaDuration>
    <CurrentURI>$uri</CurrentURI>
    <CurrentURIMetaData>$metadata</CurrentURIMetaData>
    <NextURI></NextURI>
    <NextURIMetaData></NextURIMetaData>
    <PlayMedium>NETWORK</PlayMedium>
    <RecordMedium>NOT_IMPLEMENTED</RecordMedium>
    <WriteStatus>NOT_IMPLEMENTED</WriteStatus>
</u:GetMediaInfoResponse>"""
        )
    }

    private fun handleGetPositionInfo(): NanoHTTPD.Response {
        val position = formatTime(currentPosition)
        val duration = formatTime(currentDuration)
        val metadata = escapeXml(currentMetadata)
        val uri = escapeXml(currentUri)
        return createSoapResponse(
            """<u:GetPositionInfoResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
    <Track>1</Track>
    <TrackDuration>$duration</TrackDuration>
    <TrackMetaData>$metadata</TrackMetaData>
    <TrackURI>$uri</TrackURI>
    <RelTime>$position</RelTime>
    <AbsTime>$position</AbsTime>
    <RelCount>2147483647</RelCount>
    <AbsCount>2147483647</AbsCount>
</u:GetPositionInfoResponse>"""
        )
    }

    private fun handleGetTransportInfo(): NanoHTTPD.Response {
        return createSoapResponse(
            """<u:GetTransportInfoResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
    <CurrentTransportState>$transportState</CurrentTransportState>
    <CurrentTransportStatus>OK</CurrentTransportStatus>
    <CurrentSpeed>1</CurrentSpeed>
</u:GetTransportInfoResponse>"""
        )
    }

    private fun handleGetTransportSettings(): NanoHTTPD.Response {
        return createSoapResponse(
            """<u:GetTransportSettingsResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
    <PlayMode>NORMAL</PlayMode>
    <RecQualityMode>NOT_IMPLEMENTED</RecQualityMode>
</u:GetTransportSettingsResponse>"""
        )
    }

    private fun handlePause(): NanoHTTPD.Response {
        Log.d(TAG, "Pause")
        transportState = "PAUSED_PLAYBACK"
        val intent = Intent(DlnaConstants.ACTION_CONTROL)
        intent.putExtra(DlnaConstants.EXTRA_COMMAND, DlnaConstants.CMD_PAUSE)
        intent.setPackage(service.packageName)
        service.sendBroadcast(intent)
        return createSoapResponse(
            """<u:PauseResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
</u:PauseResponse>"""
        )
    }

    private fun handlePlay(): NanoHTTPD.Response {
        Log.d(TAG, "Play")
        transportState = "PLAYING"
        val intent = Intent(DlnaConstants.ACTION_CONTROL)
        intent.putExtra(DlnaConstants.EXTRA_COMMAND, DlnaConstants.CMD_PLAY)
        intent.setPackage(service.packageName)
        service.sendBroadcast(intent)
        return createSoapResponse(
            """<u:PlayResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
</u:PlayResponse>"""
        )
    }

    private fun handleStop(): NanoHTTPD.Response {
        Log.d(TAG, "Stop")
        transportState = "STOPPED"
        val intent = Intent(DlnaConstants.ACTION_CONTROL)
        intent.putExtra(DlnaConstants.EXTRA_COMMAND, DlnaConstants.CMD_STOP)
        intent.setPackage(service.packageName)
        service.sendBroadcast(intent)
        return createSoapResponse(
            """<u:StopResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
</u:StopResponse>"""
        )
    }

    private fun handleSeek(soapBody: String): NanoHTTPD.Response {
        val unit = extractXmlValue(soapBody, "Unit") ?: "REL_TIME"
        val target = extractXmlValue(soapBody, "Target") ?: "00:00:00"
        Log.d(TAG, "Seek: unit=$unit, target=$target")
        val position = parseSeekTarget(unit, target)
        val intent = Intent(DlnaConstants.ACTION_CONTROL)
        intent.putExtra(DlnaConstants.EXTRA_COMMAND, DlnaConstants.CMD_SEEK)
        intent.putExtra(DlnaConstants.EXTRA_POSITION, position)
        intent.setPackage(service.packageName)
        service.sendBroadcast(intent)
        return createSoapResponse(
            """<u:SeekResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
</u:SeekResponse>"""
        )
    }

    private fun handleSetAvTransportUri(soapBody: String): NanoHTTPD.Response {
        val uri = extractXmlValue(soapBody, "CurrentURI") ?: ""
        val metadata = extractXmlValue(soapBody, "CurrentURIMetaData") ?: ""
        Log.d(TAG, "SetAVTransportURI: uri=$uri")
        currentUri = uri
        currentMetadata = metadata
        transportState = "STOPPED"
        service.handlePlayRequest(uri, extractXmlValue(metadata, "dc:title"), metadata)
        return createSoapResponse(
            """<u:SetAVTransportURIResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
</u:SetAVTransportURIResponse>"""
        )
    }

    private fun handleSetNextAvTransportUri(soapBody: String): NanoHTTPD.Response {
        val nextUri = extractXmlValue(soapBody, "NextURI") ?: ""
        extractXmlValue(soapBody, "NextURIMetaData")
        Log.d(TAG, "SetNextAVTransportURI: nextUri=$nextUri")
        return createSoapResponse(
            """<u:SetNextAVTransportURIResponse xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
</u:SetNextAVTransportURIResponse>"""
        )
    }

    private fun handleRenderingControl(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val postData = files["postData"] ?: ""
            val soapAction = session.headers["soapaction"] ?: ""
            val action = soapAction.replace("\"", "").substringAfterLast("#")
            Log.d(TAG, "========== RenderingControl SOAP 请求 ==========")
            Log.d(TAG, "Action: $action")
            Log.d(TAG, "SOAP Body: $postData")
            when (action) {
                "SetVolume" -> createSoapResponse(
                    """<u:SetVolumeResponse xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
</u:SetVolumeResponse>"""
                )
                "GetVolume" -> createSoapResponse(
                    """<u:GetVolumeResponse xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
    <CurrentVolume>50</CurrentVolume>
</u:GetVolumeResponse>"""
                )
                "SetMute" -> createSoapResponse(
                    """<u:SetMuteResponse xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
</u:SetMuteResponse>"""
                )
                "GetMute" -> createSoapResponse(
                    """<u:GetMuteResponse xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
    <CurrentMute>0</CurrentMute>
</u:GetMuteResponse>"""
                )
                else -> createSoapResponse("")
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理 RenderingControl 请求失败", e)
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error")
        }
    }

    private fun parseSeekTarget(unit: String, target: String): Long {
        val upperUnit = unit.uppercase(Locale.ROOT)
        if (upperUnit == "REL_TIME" || upperUnit == "ABS_TIME") {
            val parts = target.split(":")
            if (parts.size == 3) {
                val hours = parts[0].toLongOrNull() ?: 0L
                val minutes = parts[1].toLongOrNull() ?: 0L
                val secParts = parts[2].split(".")
                val seconds = secParts[0].toLongOrNull() ?: 0L
                var millis = 0L
                if (secParts.size > 1) {
                    millis = secParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                }
                return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
            }
        } else {
            return target.toLongOrNull() ?: 0L
        }
        return 0L
    }

    private fun serveAvTransportDescription(): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/xml; charset=\"utf-8\"",
            AV_TRANSPORT_SCPD
        )
    }

    private fun serveConnectionManagerDescription(): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/xml",
            CONNECTION_MANAGER_SCPD
        )
    }

    private fun serveDeviceDescription(): NanoHTTPD.Response {
        val localIp = getLocalIp()
        val listeningPort = listeningPort
        val name = deviceName
        val uuid = deviceUuid
        val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<root xmlns=\"urn:schemas-upnp-org:device-1-0\" xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">\n" +
            "    <specVersion>\n" +
            "        <major>1</major>\n" +
            "        <minor>0</minor>\n" +
            "    </specVersion>\n" +
            "    <device>\n" +
            "        <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>\n" +
            "        <presentationURL>/</presentationURL>\n" +
            "        <friendlyName>$name</friendlyName>\n" +
            "        <manufacturer>PP TV</manufacturer>\n" +
            "        <manufacturerURL>https://github.com/ppembytv</manufacturerURL>\n" +
            "        <modelDescription>DLNA Digital Media Renderer</modelDescription>\n" +
            "        <modelName>PP TV</modelName>\n" +
            "        <modelNumber>1.0</modelNumber>\n" +
            "        <modelURL>https://github.com/chaichai</modelURL>\n" +
            "        <UPC>000000000000</UPC>\n" +
            "        <serialNumber>$uuid</serialNumber>\n" +
            "        <UDN>uuid:$uuid</UDN>\n" +
            "        <dlna:X_DLNADOC xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">DMR-1.50</dlna:X_DLNADOC>\n" +
            "        <serviceList>\n" +
            "            <service>\n" +
            "                <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>\n" +
            "                <serviceId>urn:upnp-org:serviceId:AVTransport</serviceId>\n" +
            "                <SCPDURL>/AVTransport.xml</SCPDURL>\n" +
            "                <controlURL>/AVTransport/control</controlURL>\n" +
            "                <eventSubURL>/AVTransport/event</eventSubURL>\n" +
            "            </service>\n" +
            "            <service>\n" +
            "                <serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType>\n" +
            "                <serviceId>urn:upnp-org:serviceId:ConnectionManager</serviceId>\n" +
            "                <SCPDURL>/ConnectionManager.xml</SCPDURL>\n" +
            "                <controlURL>/ConnectionManager/control</controlURL>\n" +
            "                <eventSubURL>/ConnectionManager/event</eventSubURL>\n" +
            "            </service>\n" +
            "            <service>\n" +
            "                <serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType>\n" +
            "                <serviceId>urn:upnp-org:serviceId:RenderingControl</serviceId>\n" +
            "                <SCPDURL>/RenderingControl.xml</SCPDURL>\n" +
            "                <controlURL>/RenderingControl/control</controlURL>\n" +
            "                <eventSubURL>/RenderingControl/event</eventSubURL>\n" +
            "            </service>\n" +
            "        </serviceList>\n" +
            "    </device>\n" +
            "    <URLBase>http://$localIp:$listeningPort</URLBase>\n" +
            "</root>"
        Log.d(TAG, "设备描述 XML 长度: ${xml.length}")
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/xml; charset=\"utf-8\"", xml)
    }

    private fun serveRenderingControlDescription(): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/xml",
            RENDERING_CONTROL_SCPD
        )
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri = session.uri
        val method = session.method
        Log.d(TAG, "========== 收到 HTTP 请求 ==========")
        Log.d(TAG, "方法: $method, URI: $uri")
        Log.d(TAG, "来源: ${session.remoteIpAddress}")
        Log.d(TAG, "Headers: ${session.headers}")
        val response = when {
            uri == "/description.xml" -> {
                Log.d(TAG, "返回设备描述文档")
                serveDeviceDescription()
            }
            uri == "/AVTransport.xml" -> {
                Log.d(TAG, "返回 AVTransport 服务描述")
                serveAvTransportDescription()
            }
            uri == "/RenderingControl.xml" -> {
                Log.d(TAG, "返回 RenderingControl 服务描述")
                serveRenderingControlDescription()
            }
            uri == "/ConnectionManager.xml" -> {
                Log.d(TAG, "返回 ConnectionManager 服务描述")
                serveConnectionManagerDescription()
            }
            uri == "/AVTransport/control" -> handleAvTransportControl(session)
            uri == "/RenderingControl/control" -> handleRenderingControl(session)
            uri == "/ConnectionManager/control" -> handleConnectionManager(session)
            uri.endsWith("/event") -> handleEventSubscription(session)
            else -> {
                Log.w(TAG, "未知请求: $uri")
                NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
            }
        }
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, SUBSCRIBE, UNSUBSCRIBE")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, SOAPACTION, CALLBACK, NT, TIMEOUT")
        Log.d(TAG, "响应状态: ${response.status}")
        return response
    }

    override fun start() {
        super.start()
        Log.d(TAG, "HTTP 服务器已启动: hostname=$hostname, port=$listeningPort")
        Log.d(TAG, "访问地址: http://${getLocalIp()}:$listeningPort/description.xml")
    }

    fun updatePlaybackState(state: String, position: Long, duration: Long) {
        transportState = state
        currentPosition = position
        currentDuration = duration
    }

    companion object {
        const val PORT_RANGE_START = 49152
        const val PORT_RANGE_END = 65535
        private const val TAG = "DlnaHttpServer"

        private val AV_TRANSPORT_SCPD = """<?xml version="1.0" encoding="UTF-8"?>
<scpd xmlns="urn:schemas-upnp-org:service-1-0">
    <specVersion><major>1</major><minor>0</minor></specVersion>
    <actionList>
        <action><name>SetAVTransportURI</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>CurrentURI</name><direction>in</direction><relatedStateVariable>AVTransportURI</relatedStateVariable></argument>
                <argument><name>CurrentURIMetaData</name><direction>in</direction><relatedStateVariable>AVTransportURIMetaData</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>SetNextAVTransportURI</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>NextURI</name><direction>in</direction><relatedStateVariable>NextAVTransportURI</relatedStateVariable></argument>
                <argument><name>NextURIMetaData</name><direction>in</direction><relatedStateVariable>NextAVTransportURIMetaData</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>Play</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>Speed</name><direction>in</direction><relatedStateVariable>TransportPlaySpeed</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>Pause</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>Stop</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>Seek</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>Unit</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_SeekMode</relatedStateVariable></argument>
                <argument><name>Target</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_SeekTarget</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetTransportInfo</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>CurrentTransportState</name><direction>out</direction><relatedStateVariable>TransportState</relatedStateVariable></argument>
                <argument><name>CurrentTransportStatus</name><direction>out</direction><relatedStateVariable>TransportStatus</relatedStateVariable></argument>
                <argument><name>CurrentSpeed</name><direction>out</direction><relatedStateVariable>TransportPlaySpeed</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetPositionInfo</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>Track</name><direction>out</direction><relatedStateVariable>CurrentTrack</relatedStateVariable></argument>
                <argument><name>TrackDuration</name><direction>out</direction><relatedStateVariable>CurrentTrackDuration</relatedStateVariable></argument>
                <argument><name>TrackMetaData</name><direction>out</direction><relatedStateVariable>CurrentTrackMetaData</relatedStateVariable></argument>
                <argument><name>TrackURI</name><direction>out</direction><relatedStateVariable>CurrentTrackURI</relatedStateVariable></argument>
                <argument><name>RelTime</name><direction>out</direction><relatedStateVariable>RelativeTimePosition</relatedStateVariable></argument>
                <argument><name>AbsTime</name><direction>out</direction><relatedStateVariable>AbsoluteTimePosition</relatedStateVariable></argument>
                <argument><name>RelCount</name><direction>out</direction><relatedStateVariable>RelativeCounterPosition</relatedStateVariable></argument>
                <argument><name>AbsCount</name><direction>out</direction><relatedStateVariable>AbsoluteCounterPosition</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetMediaInfo</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>NrTracks</name><direction>out</direction><relatedStateVariable>NumberOfTracks</relatedStateVariable></argument>
                <argument><name>MediaDuration</name><direction>out</direction><relatedStateVariable>CurrentMediaDuration</relatedStateVariable></argument>
                <argument><name>CurrentURI</name><direction>out</direction><relatedStateVariable>AVTransportURI</relatedStateVariable></argument>
                <argument><name>CurrentURIMetaData</name><direction>out</direction><relatedStateVariable>AVTransportURIMetaData</relatedStateVariable></argument>
                <argument><name>NextURI</name><direction>out</direction><relatedStateVariable>NextAVTransportURI</relatedStateVariable></argument>
                <argument><name>NextURIMetaData</name><direction>out</direction><relatedStateVariable>NextAVTransportURIMetaData</relatedStateVariable></argument>
                <argument><name>PlayMedium</name><direction>out</direction><relatedStateVariable>PlaybackStorageMedium</relatedStateVariable></argument>
                <argument><name>RecordMedium</name><direction>out</direction><relatedStateVariable>RecordStorageMedium</relatedStateVariable></argument>
                <argument><name>WriteStatus</name><direction>out</direction><relatedStateVariable>RecordMediumWriteStatus</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetDeviceCapabilities</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>PlayMedia</name><direction>out</direction><relatedStateVariable>PossiblePlaybackStorageMedia</relatedStateVariable></argument>
                <argument><name>RecMedia</name><direction>out</direction><relatedStateVariable>PossibleRecordStorageMedia</relatedStateVariable></argument>
                <argument><name>RecQualityModes</name><direction>out</direction><relatedStateVariable>PossibleRecordQualityModes</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetTransportSettings</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>PlayMode</name><direction>out</direction><relatedStateVariable>CurrentPlayMode</relatedStateVariable></argument>
                <argument><name>RecQualityMode</name><direction>out</direction><relatedStateVariable>CurrentRecordQualityMode</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetCurrentTransportActions</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>Actions</name><direction>out</direction><relatedStateVariable>CurrentTransportActions</relatedStateVariable></argument>
            </argumentList>
        </action>
    </actionList>
    <serviceStateTable>
        <stateVariable sendEvents="no"><name>TransportState</name><dataType>string</dataType><allowedValueList><allowedValue>STOPPED</allowedValue><allowedValue>PLAYING</allowedValue><allowedValue>TRANSITIONING</allowedValue><allowedValue>PAUSED_PLAYBACK</allowedValue><allowedValue>NO_MEDIA_PRESENT</allowedValue></allowedValueList></stateVariable>
        <stateVariable sendEvents="no"><name>TransportStatus</name><dataType>string</dataType><allowedValueList><allowedValue>OK</allowedValue><allowedValue>ERROR_OCCURRED</allowedValue></allowedValueList></stateVariable>
        <stateVariable sendEvents="no"><name>TransportPlaySpeed</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>AVTransportURI</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>AVTransportURIMetaData</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>NextAVTransportURI</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>NextAVTransportURIMetaData</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>CurrentTrack</name><dataType>ui4</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>CurrentTrackDuration</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>CurrentTrackMetaData</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>CurrentTrackURI</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>RelativeTimePosition</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>AbsoluteTimePosition</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>RelativeCounterPosition</name><dataType>i4</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>AbsoluteCounterPosition</name><dataType>i4</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>NumberOfTracks</name><dataType>ui4</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>CurrentMediaDuration</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>PlaybackStorageMedium</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>RecordStorageMedium</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>RecordMediumWriteStatus</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>PossiblePlaybackStorageMedia</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>PossibleRecordStorageMedia</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>PossibleRecordQualityModes</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>CurrentPlayMode</name><dataType>string</dataType><allowedValueList><allowedValue>NORMAL</allowedValue></allowedValueList></stateVariable>
        <stateVariable sendEvents="no"><name>CurrentRecordQualityMode</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>CurrentTransportActions</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_InstanceID</name><dataType>ui4</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_SeekMode</name><dataType>string</dataType><allowedValueList><allowedValue>REL_TIME</allowedValue><allowedValue>ABS_TIME</allowedValue></allowedValueList></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_SeekTarget</name><dataType>string</dataType></stateVariable>
    </serviceStateTable>
</scpd>"""

        private val CONNECTION_MANAGER_SCPD = """<?xml version="1.0" encoding="UTF-8"?>
<scpd xmlns="urn:schemas-upnp-org:service-1-0">
    <specVersion><major>1</major><minor>0</minor></specVersion>
    <actionList>
        <action><name>GetProtocolInfo</name>
            <argumentList>
                <argument><name>Source</name><direction>out</direction><relatedStateVariable>SourceProtocolInfo</relatedStateVariable></argument>
                <argument><name>Sink</name><direction>out</direction><relatedStateVariable>SinkProtocolInfo</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetCurrentConnectionIDs</name>
            <argumentList>
                <argument><name>ConnectionIDs</name><direction>out</direction><relatedStateVariable>CurrentConnectionIDs</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetCurrentConnectionInfo</name>
            <argumentList>
                <argument><name>ConnectionID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_ConnectionID</relatedStateVariable></argument>
                <argument><name>RcsID</name><direction>out</direction><relatedStateVariable>A_ARG_TYPE_RcsID</relatedStateVariable></argument>
                <argument><name>AVTransportID</name><direction>out</direction><relatedStateVariable>A_ARG_TYPE_AVTransportID</relatedStateVariable></argument>
                <argument><name>ProtocolInfo</name><direction>out</direction><relatedStateVariable>A_ARG_TYPE_ProtocolInfo</relatedStateVariable></argument>
                <argument><name>PeerConnectionManager</name><direction>out</direction><relatedStateVariable>A_ARG_TYPE_ConnectionManager</relatedStateVariable></argument>
                <argument><name>PeerConnectionID</name><direction>out</direction><relatedStateVariable>A_ARG_TYPE_ConnectionID</relatedStateVariable></argument>
                <argument><name>Direction</name><direction>out</direction><relatedStateVariable>A_ARG_TYPE_Direction</relatedStateVariable></argument>
                <argument><name>Status</name><direction>out</direction><relatedStateVariable>A_ARG_TYPE_ConnectionStatus</relatedStateVariable></argument>
            </argumentList>
        </action>
    </actionList>
    <serviceStateTable>
        <stateVariable sendEvents="yes"><name>SourceProtocolInfo</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="yes"><name>SinkProtocolInfo</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="yes"><name>CurrentConnectionIDs</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_ConnectionStatus</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_ConnectionManager</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_Direction</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_ProtocolInfo</name><dataType>string</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_ConnectionID</name><dataType>i4</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_AVTransportID</name><dataType>i4</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_RcsID</name><dataType>i4</dataType></stateVariable>
    </serviceStateTable>
</scpd>"""

        private val RENDERING_CONTROL_SCPD = """<?xml version="1.0" encoding="UTF-8"?>
<scpd xmlns="urn:schemas-upnp-org:service-1-0">
    <specVersion><major>1</major><minor>0</minor></specVersion>
    <actionList>
        <action><name>GetVolume</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>Channel</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_Channel</relatedStateVariable></argument>
                <argument><name>CurrentVolume</name><direction>out</direction><relatedStateVariable>Volume</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>SetVolume</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>Channel</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_Channel</relatedStateVariable></argument>
                <argument><name>DesiredVolume</name><direction>in</direction><relatedStateVariable>Volume</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>GetMute</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>Channel</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_Channel</relatedStateVariable></argument>
                <argument><name>CurrentMute</name><direction>out</direction><relatedStateVariable>Mute</relatedStateVariable></argument>
            </argumentList>
        </action>
        <action><name>SetMute</name>
            <argumentList>
                <argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument>
                <argument><name>Channel</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_Channel</relatedStateVariable></argument>
                <argument><name>DesiredMute</name><direction>in</direction><relatedStateVariable>Mute</relatedStateVariable></argument>
            </argumentList>
        </action>
    </actionList>
    <serviceStateTable>
        <stateVariable sendEvents="no"><name>Volume</name><dataType>ui2</dataType><allowedValueRange><minimum>0</minimum><maximum>100</maximum></allowedValueRange></stateVariable>
        <stateVariable sendEvents="no"><name>Mute</name><dataType>boolean</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_InstanceID</name><dataType>ui4</dataType></stateVariable>
        <stateVariable sendEvents="no"><name>A_ARG_TYPE_Channel</name><dataType>string</dataType></stateVariable>
    </serviceStateTable>
</scpd>"""
    }
}
