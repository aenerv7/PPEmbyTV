package magi.aenerv7.ppembytv.data.model

data class ServerIconLibrary(
    val sourceUrl: String,
    val name: String,
    val description: String,
    val icons: List<ServerIconEntry>,
)
