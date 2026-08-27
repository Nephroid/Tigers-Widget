package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.UpcomingGame
import com.example.data.model.getPitcherImageUrl
import com.example.data.model.getTeamLogoUrl
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GameDashboard(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val games by viewModel.upcomingGames.collectAsStateWithLifecycle()
    val countdown by viewModel.countdown.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val weatherText by viewModel.upcomingGameWeather.collectAsStateWithLifecycle()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsStateWithLifecycle()
    val isGroundedLoading by viewModel.isGroundedLoading.collectAsStateWithLifecycle()
    val isAnyRefreshing = isRefreshing || isGroundedLoading

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color(0xFF050A14),
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.triggerManualRefresh()
                    Toast.makeText(context, "Refreshing Tigers game data & standings...", Toast.LENGTH_SHORT).show()
                },
                containerColor = Color(0xFFFA4616),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("fab_refresh_button")
            ) {
                if (isAnyRefreshing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Live Data",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Refresh Live Data",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF050A14),
                            Color(0xFF03070E)
                        )
                    )
                )
        ) {
            // Sleek, compact top header
            SleekHeader(
                onRefresh = { viewModel.triggerManualRefresh() },
                isRefreshing = isAnyRefreshing
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (games.isEmpty()) {
                    EmptyStatePlaceholder(isRefreshing = isRefreshing)
                } else {
                    val primaryGame = games.first()

                    // 1. Primary Countdown Matchup Hero Card
                    SleekCountdownHeroCard(
                        game = primaryGame,
                        countdownText = countdown.text,
                        isLive = countdown.isLive,
                        weatherText = weatherText,
                        isWeatherLoading = isWeatherLoading
                    )

                    // 2. Live Standings & Playoff Race
                    StandingsAndRaceSection(viewModel = viewModel)

                    // 3. 40-Man Roster & Recent Transactions
                    TigersRosterAndTransactionsSection(viewModel = viewModel)

                    // 4. Upcoming Schedule Section
                    if (games.size > 1) {
                        UpcomingScheduleSection(games = games.drop(1))
                    }
                }

                // App Version Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Detroit Tigers Widget • v${com.example.BuildConfig.VERSION_NAME}",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                // Space for FloatingActionButton clearance
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun SleekHeader(
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C2340),
                        Color(0xFF0C2340).copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Team Identity & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "D",
                        color = Color(0xFF0C2340),
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    )
                }

                Column {
                    Text(
                        text = "Detroit Tigers",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = "NEXT MATCHUP & COUNTDOWN",
                        style = TextStyle(
                            color = Color(0xFFFA4616),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                }
            }

            // Right: Refresh action button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onRefresh() }
                    .testTag("refresh_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = Color(0xFFFA4616),
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Data",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SleekCountdownHeroCard(
    game: UpcomingGame,
    countdownText: String,
    isLive: Boolean,
    weatherText: String?,
    isWeatherLoading: Boolean
) {
    val gameDateText = remember(game.gameTimeMillis) {
        val sdf = SimpleDateFormat("EEEE, MMMM d • h:mm a", Locale.US)
        sdf.format(Date(game.gameTimeMillis))
    }

    val opponentAbbr = remember(game.opponentName) {
        getTeamAbbreviation(game.opponentName)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF122A4D),
                        Color(0xFF0A182D)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Matchup Header: DET vs Opponent + Central Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tigers Logo & Abbreviation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = getTeamLogoUrl("Detroit Tigers"),
                            contentDescription = "Tigers Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "DET",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    )
                }

                // Center: Status Tags & Digital Countdown Clock
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isLive) Color(0xFFFF5722).copy(alpha = 0.2f)
                                    else Color(0xFFFF5722).copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isLive) "LIVE" else "NEXT MATCH",
                                style = TextStyle(
                                    color = Color(0xFFFA4616),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        val isPlayoffs = game.seasonType.equals("playoffs", ignoreCase = true)
                        val seasonColor = if (isPlayoffs) Color(0xFFF1C40F) else Color(0xFF3498DB)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(seasonColor.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = game.seasonType.uppercase(),
                                style = TextStyle(
                                    color = seasonColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = countdownText,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 22.sp,
                            color = if (isLive) Color(0xFFFF5722) else Color.White,
                            letterSpacing = (-0.5).sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isLive) "IN PROGRESS" else "TIME REMAINING",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                }

                // Opponent Logo & Abbreviation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = getTeamLogoUrl(game.opponentName),
                            contentDescription = "${game.opponentName} Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = opponentAbbr,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Game Date & Venue Weather Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = gameDateText,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                if (isWeatherLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFF56CCF2)
                    )
                } else if (!weatherText.isNullOrEmpty()) {
                    val weatherEmoji = remember(weatherText) {
                        val lower = weatherText.lowercase()
                        when {
                            lower.contains("sun") || lower.contains("clear") || lower.contains("fair") -> "☀️"
                            lower.contains("rain") || lower.contains("shower") || lower.contains("drizzle") -> "🌧️"
                            lower.contains("cloud") || lower.contains("overcast") -> "☁️"
                            lower.contains("snow") || lower.contains("ice") -> "❄️"
                            else -> "⛅"
                        }
                    }
                    Text(
                        text = "$weatherEmoji $weatherText",
                        style = TextStyle(
                            color = Color(0xFF56CCF2),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }

            // Quick Metrics Row (Standing, H2H, Win Prob)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tigers Standing Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "STANDING",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Text(
                            text = game.tigersStanding.substringBefore("•").trim(),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Head-to-Head Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "SEASON H2H",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Text(
                            text = game.headToHeadRecord,
                            style = TextStyle(
                                color = Color(0xFFFF823C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Win Probability Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "WIN CHANCE",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Text(
                            text = "${game.winProbability}% Tigers",
                            style = TextStyle(
                                color = Color(0xFF2ECC71),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            // Probable Pitcher Sub-Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Avatar & Pitcher Identity
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val pitcherPhotoUrl = getPitcherImageUrl(game.pitcherId)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0C2340))
                                .border(1.5.dp, Color(0xFFFA4616), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pitcherPhotoUrl != null) {
                                AsyncImage(
                                    model = pitcherPhotoUrl,
                                    contentDescription = "Starter: ${game.pitcherName}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = "${game.pitcherAge}",
                                    style = TextStyle(
                                        color = Color(0xFFFF823C),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }

                        Column {
                            val handStr = if (game.pitcherHand.isNotEmpty()) " (${game.pitcherHand})" else ""
                            Text(
                                text = "Probable Starter$handStr • Age ${game.pitcherAge}",
                                style = TextStyle(
                                    color = Color(0xFFFA4616),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                )
                            )
                            Text(
                                text = game.pitcherName,
                                style = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            )
                            Text(
                                text = "Last Game: ${String.format(Locale.US, "%.1f", game.pitcherLastIp)} IP, ${game.pitcherLastSo} SO",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }

                    // Right: Season Stats (ERA, W-L, SO)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ERA",
                                style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 8.5.sp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f", game.pitcherStatsEra),
                                style = TextStyle(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "W-L",
                                style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 8.5.sp)
                            )
                            Text(
                                text = "${game.pitcherStatsWins}-${game.pitcherStatsLosses}",
                                style = TextStyle(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SO",
                                style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 8.5.sp)
                            )
                            Text(
                                text = game.pitcherStatsStrikeouts.toString(),
                                style = TextStyle(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Location Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Stadium",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = game.stadiumName,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    )
                }

                Text(
                    text = "Capacity: ${String.format(Locale.US, "%,d", game.stadiumSize)}",
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.5.sp
                    )
                )
            }
        }
    }
}

@Composable
fun StandingsAndRaceSection(viewModel: GameViewModel) {
    val groundedStandings by viewModel.groundedStandings.collectAsStateWithLifecycle()
    val groundedSources by viewModel.groundedSources.collectAsStateWithLifecycle()
    val isGroundedLoading by viewModel.isGroundedLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Parse structured values
    val lines = groundedStandings?.lines() ?: emptyList()
    var gamesBackDivValue: String? = null
    var gamesBackWCValue: String? = null
    var playoffStatusValue: String? = null
    val parsedProseLines = mutableListOf<String>()

    lines.forEach { line ->
        val trimmed = line.trim()
        when {
            trimmed.startsWith("GAMES_BACK_DIVISION:", ignoreCase = true) -> {
                gamesBackDivValue = trimmed.substringAfter(":").trim()
            }
            trimmed.startsWith("GAMES_BACK_WILD_CARD:", ignoreCase = true) -> {
                gamesBackWCValue = trimmed.substringAfter(":").trim()
            }
            trimmed.startsWith("GAMES_BACK:", ignoreCase = true) -> {
                gamesBackDivValue = trimmed.substringAfter(":").trim()
            }
            trimmed.startsWith("PLAYOFF_STATUS:", ignoreCase = true) -> {
                playoffStatusValue = trimmed.substringAfter(":").trim()
            }
            trimmed.startsWith("AL_CENTRAL_STANDINGS:", ignoreCase = true) -> {
                // Skip header line from prose
            }
            else -> {
                parsedProseLines.add(line)
            }
        }
    }

    val cleanInsightText = if (groundedStandings != null) {
        parsedProseLines.joinToString("\n").trim()
    } else {
        null
    }

    val prefDiv by viewModel.gamesBackDivision.collectAsStateWithLifecycle()
    val prefWc by viewModel.gamesBackWildCard.collectAsStateWithLifecycle()
    val prefPlayoff by viewModel.playoffStatus.collectAsStateWithLifecycle()

    val gamesBackDivDisplay = gamesBackDivValue ?: if (prefDiv != "N/A") prefDiv else if (isGroundedLoading) "---" else "8.0"
    val gamesBackWCDisplay = gamesBackWCValue ?: if (prefWc != "N/A") prefWc else if (isGroundedLoading) "---" else "5.5"
    val playoffStatusRaw = playoffStatusValue ?: if (prefPlayoff != "UNKNOWN") prefPlayoff else if (isGroundedLoading) "---" else "OUT"
    val playoffStatusDisplay = playoffStatusRaw.uppercase()

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "AL Central & Playoff Race",
            style = TextStyle(
                color = Color(0xFFFF823C),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(start = 2.dp)
        )

        // 3-Card Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Division Games Back
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2340).copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFFA4616).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AL CENTRAL GB",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.3.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (gamesBackDivDisplay == "0" || gamesBackDivDisplay == "0.0") "1st Place!" else "$gamesBackDivDisplay GB",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // Card 2: Wild Card Games Back
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2340).copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WILD CARD GB",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.3.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (gamesBackWCDisplay == "0" || gamesBackWCDisplay == "0.0") "Bubble (0.0)" else if (gamesBackWCDisplay.startsWith("+") || gamesBackWCDisplay.startsWith("-")) gamesBackWCDisplay else "$gamesBackWCDisplay GB",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // Card 3: Playoff Spot Status
            val isPlayoffIn = playoffStatusDisplay.contains("IN")
            val isPlayoffLoading = playoffStatusDisplay.contains("---")
            val statusColor = if (isPlayoffLoading) Color.White.copy(alpha = 0.5f) else if (isPlayoffIn) Color(0xFF4CAF50) else Color(0xFFF44336)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2340).copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1.2f)
                    .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYOFF SPOT?",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.3.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPlayoffLoading) "WAITING" else if (isPlayoffIn) "IN PLAYOFFS" else "OUT",
                        style = TextStyle(
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }

        // Live Google Search Grounding Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2340).copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFA4616).copy(alpha = 0.12f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Grounding",
                            tint = Color(0xFFFF823C),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Live Google Search Insights",
                            style = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }

                    if (isGroundedLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFFFA4616),
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.fetchGroundedStandings() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Grounded Standings",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isGroundedLoading && cleanInsightText == null) {
                    Text(
                        text = "Searching Google for up-to-date Detroit Tigers standings and wild card details...",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.5.sp,
                            fontStyle = FontStyle.Italic
                        )
                    )
                } else {
                    val insightText = cleanInsightText ?: "No live insights fetched yet. Tap Refresh to search Google with Gemini."
                    Text(
                        text = insightText,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 11.5.sp,
                            lineHeight = 16.5.sp
                        )
                    )

                    if (groundedSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "SOURCES FROM GOOGLE SEARCH:",
                            style = TextStyle(
                                color = Color(0xFFFF823C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            groundedSources.take(3).forEach { source ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            source.uri?.let { url ->
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(vertical = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Source",
                                        tint = Color(0xFFFA4616).copy(alpha = 0.7f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = source.title ?: "Web Source",
                                        style = TextStyle(
                                            color = Color(0xFFFA4616),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingScheduleSection(games: List<UpcomingGame>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Upcoming Schedule",
            style = TextStyle(
                color = Color(0xFFFF823C),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(start = 2.dp)
        )

        games.forEach { game ->
            SleekSecondaryGameCard(game = game)
        }
    }
}

@Composable
fun SleekSecondaryGameCard(game: UpcomingGame) {
    val gameDateText = remember(game.gameTimeMillis) {
        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US)
        sdf.format(Date(game.gameTimeMillis))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF122A4D).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = getTeamLogoUrl("Detroit Tigers"),
                            contentDescription = "Tigers",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = getTeamLogoUrl(game.opponentName),
                            contentDescription = game.opponentName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Text(
                        text = if (game.isHomeGame) "vs ${game.opponentName}" else "at ${game.opponentName}",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (game.isHomeGame) "HOME" else "AWAY",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        )
                    }

                    val secSeasonColor = if (game.seasonType.equals("playoffs", ignoreCase = true)) {
                        Color(0xFFF1C40F)
                    } else {
                        Color(0xFF3498DB)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(secSeasonColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = game.seasonType.uppercase(),
                            style = TextStyle(
                                color = secSeasonColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        )
                    }
                }
            }

            // Info Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = gameDateText,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                )

                Text(
                    text = "Starter: ${game.pitcherName}",
                    style = TextStyle(
                        color = Color(0xFFFF823C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Bottom metric chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SecondaryStatIndicator(
                    label = "H2H",
                    value = game.headToHeadRecord.substringAfter("Record:").trim().ifEmpty { game.headToHeadRecord },
                    modifier = Modifier.weight(1f)
                )
                SecondaryStatIndicator(
                    label = "Stadium",
                    value = game.stadiumName.take(16),
                    modifier = Modifier.weight(1.2f)
                )
                SecondaryStatIndicator(
                    label = "Chance",
                    value = "${game.winProbability}%",
                    valueColor = Color(0xFF2ECC71),
                    modifier = Modifier.weight(0.8f)
                )
            }
        }
    }
}

@Composable
fun SecondaryStatIndicator(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = value,
                style = TextStyle(
                    color = valueColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyStatePlaceholder(isRefreshing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(color = Color(0xFFFA4616))
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "No games",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "No games scheduled or cached. Tap Refresh.",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Helper function to resolve team abbreviation
fun getTeamAbbreviation(teamName: String): String {
    val upper = teamName.uppercase().trim()
    val words = upper.replace(".", " ").split(Regex("\\s+")).toSet()
    return when {
        upper.contains("DODGER") || upper.contains("LA DODGER") || upper.contains("LOS ANGELES DODGER") || "LAD" in words -> "LAD"
        upper.contains("ANGEL") || upper.contains("LA ANGEL") || upper.contains("LOS ANGELES ANGEL") || upper.contains("ANAHEIM") || "LAA" in words -> "LAA"
        upper.contains("TIGER") || "DET" in words -> "DET"
        upper.contains("GUARD") || upper.contains("INDIAN") || "CLE" in words -> "CLE"
        upper.contains("TWIN") || "MIN" in words -> "MIN"
        upper.contains("ROYAL") || "KC" in words || "KCR" in words || upper.contains("KANSAS") -> "KC"
        upper.contains("WHITE SOX") || "CHW" in words || "CWS" in words || upper.contains("CHICAGO WHITE") -> "CWS"
        upper.contains("RED SOX") || "BOS" in words || upper.contains("BOSTON") -> "BOS"
        upper.contains("BLUE JAY") || "TOR" in words || upper.contains("TORONTO") -> "TOR"
        upper.contains("YANKE") || "NYY" in words -> "NYY"
        upper.contains("MET") || "NYM" in words -> "NYM"
        upper.contains("ORIOLE") || "BAL" in words || upper.contains("BALTIMORE") -> "BAL"
        upper.contains("RAY") || "TB" in words || "TBR" in words || upper.contains("TAMPA") -> "TB"
        upper.contains("ASTRO") || "HOU" in words || upper.contains("HOUSTON") -> "HOU"
        upper.contains("ATHLET") || "OAK" in words || "ATH" in words || upper.contains("OAKLAND") || "A'S" in words || "AS" in words -> "OAK"
        upper.contains("MARINER") || "SEA" in words || upper.contains("SEATTLE") -> "SEA"
        upper.contains("RANGER") || "TEX" in words || upper.contains("TEXAS") -> "TEX"
        upper.contains("BRAVE") || "ATL" in words || upper.contains("ATLANTA") -> "ATL"
        upper.contains("MARLIN") || "MIA" in words || upper.contains("MIAMI") -> "MIA"
        upper.contains("PHILLI") || "PHI" in words || upper.contains("PHILADELPHIA") -> "PHI"
        upper.contains("NATIONAL") || "WSH" in words || "WAS" in words || upper.contains("WASHINGTON") -> "WSH"
        upper.contains("CUB") || "CHC" in words || upper.contains("CHICAGO CUBS") -> "CHC"
        upper.contains("RED") || "CIN" in words || upper.contains("CINCINNATI") -> "CIN"
        upper.contains("BREWER") || "MIL" in words || upper.contains("MILWAUKEE") -> "MIL"
        upper.contains("PIRATE") || "PIT" in words || upper.contains("PITTSBURGH") -> "PIT"
        upper.contains("CARDINAL") || "STL" in words || upper.contains("ST. LOUIS") || upper.contains("ST LOUIS") -> "STL"
        upper.contains("DIAMONDBACK") || upper.contains("D-BACK") || "ARI" in words || upper.contains("ARIZONA") -> "ARI"
        upper.contains("ROCKIE") || "COL" in words || upper.contains("COLORADO") -> "COL"
        upper.contains("PADRE") || "SD" in words || "SDP" in words || upper.contains("SAN DIEGO") -> "SD"
        upper.contains("GIANT") || "SF" in words || "SFG" in words || upper.contains("SAN FRANCISCO") -> "SF"
        else -> teamName.take(3).uppercase()
    }
}

@Composable
fun TigersRosterAndTransactionsSection(viewModel: GameViewModel) {
    val roster by viewModel.roster.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val isRosterLoading by viewModel.isRosterLoading.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Roster, 1 = Transactions

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tigers Squad & Roster Moves",
                style = TextStyle(
                    color = Color(0xFFFF823C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 2.dp)
            )

            // Pill toggle for Roster vs Transactions
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0C2340).copy(alpha = 0.8f))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selectedTab == 0) Color(0xFFFA4616) else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "40-Man (${roster.size})",
                        style = TextStyle(
                            color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selectedTab == 1) Color(0xFFFA4616) else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Transactions",
                        style = TextStyle(
                            color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2340).copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFA4616).copy(alpha = 0.15f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    // 40-Man Roster View
                    val pitchers = roster.filter { it.position?.type == "Pitcher" || it.position?.abbreviation?.contains("P") == true }
                    val positionPlayers = roster.filter { it !in pitchers }

                    if (roster.isEmpty() && isRosterLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFA4616), modifier = Modifier.size(24.dp))
                        }
                    } else {
                        // Pitchers Group
                        Text(
                            text = "PITCHING STAFF (${pitchers.size})",
                            style = TextStyle(color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                        )
                        FlowRowLayout(
                            items = pitchers,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Position Players Group
                        Text(
                            text = "POSITION PLAYERS (${positionPlayers.size})",
                            style = TextStyle(color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                        )
                        FlowRowLayout(
                            items = positionPlayers,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Transactions View
                    if (transactions.isEmpty() && isRosterLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFA4616), modifier = Modifier.size(24.dp))
                        }
                    } else if (transactions.isEmpty()) {
                        Text(
                            text = "No recent transactions found.",
                            style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        )
                    } else {
                        transactions.take(8).forEach { tx ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1B365D).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tx.date ?: "Recent",
                                        style = TextStyle(color = Color(0xFFFA4616), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    )
                                    tx.typeDesc?.let {
                                        Text(
                                            text = it,
                                            style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 9.5.sp)
                                        )
                                    }
                                }
                                Text(
                                    text = tx.description ?: "Transaction update",
                                    style = TextStyle(color = Color.White, fontSize = 11.sp, lineHeight = 14.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowRowLayout(
    items: List<com.example.data.api.MlbRosterEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { player ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1B365D).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "#${player.jerseyNumber ?: "--"}",
                                style = TextStyle(
                                    color = Color(0xFFFA4616),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = player.person?.fullName ?: "Player",
                                style = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = player.position?.abbreviation ?: "",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
