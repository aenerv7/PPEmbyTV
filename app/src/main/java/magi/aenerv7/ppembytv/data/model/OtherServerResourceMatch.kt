package magi.aenerv7.ppembytv.data.model

data class OtherServerResourceMatch(
    val item: MediaItem,
    val matchedEpisodeId: String? = null,
    val matchedEpisodeLabel: String? = null,
    val matchedByProvider: String? = null,
    val mediaSource: MediaSource? = null,
    val resolutionSortValue: Int = 0,
    val bitrateSortValue: Int = 0,
    val latestEpisodeNumber: Int? = null,
    val currentEpisodeNumber: Int? = null,
)
