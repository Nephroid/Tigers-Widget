package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.UpcomingGame
import com.example.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class CountdownState(
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
    val isLive: Boolean = false,
    val text: String = "00d 00h 00m 00s"
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = GameRepository(db.gameDao())

    val upcomingGames: StateFlow<List<UpcomingGame>> = repository.upcomingGames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _useSimulatedMode = MutableStateFlow(false)
    val useSimulatedMode: StateFlow<Boolean> = _useSimulatedMode.asStateFlow()

    private val _countdown = MutableStateFlow(CountdownState())
    val countdown: StateFlow<CountdownState> = _countdown.asStateFlow()

    private val prefs = application.getSharedPreferences("TigersWidgetPrefs", android.content.Context.MODE_PRIVATE)

    private val _gamesBackDivision = MutableStateFlow(prefs.getString("games_back_division", "N/A") ?: "N/A")
    val gamesBackDivision: StateFlow<String> = _gamesBackDivision.asStateFlow()

    private val _gamesBackWildCard = MutableStateFlow(prefs.getString("games_back_wild_card", "N/A") ?: "N/A")
    val gamesBackWildCard: StateFlow<String> = _gamesBackWildCard.asStateFlow()

    private val _playoffStatus = MutableStateFlow(prefs.getString("playoff_status", "UNKNOWN") ?: "UNKNOWN")
    val playoffStatus: StateFlow<String> = _playoffStatus.asStateFlow()

    private val _groundedStandings = MutableStateFlow<String?>(null)
    val groundedStandings: StateFlow<String?> = _groundedStandings.asStateFlow()

    private val _groundedSources = MutableStateFlow<List<com.example.data.api.GeminiWebSource>>(emptyList())
    val groundedSources: StateFlow<List<com.example.data.api.GeminiWebSource>> = _groundedSources.asStateFlow()

    private val _isGroundedLoading = MutableStateFlow(false)
    val isGroundedLoading: StateFlow<Boolean> = _isGroundedLoading.asStateFlow()

    private val _upcomingGameWeather = MutableStateFlow<String?>(null)
    val upcomingGameWeather: StateFlow<String?> = _upcomingGameWeather.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    private var countdownJob: Job? = null

    init {
        // Fetch real-time schedule initially
        refreshData(forceSimulated = false)

        // Fetch Google Search Grounded Standings initially
        fetchGroundedStandings()

        // Reactively start countdown whenever games list changes
        viewModelScope.launch {
            upcomingGames.collect { games ->
                val firstGame = games.firstOrNull()
                startCountdownTicker(firstGame)
                if (firstGame != null) {
                    fetchUpcomingGameWeather(firstGame.stadiumName)
                }
            }
        }
    }

    fun fetchGroundedStandings() {
        viewModelScope.launch {
            _isGroundedLoading.value = true
            try {
                val (text, sources) = com.example.data.api.GeminiSearchClient.getTigersGroundedStandings()
                _groundedStandings.value = text
                _groundedSources.value = sources

                if (text != null) {
                    var gamesBackDiv = "N/A"
                    var gamesBackWC = "N/A"
                    var playoffStatus = "UNKNOWN"
                    var alCentralStandings = "CLE: 58-37 • MIN: 53-41 • KC: 52-43 • DET: 47-53 • CWS: 27-68"

                    text.lines().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("GAMES_BACK_DIVISION:", ignoreCase = true)) {
                            gamesBackDiv = trimmed.substringAfter(":").trim()
                        } else if (trimmed.startsWith("GAMES_BACK_WILD_CARD:", ignoreCase = true)) {
                            gamesBackWC = trimmed.substringAfter(":").trim()
                        } else if (trimmed.startsWith("GAMES_BACK:", ignoreCase = true)) {
                            gamesBackDiv = trimmed.substringAfter(":").trim()
                        } else if (trimmed.startsWith("PLAYOFF_STATUS:", ignoreCase = true)) {
                            playoffStatus = trimmed.substringAfter(":").trim()
                        } else if (trimmed.startsWith("AL_CENTRAL_STANDINGS:", ignoreCase = true)) {
                            alCentralStandings = trimmed.substringAfter(":").trim()
                        }
                    }

                    _gamesBackDivision.value = gamesBackDiv
                    _gamesBackWildCard.value = gamesBackWC
                    _playoffStatus.value = playoffStatus

                    prefs.edit().apply {
                        putString("games_back_division", gamesBackDiv)
                        putString("games_back_wild_card", gamesBackWC)
                        putString("playoff_status", playoffStatus)
                        putString("al_central_standings", alCentralStandings)
                        apply()
                    }

                    // Trigger widget update
                    com.example.widget.DetroitTigersWidgetProvider.triggerUpdate(getApplication())
                }
            } catch (e: Exception) {
                _groundedStandings.value = "Failed to fetch: ${e.message}"
            } finally {
                _isGroundedLoading.value = false
            }
        }
    }

    fun fetchUpcomingGameWeather(stadiumName: String) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            try {
                val weather = com.example.data.api.GeminiSearchClient.getVenueWeather(stadiumName)
                _upcomingGameWeather.value = weather
            } catch (e: Exception) {
                Log.w("GameViewModel", "Weather fetch fallback for $stadiumName: ${e.message}")
                _upcomingGameWeather.value = "74°F • Clear"
            } finally {
                _isWeatherLoading.value = false
            }
        }
    }

    fun refreshData(forceSimulated: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                _useSimulatedMode.value = forceSimulated
                repository.refreshGames(getApplication(), forceSimulated = forceSimulated)
                _gamesBackDivision.value = prefs.getString("games_back_division", "N/A") ?: "N/A"
                _gamesBackWildCard.value = prefs.getString("games_back_wild_card", "N/A") ?: "N/A"
                _playoffStatus.value = prefs.getString("playoff_status", "UNKNOWN") ?: "UNKNOWN"
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error refreshing data: ${e.message}")
                _errorMessage.value = "Failed to update live scores. Showing offline/simulated data."
                // Attempt fallback refresh
                repository.refreshGames(getApplication(), forceSimulated = true)
                _useSimulatedMode.value = true
                _gamesBackDivision.value = prefs.getString("games_back_division", "N/A") ?: "N/A"
                _gamesBackWildCard.value = prefs.getString("games_back_wild_card", "N/A") ?: "N/A"
                _playoffStatus.value = prefs.getString("playoff_status", "UNKNOWN") ?: "UNKNOWN"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun triggerManualRefresh() {
        val currentSimulatedMode = _useSimulatedMode.value
        refreshData(forceSimulated = currentSimulatedMode)
        fetchGroundedStandings()
    }

    fun toggleMode() {
        val nextMode = !_useSimulatedMode.value
        refreshData(forceSimulated = nextMode)
    }

    private fun startCountdownTicker(nextGame: UpcomingGame?) {
        countdownJob?.cancel()
        if (nextGame == null) {
            _countdown.value = CountdownState(text = "No Games Scheduled")
            return
        }

        countdownJob = viewModelScope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val diff = nextGame.gameTimeMillis - currentTime

                if (diff <= 0) {
                    val gameDuration = TimeUnit.HOURS.toMillis(4) // Assume average game duration of 4 hours
                    if (diff > -gameDuration) {
                        _countdown.value = CountdownState(
                            isLive = true,
                            text = "PLAY BALL! LIVE NOW"
                        )
                    } else {
                        _countdown.value = CountdownState(
                            text = "Game Finished"
                        )
                    }
                } else {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

                    val displayText = buildString {
                        if (days > 0) append("${days}d ")
                        append(String.format("%02dh %02dm %02ds", hours, minutes, seconds))
                    }

                    _countdown.value = CountdownState(
                        days = days,
                        hours = hours,
                        minutes = minutes,
                        seconds = seconds,
                        isLive = false,
                        text = displayText
                    )
                }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
