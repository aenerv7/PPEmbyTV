package magi.aenerv7.ppembytv

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.CoilUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import magi.aenerv7.ppembytv.data.ProxyManager
import magi.aenerv7.ppembytv.data.ProxySettings
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import magi.aenerv7.ppembytv.data.model.ServerConfig
import magi.aenerv7.ppembytv.data.preferences.ServerPreferences
import okhttp3.Cache
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.math.roundToLong

class PPEmbyTVApp : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "PPEmbyTVApp"

        @Volatile
        var episodeSortSettings: SharedPreferences? = null

        @Volatile
        var librarySortSettings: SharedPreferences? = null
    }

    private val serverPreferences by lazy { ServerPreferences(this) }

    @Volatile
    private var cachedServers: List<ServerConfig> = emptyList()

    @Volatile
    private var cachedServersTime: Long = 0L

    private val serversLock = Any()

    override fun onCreate() {
        super.onCreate()
        episodeSortSettings = getSharedPreferences("episode_sort_settings", Context.MODE_PRIVATE)
        librarySortSettings = getSharedPreferences("library_sort_settings", Context.MODE_PRIVATE)
        installCrashHandler()
        loadProxyConfig()
    }

    private fun loadProxyConfig() {
        Log.d(TAG, "========== 应用启动：开始加载代理配置 ==========")
        try {
            runBlocking {
                // 注意：必须用 first() 取首个值后结束，不能 collect{}（DataStore 的 data 流
                // 永不结束，会永久阻塞主线程导致启动黑屏）。与反编译参考实现 FlowKt.first 一致。
                val config = ProxySettings(this@PPEmbyTVApp).proxyConfigFlow.first()
                ProxyManager.applyProxyConfig(config)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 加载代理配置失败", e)
        }
        Log.d(TAG, "========== 代理配置加载完成 ==========")
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val message = throwable.message ?: ""
            val isSecurity =
                throwable is SecurityException && (message.contains("com.lenovo.skin") ||
                    (message.contains("Settings key") && message.contains("is not readable")))
            if (isSecurity) {
                Log.w(TAG, "忽略系统设置读取异常（不影响功能）: $message")
                return@setDefaultUncaughtExceptionHandler
            }
            Log.e(TAG, "========== 应用崩溃 ==========")
            Log.e(TAG, "线程: ${thread.name}")
            Log.e(TAG, "异常: ${throwable.javaClass.simpleName}")
            Log.e(TAG, "消息: ${throwable.message}")
            Log.e(TAG, "堆栈跟踪:", throwable)
            val sb = StringBuilder()
            sb.append("崩溃: ${throwable.javaClass.simpleName}\n")
            sb.append("原因: ${throwable.message ?: "未知"}\n")
            val stackElement = throwable.stackTrace.firstOrNull { it.className.contains("magi.aenerv7.ppembytv") }
            if (stackElement != null) {
                sb.append("位置: ${stackElement.fileName}:${stackElement.lineNumber}")
            }
            val crashInfo = sb.toString()
            try {
                Handler(Looper.getMainLooper()).post { }
            } catch (e: Exception) {
            }
            try {
                getSharedPreferences("crash_log", Context.MODE_PRIVATE).edit()
                    .putString("last_crash", crashInfo)
                    .putLong("crash_time", System.currentTimeMillis())
                    .commit()
            } catch (e: Exception) {
                Log.e(TAG, "保存崩溃信息失败", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader {
        val context = this
        val okHttpClient = buildImageOkHttpClient(context)
        val diskCache = DiskCache.Builder()
            .directory(File(context.cacheDir, "image_cache"))
            .maxSizeBytes(computeImageDiskCacheSize(context.cacheDir))
            .build()
        val memoryCache = MemoryCache.Builder(context)
            .maxSizePercent(0.15)
            .build()
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            .crossfade(true)
            .build()
    }

    private fun computeImageDiskCacheSize(cacheDir: File): Long {
        return try {
            cacheDir.mkdir()
            val statFs = StatFs(cacheDir.absolutePath)
            val freeBytes = statFs.blockSizeLong * statFs.blockCountLong
            (freeBytes * 0.05).roundToLong().coerceIn(10_485_760L, 262_144_000L)
        } catch (e: Exception) {
            10_485_760L
        }
    }

    private fun buildImageOkHttpClient(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_image_cache"), 52_428_800L))
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "public, max-age=604800")
                    .build()
            }
        if (RetrofitClient.getTrustAllCerts()) {
            Log.d(TAG, "⚠️ 图片加载器应用不安全的SSL配置（信任所有证书）")
            val (socketFactory, trustManager) = createUnsafeSslContext()
            builder.sslSocketFactory(socketFactory, trustManager)
            builder.hostnameVerifier { _, _ -> true }
        }
        val proxy = ProxyManager.currentProxy
        if (proxy != null) {
            val config = ProxyManager.currentConfig
            builder.proxySelector(object : ProxySelector() {
                override fun select(uri: URI?): List<Proxy> {
                    val host = uri?.host ?: ""
                    if (config.enabled && config.bypassLan && ProxyManager.isLanAddress(host)) {
                        return mutableListOf(Proxy.NO_PROXY)
                    }
                    return mutableListOf(proxy)
                }

                override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: java.io.IOException?) {
                    Log.e(TAG, "图片代理连接失败: $uri", ioe)
                }
            })
            if (proxy.type() == Proxy.Type.SOCKS) {
                builder.dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        return if (config.bypassLan && ProxyManager.isLanAddress(hostname)) {
                            Dns.SYSTEM.lookup(hostname)
                        } else {
                            listOf(InetAddress.getByAddress(hostname, byteArrayOf(0, 0, 0, 0)))
                        }
                    }
                })
            }
            if (config.hasCredentials) {
                builder.proxyAuthenticator(object : okhttp3.Authenticator {
                    override fun authenticate(route: Route?, response: Response): okhttp3.Request? {
                        if (response.request.header("Proxy-Authorization") != null) {
                            return null
                        }
                        return response.request.newBuilder()
                            .header("Proxy-Authorization", Credentials.basic(config.username, config.password))
                            .build()
                    }
                })
            }
        }
        return builder.build()
    }

    private fun createUnsafeSslContext(): Pair<SSLSocketFactory, X509TrustManager> {
        val trustAllCertsManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllCertsManager), SecureRandom())
        return sslContext.socketFactory to trustAllCertsManager
    }

    fun clearCacheByName(name: String, label: String) {
        val file = File(cacheDir, name)
        if (file.exists()) {
            if (file.deleteRecursively()) {
                Log.d(TAG, "✓ $label 已清除")
            } else {
                Log.w(TAG, "$label 清除不完整: ${file.absolutePath}")
            }
        }
    }

    fun clearImageServerMatchCache() {
        synchronized(serversLock) {
            cachedServers = emptyList()
            cachedServersTime = 0L
        }
        Log.d(TAG, "✓ 图片服务器匹配缓存已清除")
    }

    fun clearTempSubtitleFiles() {
        val files = cacheDir.listFiles() ?: return
        var count = 0
        for (file in files) {
            if (file.isFile && file.name.startsWith("subtitle_") &&
                Regex("_modified\\.(srt|ass|ssa)$").containsMatchIn(file.name) && file.delete()
            ) {
                count++
            }
        }
        if (count > 0) {
            Log.d(TAG, "✓ 临时字幕文件已清除: $count 个")
        }
    }

    fun findServerByUrl(httpUrl: HttpUrl): ServerConfig? {
        return allServers()
            .mapNotNull { server ->
                val base = try {
                    server.fullUrl.toHttpUrlOrNull()
                } catch (e: Exception) {
                    null
                }
                if (base == null) null else {
                    var score = 0
                    if (base.host == httpUrl.host) score++
                    if (base.port == httpUrl.port) score++
                    if (base.encodedPath == httpUrl.encodedPath) score++
                    server to score
                }
            }
            .maxByOrNull { it.second }
            ?.first
    }

    fun allServers(): List<ServerConfig> {
        val now = SystemClock.elapsedRealtime()
        cachedServers.takeIf { it.isNotEmpty() && now - cachedServersTime < 1000 }?.let { return it }
        synchronized(serversLock) {
            if (cachedServers.isEmpty() || now - cachedServersTime >= 1000) {
                cachedServers = serverPreferences.getAllServers()
                cachedServersTime = SystemClock.elapsedRealtime()
            }
        }
        return cachedServers
    }
}
