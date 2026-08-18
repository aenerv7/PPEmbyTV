package magi.aenerv7.ppembytv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.api.BaseItemDto
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.ui.PlayerLauncher
import magi.aenerv7.ppembytv.ui.UiState
import magi.aenerv7.ppembytv.ui.components.ItemPosterCard
import magi.aenerv7.ppembytv.ui.components.SectionTitle
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import magi.aenerv7.ppembytv.ui.rememberLoad
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private data class LibraryRow(
    val library: magi.aenerv7.ppembytv.api.BaseItemDto,
    val items: List<BaseItemDto>,
)

private data class HomeData(
    val resume: List<BaseItemDto>,
    val nextUp: List<BaseItemDto>,
    val latest: List<BaseItemDto>,
    val libraryRows: List<LibraryRow>,
)

private suspend fun loadHome(): HomeData {
    val server = Session.activeServer.value ?: throw IllegalStateException("未选择服务器")
    val api = Session.api()
    val userId = server.userId
    return coroutineScope {
        val r1 = async { runCatching { api.getResumeItems(userId, limit = 24).body()?.items ?: emptyList() }.getOrDefault(emptyList()) }
        val r2 = async { runCatching { api.getNextUp(userId, limit = 24).body()?.items ?: emptyList() }.getOrDefault(emptyList()) }
        val r3 = async { runCatching { api.getLatestMedia(userId, limit = 24).body() ?: emptyList() }.getOrDefault(emptyList()) }
        val r4 = async { runCatching { api.getLibraries(userId).body()?.items ?: emptyList() }.getOrDefault(emptyList()) }
        // 各媒体库的最新内容（电影/剧集类库），空库跳过
        val libraries = r4.await()
            .filter { it.type == "CollectionFolder" && (it.collectionType == "movies" || it.collectionType == "tvshows") }
        val libRows = libraries
            .map { lib ->
                async {
                    val items = runCatching {
                        api.getLatestMedia(userId, limit = 12, parentId = lib.id).body() ?: emptyList()
                    }.getOrDefault(emptyList())
                    LibraryRow(lib, items)
                }
            }
            .map { it.await() }
            .filter { it.items.isNotEmpty() }
        HomeData(r1.await(), r2.await(), r3.await(), libRows)
    }
}

/** 首页：继续观看 / 最新添加 / 剧集更新 */
@Composable
fun HomeScreen(navigator: AppNavigator) {
    val state = rememberLoad(Unit) { loadHome() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115)),
    ) {
        Text(
            text = "首页",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 16.dp),
        )
        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4DA3FF))
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("加载失败：${state.message}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                }
            }
            is UiState.Success -> {
                val data = state.data
                val server = Session.activeServer.value
                if (server == null) return
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    if (data.resume.isNotEmpty()) {
                        item {
                            ItemRow("继续观看", data.resume, server, navigator, PaddingValues(horizontal = 24.dp))
                        }
                    }
                    if (data.nextUp.isNotEmpty()) {
                        item {
                            ItemRow("接下来观看", data.nextUp, server, navigator, PaddingValues(horizontal = 24.dp))
                        }
                    }
                    if (data.latest.isNotEmpty()) {
                        item {
                            ItemRow("最新添加", data.latest, server, navigator, PaddingValues(horizontal = 24.dp))
                        }
                    }
                    val movies = data.latest.filter { it.type == "Movie" }
                    if (movies.isNotEmpty()) {
                        item {
                            ItemRow("最新电影", movies, server, navigator, PaddingValues(horizontal = 24.dp))
                        }
                    }
                    val series = data.latest.filter { it.type == "Series" }
                    if (series.isNotEmpty()) {
                        item {
                            ItemRow("最新剧集", series, server, navigator, PaddingValues(horizontal = 24.dp))
                        }
                    }
                    data.libraryRows.forEach { row ->
                        item {
                            ItemRow(
                                title = "${row.library.name} · 最新",
                                items = row.items,
                                server = server,
                                navigator = navigator,
                                contentPadding = PaddingValues(horizontal = 24.dp),
                            )
                        }
                    }
                    if (data.latest.isEmpty() && data.resume.isEmpty() && data.nextUp.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
                                Text("暂无内容，请确认服务器媒体库配置", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    title: String,
    items: List<BaseItemDto>,
    server: magi.aenerv7.ppembytv.data.ServerConfig,
    navigator: AppNavigator,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    Column {
        SectionTitle(title)
        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items, key = { it.id }) { item ->
                ItemPosterCard(
                    item = item,
                    server = server,
                    onClick = {
                        when (item.type) {
                            "Series" -> navigator.push(Screen.SeriesDetail(item.id, item.name))
                            "Episode" -> {
                                val pos = item.userData?.playbackPositionTicks ?: 0L
                                PlayerLauncher.play(context, item.id, pos)
                            }
                            "Movie" -> navigator.push(Screen.Detail(item.id, "Movie", item.name))
                            else -> navigator.push(Screen.Detail(item.id, item.type, item.name))
                        }
                    },
                )
            }
        }
    }
}
