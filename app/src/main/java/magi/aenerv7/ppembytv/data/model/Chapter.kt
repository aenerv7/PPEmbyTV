package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class Chapter(
    @SerializedName("StartPositionTicks") val startPositionTicks: Long,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("MarkerType") val markerType: String? = null,
    @SerializedName("ChapterIndex") val chapterIndex: Int? = null,
)
