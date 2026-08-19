package magi.aenerv7.ppembytv.data.api

import magi.aenerv7.ppembytv.data.model.TraktDeviceCodeRequest
import magi.aenerv7.ppembytv.data.model.TraktDeviceCodeResponse
import magi.aenerv7.ppembytv.data.model.TraktDeviceTokenRequest
import magi.aenerv7.ppembytv.data.model.TraktEpisode
import magi.aenerv7.ppembytv.data.model.TraktHistoryItem
import magi.aenerv7.ppembytv.data.model.TraktPlaybackProgressItem
import magi.aenerv7.ppembytv.data.model.TraktRefreshTokenRequest
import magi.aenerv7.ppembytv.data.model.TraktScrobbleRequest
import magi.aenerv7.ppembytv.data.model.TraktSearchResult
import magi.aenerv7.ppembytv.data.model.TraktShowWatchedProgress
import magi.aenerv7.ppembytv.data.model.TraktSyncHistoryRequest
import magi.aenerv7.ppembytv.data.model.TraktSyncHistoryResponse
import magi.aenerv7.ppembytv.data.model.TraktTokenResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TraktApiService {

    @POST("sync/history")
    suspend fun addToWatchedHistory(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String,
        @Body body: TraktSyncHistoryRequest,
    ): Response<TraktSyncHistoryResponse>

    @POST("oauth/device/code")
    suspend fun generateDeviceCode(
        @Body body: TraktDeviceCodeRequest,
    ): Response<TraktDeviceCodeResponse>

    @GET("sync/playback/episodes")
    suspend fun getEpisodePlaybackProgress(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String,
        @Query("extended") extended: String,
    ): Response<List<TraktPlaybackProgressItem>>

    @GET("sync/playback/movies")
    suspend fun getMoviePlaybackProgress(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String,
        @Query("extended") extended: String,
    ): Response<List<TraktPlaybackProgressItem>>

    @GET("sync/history/movies/{id}")
    suspend fun getMovieWatchedHistory(
        @Path("id") id: String,
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String,
        @Query("extended") extended: String,
    ): Response<List<TraktHistoryItem>>

    @GET("shows/{id}/seasons/{season}/episodes/{episode}")
    suspend fun getShowEpisode(
        @Path("id") id: String,
        @Path("season") season: Int,
        @Path("episode") episode: Int,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String,
        @Query("extended") extended: String,
    ): Response<TraktEpisode>

    @GET("shows/{id}/progress/watched")
    suspend fun getShowWatchedProgress(
        @Path("id") id: String,
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String,
        @Query("hidden") hidden: Boolean,
        @Query("specials") specials: Boolean,
        @Query("count_specials") countSpecials: Boolean,
    ): Response<TraktShowWatchedProgress>

    @GET("search/{idType}/{id}")
    suspend fun lookupById(
        @Path("idType") idType: String,
        @Path("id") id: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String,
        @Query("type") type: String,
        @Query("extended") extended: String,
    ): Response<List<TraktSearchResult>>

    @POST("oauth/device/token")
    suspend fun pollDeviceToken(
        @Body body: TraktDeviceTokenRequest,
    ): Response<TraktTokenResponse>

    @POST("oauth/token")
    suspend fun refreshToken(
        @Body body: TraktRefreshTokenRequest,
    ): Response<TraktTokenResponse>

    @POST("sync/history/remove")
    suspend fun removeFromWatchedHistory(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String,
        @Body body: TraktSyncHistoryRequest,
    ): Response<TraktSyncHistoryResponse>

    @POST("scrobble/pause")
    suspend fun scrobblePause(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String = "2D",
        @Body body: TraktScrobbleRequest,
    ): Response<ResponseBody>

    @POST("scrobble/start")
    suspend fun scrobbleStart(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String = "2D",
        @Body body: TraktScrobbleRequest,
    ): Response<ResponseBody>

    @POST("scrobble/stop")
    suspend fun scrobbleStop(
        @Header("Authorization") authorization: String,
        @Header("trakt-api-key") apiKey: String,
        @Header("trakt-api-version") apiVersion: String = "2D",
        @Body body: TraktScrobbleRequest,
    ): Response<ResponseBody>
}
