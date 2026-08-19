package magi.aenerv7.ppembytv.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import magi.aenerv7.ppembytv.data.api.IqiyiSuggestApi
import magi.aenerv7.ppembytv.data.api.IqiyiSuggestItem
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import magi.aenerv7.ppembytv.data.api.TraktRetrofitClient
import magi.aenerv7.ppembytv.data.model.AuthenticationResult
import magi.aenerv7.ppembytv.data.model.BackupRouteConfig
import magi.aenerv7.ppembytv.data.model.Library
import magi.aenerv7.ppembytv.data.model.MediaItem
import magi.aenerv7.ppembytv.data.model.QueryResult
import magi.aenerv7.ppembytv.data.model.ServerConfig
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
import magi.aenerv7.ppembytv.ui.components.TvKeyboard
import magi.aenerv7.ppembytv.ui.components.backdropUrl
import magi.aenerv7.ppembytv.ui.components.imageUrl
import magi.aenerv7.ppembytv.ui.components.tvClickable
import magi.aenerv7.ppembytv.ui.player.PlayerScreen
import magi.aenerv7.ppembytv.ui.theme.PpEmbyTvTheme
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

    PpEmbyTvTheme {
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
            )

            Screen.Search -> SearchScreen(
                server = currentServer,
                onOpenDetail = { id -> screen = Screen.Detail(id) },
                onBack = { screen = Screen.Home },
            )

            Screen.Settings -> SettingsScreen(
                serverPrefs = serverPrefs,
                server = currentServer,
                onBack = { screen = Screen.Home },
            )

            is Screen.Player -> PlayerScreen(
                server = currentServer,
                item = s.item,
                mediaSourceId = s.mediaSourceId,
                onBack = { screen = Screen.Detail(s.item.id) },
            )
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
) {
    val servers = remember { mutableStateOf(serverPrefs.getAllServers()) }
    LaunchedEffect(Unit) { servers.value = serverPrefs.getAllServers() }
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text("选择服务器", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(servers.value) { server ->
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(server.alias, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(server.displayAddress, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TvButton("进入") { onEnter(server) }
                    Spacer(Modifier.width(8.dp))
                    TvButton("删除") { onDelete(server.id); servers.value = serverPrefs.getAllServers() }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        TvButton("添加服务器") { onAdd() }
    }
}

// ===== Add server =====
@Composable
private fun AddServerScreen(
    serverPrefs: ServerPreferences,
    onSaved: (ServerConfig) -> Unit,
    onBack: () -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8096") }
    var path by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("http") }
    var showQr by remember { mutableStateOf(false) }

    if (showQr) {
        QrConfigOverlay(
            onConfig = { config ->
                serverPrefs.saveServer(config)
                onSaved(config)
            },
            onBack = { showQr = false },
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text("添加服务器", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        InputField("协议 (http/https)", protocol) { protocol = it }
        InputField("地址", host) { host = it }
        InputField("端口", port) { port = it }
        InputField("路径 (可选)", path) { path = it }
        InputField("用户名", username) { username = it }
        InputField("密码", password) { password = it }
        Spacer(Modifier.height(24.dp))
        Row {
            TvButton("保存并连接") {
                val server = ServerConfig(
                    id = serverPrefs.generateServerId(),
                    alias = host.ifBlank { "我的Emby服务器" },
                    protocol = protocol.ifBlank { "http" },
                    host = host.trim(),
                    port = port.toIntOrNull() ?: 8096,
                    path = path.trim().ifBlank { null },
                    username = username.trim(),
                    password = password,
                )
                serverPrefs.saveServer(server)
                onSaved(server)
            }
            Spacer(Modifier.width(16.dp))
            TvButton("扫码配置") { showQr = true }
            Spacer(Modifier.width(16.dp))
            TvButton("返回") { onBack() }
        }
    }
}

// ===== QR config =====
@Composable
private fun QrConfigOverlay(
    onConfig: (ServerConfig) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val manager = remember { ConfigServerManager(context) }
    var url by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        url = manager.startServer(currentConfig = null) { config ->
            onConfig(config)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { manager.stopServer() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("扫码配置服务器", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("用手机扫描下方二维码，在网页中填写服务器信息并同步", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        InputField("用户名", username) { username = it }
        InputField("密码", password) { password = it }
        if (error != null) {
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

@Composable
private fun HomeScreen(
    server: ServerConfig?,
    onOpenLibrary: (Library) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenServers: () -> Unit,
) {
    val libraries = remember { mutableStateOf<List<Library>>(emptyList()) }
    val resumeItems = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val latestItems = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val resumeTotal = remember { mutableIntStateOf(0) }
    val latestTotal = remember { mutableIntStateOf(0) }
    val resumeListState = rememberLazyListState()
    val latestListState = rememberLazyListState()

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

    suspend fun fetchLatestPage(startIndex: Int) {
        val s = server ?: return
        val api = RetrofitClient.getApiService()
        val userId = RetrofitClient.getUserId()
        runCatching {
            api.getItems(
                userId, "", "DateCreated", "Descending", HOME_FIELDS,
                true, "Movie,Episode,Series,Video", "Primary,Backdrop,Thumb", "", HOME_PAGE_SIZE, startIndex,
            ).body()
        }.onSuccess { qr ->
            if (qr == null) return@onSuccess
            val items = qr.items ?: emptyList()
            latestItems.value = if (startIndex == 0) items else (latestItems.value + items).distinctBy { it.id }
            latestTotal.intValue = qr.totalRecordCount
            Log.d("HomeScreen", "最新媒体分页加载: startIndex=$startIndex, 返回${items.size}项, 总数${qr.totalRecordCount}")
        }
    }

    LaunchedEffect(server) {
        val s = server ?: return@LaunchedEffect
        val api = RetrofitClient.getApiService()
        val userId = RetrofitClient.getUserId()
        runCatching { api.getLibraries(userId, "ItemCounts,PrimaryImageAspectRatio").body()?.items ?: emptyList() }
            .onSuccess { libraries.value = it }
        fetchResumePage(0)
        fetchLatestPage(0)
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

    // 最新媒体：同上
    LaunchedEffect(latestItems.value.size) {
        snapshotFlow { latestListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= latestItems.value.size - 4 && latestItems.value.size < latestTotal.intValue) {
                    fetchLatestPage(latestItems.value.size)
                }
            }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("皮皮 TV", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("搜索") { onOpenSearch() }
            Spacer(Modifier.width(8.dp))
            TvButton("设置") { onOpenSettings() }
            Spacer(Modifier.width(8.dp))
            TvButton("服务器") { onOpenServers() }
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(libraries.value) { lib ->
                TvButton(lib.name) { onOpenLibrary(lib) }
            }
        }
        Spacer(Modifier.height(24.dp))
        if (resumeItems.value.isNotEmpty()) {
            SectionTitle("继续观看")
            ItemRow(resumeItems.value, resumeListState, onOpenDetail)
        }
        Spacer(Modifier.height(24.dp))
        if (latestItems.value.isNotEmpty()) {
            SectionTitle("最新媒体")
            ItemRow(latestItems.value, latestListState, onOpenDetail)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ItemRow(items: List<MediaItem>, listState: LazyListState, onOpenDetail: (String) -> Unit) {
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { item ->
            PosterCard(
                title = item.name,
                subtitle = item.seriesName ?: (item.productionYear?.toString()),
                imageUrl = imageUrl(item.id, "Primary", item.imageTags?.primary),
                onClick = { onOpenDetail(item.id) },
            )
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
    val listState = rememberLazyListState()

    suspend fun fetchPage(startIndex: Int) {
        val s = server ?: return
        val api = RetrofitClient.getApiService()
        runCatching {
            api.getItems(
                RetrofitClient.getUserId(), libraryId, "SortName", "Ascending",
                "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
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

    LaunchedEffect(server, libraryId) { fetchPage(0) }

    // 滚动接近末尾时加载下一页
    LaunchedEffect(itemsState.value.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= itemsState.value.size - 6 && itemsState.value.size < totalCount.intValue) {
                    fetchPage(itemsState.value.size)
                }
            }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(libraryName, color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(itemsState.value) { item ->
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(60.dp).height(90.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        // thumbnail omitted for brevity
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        if (item.seriesName != null) {
                            Text(item.seriesName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TvButton("详情") { onOpenDetail(item.id) }
                }
            }
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
                .onSuccess { seasonsState.value = it }
        } else if (item?.type == "Season") {
            runCatching {
                api.getEpisodes(item.seriesId.orEmpty(), userId, item.id, "PrimaryImageAspectRatio,BasicSyncInfo,SeriesName,ParentIndexNumber,IndexNumber", "ParentIndexNumber,IndexNumber", "Ascending").body()?.items ?: emptyList()
            }.onSuccess { episodesState.value = it }
        }
    }

    LaunchedEffect(itemId, selectedSeasonId) {
        val item = itemState.value ?: return@LaunchedEffect
        if (item.type == "Series" && selectedSeasonId != null) {
            runCatching {
                RetrofitClient.getApiService().getEpisodes(item.id, RetrofitClient.getUserId(), selectedSeasonId.orEmpty(), "PrimaryImageAspectRatio,BasicSyncInfo,SeriesName,ParentIndexNumber,IndexNumber", "ParentIndexNumber,IndexNumber", "Ascending").body()?.items ?: emptyList()
            }.onSuccess { episodesState.value = it }
        }
    }

    val item = itemState.value
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item?.name ?: "详情", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        if (item != null) {
            Text(item.overview ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4)
            Spacer(Modifier.height(12.dp))
            if (item.seriesName != null && item.type != "Series") {
                Text("${item.seriesName} - ${item.seasonName ?: ""} 第${item.indexNumber ?: 0}集", color = Color.White)
                Spacer(Modifier.height(12.dp))
            }
            when (item.type) {
                "Series" -> {
                    if (seasonsState.value.isNotEmpty()) {
                        SectionTitle("季")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(seasonsState.value) { season ->
                                val label = season.name ?: "第${season.indexNumber ?: 0}季"
                                TvButton(label) { selectedSeasonId = season.id }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (episodesState.value.isNotEmpty()) {
                            SectionTitle("剧集")
                            EpisodeList(episodesState.value, onPlay)
                        }
                    }
                }
                "Season" -> {
                    if (episodesState.value.isNotEmpty()) {
                        SectionTitle("剧集")
                        EpisodeList(episodesState.value, onPlay)
                    }
                }
                else -> {
                    Spacer(Modifier.height(24.dp))
                    Row {
                        TvButton("播放") { onPlay(item, item.mediaSources?.firstOrNull()?.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeList(episodes: List<MediaItem>, onPlay: (MediaItem, String?) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(episodes) { episode ->
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    val idx = episode.indexNumber
                    val title = if (idx != null) "第${idx}集 ${episode.name}" else episode.name
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    if (episode.overview != null) {
                        Text(episode.overview, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
                TvButton("播放") { onPlay(episode, episode.mediaSources?.firstOrNull()?.id) }
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
) {
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val suggestions = remember { mutableStateOf<List<IqiyiSuggestItem>>(emptyList()) }
    val resultsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun doSearch() {
        val s = server ?: return
        scope.launch {
            runCatching {
                RetrofitClient.getApiService().searchItems(
                    RetrofitClient.getUserId(), query, true,
                    "PrimaryImageAspectRatio,BasicSyncInfo,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
                    "", null,
                ).body()?.items ?: emptyList()
            }.onSuccess { results.value = it }
            runCatching { IqiyiSuggestApi.fetchSuggestions(query) }
                .onSuccess { suggestions.value = it }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("搜索", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TvButton("返回") { onBack() }
        }
        Spacer(Modifier.height(16.dp))
        InputField("搜索关键词", query) { query = it }
        Spacer(Modifier.height(8.dp))
        TvButton("搜索") { doSearch() }
        Spacer(Modifier.height(16.dp))
        if (results.value.isNotEmpty()) {
            SectionTitle("媒体库结果")
            ItemRow(results.value, resultsListState, onOpenDetail)
            Spacer(Modifier.height(16.dp))
        }
        if (suggestions.value.isNotEmpty()) {
            SectionTitle("搜索联想")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions.value) { s ->
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            val title = if (s.year > 0) "${s.name} (${s.year})" else s.name
                            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            if (s.mainActor.isNotEmpty()) {
                                Text(s.mainActor.joinToString(" / "), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        TvButton("搜索") {
                            query = s.name
                            doSearch()
                        }
                    }
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
    when (page) {
        "main" -> SettingsMain(server, serverPrefs, onBack = onBack, onOpen = { page = it })
        "proxy" -> ProxySettingsPage(onBack = { page = "main" })
        "decoder" -> DecoderSettingsPage(onBack = { page = "main" })
        "dlna" -> DlnaSettingsPage(onBack = { page = "main" })
        "playback" -> PlaybackSettingsPage(onBack = { page = "main" })
        "sync" -> SyncSettingsPage(server, serverPrefs, onBack = { page = "main" }, onOpenQr = { page = it })
        "qr_backup" -> QrBackupRoutesOverlay(server, serverPrefs, onBack = { page = "sync" })
        "qr_icon" -> QrIconLibraryOverlay(onBack = { page = "sync" })
        "qr_webdav" -> QrWebDavOverlay(onBack = { page = "sync" })
        "qr_font" -> QrSubtitleFontOverlay(onBack = { page = "sync" })
        "trakt" -> TraktSettingsPage(onBack = { page = "main" })
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
        InputField("地址", config.host) { save(config.copy(host = it)) }
        InputField("端口", config.port.toString()) { v -> v.toIntOrNull()?.let { save(config.copy(port = it)) } }
        InputField("用户名", config.username) { save(config.copy(username = it)) }
        InputField("密码", config.password) { save(config.copy(password = it)) }
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
        InputField("设备名称", config.deviceName) { v ->
            scope.launch { settings.saveConfig(config.copy(deviceName = v)) }
        }
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
        InputField("Client ID", clientId) { clientId = it; persistCredentials() }
        InputField("Client Secret", clientSecret) { clientSecret = it; persistCredentials() }
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

@Composable
private fun InputField(label: String, value: String, onChange: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .tvClickable(onClick = { editing = true }, onFocusChanged = {})
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value.ifBlank { "（未填写）" }, color = Color.White)
        }
    }
    if (editing) {
        Dialog(
            onDismissRequest = { editing = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            TvKeyboard(
                initialValue = value,
                title = label,
                onDone = { onChange(it); editing = false },
                onCancel = { editing = false },
            )
        }
    }
}
