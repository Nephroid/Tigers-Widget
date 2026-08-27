package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MlbScheduleResponse(
    val dates: List<MlbScheduleDate>?
)

@JsonClass(generateAdapter = true)
data class MlbScheduleDate(
    val date: String?,
    val games: List<MlbGame>?
)

@JsonClass(generateAdapter = true)
data class MlbGame(
    val gamePk: Int,
    val gameDate: String?, // e.g., "2026-06-30T23:40:00Z"
    val status: MlbGameStatus?,
    val teams: MlbTeams?,
    val venue: MlbVenue?,
    val gameType: String?
)

@JsonClass(generateAdapter = true)
data class MlbGameStatus(
    val abstractGameState: String?, // e.g., "Preview", "Live", "Final"
    val detailedState: String?
)

@JsonClass(generateAdapter = true)
data class MlbTeams(
    val away: MlbTeamScheduleInfo?,
    val home: MlbTeamScheduleInfo?
)

@JsonClass(generateAdapter = true)
data class MlbTeamScheduleInfo(
    val team: MlbTeamInfo?,
    val probablePitcher: MlbPitcherInfo?,
    val score: Int? = null,
    val isWinner: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class MlbTeamInfo(
    val id: Int,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class MlbPitcherInfo(
    val id: Int,
    val fullName: String?,
    val pitchHand: MlbPitchHand? = null
)

@JsonClass(generateAdapter = true)
data class MlbPitchHand(
    val code: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class MlbVenue(
    val id: Int,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class MlbPlayerStatsResponse(
    val stats: List<MlbStatsGroup>?
)

@JsonClass(generateAdapter = true)
data class MlbStatsGroup(
    val splits: List<MlbStatSplit>?
)

@JsonClass(generateAdapter = true)
data class MlbStatSplit(
    val stat: MlbPitchingStats?
)

@JsonClass(generateAdapter = true)
data class MlbPitchingStats(
    val wins: Int?,
    val losses: Int?,
    val era: String?,
    val strikeOuts: Int?,
    val whip: String?
) {
    val eraDouble: Double
        get() = era?.toDoubleOrNull() ?: 0.0

    val whipDouble: Double
        get() = whip?.toDoubleOrNull() ?: 0.0
}

@JsonClass(generateAdapter = true)
data class MlbStandingsResponse(
    val records: List<MlbStandingsRecord>?
)

@JsonClass(generateAdapter = true)
data class MlbStandingsRecord(
    val division: MlbDivisionInfo?,
    val teamRecords: List<MlbTeamRecord>?
)

@JsonClass(generateAdapter = true)
data class MlbDivisionInfo(
    val id: Int,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class MlbTeamRecord(
    val team: MlbTeamInfo?,
    val divisionRank: String? = null,
    val leagueRank: String? = null,
    val wildCardRank: String? = null,
    val sportRank: String? = null,
    val gamesPlayed: Int? = null,
    val gamesBack: String? = null,
    val wildCardGamesBack: String? = null,
    val divisionGamesBack: String? = null,
    val leagueGamesBack: String? = null,
    val wins: Int? = null,
    val losses: Int? = null,
    val winningPercentage: String? = null,
    val eliminationNumber: String? = null,
    val wildCardEliminationNumber: String? = null,
    val divisionLeader: Boolean? = null,
    val hasWildcard: Boolean? = null,
    val clinched: Boolean? = null
)
