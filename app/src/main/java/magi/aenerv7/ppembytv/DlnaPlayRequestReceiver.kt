package magi.aenerv7.ppembytv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import magi.aenerv7.ppembytv.dlna.DlnaConstants

data class DlnaPlayRequestData(
    val uri: String,
    val title: String?,
    val metadata: String?,
)

class DlnaPlayRequestReceiver(
    private val activity: MainActivity,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != DlnaConstants.ACTION_PLAY_REQUEST) {
            return
        }
        val uri = intent.getStringExtra(DlnaConstants.EXTRA_URI) ?: return
        val title = intent.getStringExtra(DlnaConstants.EXTRA_TITLE)
        val metadata = intent.getStringExtra("metadata")
        Log.i("MainActivity", "收到 DLNA 播放请求: uri=$uri, title=$title")
        activity.onDlnaPlayRequest(DlnaPlayRequestData(uri, title, metadata))
    }
}
