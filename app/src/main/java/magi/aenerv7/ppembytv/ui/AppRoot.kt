package magi.aenerv7.ppembytv.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import magi.aenerv7.ppembytv.DlnaPlayRequestData
import magi.aenerv7.ppembytv.data.DecoderSettings
import magi.aenerv7.ppembytv.data.IntroOutroSettings
import magi.aenerv7.ppembytv.data.ProxyConfig
import magi.aenerv7.ppembytv.data.ProxyManager
import magi.aenerv7.ppembytv.data.ProxyProtocol
import magi.aenerv7.ppembytv.data.ProxySettings
import magi.aenerv7.ppembytv.data.SubtitleFontEntry
import magi.aenerv7.ppembytv.data.SubtitleFontManager
import magi.aenerv7.ppembytv.data.SubtitlePreferences
import magi.aenerv7.ppembytv.data.TraktSettings
import magi.aenerv7.ppembytv.data.WebDavSyncManager
import magi.aenerv7.ppembytv.data.WebDavSyncSettings
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import magi.aenerv7.ppembytv.data.api.TraktRetrofitClient
import magi.aenerv7.ppembytv.data.model.AuthenticationResult
import magi.aenerv7.ppembytv.data.model.BackupRouteConfig
import magi.aenerv7.ppembytv.data.model.Library
import magi.aenerv7.ppembytv.data.model.MediaItem
import magi.aenerv7.ppembytv.data.model.QueryResult
import magi.aenerv7.ppembytv.data.model.ServerConfig
import magi.aenerv7.ppembytv.data.model.ServerPingState
import magi.aenerv7.ppembytv.data.model.ServerPingStatus
import magi.aenerv7.ppembytv.data.model.TraktDeviceCodeRequest
import magi.aenerv7.ppembytv.data.model.TraktDeviceCodeResponse
import magi.aenerv7.ppembytv.data.model.TraktDeviceTokenRequest
import magi.aenerv7.ppembytv.data.model.isLiveTvLibrary
import magi.aenerv7.ppembytv.data.preferences.ServerPreferences
import magi.aenerv7.ppembytv.data.preferences.UserPreferences
import magi.aenerv7.ppembytv.dlna.DlnaConfig
import magi.aenerv7.ppembytv.dlna.DlnaSettings
import magi.aenerv7.ppembytv.server.BackupRouteConfigServerManager
import magi.aenerv7.ppembytv.server.ConfigServerManager
import magi.aenerv7.ppembytv.server.ServerIconLibraryInputServerManager
import magi.aenerv7.ppembytv.server.SubtitleFontUploadServerManager
import magi.aenerv7.ppembytv.server.WebDavSyncConfigServerManager
import magi.aenerv7.ppembytv.ui.components.PosterCard
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.components.TvCard
import magi.aenerv7.ppembytv.ui.components.TvCheckRow
import magi.aenerv7.ppembytv.ui.components.TvIconButton
import magi.aenerv7.ppembytv.ui.components.TvOutlinedTextField
import magi.aenerv7.ppembytv.ui.components.backdropUrl
import magi.aenerv7.ppembytv.ui.components.imageUrl
import magi.aenerv7.ppembytv.ui.components.tvClickable
import magi.aenerv7.ppembytv.ui.components.tvFocusBorder
import magi.aenerv7.ppembytv.ui.player.PlayerScreen
import magi.aenerv7.ppembytv.ui.theme.PpEmbyTvTheme
import magi.aenerv7.ppembytv.ui.theme.TvQrPanel
import magi.aenerv7.ppembytv.util.generateQrBitmap

private sealed class Screen {
    data object ServerList : Screen()
    data object AddServer : Screen()
    data class Login(val serverId: String) : Screen()
    data object Home : Screen()
    data class Library(val libraryId: String, val libraryName: String) : Screen()
    data class LiveTv(val libraryName: String) : Screen()
    data class Detail(val itemId: String) : Screen()
    data class Player(val item: MediaItem, val mediaSourceId: String?) : Screen()
    data object Favorites : Screen()
    data object Search : Screen()
    data object Settings : Screen()
}

@Composable
fun AppRoot(
    userPreferences: UserPreferences,
    dlnaPlayRequestState: MutableState<DlnaPlayRequestData?>,
) {
    val context = LocalContext.current.applicationContext
    val serverPrefs = remember { ServerPreferences(context) }
    var currentServer by remember { mutableStateOf<ServerConfig?>(null) }
    var screen by remember { mutableStateOf<Screen>(Screen.ServerList) }
    var userId by remember { mutableStateOf(RetrofitClient.getUserId()) }

    // On launch, restore the last used server.
    LaunchedEffect(Unit) {
        val server = serverPrefs.getLastUsedServer()?.effectiveServerConfig?.takeIf { it.isLoggedIn() }
        currentServer = server
        if (server != null) {
            userId = server.userId.orEmpty()
            screen = Screen.Home
        }
    }

    BackHandler(enabled = screen != Screen.ServerList) {
        screen = when (val active = screen) {
            Screen.AddServer, is Screen.Login -> Screen.ServerList
            Screen.Home -> Screen.ServerList
            is Screen.Library, is Screen.LiveTv, is Screen.Detail, Screen.Favorites, Screen.Search -> Screen.Home
            Screen.Settings -> if (currentServer == null) Screen.ServerList else Screen.Home
            is Screen.Player -> Screen.Detail(active.item.id)
            Screen.ServerList -> Screen.ServerList
        }
    }

    PpEmbyTvTheme {
        // PP TV 蓝黑背景：图标近黑底、左上蓝色光晕与底部压暗。
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Color(0xFF080B13))
                    val glowColor = Color(0xFF145FB5)
                    val maxDim = max(size.width, size.height)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.4f),
                                glowColor.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.25f),
                            radius = maxDim * 1.08f,
                        ),
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.58f to Color.Black.copy(alpha = 0.1f),
                            1.0f to Color.Black.copy(alpha = 0.52f),
                        ),
                    )
                },
        ) {
            when (val s = screen) {
                Screen.ServerList -> ServerListScreen(
                    serverPrefs = serverPrefs,
                    onAdd = { screen = Screen.AddServer },
                    onEnter = { server ->
                        currentServer = server
                        userId = server.userId.orEmpty()
                        screen = Screen.Home
                    },
                    onDelete = { id -> serverPrefs.deleteServer(id) },
                    onSettings = { screen = Screen.Settings },
                )

            Screen.AddServer -> AddServerScreen(
                serverPrefs = serverPrefs,
                onSaved = { server ->
                    currentServer = server
                    screen = if (server.isLoggedIn()) Screen.Home else Screen.Login(server.id)
                },
                onBack = { screen = Screen.ServerList },
            )

            is Screen.Login -> LoginScreen(
                serverPrefs = serverPrefs,
                serverId = s.serverId,
                onLoggedIn = { server ->
                    currentServer = server
                    userId = server.userId.orEmpty()
                    screen = Screen.Home
                },
                onBack = { screen = Screen.ServerList },
            )

            Screen.Home -> HomeScreen(
                server = currentServer,
                onOpenLibrary = { lib ->
                    screen = if (lib.isLiveTvLibrary()) Screen.LiveTv(lib.name) else Screen.Library(lib.id, lib.name)
                },
                onOpenDetail = { id -> screen = Screen.Detail(id) },
                onOpenFavorites = { screen = Screen.Favorites },
                onOpenSearch = { screen = Screen.Search },
                onOpenSettings = { screen = Screen.Settings },
                onOpenServers = { screen = Screen.ServerList },
            )

            is Screen.Library -> LibraryScreen(
                server = currentServer,
                libraryId = s.libraryId,
                libraryName = s.libraryName,
                onOpenDetail = { id -> screen = Screen.Detail(id) },
                onBack = { screen = Screen.Home },
            )

            is Screen.LiveTv -> LiveTvScreen(
                server = currentServer,
                libraryName = s.libraryName,
                onOpenDetail = { id -> screen = Screen.Detail(id) },
                onBack = { screen = Screen.Home },
            )

            is Screen.Detail -> DetailScreen(
                server = currentServer,
                itemId = s.itemId,
                onPlay = { item, mediaSourceId -> screen = Screen.Player(item, mediaSourceId) },
                onBack = { screen = Screen.Home },
                onOpenDetail = { id -> screen = Screen.Detail(id) },
                onSearch = { screen = Screen.Search },
            )

            Screen.Favorites -> FavoritesScreen(
                server = currentServer,
                onOpenDetail = { id -> screen = Screen.Detail(id) },
                onBack = { screen = Screen.Home },
                onOpenSearch = { screen = Screen.Search },
                onOpenServers = { screen = Screen.ServerList },
                onOpenSettings = { screen = Screen.Settings },
            )

            Screen.Search -> SearchScreen(
                server = currentServer,
                onOpenDetail = { id -> screen = Screen.Detail(id) },
                onBack = { screen = Screen.Home },
                onOpenFavorites = { screen = Screen.Favorites },
                onOpenServers = { screen = Screen.ServerList },
                onOpenSettings = { screen = Screen.Settings },
            )

            Screen.Settings -> Box(Modifier.fillMaxSize()) {
                if (currentServer != null) {
                    HomeScreen(
                        server = currentServer,
                        onOpenLibrary = {},
                        onOpenDetail = {},
                        onOpenFavorites = {},
                        onOpenSearch = {},
                        onOpenSettings = {},
                        onOpenServers = {},
                    )
                } else {
                    ServerListScreen(
                        serverPrefs = serverPrefs,
                        onAdd = {},
                        onEnter = {},
                        onDelete = {},
                        onSettings = {},
                    )
                }
                SettingsScreen(
                    serverPrefs = serverPrefs,
                    server = currentServer,
                    onBack = { screen = if (currentServer == null) Screen.ServerList else Screen.Home },
                )
            }

            is Screen.Player -> PlayerScreen(
                server = currentServer,
                item = s.item,
                mediaSourceId = s.mediaSourceId,
                onBack = { screen = Screen.Detail(s.item.id) },
            )
            }
        }
    }
}

