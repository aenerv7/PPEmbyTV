package magi.aenerv7.ppembytv.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TraktRetrofitClient {

    private const val BASE_URL = "https://api.trakt.tv/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                ExternalHttpClient.createApiClient(
                    routeMode = ExternalHttpClient.RouteMode.AUTO,
                    allowUnsafeSsl = false,
                    ignoreServerDirectOnly = true,
                )
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: TraktApiService by lazy {
        retrofit.create(TraktApiService::class.java)
    }
}
