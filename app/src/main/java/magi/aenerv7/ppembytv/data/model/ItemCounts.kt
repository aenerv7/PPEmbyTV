package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class ItemCounts(
    @SerializedName("MovieCount") val movieCount: Int = 0,
    @SerializedName("SeriesCount") val seriesCount: Int = 0,
    @SerializedName("EpisodeCount") val episodeCount: Int = 0,
    @SerializedName("GameCount") val gameCount: Int = 0,
    @SerializedName("ArtistCount") val artistCount: Int = 0,
    @SerializedName("ProgramCount") val programCount: Int = 0,
    @SerializedName("GameSystemCount") val gameSystemCount: Int = 0,
    @SerializedName("TrailerCount") val trailerCount: Int = 0,
    @SerializedName("SongCount") val songCount: Int = 0,
    @SerializedName("AlbumCount") val albumCount: Int = 0,
    @SerializedName("MusicVideoCount") val musicVideoCount: Int = 0,
    @SerializedName("BoxSetCount") val boxSetCount: Int = 0,
    @SerializedName("BookCount") val bookCount: Int = 0,
    @SerializedName("ItemCount") val itemCount: Int = 0,
)
