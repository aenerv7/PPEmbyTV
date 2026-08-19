package magi.aenerv7.ppembytv.data.model

data class OtherServerResourceSection(
    val server: ServerConfig,
    val matches: List<OtherServerResourceMatch> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
