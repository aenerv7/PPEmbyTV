package magi.aenerv7.ppembytv.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import magi.aenerv7.ppembytv.api.HttpClients
import magi.aenerv7.ppembytv.api.PlaybackProgressInfo
import magi.aenerv7.ppembytv.api.Session
import magi.aenerv7.ppembytv.playback.PlaybackReporter
import magi.aenerv7.ppembytv.playback.PlaybackUrlBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaybackSession(
    val itemId: String,
    val itemName: String,
    val mediaSourceId: String,
    val playSessionId: String,
    val playMethod: String,
    val runTimeTicks: Long?,
    val mediaItem: MediaItem,
    val startPositionMs: Long,
)

private sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(val session: PlaybackSession) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

private val SPEEDS = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/** 全屏播放器（Media3 ExoPlayer + 自定义 TV 控制栏） */
@Composable
fun PlayerScreen(
    itemId: String,
    startTicks: Long,
    onExit: () -> Unit,
) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context)
            .setRenderersFactory(
                OffsetRenderersFactory(context) { SubtitleOffset.offsetMs * 1000L }
            )
            .setTrackSelector(DefaultTrackSelector(context))
            .build()
            .apply { playWhenReady = true }
    }

    var uiState by remember { mutableStateOf<PlayerUiState>(PlayerUiState.Loading) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableStateOf(1.0f) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // 播放器状态监听
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val d = player.duration
                    if (d > 0) durationMs = d
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorText = error.message ?: "播放失败（${error.errorCodeName}）"
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // 位置刷新
    LaunchedEffect(Unit) {
        while (true) {
            positionMs = player.currentPosition
            val d = player.duration
            if (d > 0) durationMs = d
            delay(500)
        }
    }

    // 控制栏自动隐藏
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(5000)
            controlsVisible = false
        }
    }

    // BACK：先收控制栏，再退出
    BackHandler(enabled = true) {
        if (controlsVisible) {
            controlsVisible = false
        } else {
            onExit()
        }
    }

    // 加载播放信息并准备播放
    LaunchedEffect(itemId) {
        uiState = PlayerUiState.Loading
        val result = withContext(Dispatchers.IO) { loadPlaybackSession(itemId, startTicks) }
        result.onSuccess { session ->
            uiState = PlayerUiState.Ready(session)
            preparePlayer(player, session)
        }.onFailure { e ->
            uiState = PlayerUiState.Error(e.message ?: "播放失败")
        }
    }

    // 播放进度上报（开始 + 每 10 秒）
    val session = (uiState as? PlayerUiState.Ready)?.session
    LaunchedEffect(session?.playSessionId) {
        val s = session ?: return@LaunchedEffect
        val reporter = PlaybackReporter(Session.api())
        fun info(event: String, paused: Boolean = false) = PlaybackProgressInfo(
            itemId = s.itemId,
            mediaSourceId = s.mediaSourceId,
            positionTicks = (player.currentPosition * 10_000).coerceAtLeast(0L),
            isPaused = paused || !player.isPlaying,
            playMethod = s.playMethod,
            playSessionId = s.playSessionId,
            canSeek = true,
            eventName = event,
            playbackRate = player.playbackParameters.speed.toDouble(),
            runTimeTicks = s.runTimeTicks,
        )
        reporter.reportStart(info("startup"))
        while (isActive) {
            delay(10_000)
            reporter.reportProgress(info("timeupdate"))
        }
    }

    // 退出时上报停止（使用独立作用域，避免 rememberCoroutineScope 在销毁时已被取消）
    val exitScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO) }
    DisposableEffect(session) {
        onDispose {
            val s = session ?: return@onDispose
            val pos = player.currentPosition
            val method = s.playMethod
            val itemIdS = s.itemId
            val msId = s.mediaSourceId
            val psId = s.playSessionId
            val rt = s.runTimeTicks
            exitScope.launch {
                PlaybackReporter(Session.api()).reportStopped(
                    PlaybackProgressInfo(
                        itemId = itemIdS,
                        mediaSourceId = msId,
                        positionTicks = (pos * 10_000).coerceAtLeast(0L),
                        isPaused = false,
                        playMethod = method,
                        playSessionId = psId,
                        eventName = "stopped",
                        runTimeTicks = rt,
                    )
                )
            }
        }
    }

    // 画面
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (uiState is PlayerUiState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在加载播放…", color = Color.White, fontSize = 16.sp)
            }
        }

        errorText?.let { err ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                    .padding(24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("播放出错", color = Color(0xFFFF6B6B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(err, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        if (controlsVisible) {
            val s = (uiState as? PlayerUiState.Ready)?.session
            if (s != null) {
                PlayerControls(
                    session = s,
                    player = player,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    speed = speed,
                    subtitleOffsetMs = subtitleOffsetMs,
                    onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                    onSeekBy = { deltaMs -> player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0L)) },
                    onCycleSpeed = {
                        val idx = SPEEDS.indexOfFirst { it == speed }
                        val next = SPEEDS[(idx + 1) % SPEEDS.size]
                        speed = next
                        player.setPlaybackSpeed(next)
                    },
                    onSubtitleOffset = { deltaMs ->
                        subtitleOffsetMs += deltaMs
                        SubtitleOffset.offsetMs = subtitleOffsetMs
                    },
                    onSelectTrack = { type, groupIdx, trackIdx -> selectTrack(player, type, groupIdx, trackIdx) },
                    onDisableSubtitles = {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    },
                    onHide = { controlsVisible = false },
                    onExit = onExit,
                    onShowError = { errorText = it },
                )
            }
        }

        // 点击切换控制栏
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    controlsVisible = !controlsVisible
                }
        )
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
}

