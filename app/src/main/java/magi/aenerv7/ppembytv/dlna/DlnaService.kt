package magi.aenerv7.ppembytv.dlna

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import magi.aenerv7.ppembytv.MainActivity
import magi.aenerv7.ppembytv.R
import java.net.BindException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Foreground service that runs the DLNA media renderer: a local [DlnaHttpServer] plus an
 * SSDP responder/notifier on 239.255.255.250:1900. Ported faithfully from
 * `com.dh.myembyapp.dlna.DlnaService`.
 */
class DlnaService : Service() {

    private val binder = LocalBinder()
    private val executor = Executors.newCachedThreadPool()

    private lateinit var deviceName: String
    private lateinit var deviceUuid: String
    private var httpPort: Int = 0
    private var httpServer: DlnaHttpServer? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var ssdpThread: Thread? = null

    inner class LocalBinder : Binder() {
        fun getService(): DlnaService = this@DlnaService
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifiManager.createMulticastLock("dlna_multicast_lock")
            multicastLock = lock
            lock.setReferenceCounted(true)
            lock.acquire()
            Log.d(TAG, "Multicast Lock 已获取")
        } catch (e: Exception) {
            Log.e(TAG, "获取 Multicast Lock 失败", e)
        }
    }

    private fun createNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DLNA 投屏已开启")
            .setContentText("设备名称: $deviceName")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "DLNA 投屏服务", NotificationManager.IMPORTANCE_LOW)
            channel.description = "允许其他设备投屏到本设备"
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun extractHeader(message: String, header: String): String? {
        for (line in message.split("\r\n")) {
            if (line.uppercase(Locale.ROOT).startsWith(header.uppercase(Locale.ROOT) + ":")) {
                return line.substring(header.length + 1).trim()
            }
        }
        return null
    }

    private fun getLocalIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "获取本地 IP 失败", e)
            return null
        }
    }

    private fun getOrCreateDeviceUuid(): String {
        val prefs = getSharedPreferences("dlna_prefs", Context.MODE_PRIVATE)
        prefs.getString("device_uuid", null)?.let { return it }
        val uuid = UUID.randomUUID().toString()
        prefs.edit().putString("device_uuid", uuid).apply()
        return uuid
    }

    private fun handleSsdpMessage(message: String, address: InetAddress, port: Int) {
        if (message.startsWith("M-SEARCH")) {
            val st = extractHeader(message, "ST")
            Log.d(TAG, "收到 M-SEARCH: ST=$st, UA=${extractHeader(message, "USER-AGENT")}, from=$address:$port")
            if (st == null) {
                return
            }
            if (st == "ssdp:all" || st == "upnp:rootdevice" ||
                st.contains("MediaRenderer") || st.contains("AVTransport") ||
                st.contains("RenderingControl") || st.contains("ConnectionManager")
            ) {
                executor.execute {
                    if (st == "ssdp:all") {
                        sendSsdpResponse(address, port, "upnp:rootdevice")
                        Thread.sleep(100)
                        sendSsdpResponse(address, port, "urn:schemas-upnp-org:device:MediaRenderer:1")
                        Thread.sleep(100)
                        sendSsdpResponse(address, port, "urn:schemas-upnp-org:service:AVTransport:1")
                    } else {
                        sendSsdpResponse(address, port, st)
                    }
                }
            } else {
                Log.d(TAG, "不响应此 ST: $st")
            }
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.release()
            multicastLock = null
            Log.d(TAG, "Multicast Lock 已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放 Multicast Lock 失败", e)
        }
    }

    private fun sendSsdpNotify() {
        executor.execute {
            try {
                val localIp = getLocalIpAddress()
                if (localIp == null) {
                    Log.e(TAG, "无法获取本地 IP，跳过 SSDP NOTIFY")
                    return@execute
                }
                if (httpPort == 0) {
                    Log.w(TAG, "HTTP 服务器未启动，跳过 SSDP NOTIFY")
                    return@execute
                }
                val location = "http://$localIp:$httpPort/description.xml"
                Log.d(TAG, "发送 SSDP NOTIFY, Location: $location")
                val notifications = listOf(
                    "upnp:rootdevice" to "uuid:$deviceUuid::upnp:rootdevice",
                    "uuid:$deviceUuid" to "uuid:$deviceUuid",
                    "urn:schemas-upnp-org:device:MediaRenderer:1" to "uuid:$deviceUuid::urn:schemas-upnp-org:device:MediaRenderer:1",
                    "urn:schemas-upnp-org:service:AVTransport:1" to "uuid:$deviceUuid::urn:schemas-upnp-org:service:AVTransport:1",
                    "urn:schemas-upnp-org:service:RenderingControl:1" to "uuid:$deviceUuid::urn:schemas-upnp-org:service:RenderingControl:1",
                    "urn:schemas-upnp-org:service:ConnectionManager:1" to "uuid:$deviceUuid::urn:schemas-upnp-org:service:ConnectionManager:1"
                )
                val group = InetAddress.getByName(SSDP_ADDRESS)
                DatagramSocket().use { socket ->
                    for ((nt, usn) in notifications) {
                        val message = buildString {
                            append("NOTIFY * HTTP/1.1\r\n")
                            append("HOST: 239.255.255.250:1900\r\n")
                            append("CACHE-CONTROL: max-age=1800\r\n")
                            append("LOCATION: $location\r\n")
                            append("NT: $nt\r\n")
                            append("NTS: ssdp:alive\r\n")
                            append("SERVER: Linux/3.0 UPnP/1.0 PPEmbyTV/1.0\r\n")
                            append("USN: $usn\r\n")
                            append("\r\n")
                        }
                        val bytes = message.toByteArray(Charsets.UTF_8)
                        val packet = DatagramPacket(bytes, bytes.size, group, SSDP_PORT)
                        socket.send(packet)
                        Thread.sleep(100)
                        socket.send(packet)
                    }
                }
                Log.d(TAG, "SSDP NOTIFY 广播已发送")
            } catch (e: Exception) {
                Log.e(TAG, "发送 SSDP NOTIFY 失败", e)
            }
        }
    }

    private fun sendSsdpResponse(address: InetAddress, port: Int, searchTarget: String) {
        try {
            val localIp = getLocalIpAddress()
            if (localIp == null) {
                Log.e(TAG, "无法获取本地 IP 地址")
                return
            }
            if (httpPort == 0) {
                Log.w(TAG, "HTTP 服务器未启动，跳过 SSDP 响应")
                return
            }
            val location = "http://$localIp:$httpPort/description.xml"
            Log.d(TAG, "SSDP 响应 Location: $location")
            var st = "upnp:rootdevice"
            if (searchTarget != "ssdp:all" && searchTarget != "upnp:rootdevice") {
                val usn = "uuid:$deviceUuid::$searchTarget"
                val message = buildString {
                    append("HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=1800\r\n")
                    append("DATE: " + SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).format(Date()) + "\r\n")
                    append("EXT:\r\n")
                    append("LOCATION: $location\r\n")
                    append("SERVER: Linux/3.0 UPnP/1.0 PPEmbyTV/1.0\r\n")
                    append("ST: $st\r\n")
                    append("USN: $usn\r\n")
                    append("\r\n")
                }
                sendResponseDatagram(localIp, address, port, message)
                Log.d(TAG, "已发送 SSDP 响应到 $address:$port, ST=$st, Location=$location (从 $localIp)")
            }
            val rootUsn = "uuid:$deviceUuid::upnp:rootdevice"
            val message = buildString {
                append("HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=1800\r\n")
                append("DATE: " + SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).format(Date()) + "\r\n")
                append("EXT:\r\n")
                append("LOCATION: $location\r\n")
                append("SERVER: Linux/3.0 UPnP/1.0 ChaiChaiTV/1.0\r\n")
                append("ST: $st\r\n")
                append("USN: $rootUsn\r\n")
                append("\r\n")
            }
            sendResponseDatagram(localIp, address, port, message)
            Log.d(TAG, "已发送 SSDP 响应到 $address:$port, ST=$st, Location=$location (从 $localIp)")
        } catch (e: Exception) {
            Log.e(TAG, "发送 SSDP 响应失败", e)
        }
    }

    private fun sendResponseDatagram(localIp: String, target: InetAddress, targetPort: Int, message: String) {
        val socket = DatagramSocket(null)
        try {
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(InetAddress.getByName(localIp), 0))
            val bytes = message.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(bytes, bytes.size, target, targetPort)
            socket.send(packet)
            Thread.sleep(50)
            socket.send(packet)
            Thread.sleep(50)
            socket.send(packet)
        } finally {
            socket.close()
        }
    }

    private fun startHttpServer() {
        try {
            stopHttpServer()
            var started = false
            var port = DlnaHttpServer.PORT_RANGE_START
            while (!started && port <= DlnaHttpServer.PORT_RANGE_END) {
                try {
                    val server = DlnaHttpServer(this, deviceUuid, deviceName, port)
                    httpServer = server
                    server.start()
                    started = true
                    httpPort = httpServer?.listeningPort ?: port
                    Log.d(TAG, "HTTP 服务器启动成功，端口: $httpPort")
                } catch (e: BindException) {
                    Log.d(TAG, "端口 $port 被占用，尝试下一个")
                    httpServer = null
                    port++
                }
            }
            if (!started) {
                Log.e(TAG, "HTTP 服务器启动失败，无可用端口")
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP 服务器启动失败", e)
        }
    }

    private fun startSsdpListener() {
        val thread = Thread {
            try {
                val socket = MulticastSocket(SSDP_PORT)
                socket.reuseAddress = true
                val group = InetAddress.getByName(SSDP_ADDRESS)
                socket.joinGroup(group)
                Log.d(TAG, "SSDP 监听已启动")
                val buffer = ByteArray(8192)
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        val packet = DatagramPacket(buffer, 8192)
                        socket.receive(packet)
                        val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        handleSsdpMessage(message, packet.address!!, packet.port)
                    } catch (e: Exception) {
                        if (!Thread.currentThread().isInterrupted) {
                            Log.e(TAG, "SSDP 接收错误", e)
                        }
                    }
                }
                socket.leaveGroup(group)
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "SSDP 监听启动失败", e)
            }
        }
        ssdpThread = thread
        thread.start()
    }

    private fun stopHttpServer() {
        try {
            httpServer?.let {
                it.stop()
                it.closeAllConnections()
            }
            httpServer = null
            Log.d(TAG, "HTTP 服务器已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止 HTTP 服务器失败", e)
            httpServer = null
        }
    }

    private fun stopSsdpListener() {
        ssdpThread?.interrupt()
        ssdpThread = null
        Log.d(TAG, "SSDP 监听已停止")
    }

    fun handlePlayRequest(uri: String, title: String?, metadata: String?) {
        Log.d(TAG, "处理播放请求: uri=$uri, title=$title")
        val playRequest = DlnaPlayRequest(uri, title, metadata)
        val intent = Intent(DlnaConstants.ACTION_PLAY_REQUEST)
        intent.putExtra(DlnaConstants.EXTRA_URI, uri)
        intent.putExtra(DlnaConstants.EXTRA_TITLE, title)
        intent.putExtra(DlnaConstants.EXTRA_METADATA, metadata)
        intent.setPackage(packageName)
        sendBroadcast(intent)
        onPlayRequest?.invoke(playRequest)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DLNA 服务创建")
        deviceUuid = getOrCreateDeviceUuid()
        deviceName = DlnaSettings(this).configSync.deviceName
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DLNA 服务销毁")
        stopSsdpListener()
        stopHttpServer()
        releaseMulticastLock()
        executor.shutdown()
        isRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DLNA 服务启动")
        startForeground(NOTIFICATION_ID, createNotification())
        acquireMulticastLock()
        executor.execute {
            startHttpServer()
            startSsdpListener()
            sendSsdpNotify()
        }
        isRunning = true
        return START_STICKY
    }

    companion object {
        private const val TAG = "DlnaService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "dlna_service_channel"
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var onPlayRequest: ((DlnaPlayRequest) -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, DlnaService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DlnaService::class.java))
        }
    }
}
