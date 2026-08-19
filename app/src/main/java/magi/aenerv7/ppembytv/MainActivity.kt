package magi.aenerv7.ppembytv

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import magi.aenerv7.ppembytv.data.DeviceIdManager
import magi.aenerv7.ppembytv.data.ProxyManager
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import magi.aenerv7.ppembytv.data.preferences.ServerPreferences
import magi.aenerv7.ppembytv.data.preferences.UserPreferences
import magi.aenerv7.ppembytv.dlna.DlnaConstants
import magi.aenerv7.ppembytv.dlna.DlnaService
import magi.aenerv7.ppembytv.dlna.DlnaSettings
import magi.aenerv7.ppembytv.ui.AppRoot

class MainActivity : ComponentActivity() {

    companion object {
        private const val APP_VERSION = "0.3.1"
    }

    private lateinit var userPreferences: UserPreferences
    private var dlnaReceiver: DlnaPlayRequestReceiver? = null

    private val dlnaPlayRequestState = mutableStateOf<DlnaPlayRequestData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences = UserPreferences(this)

        val deviceId = DeviceIdManager.getDeviceId(this)
        RetrofitClient.setDeviceId(deviceId)
        Log.i("MainActivity", "应用启动，DeviceId已初始化: $deviceId")
        RetrofitClient.setAppVersion(APP_VERSION)

        val lastUsedServer = ServerPreferences(this).getLastUsedServer()
        val server = lastUsedServer?.effectiveServerConfig?.takeIf { it.isLoggedIn() }
        if (server == null) {
            RetrofitClient.setAuthToken("", "")
            RetrofitClient.setTrustAllCerts(false)
            ProxyManager.setDirectOnly(false)
            Log.i("MainActivity", "启动时未找到可恢复的已登录服务器")
        } else {
            RetrofitClient.initialize(server.fullUrl)
            RetrofitClient.setAuthToken(server.accessToken.orEmpty(), server.userId.orEmpty())
            RetrofitClient.setDeviceId(deviceId)
            RetrofitClient.setTrustAllCerts(server.trustAllCerts)
            ProxyManager.setDirectOnly(server.directOnly)
            Log.i("MainActivity", "启动时恢复服务器配置: id=${server.id}, alias=${server.alias}")
        }

        if (DlnaSettings(this).configSync.enabled) {
            DlnaService.start(this)
            Log.i("MainActivity", "DLNA 服务已启动")
        }

        val receiver = DlnaPlayRequestReceiver(this)
        dlnaReceiver = receiver
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(DlnaConstants.ACTION_PLAY_REQUEST),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            AppRoot(userPreferences = userPreferences, dlnaPlayRequestState = dlnaPlayRequestState)
        }
    }

    fun onDlnaPlayRequest(data: DlnaPlayRequestData) {
        dlnaPlayRequestState.value = data
    }

    override fun onDestroy() {
        super.onDestroy()
        dlnaReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
            }
        }
        dlnaReceiver = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.i("MainActivity", "复用已有任务处理新 Intent: taskId=${taskId}, flags=${intent.flags}, action=${intent.action}")
    }
}
