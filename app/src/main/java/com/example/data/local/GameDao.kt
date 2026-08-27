package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UpcomingGame
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM upcoming_games ORDER BY gameTimeMillis ASC LIMIT 2")
    fun getUpcomingGames(): Flow<List<UpcomingGame>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<UpcomingGame>)

    @Query("DELETE FROM upcoming_games")
    suspend fun clearGames()
}
