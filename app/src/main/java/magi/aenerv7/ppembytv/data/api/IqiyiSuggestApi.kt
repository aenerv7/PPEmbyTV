package magi.aenerv7.ppembytv.data.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object IqiyiSuggestApi {

    private const val TAG = "IqiyiSuggestApi"
    private const val SUGGEST_URL = "https://suggest.video.iqiyi.com/?if=mobile&key="

    private val gson = Gson()

    private val client: OkHttpClient by lazy {
        ExternalHttpClient.createApiClient(
            routeMode = ExternalHttpClient.RouteMode.AUTO,
            allowUnsafeSsl = true,
            ignoreServerDirectOnly = true,
        )
    }

    suspend fun fetchSuggestions(keyword: String): List<IqiyiSuggestItem> =
        withContext(Dispatchers.IO) {
            val trimmed = keyword.trim()
            if (trimmed.isEmpty()) {
                return@withContext emptyList()
            }
            try {
                val request = Request.Builder()
                    .url(SUGGEST_URL + URLEncoder.encode(trimmed, "UTF-8"))
                    .header("Accept", "application/json")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        gson.fromJson(body, IqiyiSuggestResponse::class.java).data.take(10)
                    } else {
                        Log.w(TAG, "请求失败: " + response.code)
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取联想建议失败", e)
                emptyList()
            }
        }
}
