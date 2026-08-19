package magi.aenerv7.ppembytv.data.api

import com.google.gson.JsonObject
import magi.aenerv7.ppembytv.data.model.AuthenticationResult
import magi.aenerv7.ppembytv.data.model.ItemCounts
import magi.aenerv7.ppembytv.data.model.LibraryQueryResult
import magi.aenerv7.ppembytv.data.model.MediaItem
import magi.aenerv7.ppembytv.data.model.PlaybackInfoRequest
import magi.aenerv7.ppembytv.data.model.QueryResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EmbyApiService {

    @POST("Users/AuthenticateByName")
    suspend fun authenticateUser(@Body body: Map<String, String>): Response<AuthenticationResult>

    @POST("LiveStreams/Close")
    suspend fun closeLiveStream(@Query("LiveStreamId") liveStreamId: String): Response<Unit>

    @DELETE("Users/{userId}/PlayedItems/{itemId}")
    suspend fun deletePlayedItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): Response<Unit>

    @GET("Users/{userId}/Items")
    suspend fun findItemsByProviderId(
        @Path("userId") userId: String,
        @Query("AnyProviderIdEquals") anyProviderIdEquals: String,
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("Recursive") recursive: Boolean,
        @Query("GroupProgramsBySeries") groupProgramsBySeries: Boolean,
        @Query("EnableImageTypes") enableImageTypes: String,
        @Query("Fields") fields: String,
        @Query("Limit") limit: Int,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items")
    suspend fun getContainingCollections(
        @Path("userId") userId: String,
        @Query("ListItemIds") listItemIds: String,
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("Recursive") recursive: Boolean,
        @Query("SortBy") sortBy: String,
        @Query("Fields") fields: String,
        @Query("EnableImageTypes") enableImageTypes: String,
        @Query("Limit") limit: Int,
    ): Response<QueryResult>

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("SeasonId") seasonId: String,
        @Query("Fields") fields: String,
        @Query("SortBy") sortBy: String,
        @Query("SortOrder") sortOrder: String,
    ): Response<QueryResult>

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodesForCrossServerMatch(
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("SeasonId") seasonId: String,
        @Query("IsMissing") isMissing: Boolean,
        @Query("EnableImageTypes") enableImageTypes: String,
        @Query("Fields") fields: String,
        @Query("SortBy") sortBy: String,
        @Query("SortOrder") sortOrder: String,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items")
    suspend fun getFavoriteItems(
        @Path("userId") userId: String,
        @Query("Filters") filters: String,
        @Query("Recursive") recursive: Boolean,
        @Query("SortBy") sortBy: String,
        @Query("SortOrder") sortOrder: String,
        @Query("Fields") fields: String,
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("CollapseBoxSetItems") collapseBoxSetItems: Boolean,
        @Query("EnableImageTypes") enableImageTypes: String,
        @Query("Limit") limit: Int?,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemChapters(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("Fields") fields: String = "Chapters",
    ): Response<MediaItem>

    @GET("Items/Counts")
    suspend fun getItemCounts(): Response<ItemCounts>

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemDetails(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("Fields") fields: String,
    ): Response<MediaItem>

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String,
        @Query("SortBy") sortBy: String,
        @Query("SortOrder") sortOrder: String,
        @Query("Fields") fields: String,
        @Query("Recursive") recursive: Boolean,
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("EnableImageTypes") enableImageTypes: String,
        @Query("Filters") filters: String,
        @Query("Limit") limit: Int?,
        @Query("StartIndex") startIndex: Int?,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items")
    suspend fun getItemsByIds(
        @Path("userId") userId: String,
        @Query("Ids") ids: String,
        @Query("Fields") fields: String,
        @Query("Recursive") recursive: Boolean,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items")
    suspend fun getItemsByPerson(
        @Path("userId") userId: String,
        @Query("PersonIds") personIds: String,
        @Query("Recursive") recursive: Boolean,
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("SortBy") sortBy: String,
        @Query("SortOrder") sortOrder: String,
        @Query("Fields") fields: String,
        @Query("Limit") limit: Int?,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items")
    suspend fun getLastPlayedEpisodeForSeries(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String,
        @Query("Limit") limit: Int,
        @Query("Recursive") recursive: Boolean,
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("SortBy") sortBy: String,
        @Query("SortOrder") sortOrder: String,
        @Query("Filters") filters: String,
        @Query("Fields") fields: String,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestMedia(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int,
        @Query("Fields") fields: String,
        @Query("ParentId") parentId: String,
    ): Response<List<MediaItem>>

    @GET("Users/{userId}/Views")
    suspend fun getLibraries(
        @Path("userId") userId: String,
        @Query("Fields") fields: String,
    ): Response<LibraryQueryResult>

    @GET("LiveTv/Channels")
    suspend fun getLiveTvChannels(
        @Query("UserId") userId: String,
        @Query("Fields") fields: String,
        @Query("EnableImages") enableImages: Boolean,
        @Query("ImageTypeLimit") imageTypeLimit: Int,
        @Query("EnableImageTypes") enableImageTypes: String,
        @Query("EnableUserData") enableUserData: Boolean,
        @Query("AddCurrentProgram") addCurrentProgram: Boolean,
        @Query("SortBy") sortBy: String,
        @Query("SortOrder") sortOrder: String,
        @Query("StartIndex") startIndex: Int,
        @Query("Limit") limit: Int,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items/{personId}")
    suspend fun getPersonDetails(
        @Path("userId") userId: String,
        @Path("personId") personId: String,
        @Query("Fields") fields: String = "Overview,PremiereDate",
    ): Response<MediaItem>

    @POST("Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("AutoOpenLiveStream") autoOpenLiveStream: Boolean = false,
        @Query("IsPlayback") isPlayback: Boolean = true,
        @Body body: PlaybackInfoRequest,
    ): Response<PlaybackInfoResponse>

    @GET("Users/{userId}/Items")
    suspend fun getRecentlyPlayedItems(
        @Path("userId") userId: String,
        @Query("Filters") filters: String = "IsPlayed",
        @Query("Recursive") recursive: Boolean = true,
        @Query("SortBy") sortBy: String = "DatePlayed",
        @Query("SortOrder") sortOrder: String = "Descending",
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,ProductionYear,PremiereDate,CommunityRating,ChildCount,UserData,SeriesName,ParentIndexNumber,IndexNumber,SeriesId",
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Episode,Video",
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
        @Query("Limit") limit: Int = 20,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items")
    suspend fun getResumeItems(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int,
        @Query("Filters") filters: String,
        @Query("Recursive") recursive: Boolean,
        @Query("SortBy") sortBy: String,
        @Query("SortOrder") sortOrder: String,
        @Query("Fields") fields: String,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItemsForSeries(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String,
        @Query("Limit") limit: Int,
        @Query("Recursive") recursive: Boolean,
        @Query("Fields") fields: String,
        @Query("MediaTypes") mediaTypes: String,
    ): Response<QueryResult>

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItemsV2(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int,
        @Query("Recursive") recursive: Boolean,
        @Query("Fields") fields: String,
        @Query("ImageTypeLimit") imageTypeLimit: Int,
        @Query("EnableImageTypes") enableImageTypes: String,
        @Query("MediaTypes") mediaTypes: String,
    ): Response<QueryResult>

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio",
    ): Response<QueryResult>

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasonsForCrossServerMatch(
        @Path("seriesId") seriesId: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = "BasicSyncInfo,CommunityRating,ProductionYear,EndDate,Container,IndexNumber",
    ): Response<QueryResult>

    @GET("Items/{itemId}/Similar")
    suspend fun getSimilarItems(
        @Path("itemId") itemId: String,
        @Query("UserId") userId: String,
        @Query("Limit") limit: Int,
        @Query("ImageTypeLimit") imageTypeLimit: Int,
        @Query("Fields") fields: String,
        @Query("EnableTotalRecordCount") enableTotalRecordCount: Boolean,
    ): Response<QueryResult>

    @GET("Users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): Response<JsonObject>

    @POST("Users/{userId}/Items/{itemId}/HideFromResume")
    suspend fun hideFromResume(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("Hide") hide: Boolean = true,
    ): Response<Unit>

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): Response<Unit>

    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markPlayedItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): Response<Unit>

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(@Body body: PlaybackProgressInfo): Response<Unit>

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(@Body body: PlaybackProgressInfo): Response<Unit>

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(@Body body: PlaybackProgressInfo): Response<Unit>

    @GET("Users/{userId}/Items")
    suspend fun searchItems(
        @Path("userId") userId: String,
        @Query("SearchTerm") searchTerm: String,
        @Query("Recursive") recursive: Boolean,
        @Query("Fields") fields: String,
        @Query("IncludeItemTypes") includeItemTypes: String,
        @Query("Limit") limit: Int?,
    ): Response<QueryResult>

    @DELETE("Users/{userId}/PlayingItems/{itemId}")
    suspend fun stopPlayingItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("PositionTicks") positionTicks: Long,
    ): Response<Unit>

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun unmarkFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): Response<Unit>

    @POST("Users/{userId}/PlayingItems/{itemId}/Progress")
    suspend fun updatePlaybackProgress(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("PositionTicks") positionTicks: Long,
    ): Response<Unit>

    @POST("Users/{userId}/Configuration")
    suspend fun updateUserConfiguration(
        @Path("userId") userId: String,
        @Body body: JsonObject,
    ): Response<Unit>

    @POST("Users/{userId}/Items/{itemId}/UserData")
    suspend fun updateUserData(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Body body: UserDataRequest,
    ): Response<Unit>
}
