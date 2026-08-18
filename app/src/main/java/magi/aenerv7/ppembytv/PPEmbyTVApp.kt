package magi.aenerv7.ppembytv

import android.app.Application
import android.content.Context
import magi.aenerv7.ppembytv.api.HttpClients
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.data.ServerRepository
import magi.aenerv7.ppembytv.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 全局对象图（简易 DI）。
 */
object AppGraph {
    lateinit var appContext: Context
        private set
    lateinit var serverRepository: ServerRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        serverRepository = ServerRepository(appContext)
        settingsRepository = SettingsRepository(appContext)
    }
}

class PPEmbyTVApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        HttpClients.setAppVersion(BuildConfig.VERSION_NAME)

        // 代理设置变化时同步到网络层
        appScope.launch {
            AppGraph.settingsRepository.proxy
                .distinctUntilChanged()
                .collect { proxy ->
                    Session.setProxy(proxy)
                    Session.invalidate()
                }
        }
    }
}