// ===== Server list =====
@Composable
private fun ServerListScreen(
    serverPrefs: ServerPreferences,
    onAdd: () -> Unit,
    onEnter: (ServerConfig) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val servers = remember { mutableStateOf(serverPrefs.getAllServers()) }
    val lastUsedId = remember { mutableStateOf(serverPrefs.getLastUsedServerId()) }
    val pingStates = remember { mutableStateOf<Map<String, ServerPingState>>(emptyMap()) }
    val contentFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) {
        servers.value = serverPrefs.getAllServers()
        lastUsedId.value = serverPrefs.getLastUsedServerId()
        // 参考 App 默认聚焦第一个服务器；空列表时聚焦添加按钮。
        repeat(10) {
            contentFocusRequester.requestFocus()
            delay(100)
        }
    }
    // 复刻原版：服务器列表逐个测速（显示延迟 ms / 不通）
    LaunchedEffect(servers.value) {
        servers.value.forEach { s ->
            pingStates.value = pingStates.value + (s.id to ServerPingState.Measuring)
            pingStates.value = pingStates.value + (s.id to pingServer(s))
        }
    }

    Box(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 15.dp)) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "服务器",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.displayMedium,
                )
            }
            Spacer(Modifier.height(32.dp))
            if (servers.value.isEmpty()) {
                EmptyServerList(onAdd = onAdd, focusRequester = contentFocusRequester)
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(servers.value) { server ->
                        ServerCard(
                            server = server,
                            isLastUsed = server.id == lastUsedId.value,
                            pingState = pingStates.value[server.id] ?: ServerPingState.Idle,
                            focusRequester = if (server.id == servers.value.firstOrNull()?.id) contentFocusRequester else null,
                            onEnter = { onEnter(server) },
                            onDelete = { onDelete(server.id); servers.value = serverPrefs.getAllServers() },
                        )
                    }
                    item {
                        AddServerCard(onAdd = onAdd)
                    }
                }
            }
        }
        // 左上角设置齿轮（复刻原版）
        TvIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "设置",
            modifier = Modifier.align(Alignment.TopStart),
            onClick = onSettings,
        )
    }
}

@Composable
private fun EmptyServerList(
    onAdd: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester,
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "还没有保存的服务器",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(32.dp))
        TvButton(
            "+ 添加服务器",
            onClick = onAdd,
            modifier = Modifier.width(300.dp),
            horizontalPadding = 0,
            focusRequester = focusRequester,
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerConfig,
    isLastUsed: Boolean,
    pingState: ServerPingState,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onEnter: () -> Unit,
    onDelete: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .width(207.dp)
            .height(134.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvClickable(onClick = onEnter, onFocusChanged = { focused = it })
            .clip(shape)
            .background(
                if (focused) Color(0xFF174B82)
                else Color(0xFF152235)
            )
            .tvFocusBorder(focused, shape),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ServerMark(size = 38)
                Spacer(Modifier.width(10.dp))
                Text(
                    server.alias,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isLastUsed) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "上次使用",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "线路：${server.currentRouteDisplayName}",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (server.isLoggedIn()) "未播放" else "需重新登录",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    server.username,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                PingText(pingState)
            }
        }
    }
}

@Composable
private fun AddServerCard(onAdd: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .width(186.dp)
            .height(120.dp)
            .tvClickable(onClick = onAdd, onFocusChanged = { focused = it })
            .clip(shape)
            .background(
                if (focused) Color(0xFF174B82)
                else Color(0xFF182C48)
            )
            .tvFocusBorder(focused, shape)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("+", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "添加新服务器",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun PingText(state: ServerPingState) {
    val status = state.status
    val text: String
    val color: Color
    when (status) {
        ServerPingStatus.IDLE, ServerPingStatus.MEASURING -> {
            text = "..."
            color = Color.White.copy(alpha = 0.68f)
        }
        ServerPingStatus.UNREACHABLE -> {
            text = "不通"
            color = Color.White.copy(alpha = 0.68f)
        }
        ServerPingStatus.HTTP_ERROR -> {
            text = state.httpStatusCode?.toString() ?: "ERR"
            color = Color(0xFFF5C15D)
        }
        ServerPingStatus.GOOD -> {
            text = "${state.latencyMs ?: 0}ms"
            color = Color(0xFF4FD096)
        }
        ServerPingStatus.NORMAL -> {
            text = "${state.latencyMs ?: 0}ms"
            color = Color(0xFF6CB7FF)
        }
        ServerPingStatus.WARNING -> {
            text = "${state.latencyMs ?: 0}ms"
            color = Color(0xFFF5C15D)
        }
        ServerPingStatus.HIGH -> {
            text = "${state.latencyMs ?: 0}ms"
            color = Color(0xFFFF9F43)
        }
        ServerPingStatus.BAD -> {
            text = "${state.latencyMs ?: 0}ms"
            color = MaterialTheme.colorScheme.error
        }
    }
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        maxLines = 1,
    )
}

private suspend fun pingServer(server: ServerConfig): ServerPingState = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val start = System.currentTimeMillis()
    try {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        val request = okhttp3.Request.Builder().url(server.fullUrl).head().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                return@withContext ServerPingState.fromHttpStatusCode(resp.code)
            }
            val latency = System.currentTimeMillis() - start
            ServerPingState.fromLatency(latency)
        }
    } catch (e: Exception) {
        ServerPingState.Unreachable
    }
}

/** 复刻原版 q2.c 的地址解析：剥离 http(s):// 与尾部 :port。 */
private fun parseServerAddress(input: String): Triple<String, String?, Int?> {
    var s = input.trim()
    var protocol: String? = null
    Regex("^(https?)://", RegexOption.IGNORE_CASE).find(s)?.let { m ->
        protocol = m.groupValues[1].lowercase()
        s = s.substring(m.value.length)
    }
    s = s.trimEnd('/', '\\')
    var port: Int? = null
    Regex(":(\\d+)$").find(s)?.let { m ->
        port = m.groupValues[1].toIntOrNull()
        s = s.substring(0, m.range.first)
    }
    return Triple(s, protocol, port)
}

