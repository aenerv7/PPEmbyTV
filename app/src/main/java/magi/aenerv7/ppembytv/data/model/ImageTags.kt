package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class ImageTags(
    @SerializedName("Primary") val primary: String? = null,
    @SerializedName("Logo") val logo: String? = null,
    @SerializedName("Banner") val banner: String? = null,
    @SerializedName("Thumb") val thumb: String? = null,
)
