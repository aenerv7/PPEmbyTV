package magi.aenerv7.ppembytv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import magi.aenerv7.ppembytv.AppGraph
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.ui.NavRail
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import magi.aenerv7.ppembytv.ui.screens.AddServerScreen
import magi.aenerv7.ppembytv.ui.screens.DetailScreen
import magi.aenerv7.ppembytv.ui.screens.HomeScreen
import magi.aenerv7.ppembytv.ui.screens.LibraryScreen
import magi.aenerv7.ppembytv.ui.screens.LoginScreen
import magi.aenerv7.ppembytv.ui.screens.QrConfigScreen
import magi.aenerv7.ppembytv.ui.screens.SearchScreen
import magi.aenerv7.ppembytv.ui.screens.SeasonEpisodesScreen
import magi.aenerv7.ppembytv.ui.screens.SeriesDetailScreen
import magi.aenerv7.ppembytv.ui.screens.ServerListScreen
import magi.aenerv7.ppembytv.ui.screens.SettingsScreen
import magi.aenerv7.ppembytv.ui.theme.PPEmbyTVTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContent {
            PPEmbyTVTheme {
                App()
            }
        }
    }

    /** 隐藏状态栏/导航栏，内容沉浸式全屏 */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @Composable
    private fun App() {
        val navigator = remember { AppNavigator() }
        var ready by remember { mutableStateOf(false) }

        // 启动时恢复上次使用的服务器
        LaunchedEffect(Unit) {
            val server = AppGraph.serverRepository.getLastUsedServer()
            if (server != null && server.isLoggedIn && server.accessToken.isNotEmpty()) {
                Session.setActiveServer(server)
                navigator.resetToHome()
            } else {
                navigator.start(Screen.Servers)
            }
            ready = true
        }

        if (!ready) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0F1115)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4DA3FF))
            }
            return
        }

        BackHandler(enabled = true) {
            if (!navigator.pop()) {
                finish()
            }
        }

        when (val screen = navigator.current) {
            is Screen.Servers -> ServerListScreen(navigator)
            is Screen.AddServer -> AddServerScreen(navigator)
            is Screen.QrConfig -> QrConfigScreen(navigator)
            is Screen.Home -> TopLevelScaffold(navigator, Screen.Home) { HomeScreen(navigator) }
            is Screen.Movies -> TopLevelScaffold(navigator, Screen.Movies) {
                LibraryScreen(navigator, parentId = null, includeItemTypes = "Movie", title = "电影")
            }
            is Screen.TvShows -> TopLevelScaffold(navigator, Screen.TvShows) {
                LibraryScreen(navigator, parentId = null, includeItemTypes = "Series", title = "电视剧")
            }
            is Screen.Library -> TopLevelScaffold(navigator, Screen.Home) {
                LibraryScreen(navigator, screen.parentId, screen.includeItemTypes, screen.title)
            }
            is Screen.Search -> TopLevelScaffold(navigator, Screen.Search) { SearchScreen(navigator) }
            is Screen.Settings -> TopLevelScaffold(navigator, Screen.Settings) { SettingsScreen(navigator) }
            is Screen.Detail -> DetailScreen(navigator, screen.itemId, screen.type, screen.name)
            is Screen.SeriesDetail -> SeriesDetailScreen(navigator, screen.seriesId, screen.name)
            is Screen.SeasonEpisodes -> SeasonEpisodesScreen(
                navigator, screen.seriesId, screen.seasonId, screen.seriesName, screen.seasonName
            )
            is Screen.Login -> LoginScreen(navigator, screen.serverId)
        }
    }

    /** 顶层页面：左侧导航栏 + 内容 */
    @Composable
    private fun TopLevelScaffold(
        navigator: AppNavigator,
        selected: Screen,
        content: @Composable () -> Unit,
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F1115))) {
            NavRail(
                navigator = navigator,
                selected = selected,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 96.dp)
            ) {
                content()
            }
        }
    }
}
