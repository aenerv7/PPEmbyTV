package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class LiveTvProgram(
    @SerializedName("Id") val id: String? = null,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("ImageTags") val imageTags: ImageTags? = null,
    @SerializedName("PrimaryImageAspectRatio") val primaryImageAspectRatio: Double? = null,
    @SerializedName("StartDate") val startDate: String? = null,
    @SerializedName("EndDate") val endDate: String? = null,
)
