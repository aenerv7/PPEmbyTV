package magi.aenerv7.ppembytv.data

/**
 * In-memory record of the last selected audio track (all fields optional).
 * [timestamp] defaults to the creation time.
 */
data class AudioTrackMemory(
    val key: String? = null,
    val itemId: String? = null,
    val apiStreamIndex: Int? = null,
    val languagePreference: String? = null,
    val codecType: String? = null,
    val channelCount: Int? = null,
    val titleKey: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
