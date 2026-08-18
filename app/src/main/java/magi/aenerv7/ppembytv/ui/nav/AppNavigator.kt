package magi.aenerv7.ppembytv.ui.nav

import androidx.compose.runtime.mutableStateListOf

/** 屏幕路由 */
sealed class Screen {
    data object Servers : Screen()
    data object AddServer : Screen()
    data object QrConfig : Screen()
    data object Home : Screen()
    data object Movies : Screen()
    data object TvShows : Screen()
    data object Search : Screen()
    data object Settings : Screen()

    data class Library(val parentId: String?, val includeItemTypes: String, val title: String) : Screen()
    data class Detail(val itemId: String, val type: String, val name: String) : Screen()
    data class SeriesDetail(val seriesId: String, val name: String) : Screen()
    data class SeasonEpisodes(
        val seriesId: String,
        val seasonId: String,
        val seriesName: String,
        val seasonName: String,
    ) : Screen()
    data class Login(val serverId: String) : Screen()
}

/**
 * 手动管理的返回栈导航（TV 遥控器 BACK 键）。
 */
class AppNavigator {
    private val _stack = mutableStateListOf<Screen>()

    val current: Screen get() = _stack.last()

    fun isEmpty() = _stack.isEmpty()

    fun start(screen: Screen) {
        _stack.clear()
        _stack.add(screen)
    }

    fun push(screen: Screen) {
        _stack.add(screen)
    }

    fun replace(screen: Screen) {
        if (_stack.isNotEmpty()) _stack[_stack.lastIndex] = screen
        else _stack.add(screen)
    }

    fun pop(): Boolean {
        if (_stack.size > 1) {
            _stack.removeAt(_stack.lastIndex)
            return true
        }
        return false
    }

    /** 回到根屏幕（如切换到某个服务器首页） */
    fun resetToHome() {
        _stack.clear()
        _stack.add(Screen.Home)
    }
}
