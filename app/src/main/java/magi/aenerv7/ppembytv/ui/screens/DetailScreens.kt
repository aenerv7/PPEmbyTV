package magi.aenerv7.ppembytv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import magi.aenerv7.ppembytv.AppGraph
import magi.aenerv7.ppembytv.api.BaseItemDto
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.playback.PlaybackUrlBuilder
import magi.aenerv7.ppembytv.ui.PlayerLauncher
import magi.aenerv7.ppembytv.ui.UiState
import magi.aenerv7.ppembytv.ui.components.ItemPosterCard
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import magi.aenerv7.ppembytv.ui.rememberLoad
import magi.aenerv7.ppembytv.util.Formatting
import kotlinx.coroutines.launch

// ============ 通用：电影 / 集详情 ============

@Composable
fun DetailScreen(
    navigator: AppNavigator,
    itemId: String,
    type: String,
    name: String,
) {
    val state = rememberLoad(itemId) {
        val server = Session.activeServer.value ?: throw IllegalStateException("未选择服务器")
        Session.api().getItemDetails(server.userId, itemId).body()
            ?: throw Exception("条目不存在")
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val server = Session.activeServer.value

    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize().background(Color(0xFF0F1115)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4DA3FF))
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().background(Color(0xFF0F1115)), contentAlignment = Alignment.Center) {
            Text("加载失败：${state.message}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
        is UiState.Success -> {
            val item = state.data
            if (server == null) return
            val imageLoader = magi.aenerv7.ppembytv.ui.components.rememberServerImageLoader(server)
            val backdropItemId = if (item.backdropImageTag != null) item.id
            else item.parentBackdropItemId ?: item.id
            val backdropTag = item.backdropImageTag ?: item.parentBackdropImageTags?.firstOrNull()
            val backdrop = PlaybackUrlBuilder.backdropUrl(server, backdropItemId, backdropTag)
                ?: PlaybackUrlBuilder.imageUrl(server, item.id, "Primary", item.primaryImageTag, 1920)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1115)),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        if (backdrop != null) {
                            AsyncImage(
                                model = backdrop,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                imageLoader = imageLoader,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF0F1115).copy(alpha = 0.15f),
                                                Color(0xFF0F1115),
                                            )
                                        )
                                    )
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(Color(0xFF171A20)))
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 28.dp, end = 28.dp, bottom = 20.dp)
                        ) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = metaLine(item),
                                color = Color(0xFFC8CCD4),
                                fontSize = 15.sp,
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.padding(start = 28.dp, top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        TvButton(
                            "▶ 播放",
                            {
                                val pos = item.userData?.playbackPositionTicks ?: 0L
                                PlayerLauncher.play(context, item.id, pos)
                            },
                        )
                        val isFav = item.userData?.isFavorite == true
                        TvButton(
                            if (isFav) "★ 已收藏" else "☆ 收藏",
                            {
                                scope.launch {
                                    runCatching {
                                        val s = Session.activeServer.value ?: return@launch
                                        if (isFav) Session.api().unmarkFavorite(s.userId, item.id)
                                        else Session.api().markFavorite(s.userId, item.id)
                                    }
                                }
                            },
                        )
                    }
                }

                if (!item.overview.isNullOrBlank()) {
                    item {
                        Text(
                            text = item.overview,
                            color = Color(0xFFD4D7DE),
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp),
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

// ============ 剧集详情 ============

@Composable
fun SeriesDetailScreen(
    navigator: AppNavigator,
    seriesId: String,
    name: String,
) {
    val state = rememberLoad(seriesId) {
        val server = Session.activeServer.value ?: throw IllegalStateException("未选择服务器")
        val api = Session.api()
        val detail = api.getItemDetails(server.userId, seriesId).body() ?: throw Exception("条目不存在")
        val seasons = api.getSeasons(seriesId, server.userId).body()?.items ?: emptyList()
        SeriesDetailData(detail, seasons)
    }

    val server = Session.activeServer.value
    val scope = rememberCoroutineScope()

    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize().background(Color(0xFF0F1115)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4DA3FF))
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().background(Color(0xFF0F1115)), contentAlignment = Alignment.Center) {
            Text("加载失败：${state.message}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
        is UiState.Success -> {
            val data = state.data
            val item = data.series
            if (server == null) return
            val context = LocalContext.current
            val imageLoader = magi.aenerv7.ppembytv.ui.components.rememberServerImageLoader(server)
            val backdrop = PlaybackUrlBuilder.backdropUrl(server, item.id, item.backdropImageTag) ?: run {
                PlaybackUrlBuilder.imageUrl(server, item.id, "Primary", item.primaryImageTag, 1920)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1115)),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        if (backdrop != null) {
                            AsyncImage(
                                model = backdrop,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                imageLoader = imageLoader,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF0F1115).copy(alpha = 0.15f),
                                                Color(0xFF0F1115),
                                            )
                                        )
                                    )
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(Color(0xFF171A20)))
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 28.dp, end = 28.dp, bottom = 18.dp)
                        ) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = metaLine(item),
                                color = Color(0xFFC8CCD4),
                                fontSize = 15.sp,
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.padding(start = 28.dp, top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        TvButton(
                            "▶ 播放",
                            {
                                scope.launch {
                                    val api = Session.api()
                                    val seasons = data.seasons
                                    val targetSeason = seasons.firstOrNull { it.type == "Season" && it.indexNumber == 1 }
                                        ?: seasons.firstOrNull { it.type == "Season" }
                                        ?: return@launch
                                    val episodes = api.getEpisodes(seriesId, server.userId, targetSeason.id)
                                        .body()?.items ?: return@launch
                                    val ep = episodes.firstOrNull { it.userData?.played != true }
                                        ?: episodes.firstOrNull()
                                        ?: return@launch
                                    PlayerLauncher.play(context, ep.id, ep.userData?.playbackPositionTicks ?: 0L)
                                }
                            },
                        )
                    }
                }

                if (!item.overview.isNullOrBlank()) {
                    item {
                        Text(
                            text = item.overview,
                            color = Color(0xFFD4D7DE),
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp),
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "季",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 28.dp, bottom = 10.dp),
                    )
                }

                item {
                    val seasons = data.seasons.filter { it.type == "Season" }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(seasons, key = { it.id }) { season ->
                            ItemPosterCard(
                                item = season,
                                server = server,
                                onClick = {
                                    navigator.push(
                                        Screen.SeasonEpisodes(
                                            seriesId = seriesId,
                                            seasonId = season.id,
                                            seriesName = item.name,
                                            seasonName = season.name,
                                        )
                                    )
                                },
                                width = 150.dp,
                                height = 200.dp,
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

// ============ 季内集列表 ============

@Composable
fun SeasonEpisodesScreen(
    navigator: AppNavigator,
    seriesId: String,
    seasonId: String,
    seriesName: String,
    seasonName: String,
) {
    val state = rememberLoad(seasonId) {
        val server = Session.activeServer.value ?: throw IllegalStateException("未选择服务器")
        Session.api().getEpisodes(seriesId, server.userId, seasonId).body()?.items ?: emptyList()
    }
    val context = LocalContext.current
    val server = Session.activeServer.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(top = 20.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 24.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$seriesName · $seasonName",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.width(14.dp))
            TvButton("← 返回", { navigator.pop() })
        }
        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4DA3FF))
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载失败：${state.message}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            is UiState.Success -> {
                val episodes = state.data
                if (episodes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("本季暂无剧集", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(episodes, key = { it.id }) { ep ->
                            EpisodeRowCard(
                                episode = ep,
                                server = server ?: return@items,
                                onClick = {
                                    PlayerLauncher.play(context, ep.id, ep.userData?.playbackPositionTicks ?: 0L)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRowCard(
    episode: BaseItemDto,
    server: magi.aenerv7.ppembytv.data.ServerConfig,
    onClick: () -> Unit,
) {
    val imageUrl = PlaybackUrlBuilder.imageUrl(
        server, episode.id, "Primary", episode.primaryImageTag, 400,
    )
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (episode.userData?.played == true) Color(0xFF1A1E26) else Color(0xFF20242E),
                androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            )
            .then(Modifier)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(100.dp)
                .background(Color(0xFF14171D), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    imageLoader = magi.aenerv7.ppembytv.ui.components.rememberServerImageLoader(server),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "第${episode.indexNumber ?: 0}集",
                    color = Color(0xFF4DA3FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (episode.userData?.played == true) {
                    Spacer(Modifier.width(8.dp))
                    Text("已看", color = Color(0xFF4CD964), fontSize = 12.sp)
                }
            }
            Text(
                text = episode.name,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (!episode.overview.isNullOrBlank()) {
                Text(
                    text = episode.overview,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        TvButton("▶ 播放", onClick)
    }
}

// ============ 工具 ============

private data class SeriesDetailData(
    val series: BaseItemDto,
    val seasons: List<BaseItemDto>,
)

private fun metaLine(item: BaseItemDto): String {
    val parts = mutableListOf<String>()
    item.productionYear?.let { parts.add(it.toString()) }
    item.communityRating?.takeIf { it > 0 }?.let { parts.add("★ ${String.format(java.util.Locale.US, "%.1f", it)}") }
    item.runTimeTicks?.let { parts.add(Formatting.runtime(it) ?: "") }
    item.genres?.take(3)?.let { parts.addAll(it) }
    return parts.filter { it.isNotBlank() }.joinToString(" · ")
}
