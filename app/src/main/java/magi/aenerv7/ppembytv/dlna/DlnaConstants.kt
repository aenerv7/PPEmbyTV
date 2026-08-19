package magi.aenerv7.ppembytv.dlna

/**
 * Actions / extras / commands used between the DLNA HTTP server and the app.
 *
 * Ported from `com.dh.myembyapp.dlna.DlnaConstants` (values kept EXACT).
 */
object DlnaConstants {
    const val ACTION_PLAY_REQUEST = "magi.aenerv7.ppembytv.dlna.PLAY_REQUEST"
    const val ACTION_CONTROL = "magi.aenerv7.ppembytv.dlna.CONTROL"

    const val EXTRA_URI = "uri"
    const val EXTRA_TITLE = "title"
    const val EXTRA_METADATA = "metadata"
    const val EXTRA_COMMAND = "command"
    const val EXTRA_POSITION = "position"

    const val CMD_PLAY = "play"
    const val CMD_PAUSE = "pause"
    const val CMD_STOP = "stop"
    const val CMD_SEEK = "seek"
}
