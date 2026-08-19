package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class TraktScrobbleRequest(
    @SerializedName("progress")
    val progress: Double,
    @SerializedName("movie")
    val movie: TraktScrobbleMovie? = null,
    @SerializedName("show")
    val show: TraktScrobbleShow? = null,
    @SerializedName("episode")
    val episode: TraktScrobbleEpisode? = null,
)
