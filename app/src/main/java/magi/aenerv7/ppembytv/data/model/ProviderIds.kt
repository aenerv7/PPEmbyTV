package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class ProviderIds(
    @SerializedName("Tmdb") val tmdb: String? = null,
    @SerializedName("Imdb") val imdb: String? = null,
    @SerializedName("Tvdb") val tvdb: String? = null,
) {
    fun buildAnyProviderIdQueries(): List<String> = buildList {
        tmdb?.trim()?.takeIf { it.isNotEmpty() }?.let { add("tmdb.$it") }
    }

    fun isEmpty(): Boolean = buildAnyProviderIdQueries().isEmpty()
}
