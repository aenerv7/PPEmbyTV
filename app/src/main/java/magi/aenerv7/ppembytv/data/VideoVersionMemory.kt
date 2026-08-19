package magi.aenerv7.ppembytv.data

/**
 * In-memory record of the last selected video version (all fields optional).
 * [timestamp] defaults to the creation time.
 */
data class VideoVersionMemory(
    val key: String? = null,
    val itemId: String? = null,
    val mediaSourceId: String? = null,
    val quality: String? = null,
    val resolutionSortValue: Int? = null,
    val stableMatchIndex: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
