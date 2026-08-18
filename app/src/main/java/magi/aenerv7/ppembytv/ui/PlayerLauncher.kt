package magi.aenerv7.ppembytv.ui

import android.content.Context
import android.content.Intent
import magi.aenerv7.ppembytv.PlayerActivity

/** 启动播放器 Activity */
object PlayerLauncher {
    const val EXTRA_ITEM_ID = "item_id"
    const val EXTRA_START_TICKS = "start_ticks"
    const val EXTRA_PLAY_SESSION_ID = "play_session_id"

    fun play(context: Context, itemId: String, startPositionTicks: Long = 0L) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_START_TICKS, startPositionTicks)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
