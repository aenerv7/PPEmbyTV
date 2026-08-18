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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import magi.aenerv7.ppembytv.api.SearchHint
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.api.SearchHintResult
import magi.aenerv7.ppembytv.ui.PlayerLauncher
import magi.aenerv7.ppembytv.ui.UiState
import magi.aenerv7.ppembytv.ui.components.PosterCard
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.nav.AppNavigator
import magi.aenerv7.ppembytv.ui.nav.Screen
import magi.aenerv7.ppembytv.ui.rememberLoad
import magi.aenerv7.ppembytv.playback.PlaybackUrlBuilder

private val KEYBOARD_ROWS = listOf(
    "qwertyuiop",
    "asdfghjkl",
    "zxcvbnm",
    "0123456789",
)

/** 搜索页（带电视端屏幕键盘） */
@Composable
fun SearchScreen(navigator: AppNavigator) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()
    val state = rememberLoad(trimmed.ifEmpty { "____none____" }) {
        if (trimmed.isEmpty()) {
            SearchHintResult()
        } else {
            val server = Session.activeServer.value ?: throw IllegalStateException("未选择服务器")
            Session.api().searchHints(searchTerm = trimmed, userId = server.userId).body()
                ?: SearchHintResult()
        }
    }
    val context = LocalContext.current
    val server = Session.activeServer.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🔍 搜索",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(20.dp))
            // 搜索关键词显示
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1D24), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(
                    text = if (query.isEmpty()) "输入关键词搜索…" else query,
                    color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    fontSize = 18.sp,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        // 屏幕键盘
        TvKeyboard(
            query = query,
            onKey = { ch -> query += ch },
            onBackspace = { query = query.dropLast(1) },
            onClear = { query = "" },
        )

        Spacer(Modifier.height(14.dp))

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("搜索中…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("搜索失败：${state.message}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            is UiState.Success -> {
                val hints = state.data.searchHints
                if (hints.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (trimmed.isEmpty()) "输入关键词开始搜索" else "没有找到相关结果",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(hints, key = { it.itemId }) { hint ->
                            SearchResultCard(
                                hint = hint,
                                server = server ?: return@items,
                                onClick = {
                                    when (hint.type) {
                                        "Series" -> navigator.push(Screen.SeriesDetail(hint.itemId, hint.name))
                                        "Episode" -> PlayerLauncher.play(context, hint.itemId)
                                        "Movie" -> navigator.push(Screen.Detail(hint.itemId, "Movie", hint.name))
                                        else -> navigator.push(Screen.Detail(hint.itemId, hint.type, hint.name))
                                    }
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
private fun TvKeyboard(
    query: String,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in KEYBOARD_ROWS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (ch in row) {
                    KeyButton(ch.toString(), Modifier.weight(1f)) { onKey(ch) }
                }
                if (row == KEYBOARD_ROWS.last()) {
                    KeyButton("空格", Modifier.weight(1f)) { onKey(' ') }
                    KeyButton("⌫", Modifier.weight(1f)) { onBackspace() }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TvButton(
        text = label,
        onClick = onClick,
        modifier = modifier.height(44.dp),
    )
}

@Composable
private fun SearchResultCard(
    hint: SearchHint,
    server: magi.aenerv7.ppembytv.data.ServerConfig,
    onClick: () -> Unit,
) {
    val imageUrl = PlaybackUrlBuilder.imageUrl(
        server = server,
        itemId = hint.itemId,
        type = "Primary",
        tag = hint.primaryImageTag,
        maxWidth = 300,
    )
    val imageLoader = magi.aenerv7.ppembytv.ui.components.rememberServerImageLoader(server)
    PosterCard(
        title = hint.name,
        subtitle = when {
            hint.type == "Episode" -> listOfNotNull(
                hint.parentIndexNumber?.let { "S${it}E${hint.indexNumber ?: ""}" },
                hint.series,
            ).joinToString(" · ").ifEmpty { hint.type }
            else -> listOfNotNull(hint.type, hint.productionYear?.toString()).joinToString(" · ")
        },
        imageUrl = imageUrl,
        onClick = onClick,
        imageLoader = imageLoader,
    )
}