/** 加载播放信息并构建 MediaItem（返回给 UI 使用） */
private suspend fun loadPlaybackSession(itemId: String, startTicks: Long): Result<PlaybackSession> {
    return runCatching {
        val server = Session.activeServer.value ?: throw IllegalStateException("未选择服务器")
        val api = Session.api()
        val userId = server.userId

        val item = api.getItemDetails(userId, itemId).body()
            ?: throw Exception("无法获取条目信息")
        val playbackInfo = api.getPlaybackInfo(
            itemId = itemId,
            userId = userId,
            isPlayback = true,
        ).body()
        val source = playbackInfo?.mediaSources?.firstOrNull()
            ?: item.mediaSources?.firstOrNull()
            ?: throw Exception("没有可播放的媒体源")

        val direct = source.supportsDirectPlay || source.supportsDirectStream
        val url = if (direct) {
            PlaybackUrlBuilder.directStreamUrl(server, itemId, source)
        } else {
            PlaybackUrlBuilder.transcodeUrl(server, itemId, source)
                ?: throw Exception("该媒体无法直接播放且不支持转码")
        }

        val builder = MediaItem.Builder()
            .setMediaId(itemId)
            .setUri(url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(item.name)
                    .setArtist(item.seriesName)
                    .build()
            )

        // 外挂/提取文本字幕
        val externalSubs = source.mediaStreams
            .filter { it.type == "Subtitle" && it.isExternal && it.isTextSubtitleStream }
        if (externalSubs.isNotEmpty()) {
            val subtitles = externalSubs.mapNotNull { stream ->
                val subUrl = PlaybackUrlBuilder.subtitleUrl(server, itemId, source, stream) ?: return@mapNotNull null
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subUrl))
                    .setMimeType(subtitleMimeType(stream.codec))
                    .setLanguage(stream.language)
                    .setLabel(stream.displayTitle ?: stream.language ?: "字幕")
                    .setSelectionFlags(if (stream.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }
            builder.setSubtitleConfigurations(subtitles)
        }

        PlaybackSession(
            itemId = itemId,
            itemName = item.name,
            mediaSourceId = source.id,
            playSessionId = playbackInfo?.playSessionId.orEmpty(),
            playMethod = if (direct) "DirectStream" else "Transcode",
            runTimeTicks = source.runTimeTicks ?: item.runTimeTicks,
            mediaItem = builder.build(),
            startPositionMs = (startTicks / 10_000).coerceAtLeast(0L),
        )
    }
}

private fun preparePlayer(player: ExoPlayer, session: PlaybackSession) {
    val server = Session.activeServer.value ?: return
    val client = HttpClients.buildOkHttpClient(server, Session.currentProxy())
    val dataSourceFactory = OkHttpDataSource.Factory(client)
    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
    player.setMediaSource(mediaSourceFactory.createMediaSource(session.mediaItem))
    player.prepare()
    if (session.startPositionMs > 0) {
        player.seekTo(session.startPositionMs)
    }
}

private fun subtitleMimeType(codec: String): String = when (codec.lowercase()) {
    "ass", "ssa" -> "text/x-ssa"
    "vtt", "webvtt" -> "text/vtt"
    else -> "application/x-subrip"
}

/** 切换音轨/字幕轨 */
private fun selectTrack(player: ExoPlayer, type: Int, groupIndex: Int, trackIndex: Int) {
    val groups = player.currentTracks.groups
    if (groupIndex < 0 || groupIndex >= groups.size) return
    val group = groups[groupIndex].mediaTrackGroup
    val params: TrackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .clearOverridesOfType(type)
        .setOverrideForType(TrackSelectionOverride(group, trackIndex))
        .setTrackTypeDisabled(type, false)
        .build()
    player.trackSelectionParameters = params
}

/** 轨道列表（用于控制栏弹层） */
data class PlayerTrack(
    val type: Int,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean,
)

fun currentTracks(player: ExoPlayer): List<PlayerTrack> {
    val result = mutableListOf<PlayerTrack>()
    val groups = player.currentTracks.groups
    for (gi in groups.indices) {
        val group = groups[gi]
        val type = group.type
        if (type != C.TRACK_TYPE_AUDIO && type != C.TRACK_TYPE_TEXT) continue
        for (ti in 0 until group.length) {
            val format = group.getTrackFormat(ti)
            val label = when (type) {
                C.TRACK_TYPE_AUDIO -> {
                    val lang = format.language ?: ""
                    val codec = format.codecs?.substringBefore('.') ?: ""
                    listOf(lang.ifBlank { null }, codec.ifBlank { null })
                        .filterNotNull().joinToString(" · ").ifEmpty { "音轨 ${ti + 1}" }
                }
                else -> {
                    val label = format.label
                    val lang = format.language ?: ""
                    label?.ifBlank { null } ?: lang.ifBlank { "字幕 ${ti + 1}" }
                }
            }
            result.add(
                PlayerTrack(
                    type = type,
                    groupIndex = gi,
                    trackIndex = ti,
                    label = label,
                    isSelected = group.isTrackSelected(ti),
                )
            )
        }
    }
    return result
}
