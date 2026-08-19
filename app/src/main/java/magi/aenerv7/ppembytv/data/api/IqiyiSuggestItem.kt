package magi.aenerv7.ppembytv.data.api

import com.google.gson.annotations.SerializedName

data class IqiyiSuggestItem(
    val name: String = "",
    val cname: String = "",
    val link: String = "",
    @SerializedName("picture_url")
    val pictureUrl: String = "",
    val director: List<String> = emptyList(),
    @SerializedName("main_actor")
    val mainActor: List<String> = emptyList(),
    val year: Int = 0,
    @SerializedName("normalize_query")
    val normalizeQuery: String = "",
    @SerializedName("match_part")
    val matchPart: String = "",
)
