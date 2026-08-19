package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class PersonInfo(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Role") val role: String? = null,
    @SerializedName("Type") val type: String? = null,
    @SerializedName("PrimaryImageTag") val primaryImageTag: String? = null,
)
