package magi.aenerv7.ppembytv.dlna

/**
 * A play request raised by the DLNA renderer when a control point sets a URI.
 *
 * Ported from `com.dh.myembyapp.dlna.DlnaPlayRequest`.
 */
data class DlnaPlayRequest(
    val uri: String,
    val title: String? = null,
    val metadata: String? = null,
)