// ===== Add server =====
@Composable
private fun AddServerScreen(
    serverPrefs: ServerPreferences,
    onSaved: (ServerConfig) -> Unit,
    onBack: () -> Unit,
) {
    var protocol by remember { mutableStateOf("http") }
    var host by remember { mutableStateOf("192.168.1.1") }
    var port by remember { mutableStateOf("8096") }
    var path by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("我的Emby服务器") }
    var note by remember { mutableStateOf("") }
    var directOnly by remember { mutableStateOf(false) }
    var strmDirect by remember { mutableStateOf(false) }
    var trustAllCerts by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext
    val manager = remember { ConfigServerManager(context) }
    var qrUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val firstFieldFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    // 复刻原版：进入添加页即自动启动扫码配置服务，扫码内容实时同步到右侧表单
    LaunchedEffect(Unit) {
        qrUrl = manager.startServer(currentConfig = null) { config ->
            protocol = config.protocol
            host = config.host
            port = config.port.toString()
            path = config.path.orEmpty()
            username = config.username
            password = config.password
            alias = config.alias
            note = config.note.orEmpty()
            directOnly = config.directOnly
            strmDirect = config.enableStrmDirectPlay
            trustAllCerts = config.trustAllCerts
            error = null
        }
        repeat(10) {
            firstFieldFocusRequester.requestFocus()
            delay(100)
        }
    }
    DisposableEffect(Unit) {
        onDispose { manager.stopServer() }
    }

    fun save() {
        if (saving) return
        if (host.isBlank()) {
            error = "服务器地址不能为空"
            return
        }
        val parsed = parseServerAddress(host)
        val finalProtocol = parsed.second ?: protocol
        val finalPort = (parsed.third ?: port.toIntOrNull()) ?: 8096
        val finalHost = parsed.first
        val server = ServerConfig(
            id = serverPrefs.generateServerId(),
            alias = alias.ifBlank { "我的Emby服务器" },
            protocol = finalProtocol,
            host = finalHost,
            port = finalPort,
            path = path.trim().ifBlank { null },
            username = username.trim(),
            password = password,
            directOnly = directOnly,
            enableStrmDirectPlay = strmDirect,
            trustAllCerts = trustAllCerts,
            note = note.trim().ifBlank { null },
        )
        saving = true
        error = null
        scope.launch {
            try {
                ProxyManager.setDirectOnly(server.directOnly)
                RetrofitClient.initialize(server.fullUrl)
                RetrofitClient.setAuthToken("", "")
                RetrofitClient.setTrustAllCerts(server.trustAllCerts)
                val response = RetrofitClient.getApiService().authenticateUser(
                    mapOf("Username" to username, "Pw" to password)
                )
                val auth: AuthenticationResult? = response.body()
                if (response.isSuccessful && auth?.user?.id != null) {
                    val saved = server.copy(
                        userId = auth.user.id,
                        accessToken = auth.accessToken,
                        lastLoginTime = System.currentTimeMillis(),
                        isVerified = true,
                    )
                    serverPrefs.saveServer(saved)
                    serverPrefs.setLastUsedServerId(saved.id)
                    RetrofitClient.setAuthToken(auth.accessToken, auth.user.id)
                    onSaved(saved)
                } else {
                    error = "连接验证失败，请检查服务器地址和登录信息"
                }
            } catch (e: java.net.ConnectException) {
                error = "无法连接到服务器\n请检查地址、端口是否正确，以及服务器是否在运行"
            } catch (e: java.net.SocketTimeoutException) {
                error = "连接超时\n请检查网络连接或服务器是否可访问"
            } catch (e: java.net.UnknownHostException) {
                error = "无法解析服务器地址: ${server.host}\n请检查地址是否正确或网络连接"
            } catch (e: javax.net.ssl.SSLHandshakeException) {
                error = "SSL证书验证失败\n如果使用自签名证书，请勾选「信任所有SSL证书」选项"
            } catch (e: Exception) {
                error = "验证失败: ${e.message}"
            } finally {
                saving = false
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 15.dp)) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 左侧扫码面板（复刻原版：整列卡片）
            Column(
                modifier = Modifier
                    .width(350.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvQrPanel)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "📱 手机扫码配置",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(24.dp))
                val u = qrUrl
                if (u != null) {
                    val qr = remember(u) { generateQrBitmap(u) }
                    if (qr != null) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = "二维码",
                            modifier = Modifier.size(220.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(u, color = Color(0xFF91A8C5), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "正在启动扫码配置服务...",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "扫码后填写的内容会实时同步到右侧表单",
                    color = Color(0xFF91A8C5),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }

            // 右侧表单（复刻原版字段与顺序）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    if (false) "编辑服务器" else "添加服务器",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(16.dp))
                TvOutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = "服务器别名",
                    focusRequester = firstFieldFocusRequester,
                )
                Spacer(Modifier.height(16.dp))
                TvOutlinedTextField(value = note, onValueChange = { note = it }, placeholder = "备注（可选）")
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.width(120.dp)) {
                        Text(
                            "协议",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                        ProtocolToggle(protocol) { protocol = it }
                    }
                    TvOutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = "服务器地址",
                        placeholder = "192.168.1.100 或 emby.example.com",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                TvOutlinedTextField(value = port, onValueChange = { port = it }, label = "端口", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(16.dp))
                TvOutlinedTextField(value = path, onValueChange = { path = it }, placeholder = "路径（可选）")
                Spacer(Modifier.height(16.dp))
                TvOutlinedTextField(value = username, onValueChange = { username = it }, label = "用户名")
                Spacer(Modifier.height(16.dp))
                TvOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "密码",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(16.dp))
                TvCheckRow("仅直连（不使用代理）", directOnly) { directOnly = !directOnly }
                Spacer(Modifier.height(8.dp))
                TvCheckRow(
                    "STRM直链播放",
                    strmDirect,
                    description = "开启后，此服务器的STRM文件会优先尝试直链播放，不清楚请不要开启",
                ) { strmDirect = !strmDirect }
                Spacer(Modifier.height(8.dp))
                TvCheckRow("信任所有SSL证书（不安全）", trustAllCerts) { trustAllCerts = !trustAllCerts }
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton("取消", onClick = onBack, containerColor = Color(0xFF172234))
                    TvButton(if (saving) "保存中..." else "保存", onClick = { save() })
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "点击保存将验证服务器连接并保存配置",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ProtocolToggle(protocol: String, onSelect: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .tvClickable(onClick = { onSelect(if (protocol == "http") "https" else "http") }, onFocusChanged = { focused = it })
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .tvFocusBorder(focused, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(protocol, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

// ===== Login =====
@Composable
private fun LoginScreen(
    serverPrefs: ServerPreferences,
    serverId: String,
    onLoggedIn: (ServerConfig) -> Unit,
    onBack: () -> Unit,
) {
    val server = remember { serverPrefs.getServerById(serverId) }
    var username by remember { mutableStateOf(server?.username.orEmpty()) }
    var password by remember { mutableStateOf(server?.password.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text("登录服务器", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Text(server?.displayAddress.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        TvOutlinedTextField(value = username, onValueChange = { username = it }, label = "用户名")
        Spacer(Modifier.height(16.dp))
        TvOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = "密码",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error.orEmpty(), color = Color(0xFFFF6B6B))
        }
        Spacer(Modifier.height(24.dp))
        Row {
            TvButton(if (loading) "登录中..." else "登录") {
                loading = true
                error = null
                val s = server ?: return@TvButton
                scope.launch {
                    try {
                        RetrofitClient.initialize(s.fullUrl)
                        RetrofitClient.setAuthToken("", "")
                        val response = RetrofitClient.getApiService().authenticateUser(
                            mapOf("Username" to username, "Pw" to password)
                        )
                        val auth: AuthenticationResult? = response.body()
                        if (response.isSuccessful && auth?.user?.id != null) {
                            serverPrefs.updateServerLoginInfo(s.id, auth.user.id, auth.accessToken)
                            val updated = serverPrefs.getServerById(s.id)
                            RetrofitClient.initialize(updated?.fullUrl ?: s.fullUrl)
                            RetrofitClient.setAuthToken(auth.accessToken, auth.user.id)
                            onLoggedIn(updated ?: s)
                        } else {
                            error = "登录失败"
                        }
                    } catch (e: Exception) {
                        error = "登录失败: ${e.message}"
                    } finally {
                        loading = false
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            TvButton("返回") { onBack() }
        }
    }
}

// ===== Home =====
private const val HOME_PAGE_SIZE = 20
private const val HOME_FIELDS =
    "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,SeriesName,ParentIndexNumber,IndexNumber,SeriesId"
private const val HOME_LIBRARY_FIELDS =
    "$HOME_FIELDS,CommunityRating,ChildCount,RecursiveItemCount,UserData"
private const val HOME_LIBRARY_SORT = "DateLastContentAdded,DateCreated,SortName"

private fun Library.homeIncludeItemTypes(): String = when (collectionType?.lowercase()) {
    "movies" -> "Movie,Video"
    "tvshows" -> "Series"
    "musicvideos" -> "MusicVideo"
    "boxsets" -> "BoxSet"
    "music" -> "MusicAlbum"
    else -> "Series,Movie,Video,MusicVideo,MusicAlbum"
}

private fun Library.isHomeMediaLibrary(): Boolean =
    name != "播放列表" && !collectionType.equals("playlists", true)

@Composable
private fun HomeScreen(
    server: ServerConfig?,
    onOpenLibrary: (Library) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenServers: () -> Unit,
) {
    val libraries = remember { mutableStateOf<List<Library>>(emptyList()) }
    val resumeItems = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val resumeTotal = remember { mutableIntStateOf(0) }
    val resumeListState = rememberLazyListState()
    val mediaFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(Unit) {
        repeat(10) {
            mediaFocusRequester.requestFocus()
            delay(100)
        }
    }

    suspend fun fetchResumePage(startIndex: Int) {
        val s = server ?: return
        val api = RetrofitClient.getApiService()
        val userId = RetrofitClient.getUserId()
        runCatching {
            api.getResumeItemsV2(userId, HOME_PAGE_SIZE, startIndex, true, HOME_FIELDS, 1, "Primary,Backdrop,Thumb", "Video").body()
        }.onSuccess { qr ->
            if (qr == null) return@onSuccess
            val items = qr.items ?: emptyList()
            resumeItems.value = if (startIndex == 0) items else (resumeItems.value + items).distinctBy { it.id }
            resumeTotal.intValue = qr.totalRecordCount
            Log.d("HomeScreen", "继续观看分页加载: startIndex=$startIndex, 返回${items.size}项, 总数${qr.totalRecordCount}")
        }
    }

    LaunchedEffect(server) {
        val s = server ?: return@LaunchedEffect
        val api = RetrofitClient.getApiService()
        val userId = RetrofitClient.getUserId()
        runCatching { api.getLibraries(userId, "ItemCounts,PrimaryImageAspectRatio").body()?.items ?: emptyList() }
            .onSuccess { libraries.value = it }
        fetchResumePage(0)
    }

    // 继续观看：滚动接近末尾时加载下一页（与参考 APK 首页分页行为一致）
    LaunchedEffect(resumeItems.value.size) {
        snapshotFlow { resumeListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= resumeItems.value.size - 4 && resumeItems.value.size < resumeTotal.intValue) {
                    fetchResumePage(resumeItems.value.size)
                }
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeServerChip(server?.alias ?: "服务器", onOpenServers)
            Spacer(Modifier.weight(1f))
            HomeNavChip("媒体", Icons.Default.VideoLibrary, active = true, focusRequester = mediaFocusRequester) {}
            HomeNavChip("收藏", Icons.Default.Favorite, active = false, onClick = onOpenFavorites)
            HomeNavChip("搜索", Icons.Default.Search, active = false, onClick = onOpenSearch)
            HomeNavChip("", Icons.Default.Settings, active = false, onClick = onOpenSettings)
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle("我的媒体")
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            items(libraries.value.filter { it.isHomeMediaLibrary() }, key = { it.id }) { lib ->
                LibraryCard(
                    library = lib,
                    onClick = { onOpenLibrary(lib) },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        if (resumeItems.value.isNotEmpty()) {
            SectionTitle("继续观看")
            Spacer(Modifier.height(10.dp))
            ItemRow(resumeItems.value, resumeListState, onOpenDetail)
            Spacer(Modifier.height(18.dp))
        }
        libraries.value.filter { it.isHomeMediaLibrary() }.forEach { library ->
            androidx.compose.runtime.key(library.id) {
                HomeLibrarySection(
                    server = server,
                    library = library,
                    onOpenDetail = onOpenDetail,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeLibrarySection(
    server: ServerConfig?,
    library: Library,
    onOpenDetail: (String) -> Unit,
) {
    val sectionItems = remember(library.id) { mutableStateOf<List<MediaItem>>(emptyList()) }
    val totalCount = remember(library.id) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    var loading by remember(library.id) { mutableStateOf(false) }

    suspend fun fetchPage(startIndex: Int) {
        if (server == null || loading) return
        loading = true
        try {
            val api = RetrofitClient.getApiService()
            val userId = RetrofitClient.getUserId()
            val response = if (library.isLiveTvLibrary()) {
                api.getLiveTvChannels(
                    userId = userId,
                    fields = HOME_LIBRARY_FIELDS,
                    enableImages = true,
                    imageTypeLimit = 1,
                    enableImageTypes = "Primary,Backdrop,Thumb",
                    enableUserData = true,
                    addCurrentProgram = true,
                    sortBy = "DefaultChannelOrder",
                    sortOrder = "Ascending",
                    startIndex = startIndex,
                    limit = HOME_PAGE_SIZE,
                )
            } else {
                var mediaResponse = api.getItems(
                    userId = userId,
                    parentId = library.id,
                    sortBy = HOME_LIBRARY_SORT,
                    sortOrder = "Descending",
                    fields = HOME_LIBRARY_FIELDS,
                    recursive = true,
                    includeItemTypes = library.homeIncludeItemTypes(),
                    enableImageTypes = "Primary,Backdrop,Thumb",
                    filters = "",
                    limit = HOME_PAGE_SIZE,
                    startIndex = startIndex,
                )
                if (!mediaResponse.isSuccessful && mediaResponse.code() in setOf(400, 500)) {
                    Log.w(
                        "HomeScreen",
                        "媒体库栏目不支持首页排序，回退到 DateCreated: library=${library.name}",
                    )
                    mediaResponse = api.getItems(
                        userId = userId,
                        parentId = library.id,
                        sortBy = "DateCreated,SortName",
                        sortOrder = "Descending",
                        fields = HOME_LIBRARY_FIELDS,
                        recursive = true,
                        includeItemTypes = library.homeIncludeItemTypes(),
                        enableImageTypes = "Primary,Backdrop,Thumb",
                        filters = "",
                        limit = HOME_PAGE_SIZE,
                        startIndex = startIndex,
                    )
                }
                mediaResponse
            }

            if (!response.isSuccessful) {
                Log.w(
                    "HomeScreen",
                    "媒体库栏目加载失败: library=${library.name}, startIndex=$startIndex, HTTP ${response.code()}",
                )
                return
            }
            val result = response.body() ?: return
            val pageItems = result.items ?: emptyList()
            sectionItems.value = if (startIndex == 0) {
                pageItems.distinctBy { it.id }
            } else {
                (sectionItems.value + pageItems).distinctBy { it.id }
            }
            totalCount.intValue = result.totalRecordCount
            Log.d(
                "HomeScreen",
                "媒体库栏目分页加载: library=${library.name}, startIndex=$startIndex, " +
                    "返回${pageItems.size}项, 总数${result.totalRecordCount}",
            )
        } catch (e: Exception) {
            Log.w("HomeScreen", "媒体库栏目加载失败: library=${library.name}, startIndex=$startIndex", e)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(server, library.id) {
        sectionItems.value = emptyList()
        totalCount.intValue = 0
        fetchPage(0)
    }

    LaunchedEffect(server, library.id, listState) {
        snapshotFlow {
            Triple(
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                sectionItems.value.size,
                totalCount.intValue,
            )
        }
            .distinctUntilChanged()
            .collect { (lastVisible, itemCount, total) ->
                if (itemCount > 0 && lastVisible >= itemCount - 4 && itemCount < total) {
                    fetchPage(itemCount)
                }
            }
    }

    if (sectionItems.value.isNotEmpty()) {
        SectionTitle(library.name)
        Spacer(Modifier.height(10.dp))
        ItemRow(sectionItems.value, listState, onOpenDetail)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ItemRow(items: List<MediaItem>, listState: LazyListState, onOpenDetail: (String) -> Unit) {
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        items(items, key = { it.id }) { item ->
            MediaLandscapeCard(
                title = item.name,
                subtitle = item.currentProgram?.name ?: item.seriesName ?: (item.productionYear?.toString()),
                imageUrl = item.backdropImageTags?.firstOrNull()?.let { tag -> backdropUrl(item.id, tag, 640) }
                    ?: imageUrl(item.id, "Primary", item.imageTags?.primary, 640),
                progress = item.userData?.playedPercentage?.toFloat(),
                onClick = { onOpenDetail(item.id) },
            )
        }
    }
}

@Composable
private fun HomeServerChip(alias: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .height(45.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(shape)
            .background(if (focused) Color(0xFF1556A3) else Color.Transparent)
            .tvFocusBorder(focused, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServerMark(size = 24)
        Spacer(Modifier.width(14.dp))
        Text(alias, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun ServerMark(size: Int) {
    Box(
        modifier = Modifier.size(size.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size((size * 0.78f).dp)
                .rotate(45f)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF145AA8)),
        )
        Box(
            Modifier
                .size((size * 0.60f).dp)
                .rotate(45f)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF2E8BFF)),
        )
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size * 0.62f).dp),
        )
    }
}

@Composable
private fun HomeNavChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .height(45.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(shape)
            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
            .tvFocusBorder(focused || active, shape)
            .padding(
                horizontal = when {
                    label.isBlank() -> 12.dp
                    active -> 16.dp
                    else -> 10.dp
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label.ifBlank { "设置" }, tint = Color.White, modifier = Modifier.size(20.dp))
        if (label.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun LibraryCard(library: Library, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(220.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .scale(if (focused) 1.04f else 1f)
            .clip(shape)
            .tvFocusBorder(focused, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(123.dp)
                .clip(shape)
                .background(Color(0xFF111A29)),
        ) {
            val tag = library.imageTags?.primary ?: library.primaryImageTag
            AsyncImage(
                model = imageUrl(library.id, "Primary", tag, 640),
                contentDescription = library.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))
                    ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            library.name,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaLandscapeCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    progress: Float?,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(220.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .scale(if (focused) 1.04f else 1f),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(124.dp)
                .clip(shape)
                .background(Color(0xFF111A29))
                .tvFocusBorder(focused, shape),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "播放", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            if (progress != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                ) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ===== Library =====
private const val LIBRARY_PAGE_SIZE = 50

@Composable
private fun LibraryScreen(
    server: ServerConfig?,
    libraryId: String,
    libraryName: String,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
) {
    val itemsState = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val totalCount = remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()
    var sortBy by remember { mutableStateOf("DateCreated") }
    var sortOrder by remember { mutableStateOf("Descending") }

    suspend fun fetchPage(startIndex: Int) {
        val s = server ?: return
        val api = RetrofitClient.getApiService()
        runCatching {
            api.getItems(
                RetrofitClient.getUserId(), libraryId, sortBy, sortOrder,
                "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,SeriesName,ParentIndexNumber,IndexNumber,SeriesId,CommunityRating,ChildCount,RecursiveItemCount,UserData",
                false, "", "Primary,Backdrop,Thumb", "", LIBRARY_PAGE_SIZE, startIndex,
            ).body()
        }.onSuccess { qr ->
            if (qr == null) return@onSuccess
            val items = qr.items ?: emptyList()
            itemsState.value = if (startIndex == 0) items else (itemsState.value + items).distinctBy { it.id }
            totalCount.intValue = qr.totalRecordCount
            Log.d("LibraryScreen", "媒体库分页加载: startIndex=$startIndex, 返回${items.size}项, 总数${qr.totalRecordCount}")
        }
    }

    LaunchedEffect(server, libraryId, sortBy, sortOrder) {
        itemsState.value = emptyList()
        totalCount.intValue = 0
        fetchPage(0)
    }

    // 滚动接近末尾时加载下一页
    LaunchedEffect(itemsState.value.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= itemsState.value.size - 6 && itemsState.value.size < totalCount.intValue) {
                    fetchPage(itemsState.value.size)
                }
            }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 900.dp || maxHeight < 500.dp
        val gridColumns = if (compact) 4 else 6
        Column(Modifier.fillMaxSize().padding(horizontal = if (compact) 24.dp else 60.dp, vertical = 12.dp)) {
            Box(Modifier.fillMaxWidth().height(38.dp)) {
            TvButton(
                text = if (sortBy == "DateCreated") "排序：最新内容添加 ↓" else "排序：名称 A-Z ↓",
                modifier = Modifier.align(Alignment.CenterStart).width(if (compact) 180.dp else 172.dp),
                height = 38,
                horizontalPadding = 12,
            ) {
                if (sortBy == "DateCreated") {
                    sortBy = "SortName"
                    sortOrder = "Ascending"
                } else {
                    sortBy = "DateCreated"
                    sortOrder = "Descending"
                }
            }
                Text(libraryName, color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center))
                Text(
                    "共 ${itemsState.value.size}/${totalCount.intValue} 项",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Spacer(Modifier.height(if (compact) 20.dp else 36.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                gridItems(itemsState.value, key = { it.id }) { item ->
                    LibraryPosterCard(item = item, onClick = { onOpenDetail(item.id) })
                }
            }
        }
    }
}

@Composable
private fun LibraryPosterCard(item: MediaItem, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .width(120.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .scale(if (focused) 1.05f else 1f),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .tvFocusBorder(focused, shape),
        ) {
            AsyncImage(
                model = imageUrl(item.id, "Primary", item.imageTags?.primary, 360),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            val count = item.childCount ?: item.recursiveItemCount
            if (count != null && count > 0) {
                Text(
                    count.toString(),
                    color = Color.Black,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFD400))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
            item.communityRating?.takeIf { it > 0f }?.let { rating ->
                Text(
                    String.format(java.util.Locale.US, "%.1f", rating),
                    color = Color.Black,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(Color(0xFFFFD400))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        item.productionYear?.let { year ->
            Text(year.toString(), color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ===== Live TV =====
@Composable
private fun LiveTvScreen(
    server: ServerConfig?,
    libraryName: String,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
) {
    val channels = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(server) {
        val s = server ?: return@LaunchedEffect
        runCatching {
            RetrofitClient.getApiService().getLiveTvChannels(
                RetrofitClient.getUserId(), "PrimaryImageAspectRatio", true, 1, "Primary,Thumb", true, true,
                "Number", "Ascending", 0, 200,
            ).body()?.items ?: emptyList()
        }.onSuccess { channels.value = it }
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(libraryName, color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(channels.value) { channel ->
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(channel.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        channel.currentProgram?.let { program ->
                            Text(
                                "正在播放: ${program.name}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    TvButton("详情") { onOpenDetail(channel.id) }
                }
            }
        }
    }
}

// ===== Detail =====
@Composable
private fun DetailScreen(
    server: ServerConfig?,
    itemId: String,
    onPlay: (MediaItem, String?) -> Unit,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val itemState = remember { mutableStateOf<MediaItem?>(null) }
    val seasonsState = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val episodesState = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedSeasonId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(itemId) {
        val api = RetrofitClient.getApiService()
        val userId = RetrofitClient.getUserId()
        val item = runCatching {
            api.getItemDetails(
                userId, itemId, "PrimaryImageAspectRatio,Overview,Genres,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,SeasonName,IndexNumber,ParentIndexNumber,SeriesId,SeasonId,People,MediaSources,Chapters"
            ).body()
        }.getOrNull()
        itemState.value = item
        if (item?.type == "Series") {
            runCatching { api.getSeasons(item.id, userId).body()?.items ?: emptyList() }
                .onSuccess { seasons ->
                    seasonsState.value = seasons
                    selectedSeasonId = seasons.firstOrNull()?.id
                }
        } else if (item?.type == "Season") {
            runCatching {
                api.getEpisodes(item.seriesId.orEmpty(), userId, item.id, "PrimaryImageAspectRatio,BasicSyncInfo,Overview,RunTimeTicks,MediaSources,UserData,BackdropImageTags,SeriesName,ParentIndexNumber,IndexNumber", "ParentIndexNumber,IndexNumber", "Ascending").body()?.items ?: emptyList()
            }.onSuccess { episodesState.value = it }
        }
    }

    LaunchedEffect(itemId, selectedSeasonId) {
        val item = itemState.value ?: return@LaunchedEffect
        if (item.type == "Series" && selectedSeasonId != null) {
            runCatching {
                RetrofitClient.getApiService().getEpisodes(item.id, RetrofitClient.getUserId(), selectedSeasonId.orEmpty(), "PrimaryImageAspectRatio,BasicSyncInfo,Overview,RunTimeTicks,MediaSources,UserData,BackdropImageTags,SeriesName,ParentIndexNumber,IndexNumber", "ParentIndexNumber,IndexNumber", "Ascending").body()?.items ?: emptyList()
            }.onSuccess { episodesState.value = it }
        }
    }

    val item = itemState.value
    val scope = rememberCoroutineScope()
    var favorite by remember(item?.id) { mutableStateOf(item?.userData?.isFavorite ?: false) }
    var played by remember(item?.id) { mutableStateOf(item?.userData?.played ?: false) }
    val playFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val playableItem = when (item?.type) {
        "Series", "Season" -> episodesState.value.firstOrNull()
        else -> item
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF080B13))) {
        if (item != null) {
            AsyncImage(
                model = backdropUrl(item.id, item.backdropImageTags?.firstOrNull(), 1920),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.42f to Color.Black.copy(alpha = 0.14f),
                    0.76f to Color(0xD9080B13),
                    1f to Color(0xFF080B13),
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.36f), Color.Transparent, Color.Transparent)
                )
            )
        )

        if (item == null) {
            Text("加载中...", color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            val compact = maxWidth < 900.dp || maxHeight < 500.dp
            val leftPadding = if (compact) 24.dp else 48.dp
            LaunchedEffect(playableItem?.id, compact) {
                if (playableItem != null && !compact) playFocusRequester.requestFocus()
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(start = leftPadding, end = leftPadding),
            ) {
                Spacer(Modifier.height(if (compact) 112.dp else 176.dp))
                if (item.imageTags?.logo != null) {
                    AsyncImage(
                        model = imageUrl(item.id, "Logo", item.imageTags.logo, 900),
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.width(if (compact) 300.dp else 410.dp).height(if (compact) 90.dp else 96.dp),
                    )
                } else {
                    Text(
                        item.name,
                        color = Color.White,
                        style = if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item.communityRating?.takeIf { it > 0f }?.let { rating ->
                        Text(
                            String.format(java.util.Locale.US, "%.1f", rating),
                            color = Color(0xFF343434),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0xFFFFC400)).padding(horizontal = 11.dp, vertical = 5.dp),
                        )
                    }
                    item.productionYear?.let { Text(it.toString(), color = Color.White, style = MaterialTheme.typography.titleLarge) }
                    if (item.type == "Series") {
                        Text("共${seasonsState.value.size}季", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                    item.genres?.take(3)?.takeIf { it.isNotEmpty() }?.let {
                        Text(it.joinToString(" · "), color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRoundIcon(Icons.Default.Movie, "预告片") { }
                    DetailRoundIcon(Icons.Default.MusicNote, "音轨") { }
                    DetailRoundIcon(Icons.Default.Subtitles, "字幕") { }
                    DetailRoundIcon(Icons.Default.FileDownload, "下载") { }
                    if (seasonsState.value.isNotEmpty()) {
                        val selectedIndex = seasonsState.value.indexOfFirst { it.id == selectedSeasonId }.coerceAtLeast(0)
                        TvButton(
                            text = seasonsState.value[selectedIndex].name ?: "第 ${selectedIndex + 1} 季",
                            height = 46,
                            horizontalPadding = 22,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        ) {
                            val next = (selectedIndex + 1) % seasonsState.value.size
                            selectedSeasonId = seasonsState.value[next].id
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (playableItem != null) {
                        val playLabel = if (item.type == "Series" || item.type == "Season") {
                            "播放第${playableItem.indexNumber ?: 1}集"
                        } else {
                            "播放"
                        }
                        TvButton(
                            text = playLabel,
                            modifier = Modifier.width(128.dp),
                            height = 48,
                            horizontalPadding = 12,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            focusRequester = playFocusRequester,
                        ) { onPlay(playableItem, playableItem.mediaSources?.firstOrNull()?.id) }
                    }
                    DetailRoundIcon(Icons.Default.Search, "搜索", onClick = onSearch)
                    DetailRoundIcon(Icons.Default.Check, "标记已看", active = played) {
                        played = !played
                        scope.launch {
                            val api = RetrofitClient.getApiService()
                            if (played) api.markPlayedItem(RetrofitClient.getUserId(), item.id)
                            else api.deletePlayedItem(RetrofitClient.getUserId(), item.id)
                        }
                    }
                    DetailRoundIcon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "收藏", active = favorite) {
                        favorite = !favorite
                        scope.launch {
                            val api = RetrofitClient.getApiService()
                            if (favorite) api.markFavorite(RetrofitClient.getUserId(), item.id)
                            else api.unmarkFavorite(RetrofitClient.getUserId(), item.id)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                val episode = playableItem
                val runtime = episode?.runTimeTicks ?: item.runTimeTicks
                val summaryTitle = buildString {
                    if (episode != null && episode !== item) append("第${episode.indexNumber ?: 1}集")
                    else append(item.name)
                    runtime?.let { append(" · ${formatRuntime(it)}") }
                }
                Text(summaryTitle, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                MediaBadges(episode ?: item)
                val overview = episode?.overview?.takeIf { it.isNotBlank() } ?: item.overview.orEmpty()
                if (overview.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        overview,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (compact) 5 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(if (compact) 1f else 0.62f),
                    )
                }
                if (episodesState.value.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(22.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                        items(episodesState.value, key = { it.id }) { episodeItem ->
                            DetailEpisodeCard(episodeItem) {
                                onPlay(episodeItem, episodeItem.mediaSources?.firstOrNull()?.id)
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRoundIcon(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(44.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .scale(if (focused) 1.06f else 1f)
            .clip(RoundedCornerShape(50))
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .tvFocusBorder(focused, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun MediaBadges(item: MediaItem) {
    val source = item.mediaSources?.firstOrNull() ?: return
    val video = source.mediaStreams?.firstOrNull { it.type == "Video" }
    val labels = buildList {
        video?.height?.let { add(if (it >= 2160) "4K" else "${it}P") }
        video?.videoRangeType?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        source.container.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        source.size?.takeIf { it > 0 }?.let { add(String.format(java.util.Locale.US, "%.2fG", it / 1_073_741_824.0)) }
        source.bitrate?.takeIf { it > 0 }?.let { add(String.format(java.util.Locale.US, "%.1fMbps", it / 1_000_000.0)) }
    }
    if (labels.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
        labels.forEach { label ->
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.48f), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun DetailEpisodeCard(episode: MediaItem, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.width(194.dp).tvClickable(onClick = onClick, onFocusChanged = { focused = it }).scale(if (focused) 1.04f else 1f),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(Color(0xFF111A29)).tvFocusBorder(focused, shape),
        ) {
            AsyncImage(
                model = imageUrl(episode.id, "Primary", episode.imageTags?.primary, 640),
                contentDescription = episode.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.size(38.dp).align(Alignment.Center).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "播放", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("第${episode.indexNumber ?: 1}集 ${episode.name}", color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatRuntime(ticks: Long): String {
    val totalMinutes = ticks / 600_000_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}小时${minutes}分钟" else "${minutes}分钟"
}

// ===== Favorites =====
@Composable
private fun FavoritesScreen(
    server: ServerConfig?,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val favorites = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(server) {
        val s = server ?: return@LaunchedEffect
        runCatching {
            RetrofitClient.getApiService().getFavoriteItems(
                RetrofitClient.getUserId(), "IsFavorite", true,
                "SortName", "Ascending",
                "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,CommunityRating,ChildCount,RecursiveItemCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
                "Movie,Series", true, "Primary,Backdrop,Thumb", 200,
            ).body()?.items ?: emptyList()
        }.onSuccess { favorites.value = it }
        loaded = true
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 900.dp || maxHeight < 500.dp
        val columns = if (compact) 4 else 6
        val pagePadding = if (compact) 24.dp else 48.dp
        Column(Modifier.fillMaxSize().padding(horizontal = pagePadding, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                HomeServerChip(server?.alias ?: "服务器", onOpenServers)
                Spacer(Modifier.weight(1f))
                HomeNavChip("媒体", Icons.Default.VideoLibrary, active = false, onClick = onBack)
                HomeNavChip("收藏", Icons.Default.Favorite, active = true) {}
                HomeNavChip("搜索", Icons.Default.Search, active = false, onClick = onOpenSearch)
                HomeNavChip("", Icons.Default.Settings, active = false, onClick = onOpenSettings)
            }
            Spacer(Modifier.height(42.dp))
            if (favorites.value.isEmpty()) {
                if (loaded) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "暂无收藏内容",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    gridItems(favorites.value, key = { it.id }) { favorite ->
                        LibraryPosterCard(favorite) { onOpenDetail(favorite.id) }
                    }
                }
            }
        }
    }
}

// ===== Search =====
@Composable
private fun SearchScreen(
    server: ServerConfig?,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val inputFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(server, query) {
        val s = server ?: return@LaunchedEffect
        if (query.isNotBlank()) delay(350)
        runCatching {
            RetrofitClient.getApiService().searchItems(
                RetrofitClient.getUserId(), query.trim(), true,
                "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,CommunityRating,ChildCount,RecursiveItemCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
                "Movie,Series", 60,
            ).body()?.items ?: emptyList()
        }.onSuccess { results.value = it }
    }
    LaunchedEffect(Unit) { inputFocusRequester.requestFocus() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 900.dp || maxHeight < 500.dp
        val columns = if (compact) 4 else 6
        val pagePadding = if (compact) 18.dp else 48.dp
        Column(Modifier.fillMaxSize().padding(horizontal = pagePadding, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                HomeServerChip(server?.alias ?: "服务器", onOpenServers)
                Spacer(Modifier.weight(1f))
                HomeNavChip("媒体", Icons.Default.VideoLibrary, active = false, onClick = onBack)
                HomeNavChip("收藏", Icons.Default.Favorite, active = false, onClick = onOpenFavorites)
                HomeNavChip("搜索", Icons.Default.Search, active = true) {}
                HomeNavChip("", Icons.Default.Settings, active = false, onClick = onOpenSettings)
            }
            Spacer(Modifier.height(20.dp))
            TvOutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索",
                keyboardType = KeyboardType.Text,
                focusRequester = inputFocusRequester,
            )
            Spacer(Modifier.height(if (compact) 20.dp else 40.dp))
            Text(
                if (query.isBlank()) "推荐观看" else "搜索结果",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                gridItems(results.value, key = { it.id }) { result ->
                    LibraryPosterCard(result) { onOpenDetail(result.id) }
                }
            }
        }
    }
}

// ===== Settings =====
@Composable
private fun SettingsScreen(
    serverPrefs: ServerPreferences,
    server: ServerConfig?,
    onBack: () -> Unit,
) {
    var page by remember { mutableStateOf("main") }
    BackHandler {
        if (page == "main") onBack() else page = "main"
    }
    val labels = listOf(
        "main" to "配置",
        "playback" to "播放",
        "appearance" to "外观",
        "proxy" to "网络",
        "sync" to "同步",
        "about" to "关于",
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f))) {
        val compact = maxHeight < 400.dp
        Row(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(if (compact) 0.82f else 0.75f)
                .fillMaxHeight(if (compact) 0.94f else 0.85f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F1623))
                .padding(horizontal = if (compact) 10.dp else 20.dp, vertical = if (compact) 5.dp else 10.dp),
        ) {
            Column(Modifier.width(if (compact) 160.dp else 200.dp).fillMaxHeight()) {
                Text(
                    "设置",
                    color = MaterialTheme.colorScheme.primary,
                    style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
                labels.forEach { (key, label) ->
                    SettingsTab(label = label, selected = page == key, compact = compact) { page = key }
                    Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
                }
            }
            Spacer(Modifier.width(if (compact) 8.dp else 16.dp))
            Box(Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF2A3B52)))
            Spacer(Modifier.width(if (compact) 8.dp else 16.dp))
            Box(Modifier.weight(1f).fillMaxHeight().padding(top = if (compact) 27.dp else 47.dp)) {
                when (page) {
                    "main" -> SettingsActionPanel(server, serverPrefs, onOpen = { page = it })
                    "features" -> FeatureSettingsPanel(server, serverPrefs, onOpen = { page = it })
                    "playback" -> PlaybackSettingsPage(onBack = { page = "main" })
                    "proxy" -> ProxySettingsPage(onBack = { page = "main" })
                    "decoder" -> DecoderSettingsPage(onBack = { page = "main" })
                    "dlna" -> DlnaSettingsPage(onBack = { page = "main" })
                    "trakt" -> TraktSettingsPage(onBack = { page = "main" })
                    "sync" -> SyncSettingsPage(server, serverPrefs, onBack = { page = "main" }, onOpenQr = { page = it })
                    "qr_backup" -> QrBackupRoutesOverlay(server, serverPrefs, onBack = { page = "sync" })
                    "qr_icon" -> QrIconLibraryOverlay(onBack = { page = "sync" })
                    "qr_webdav" -> QrWebDavOverlay(onBack = { page = "sync" })
                    "qr_font" -> QrSubtitleFontOverlay(onBack = { page = "sync" })
                    "appearance" -> AppearanceSettingsPanel(onBack = { page = "main" })
                    "about" -> AboutSettingsPanel(onBack = { page = "main" })
                    else -> SettingsActionPanel(server, serverPrefs, onOpen = { page = it })
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(label: String, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (compact) 36.dp else 50.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(shape)
            .background(if (selected) Color(0xFF174B82) else Color(0xFF152235))
            .tvFocusBorder(focused || selected, shape)
            .padding(horizontal = if (compact) 18.dp else 30.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, color = Color.White, style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingsActionPanel(server: ServerConfig?, serverPrefs: ServerPreferences, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("配置", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(0.dp))
        SettingsFeatureButton("功能按钮") { onOpen("features") }
    }
}

@Composable
private fun FeatureSettingsPanel(server: ServerConfig?, serverPrefs: ServerPreferences, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("功能按钮", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
        Text("以下设置为整个软件全局生效，不区分服务器。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        SettingsFeatureButton("网络与代理") { onOpen("proxy") }
        Spacer(Modifier.height(10.dp))
        SettingsFeatureButton("解码器与音频") { onOpen("decoder") }
        Spacer(Modifier.height(10.dp))
        SettingsFeatureButton("DLNA 投屏") { onOpen("dlna") }
        Spacer(Modifier.height(10.dp))
        SettingsFeatureButton("Trakt 同步") { onOpen("trakt") }
        Spacer(Modifier.height(10.dp))
        SettingsFeatureButton("退出登录") {
            server?.let { serverPrefs.clearLastUsedServer() }
            RetrofitClient.setAuthToken("", "")
        }
    }
}

@Composable
private fun SettingsFeatureButton(label: String, onClick: () -> Unit) {
    TvButton(
        text = label,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        height = 55,
        horizontalPadding = 24,
        onClick = onClick,
    )
}

@Composable
private fun AppearanceSettingsPanel(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("外观", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        Text("当前主题：蓝黑", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        TvButton("返回配置") { onBack() }
    }
}

@Composable
private fun AboutSettingsPanel(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("关于", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        Text("皮皮 TV", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text("Emby Android TV 客户端", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        TvButton("返回配置") { onBack() }
    }
}

@Composable
private fun SettingsMain(
    server: ServerConfig?,
    serverPrefs: ServerPreferences,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("设置", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(24.dp))
        Text("当前服务器: ${server?.alias ?: "无"}", color = Color.White)
        Text(server?.displayAddress ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        TvButton("代理设置") { onOpen("proxy") }
        Spacer(Modifier.height(8.dp))
        TvButton("解码器设置") { onOpen("decoder") }
        Spacer(Modifier.height(8.dp))
        TvButton("DLNA 投屏设置") { onOpen("dlna") }
        Spacer(Modifier.height(8.dp))
        TvButton("播放与字幕设置") { onOpen("playback") }
        Spacer(Modifier.height(8.dp))
        TvButton("同步与其它") { onOpen("sync") }
        Spacer(Modifier.height(8.dp))
        TvButton("Trakt 同步") { onOpen("trakt") }
        Spacer(Modifier.height(16.dp))
        TvButton("退出登录") {
            server?.let { serverPrefs.clearLastUsedServer() }
            RetrofitClient.setAuthToken("", "")
            onBack()
        }
    }
}

// ===== Proxy settings =====
@Composable
private fun ProxySettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val settings = remember { ProxySettings(context) }
    val scope = rememberCoroutineScope()
    val config by settings.proxyConfigFlow.collectAsState(initial = ProxyConfig())

    fun save(next: ProxyConfig) {
        scope.launch {
            settings.saveProxyConfig(next)
            ProxyManager.applyProxyConfig(next)
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("代理设置", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        ToggleRow("启用代理", config.enabled) { save(config.copy(enabled = it)) }
        ToggleRow("局域网不代理", config.bypassLan) { save(config.copy(bypassLan = it)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("协议: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (p in ProxyProtocol.entries) {
                TvButton(if (config.protocol == p) "[${p.name}]" else p.name) { save(config.copy(protocol = p)) }
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        TvOutlinedTextField(value = config.host, onValueChange = { save(config.copy(host = it)) }, label = "地址")
        Spacer(Modifier.height(16.dp))
        TvOutlinedTextField(
            value = config.port.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { save(config.copy(port = it)) } },
            label = "端口",
            keyboardType = KeyboardType.Number,
        )
        Spacer(Modifier.height(16.dp))
        TvOutlinedTextField(value = config.username, onValueChange = { save(config.copy(username = it)) }, label = "用户名")
        Spacer(Modifier.height(16.dp))
        TvOutlinedTextField(
            value = config.password,
            onValueChange = { save(config.copy(password = it)) },
            label = "密码",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
    }
}

// ===== Decoder settings =====
@Composable
private fun DecoderSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val settings = remember { DecoderSettings(context) }
    val scope = rememberCoroutineScope()
    val config by settings.decoderConfigFlow.collectAsState(initial = DecoderSettings.DecoderConfig())

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("解码器设置", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        val modes = listOf(
            "auto" to "自动",
            "hardware" to "硬解",
            "software" to "软解",
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("视频解码: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            for ((value, label) in modes) {
                TvButton(if (config.mode == value) "[$label]" else label) {
                    scope.launch { settings.saveDecoderMode(value) }
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("音频解码: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            for ((value, label) in listOf(
                DecoderSettings.AUDIO_DECODER_AUTO to "自动",
                DecoderSettings.AUDIO_DECODER_FORCE_FFMPEG to "强制FFmpeg",
            )) {
                TvButton(if (config.audioMode == value) "[$label]" else label) {
                    scope.launch { settings.saveAudioDecoderMode(value) }
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        ToggleRow("音频直通优先", config.audioPassthroughPriorityEnabled) {
            scope.launch { settings.saveAudioPassthroughPriorityEnabled(it) }
        }
        ToggleRow("杜比视界7兼容", config.dv7CompatibilityEnabled) {
            scope.launch { settings.saveDv7CompatibilityEnabled(it) }
        }
    }
}

// ===== DLNA settings =====
@Composable
private fun DlnaSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val settings = remember { DlnaSettings(context) }
    val scope = rememberCoroutineScope()
    val config by settings.configFlow.collectAsState(initial = DlnaConfig())

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("DLNA 投屏设置", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        ToggleRow("启用 DLNA 投屏", config.enabled) {
            scope.launch { settings.saveConfig(config.copy(enabled = it)) }
        }
        TvOutlinedTextField(
            value = config.deviceName,
            onValueChange = { v ->
                scope.launch { settings.saveConfig(config.copy(deviceName = v)) }
            },
            label = "设备名称",
        )
    }
}

// ===== Trakt settings =====
@Composable
private fun TraktSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val settings = remember { TraktSettings(context) }
    val scope = rememberCoroutineScope()
    val api = remember { TraktRetrofitClient.apiService }

    var clientId by remember { mutableStateOf(settings.clientId) }
    var clientSecret by remember { mutableStateOf(settings.clientSecret) }
    var enabled by remember { mutableStateOf(settings.enabled) }
    var authorized by remember { mutableStateOf(settings.isAuthorized) }
    var deviceCode by remember { mutableStateOf<TraktDeviceCodeResponse?>(null) }
    var polling by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    fun persistCredentials() {
        settings.clientId = clientId
        settings.clientSecret = clientSecret
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Trakt 同步", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        TvOutlinedTextField(value = clientId, onValueChange = { clientId = it; persistCredentials() }, label = "Client ID")
        Spacer(Modifier.height(16.dp))
        TvOutlinedTextField(value = clientSecret, onValueChange = { clientSecret = it; persistCredentials() }, label = "Client Secret")
        Spacer(Modifier.height(8.dp))
        if (authorized) {
            Text("已授权", color = Color(0xFF4CAF50), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TvButton("重新授权") {
                settings.clearAuth()
                authorized = false
                status = ""
            }
        } else {
            TvButton("授权") {
                persistCredentials()
                scope.launch {
                    status = "正在获取设备码..."
                    runCatching { api.generateDeviceCode(TraktDeviceCodeRequest(clientId)).body() }
                        .onSuccess { resp ->
                            if (resp == null) {
                                status = "获取设备码失败"
                            } else {
                                deviceCode = resp
                                status = ""
                                polling = true
                                try {
                                    while (true) {
                                        delay((resp.interval.coerceAtLeast(2)) * 1000)
                                        val tokenResp = api.pollDeviceToken(
                                            TraktDeviceTokenRequest(resp.deviceCode, clientId, clientSecret)
                                        )
                                        if (tokenResp.isSuccessful) {
                                            tokenResp.body()?.let {
                                                settings.saveToken(it.accessToken, it.refreshToken)
                                            }
                                            authorized = true
                                            deviceCode = null
                                            status = "授权成功"
                                            break
                                        } else if (tokenResp.code() != 400) {
                                            status = "授权失败或已过期 (${tokenResp.code()})"
                                            deviceCode = null
                                            break
                                        }
                                    }
                                } finally {
                                    polling = false
                                }
                            }
                        }
                        .onFailure { status = "获取设备码失败: ${it.message}" }
                }
            }
        }
        deviceCode?.let { dc ->
            Spacer(Modifier.height(16.dp))
            Text("在浏览器打开 ${dc.verificationUrl}，输入代码 ${dc.userCode}", color = Color.White)
            Spacer(Modifier.height(8.dp))
            val qr = remember(dc) { generateQrBitmap(dc.activationUrl) }
            if (qr != null) {
                Image(bitmap = qr.asImageBitmap(), contentDescription = "授权二维码", modifier = Modifier.width(240.dp).height(240.dp))
            }
            if (polling) {
                Spacer(Modifier.height(8.dp))
                Text("等待授权中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (status.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        ToggleRow("启用 Trakt 同步", enabled) { enabled = it; settings.enabled = it }
        ToggleRow("提示云端进度", settings.promptCloudProgress) { settings.promptCloudProgress = it }
        ToggleRow("同步本地进度到 Trakt", settings.syncLocalProgressToTrakt) { settings.syncLocalProgressToTrakt = it }
    }
}

// ===== Playback & subtitle settings =====
@Composable
private fun PlaybackSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val introOutro = remember { IntroOutroSettings(context) }
    val subtitlePrefs = remember { SubtitlePreferences(context) }
    val scope = rememberCoroutineScope()
    val introSettings by introOutro.settingsFlow.collectAsState(initial = IntroOutroSettings.Settings())

    var subtitlesEnabled by remember { mutableStateOf(subtitlePrefs.isSubtitlesEnabled()) }
    var brightnessEnabled by remember { mutableStateOf(subtitlePrefs.isBitmapSubtitleBrightnessEnabled()) }
    var brightness by remember { mutableStateOf(subtitlePrefs.getBitmapSubtitleBrightness()) }
    var fontScale by remember { mutableStateOf(subtitlePrefs.getSubtitleFontScale()) }
    var fontColor by remember { mutableStateOf(subtitlePrefs.getSubtitleFontColor()) }
    var selectedFont by remember { mutableStateOf(SubtitleFontManager(context).getSelectedFont()?.name) }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("播放与字幕设置", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        Text("片头片尾", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        ToggleRow("自动优先级", introSettings.autoPriority) {
            scope.launch { introOutro.saveSettings(introSettings.copy(autoPriority = it)) }
        }
        ToggleRow("跳过片头", introSettings.skipIntro) {
            scope.launch { introOutro.saveSettings(introSettings.copy(skipIntro = it)) }
        }
        ToggleRow("跳过片尾", introSettings.skipOutro) {
            scope.launch { introOutro.saveSettings(introSettings.copy(skipOutro = it)) }
        }
        Spacer(Modifier.height(16.dp))
        Text("字幕", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        ToggleRow("启用字幕", subtitlesEnabled) {
            subtitlesEnabled = it
            subtitlePrefs.saveSubtitlesEnabled(it)
        }
        ToggleRow("图形字幕亮度增强", brightnessEnabled) {
            brightnessEnabled = it
            subtitlePrefs.saveBitmapSubtitleBrightnessEnabled(it)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("图形字幕亮度: $brightness", color = Color.White, modifier = Modifier.weight(1f))
            TvButton("-") {
                brightness = (brightness - 5).coerceAtLeast(5)
                subtitlePrefs.saveBitmapSubtitleBrightness(brightness)
            }
            Spacer(Modifier.width(8.dp))
            TvButton("+") {
                brightness = (brightness + 5).coerceAtMost(100)
                subtitlePrefs.saveBitmapSubtitleBrightness(brightness)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("ASS 字幕增强", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("字幕字号: ${(fontScale * 100).toInt()}%", color = Color.White, modifier = Modifier.weight(1f))
            TvButton("-") {
                fontScale = (fontScale - 0.1f).coerceAtLeast(SubtitlePreferences.SUBTITLE_FONT_SCALE_MIN)
                subtitlePrefs.saveSubtitleFontScale(fontScale)
            }
            Spacer(Modifier.width(8.dp))
            TvButton("+") {
                fontScale = (fontScale + 0.1f).coerceAtMost(SubtitlePreferences.SUBTITLE_FONT_SCALE_MAX)
                subtitlePrefs.saveSubtitleFontScale(fontScale)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("字幕颜色: ${fontColor.displayName}", color = Color.White, modifier = Modifier.weight(1f))
            TvButton("换色") {
                val next = SubtitlePreferences.SubtitleColor.entries[(fontColor.ordinal + 1) % SubtitlePreferences.SubtitleColor.entries.size]
                fontColor = next
                subtitlePrefs.saveSubtitleFontColor(next)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("字幕字体: ${selectedFont ?: "默认"}", color = Color.White, modifier = Modifier.weight(1f))
            TvButton("清除") {
                SubtitleFontManager(context).clearSelectedFont()
                selectedFont = null
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TvButton(if (checked) "开" else "关") { onToggle(!checked) }
    }
}

// ===== Sync & other (QR config flows) =====
@Composable
private fun SyncSettingsPage(
    server: ServerConfig?,
    serverPrefs: ServerPreferences,
    onBack: () -> Unit,
    onOpenQr: (String) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val webDavSettings = remember { WebDavSyncSettings(context) }
    val webDavManager = remember { WebDavSyncManager(context) }
    val scope = rememberCoroutineScope()
    var webDavStatus by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("同步与其它", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        TvButton("备用线路配置") { onOpenQr("qr_backup") }
        Spacer(Modifier.height(8.dp))
        TvButton("服务器图标库") { onOpenQr("qr_icon") }
        Spacer(Modifier.height(8.dp))
        TvButton("WebDAV 配置") { onOpenQr("qr_webdav") }
        Spacer(Modifier.height(8.dp))
        TvButton("字幕字体上传") { onOpenQr("qr_font") }

        Spacer(Modifier.height(24.dp))
        Text("WebDAV 同步", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        val wdConfig = webDavSettings.load()
        Text(
            if (wdConfig.serverUrl.isBlank()) {
                "未配置 WebDAV 服务器，请先扫码配置"
            } else {
                "服务器: ${wdConfig.serverUrl}（同步服务器: ${if (wdConfig.syncServerConfigurations) "开" else "关"}，同步设置: ${if (wdConfig.syncAppSettings) "开" else "关"}）"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvButton("立即上传") {
                if (!syncing) {
                    syncing = true
                    webDavStatus = null
                    scope.launch {
                        webDavStatus = runCatching { webDavManager.uploadSync(webDavSettings.load()) }
                            .fold({ it }, { "上传失败: ${it.message}" })
                        syncing = false
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            TvButton("立即下载") {
                if (!syncing) {
                    syncing = true
                    webDavStatus = null
                    scope.launch {
                        webDavStatus = runCatching { webDavManager.downloadSync(webDavSettings.load()) }
                            .fold({ it }, { "下载失败: ${it.message}" })
                        syncing = false
                    }
                }
            }
        }
        if (syncing) {
            Spacer(Modifier.height(8.dp))
            Text("同步中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        webDavStatus?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color.White)
        }
    }
}

@Composable
private fun QrBackupRoutesOverlay(
    server: ServerConfig?,
    serverPrefs: ServerPreferences,
    onBack: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val manager = remember { BackupRouteConfigServerManager(context) }
    QrServerOverlay(
        title = "备用线路配置",
        description = "手机扫码后在网页中配置服务器备用线路",
        start = {
            manager.startServer(
                serverAlias = server?.alias ?: "",
                initialRoutes = server?.backupRoutesSafe ?: emptyList(),
            ) { routes ->
                server?.let { s ->
                    serverPrefs.saveServer(
                        s.copy(backupRoutes = routes, activeBackupRouteId = routes.firstOrNull()?.id)
                    )
                }
            }
        },
        stop = { manager.stopServer() },
        onBack = onBack,
    )
}

@Composable
private fun QrIconLibraryOverlay(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val manager = remember { ServerIconLibraryInputServerManager(context) }
    val userPrefs = remember { UserPreferences(context) }
    QrServerOverlay(
        title = "服务器图标库",
        description = "手机扫码后可配置服务器图标库地址",
        start = {
            manager.startServer(initialUrl = userPrefs.serverIconLibraryUrl ?: "") { url ->
                userPrefs.saveServerIconLibraryUrl(url)
            }
        },
        stop = { manager.stopServer() },
        onBack = onBack,
    )
}

@Composable
private fun QrWebDavOverlay(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val manager = remember { WebDavSyncConfigServerManager(context) }
    val settings = remember { WebDavSyncSettings(context) }
    QrServerOverlay(
        title = "WebDAV 同步",
        description = "手机扫码后可配置 WebDAV 同步服务器",
        start = {
            manager.startServer(initialConfig = settings.load()) { config ->
                settings.save(config)
            }
        },
        stop = { manager.stopServer() },
        onBack = onBack,
    )
}

@Composable
private fun QrSubtitleFontOverlay(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val manager = remember { SubtitleFontUploadServerManager(context) }
    val fontManager = remember { SubtitleFontManager(context) }
    var uploadedName by remember { mutableStateOf<String?>(null) }
    QrServerOverlay(
        title = "字幕字体上传",
        description = if (uploadedName != null) "已上传字体: $uploadedName" else "手机扫码后可上传 ASS 字幕字体（ttf/otf）",
        start = {
            manager.startServer { entry: SubtitleFontEntry ->
                fontManager.addFont(entry)
                uploadedName = entry.name
            }
        },
        stop = { manager.stopServer() },
        onBack = onBack,
    )
}

@Composable
private fun QrServerOverlay(
    title: String,
    description: String,
    start: () -> String?,
    stop: () -> Unit,
    onBack: () -> Unit,
) {
    var url by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { url = start() }
    DisposableEffect(Unit) { onDispose { stop() } }

    Box(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            val u = url
            if (u != null) {
                val qr = remember(u) { generateQrBitmap(u) }
                if (qr != null) {
                    Image(bitmap = qr.asImageBitmap(), contentDescription = "二维码", modifier = Modifier.width(320.dp).height(320.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(u, color = Color.White, style = MaterialTheme.typography.titleMedium)
            } else {
                Text("正在启动配置服务...", color = Color.White)
            }
            Spacer(Modifier.height(24.dp))
            TvButton("返回") { onBack() }
        }
    }
}
