package magi.aenerv7.ppembytv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import magi.aenerv7.ppembytv.data.api.PlaybackProgressInfo
import magi.aenerv7.ppembytv.data.api.RetrofitClient
import magi.aenerv7.ppembytv.data.model.MediaItem
import magi.aenerv7.ppembytv.data.model.MediaSource
import magi.aenerv7.ppembytv.data.model.PlaybackInfoRequest
import magi.aenerv7.ppembytv.data.model.ServerConfig
import magi.aenerv7.ppembytv.data.model.createAndroidTvDeviceProfile
import magi.aenerv7.ppembytv.ui.components.TvButton

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
    var mediaSource by remember { mutableStateOf<MediaSource?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var startTicks by remember { mutableLongStateOf(item.userData?.playbackPositionTicks ?: 0L) }
    var speedIndex by remember { mutableIntStateOf(2) }
    var showControls by remember { mutableStateOf(true) }

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

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (mediaSource != null) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
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
        if (showControls) {
            // Bottom control bar.
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xCC000000)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TvButton(if (player.isPlaying) "暂停" else "播放") {
                    if (player.isPlaying) player.pause() else player.play()
                }
                Spacer(Modifier.width(8.dp))
                TvButton("+30s") { player.seekTo(player.currentPosition + 30_000) }
                Spacer(Modifier.width(8.dp))
                TvButton("-30s") { player.seekTo((player.currentPosition - 30_000).coerceAtLeast(0)) }
                Spacer(Modifier.width(8.dp))
                TvButton("倍速 ${SPEEDS[speedIndex]}x") { cycleSpeed() }
                Spacer(Modifier.width(8.dp))
                TvButton("音轨") { cycleTrack(C.TRACK_TYPE_AUDIO) }
                Spacer(Modifier.width(8.dp))
                TvButton("字幕") { cycleTrack(C.TRACK_TYPE_TEXT) }
                Spacer(Modifier.width(8.dp))
                TvButton("返回") { onBack() }
            }
        }
    }
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
