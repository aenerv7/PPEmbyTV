package magi.aenerv7.ppembytv.data.model

import com.google.gson.annotations.SerializedName

data class ProfileCondition(
    @SerializedName("Condition") val condition: String,
    @SerializedName("Property") val property: String,
    @SerializedName("Value") val value: String,
    @SerializedName("IsRequired") val isRequired: Boolean = false,
)
