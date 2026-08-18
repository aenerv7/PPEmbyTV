package magi.aenerv7.ppembytv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import magi.aenerv7.ppembytv.ui.PlayerLauncher
import magi.aenerv7.ppembytv.ui.player.PlayerScreen
import magi.aenerv7.ppembytv.ui.theme.PPEmbyTVTheme

/** 全屏播放器 Activity */
class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra(PlayerLauncher.EXTRA_ITEM_ID)
        val startTicks = intent.getLongExtra(PlayerLauncher.EXTRA_START_TICKS, 0L)
        if (itemId.isNullOrBlank()) {
            finish()
            return
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            PPEmbyTVTheme {
                PlayerScreen(
                    itemId = itemId,
                    startTicks = startTicks,
                    onExit = { finish() },
                )
            }
        }
    }
}
