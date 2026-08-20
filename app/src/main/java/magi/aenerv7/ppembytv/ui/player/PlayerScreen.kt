@file:androidx.media3.common.util.UnstableApi

package magi.aenerv7.ppembytv.ui.player

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import magi.aenerv7.ppembytv.data.SubtitleFontManager
import magi.aenerv7.ppembytv.data.SubtitlePreferences
import magi.aenerv7.ppembytv.data.api.PlaybackProgressInfo
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import magi.aenerv7.ppembytv.data.model.MediaItem
import magi.aenerv7.ppembytv.data.model.MediaSource
import magi.aenerv7.ppembytv.data.model.PlaybackInfoRequest
import magi.aenerv7.ppembytv.data.model.ServerConfig
import magi.aenerv7.ppembytv.data.model.createAndroidTvDeviceProfile
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.ui.components.tvClickable
import magi.aenerv7.ppembytv.ui.components.tvFocusBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@Composable
fun PlayerScreen(
    server: ServerConfig?,
    item: MediaItem,
    mediaSourceId: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setHandleAudioBecomingNoisy(true)
        }
    }
    val fontManager = remember { SubtitleFontManager(context) }
    val subtitlePrefs = remember { SubtitlePreferences(context) }
    var mediaSource by remember { mutableStateOf<MediaSource?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var startTicks by remember { mutableLongStateOf(item.userData?.playbackPositionTicks ?: 0L) }
    var speedIndex by remember { mutableIntStateOf(2) }
    var showControls by remember { mutableStateOf(true) }
    var controlActivity by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var clockText by remember { mutableStateOf("") }
    val playerFocusRequester = remember { FocusRequester() }
    val pauseFocusRequester = remember { FocusRequester() }

    fun revealControls() {
        showControls = true
        controlActivity++
    }

    // Fetch playback info and prepare the player.
    LaunchedEffect(item.id, mediaSourceId) {
        val s = server ?: return@LaunchedEffect
        val api = RetrofitClient.getApiService()
        val userId = RetrofitClient.getUserId()
        try {
            val request = PlaybackInfoRequest(
                deviceProfile = createAndroidTvDeviceProfile(),
                mediaSourceId = mediaSourceId,
                startTimeTicks = startTicks,
                audioStreamIndex = null,
                subtitleStreamIndex = null,
                maxStreamingBitrate = null,
                enableDirectPlay = true,
                enableDirectStream = true,
                enableTranscoding = true,
            )
            val response = api.getPlaybackInfo(item.id, userId, autoOpenLiveStream = false, isPlayback = true, body = request)
            if (response.isSuccessful) {
                val sources = response.body()?.mediaSources ?: emptyList()
                val selected = sources.firstOrNull { it.id == mediaSourceId } ?: sources.firstOrNull()
                mediaSource = selected
                val url = buildPlaybackUrl(item.id, selected, startTicks)
                if (url != null) {
                    player.setMediaItem(Media3Item.fromUri(url))
                    player.prepare()
                    player.play()
                    reportStart(item, selected)
                } else {
                    error = "无法构建播放地址"
                }
            } else {
                error = "获取播放信息失败 (${response.code()})"
            }
        } catch (e: Exception) {
            error = "播放失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Periodic progress reporting.
    LaunchedEffect(player, mediaSource) {
        while (mediaSource != null) {
            delay(10_000)
            val ms = mediaSource ?: continue
            reportProgress(item, ms, player)
        }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            isPlaying = player.isPlaying
            clockText = SimpleDateFormat("h:mm a", Locale.US).format(Date())
            delay(500)
        }
    }

    LaunchedEffect(Unit) { playerFocusRequester.requestFocus() }
    LaunchedEffect(showControls, controlActivity, isLoading, error) {
        if (showControls && !isLoading && error == null) {
            delay(5_000)
            showControls = false
            playerFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(showControls, isLoading, error) {
        if (showControls && !isLoading && error == null) pauseFocusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            reportStopped(item, mediaSource, player)
            player.release()
        }
    }

    fun cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEEDS.size
        player.setPlaybackSpeed(SPEEDS[speedIndex])
    }

    fun cycleTrack(trackType: Int) {
        val tracks = player.currentTracks
        val group = tracks.groups.firstOrNull { it.type == trackType } ?: return
        val trackGroup = group.mediaTrackGroup
        val trackCount = trackGroup.length
        val params = player.trackSelectionParameters
        val currentIndex = params.overrides[trackGroup]?.trackIndices?.firstOrNull() ?: -1
        val nextIndex = if (trackType == C.TRACK_TYPE_TEXT) {
            when {
                currentIndex < 0 -> 0
                currentIndex >= trackCount - 1 -> -1
                else -> currentIndex + 1
            }
        } else {
            (currentIndex + 1) % trackCount
        }
        val builder = params.buildUpon()
        if (nextIndex < 0) {
            builder.clearOverridesOfType(trackType)
        } else {
            builder.setOverrideForType(TrackSelectionOverride(trackGroup, nextIndex))
        }
        player.trackSelectionParameters = builder.build()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || showControls) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown, Key.DirectionUp -> {
                        revealControls()
                        true
                    }
                    Key.DirectionLeft -> {
                        player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L))
                        revealControls()
                        true
                    }
                    Key.DirectionRight -> {
                        player.seekTo(player.currentPosition + 10_000)
                        revealControls()
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (player.isPlaying) player.pause() else player.play()
                        revealControls()
                        true
                    }
                    else -> false
                }
            },
    ) {
        if (mediaSource != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        subtitleView?.let { applySubtitleEnhancement(it, fontManager, subtitlePrefs) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isLoading) {
            Text("加载中...", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        if (error != null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error.orEmpty(), color = Color(0xFFFF6B6B))
                Spacer(Modifier.height(16.dp))
                TvButton("返回") { onBack() }
            }
        }
        if (!showControls && error == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { revealControls() })
                    },
            )
        }
        if (showControls) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.88f),
                            0.16f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.94f),
                        )
                    ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.seriesName ?: item.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(clockText, color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.titleLarge)
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 26.dp, vertical = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(item.name, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        if (item.parentIndexNumber != null || item.indexNumber != null) {
                            Spacer(Modifier.width(18.dp))
                            Text(
                                "S${item.parentIndexNumber ?: 1}:E${item.indexNumber ?: 1}",
                                color = Color.White.copy(alpha = 0.82f),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(playerMediaSummary(mediaSource), color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatPosition(positionMs), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = positionMs.coerceAtMost(durationMs).toFloat(),
                            onValueChange = {
                                positionMs = it.toLong()
                                player.seekTo(positionMs)
                                revealControls()
                            },
                            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                            ),
                        )
                        Text(formatPosition(durationMs), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            PlayerIconButton(Icons.Default.Replay10, "后退 10 秒") {
                                player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L))
                                revealControls()
                            }
                            PlayerIconButton(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) "暂停" else "播放",
                                focusRequester = pauseFocusRequester,
                            ) {
                                if (player.isPlaying) player.pause() else player.play()
                                revealControls()
                            }
                            PlayerIconButton(Icons.Default.Forward10, "前进 10 秒") {
                                player.seekTo(player.currentPosition + 10_000)
                                revealControls()
                            }
                            PlayerIconButton(Icons.Default.SkipNext, "下一集") {
                                if (durationMs > 0L) player.seekTo(durationMs)
                                revealControls()
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            PlayerIconButton(Icons.AutoMirrored.Filled.FormatListBulleted, "播放列表") { revealControls() }
                            PlayerIconButton(Icons.Default.Subtitles, "字幕") {
                                cycleTrack(C.TRACK_TYPE_TEXT)
                                revealControls()
                            }
                            PlayerIconButton(Icons.Default.MusicNote, "音轨") {
                                cycleTrack(C.TRACK_TYPE_AUDIO)
                                revealControls()
                            }
                            PlayerPillButton("${SPEEDS[speedIndex]}x") {
                                cycleSpeed()
                                revealControls()
                            }
                            PlayerPillButton("解码") { revealControls() }
                            PlayerIconButton(Icons.Default.MoreHoriz, "更多") { revealControls() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(46.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(shape)
            .background(if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .tvFocusBorder(focused, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(25.dp))
    }
}

@Composable
private fun PlayerPillButton(text: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .height(42.dp)
            .tvClickable(onClick = onClick, onFocusChanged = { focused = it })
            .clip(shape)
            .background(if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .tvFocusBorder(focused, shape)
            .padding(horizontal = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatPosition(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun playerMediaSummary(source: MediaSource?): String {
    val mediaSource = source ?: return ""
    val video = mediaSource.mediaStreams?.firstOrNull { it.type == "Video" }
    return buildList {
        video?.height?.let { add(if (it >= 2160) "4K" else "${it}P") }
        video?.videoRangeType?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        mediaSource.container.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        mediaSource.size?.takeIf { it > 0L }?.let { add(String.format(Locale.US, "%.2fG", it / 1_073_741_824.0)) }
        mediaSource.bitrate?.takeIf { it > 0 }?.let { add(String.format(Locale.US, "%.1fMbps", it / 1_000_000.0)) }
    }.joinToString(" · ")
}

/**
 * ASS 增强渲染：把上传的自定义字幕字体、全局字幕字号/颜色应用到 Media3 的 SubtitleView。
 * - [SubtitleView.setApplyEmbeddedStyles]/[SubtitleView.setApplyEmbeddedFontSizes]：保留 ASS 内置样式与字号标签
 * - CaptionStyleCompat.typeface：Media3 的 SubtitlePainter 会把它设到 TextPaint 上（已核对 media3-ui 1.5.1 字节码）
 * - 未带颜色标签的字幕文字使用用户选择的颜色；字号按倍率整体缩放
 */
private fun applySubtitleEnhancement(
    subtitleView: SubtitleView,
    fontManager: SubtitleFontManager,
    subtitlePrefs: SubtitlePreferences,
) {
    subtitleView.setApplyEmbeddedStyles(true)
    subtitleView.setApplyEmbeddedFontSizes(true)
    val typeface = fontManager.selectedTypeface()
    val color = subtitlePrefs.getSubtitleFontColor()
    val style = CaptionStyleCompat(
        color.colorValue,
        AndroidColor.TRANSPARENT,
        AndroidColor.TRANSPARENT,
        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        AndroidColor.BLACK,
        typeface,
    )
    subtitleView.setStyle(style)
    subtitleView.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlePrefs.getSubtitleFontScale())
}

private fun buildPlaybackUrl(itemId: String, source: MediaSource?, startTicks: Long): String? {
    if (source == null) return null
    // 优先使用服务器返回的直接流/转码 URL（已含正确的直连/转码判断与起播位置）。
    source.directStreamUrl?.let { url ->
        return RetrofitClient.remapAbsoluteMediaUrlToBaseUrl(url)
    }
    source.transcodingUrl?.let { url ->
        return RetrofitClient.remapAbsoluteMediaUrlToBaseUrl(url)
    }
    // 兜底：客户端拼接直连 URL。
    val container = source.container.takeIf { it.isNotBlank() } ?: "mkv"
    return RetrofitClient.getVideoUrl(itemId, source.id, container, startTicks)
}

private fun reportStart(item: MediaItem, source: MediaSource?) {
    val ms = source ?: return
    val scope = kotlinx.coroutines.MainScope()
    scope.launch {
        runCatching {
            RetrofitClient.getApiService().reportPlaybackStart(buildProgressInfo(item, ms, 0L, "DirectPlay", 0L))
        }
    }
}

private fun reportProgress(item: MediaItem, source: MediaSource, player: Player) {
    val scope = kotlinx.coroutines.MainScope()
    scope.launch {
        runCatching {
            RetrofitClient.getApiService().reportPlaybackProgress(
                buildProgressInfo(item, source, player.currentPosition, "DirectPlay", player.duration)
            )
        }
    }
}

private fun reportStopped(item: MediaItem, source: MediaSource?, player: Player) {
    val ms = source ?: return
    val scope = kotlinx.coroutines.MainScope()
    scope.launch {
        runCatching {
            RetrofitClient.getApiService().reportPlaybackStopped(
                buildProgressInfo(item, ms, player.currentPosition, "DirectPlay", player.duration)
            )
        }
    }
}

private fun buildProgressInfo(
    item: MediaItem,
    source: MediaSource,
    positionMs: Long,
    playMethod: String,
    durationMs: Long,
): PlaybackProgressInfo {
    return PlaybackProgressInfo(
        itemId = item.id,
        positionTicks = positionMs * 10_000L,
        isPaused = false,
        playMethod = playMethod,
        canSeek = true,
        mediaSourceId = source.id,
        playSessionId = "",
        eventName = null,
        isMuted = false,
        playbackRate = 1,
        repeatMode = "RepeatNone",
        playlistIndex = 0,
        playlistLength = 1,
        runTimeTicks = if (durationMs > 0) durationMs * 10_000L else null,
        nowPlayingQueue = null,
    )
}
