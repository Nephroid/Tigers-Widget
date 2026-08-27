package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upcoming_games")
data class UpcomingGame(
    @PrimaryKey val gameId: Int,
    val gameTimeMillis: Long,
    val opponentName: String,
    val stadiumName: String,
    val stadiumSize: Int,
    val pitcherName: String,
    val pitcherStatsWins: Int,
    val pitcherStatsLosses: Int,
    val pitcherStatsEra: Double,
    val pitcherStatsStrikeouts: Int,
    val pitcherStatsWhip: Double,
    val isHomeGame: Boolean,
    val isSimulated: Boolean,
    val seasonType: String = "Regular Season",
    val winProbability: Int = 50,
    val pitcherAge: Int = 27,
    val tigersStanding: String = "2nd in AL Central • 5th in AL",
    val headToHeadRecord: String = "Record: 3-2 vs Opponent",
    val pitcherId: Int? = null,
    val pitcherLastIp: Double = 6.0,
    val pitcherLastSo: Int = 5,
    val pitcherHand: String = "L"
)
