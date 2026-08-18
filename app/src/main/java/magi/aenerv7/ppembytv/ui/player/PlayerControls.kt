package magi.aenerv7.ppembytv.ui.player

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import magi.aenerv7.ppembytv.ui.components.TvButton
import magi.aenerv7.ppembytv.util.Formatting

/**
 * 播放器控制栏：顶部标题 + 底部进度与按钮。
 * 支持：播放/暂停、±跳转、倍速、字幕延迟、音轨/字幕切换。
 */
@Composable
fun PlayerControls(
    session: PlaybackSession,
    player: ExoPlayer,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    subtitleOffsetMs: Long,
    onPlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onSubtitleOffset: (Long) -> Unit,
    onSelectTrack: (type: Int, groupIndex: Int, trackIndex: Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onHide: () -> Unit,
    onExit: () -> Unit,
    onShowError: (String) -> Unit,
) {
    var trackPanel by remember { mutableStateOf<TrackPanel?>(null) }

    Box(Modifier.fillMaxSize()) {
        // 上下渐变压暗，保证文字可读
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC000000), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xE6000000))
                    )
                )
        )

        // 顶部：标题 + 返回
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvButton("退出", onExit)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.itemName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${if (session.playMethod == "Transcode") "转码播放" else "直连播放"} · ${session.mediaSourceId.take(8)}",
                    color = Color(0xFFB8BDC6),
                    fontSize = 12.sp,
                )
            }
            TvButton("隐藏", onHide)
        }

        // 底部：进度 + 按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            // 进度条
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatting.formatDuration(positionMs / 1000),
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.width(70.dp),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(Color(0x55333333), RoundedCornerShape(4.dp))
                ) {
                    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .background(Color(0xFF4DA3FF), RoundedCornerShape(4.dp))
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = Formatting.formatDuration(durationMs / 1000),
                    color = Color(0xFFB8BDC6),
                    fontSize = 15.sp,
                    modifier = Modifier.width(70.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvButton(if (isPlaying) "暂停" else "播放", onPlayPause)
                TvButton("-10秒", { onSeekBy(-10_000) })
                TvButton("+30秒", { onSeekBy(30_000) })
                TvButton("倍速 ${speed}x", onCycleSpeed)
                TvButton(
                    if (subtitleOffsetMs == 0L) "字幕延迟" else "字幕 ${if (subtitleOffsetMs > 0) "+" else ""}${subtitleOffsetMs / 1000}s",
                    { onSubtitleOffset(-500) },
                )
                TvButton("字幕 +0.5s", { onSubtitleOffset(500) })
                TvButton("音轨", { trackPanel = TrackPanel.AUDIO })
                TvButton("字幕", { trackPanel = TrackPanel.SUBTITLE })
            }
        }

        // 轨道选择面板
        trackPanel?.let { panel ->
            val tracks = currentTracks(player)
            val filtered = when (panel) {
                TrackPanel.AUDIO -> tracks.filter { it.type == C.TRACK_TYPE_AUDIO }
                TrackPanel.SUBTITLE -> tracks.filter { it.type == C.TRACK_TYPE_TEXT }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .padding(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFF1C1F26), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                        .width(560.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (panel == TrackPanel.AUDIO) "选择音轨" else "选择字幕",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (panel == TrackPanel.SUBTITLE) {
                        TrackOption("关闭字幕", selected = filtered.none { it.isSelected }) {
                            onDisableSubtitles()
                            trackPanel = null
                        }
                    }
                    if (filtered.isEmpty()) {
                        Text("无可切换轨道", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    filtered.forEach { track ->
                        TrackOption(
                            label = (if (track.isSelected) "● " else "○ ") + track.label,
                            selected = track.isSelected,
                        ) {
                            runCatching {
                                onSelectTrack(track.type, track.groupIndex, track.trackIndex)
                            }.onFailure { onShowError(it.message ?: "切换失败") }
                            trackPanel = null
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TvButton("关闭", { trackPanel = null })
                }
            }
        }
    }
}

private enum class TrackPanel { AUDIO, SUBTITLE }

@Composable
private fun TrackOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TvButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}
