package magi.aenerv7.ppembytv.data.model

data class EpisodeLocator(
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val fallbackToFirstEpisode: Boolean = false,
)
