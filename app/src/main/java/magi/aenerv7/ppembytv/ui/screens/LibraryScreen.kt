package magi.aenerv7.ppembytv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import magi.aenerv7.ppembytv.ui.components.ItemPosterCard
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import magi.aenerv7.ppembytv.ui.rememberLoad
import magi.aenerv7.ppembytv.ui.UiState

private const val PAGE_SIZE = 60

private data class LibraryData(
    val items: List<BaseItemDto>,
    val total: Int,
)

private suspend fun loadLibrary(parentId: String?, includeItemTypes: String, startIndex: Int): LibraryData {
    val server = Session.activeServer.value ?: throw IllegalStateException("未选择服务器")
    val api = Session.api()
    val resp = api.getItems(
        userId = server.userId,
        parentId = parentId,
        sortBy = "SortName",
        includeItemTypes = includeItemTypes,
        recursive = parentId != null || includeItemTypes == "Movie" || includeItemTypes == "Series",
        limit = PAGE_SIZE,
        startIndex = startIndex,
    )
    val body = resp.body() ?: throw Exception("HTTP ${resp.code()}")
    return LibraryData(body.items, body.totalRecordCount)
}

/** 媒体库网格页（电影 / 剧集 / 自定义媒体库） */
@Composable
fun LibraryScreen(
    navigator: AppNavigator,
    parentId: String?,
    includeItemTypes: String,
    title: String,
) {
    var startIndex by remember { mutableIntStateOf(0) }
    val state = rememberLoad("$parentId|$includeItemTypes|$startIndex") {
        loadLibrary(parentId, includeItemTypes, startIndex)
    }
    val context = LocalContext.current
    val server = Session.activeServer.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115)),
    ) {
        Row(
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (state is UiState.Success) "共 ${state.data.total} 个" else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 14.dp),
            )
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4DA3FF))
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载失败：${state.message}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            is UiState.Success -> {
                val data = state.data
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(data.items, key = { it.id }) { item ->
                        ItemPosterCard(
                            item = item,
                            server = server ?: return@items,
                            onClick = {
                                when (item.type) {
                                    "Series" -> navigator.push(Screen.SeriesDetail(item.id, item.name))
                                    "Episode" -> PlayerLauncher.play(context, item.id, item.userData?.playbackPositionTicks ?: 0L)
                                    else -> navigator.push(Screen.Detail(item.id, item.type, item.name))
                                }
                            },
                        )
                    }
                    if (startIndex + data.items.size < data.total) {
                        item {
                            Column(
                                modifier = Modifier
                                    .height(220.dp)
                                    .padding(top = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                TvButton("加载更多", { startIndex += PAGE_SIZE })
                            }
                        }
                    }
                }
            }
        }
    }
}
