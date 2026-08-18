package magi.aenerv7.ppembytv.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Emby REST API（与参考项目 EmbyApiService 对应，去掉弹幕相关端点）。
 */
interface EmbyApiService {

    @POST("Users/AuthenticateByName")
    suspend fun authenticate(
        @Body body: Map<String, String>,
    ): retrofit2.Response<AuthenticationResult>

    @GET("System/Info")
    suspend fun getSystemInfo(): retrofit2.Response<SystemInfo>

    @GET("Users/{userId}/Views")
    suspend fun getLibraries(
        @Path("userId") userId: String,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,ItemCounts,UserData",
    ): retrofit2.Response<LibraryQueryResult>

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String? = null,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
        @Query("Recursive") recursive: Boolean = false,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
        @Query("Filters") filters: String? = null,
        @Query("Limit") limit: Int? = null,
        @Query("StartIndex") startIndex: Int? = null,
    ): retrofit2.Response<QueryResult<BaseItemDto>>

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 24,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
        @Query("MediaTypes") mediaTypes: String = "Video",
    ): retrofit2.Response<QueryResult<BaseItemDto>>

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestMedia(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
        @Query("ParentId") parentId: String? = null,
    ): retrofit2.Response<List<BaseItemDto>>

    @GET("Shows/NextUp")
    suspend fun getNextUp(
        @Query("UserId") userId: String,
        @Query("SeriesId") seriesId: String? = null,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
    ): retrofit2.Response<QueryResult<BaseItemDto>>

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemDetails(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId,MediaSources,Genres,People,Studios,Overview,Taglines,OfficialRating,RunTimeTicks,Status,EndDate,SeasonCount,EpisodeCount",
    ): retrofit2.Response<BaseItemDto>

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
    ): retrofit2.Response<QueryResult<BaseItemDto>>

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("SeasonId") seasonId: String? = null,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
    ): retrofit2.Response<QueryResult<BaseItemDto>>

    @GET("Items/{itemId}/Similar")
    suspend fun getSimilarItems(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
    ): retrofit2.Response<QueryResult<BaseItemDto>>

    @GET("Search/Hints")
    suspend fun searchHints(
        @Query("SearchTerm") searchTerm: String,
        @Query("UserId") userId: String,
        @Query("Limit") limit: Int = 30,
        @Query("IncludePeople") includePeople: Boolean = false,
        @Query("IncludeMedia") includeMedia: Boolean = true,
        @Query("IncludeGenres") includeGenres: Boolean = false,
        @Query("IncludeStudios") includeStudios: Boolean = false,
    ): retrofit2.Response<SearchHintResult>

    @POST("Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("AutoOpenLiveStream") autoOpenLiveStream: Boolean = false,
        @Query("IsPlayback") isPlayback: Boolean = true,
        @Body request: PlaybackInfoRequest = PlaybackInfoRequest(),
    ): retrofit2.Response<PlaybackInfoResponse>

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(
        @Body info: PlaybackProgressInfo,
    ): retrofit2.Response<Unit>

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Body info: PlaybackProgressInfo,
    ): retrofit2.Response<Unit>

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(
        @Body info: PlaybackProgressInfo,
    ): retrofit2.Response<Unit>

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): retrofit2.Response<Unit>

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun unmarkFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): retrofit2.Response<Unit>

    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markPlayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): retrofit2.Response<Unit>

    @DELETE("Users/{userId}/PlayedItems/{itemId}")
    suspend fun unmarkPlayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): retrofit2.Response<Unit>
}
