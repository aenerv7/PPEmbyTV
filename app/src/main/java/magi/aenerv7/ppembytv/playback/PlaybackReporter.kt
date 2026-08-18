package magi.aenerv7.ppembytv.playback

import magi.aenerv7.ppembytv.api.EmbyApiService
import magi.aenerv7.ppembytv.api.PlaybackProgressInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 播放进度同步（对应参考项目的进度上报）：
 * 开始播放 -> POST /Sessions/Playing
 * 播放中  -> POST /Sessions/Playing/Progress（每 10 秒）
 * 停止    -> POST /Sessions/Playing/Stopped
 */
class PlaybackReporter(
    private val api: EmbyApiService,
) {

    suspend fun reportStart(info: PlaybackProgressInfo) {
        runCatching { withContext(Dispatchers.IO) { api.reportPlaybackStart(info) } }
    }

    suspend fun reportProgress(info: PlaybackProgressInfo) {
        runCatching { withContext(Dispatchers.IO) { api.reportPlaybackProgress(info) } }
    }

    suspend fun reportStopped(info: PlaybackProgressInfo) {
        runCatching { withContext(Dispatchers.IO) { api.reportPlaybackStopped(info) } }
    }
}
