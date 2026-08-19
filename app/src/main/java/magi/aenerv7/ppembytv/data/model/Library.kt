package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class Library(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Guid") val guid: String? = null,
    @SerializedName("PresentationUniqueKey") val presentationUniqueKey: String? = null,
    @SerializedName("Type") val type: String? = null,
    @SerializedName("IsFolder") val isFolder: Boolean = false,
    @SerializedName("CollectionType") val collectionType: String? = null,
    @SerializedName("LocationType") val locationType: String? = null,
    @SerializedName("ImageTags") val imageTags: ImageTags? = null,
    @SerializedName("PrimaryImageTag") val primaryImageTag: String? = null,
)
