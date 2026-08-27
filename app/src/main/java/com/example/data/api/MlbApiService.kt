package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MlbApiService {
    @GET("api/v1/schedule")
    suspend fun getSchedule(
        @Query("sportId") sportId: Int = 1,
        @Query("teamId") teamId: Int = 116,
        @Query("opponentId") opponentId: Int? = null,
        @Query("hydrate") hydrate: String = "probablePitcher,venue,team",
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): MlbScheduleResponse

    @GET("api/v1/people/{personId}/stats")
    suspend fun getPlayerStats(
        @Path("personId") personId: Int,
        @Query("stats") stats: String = "statsSingleSeason",
        @Query("group") group: String = "pitching"
    ): MlbPlayerStatsResponse

    @GET("api/v1/standings")
    suspend fun getStandings(
        @Query("leagueId") leagueId: Int = 103, // AL is 103
        @Query("season") season: String? = null
    ): MlbStandingsResponse
}
