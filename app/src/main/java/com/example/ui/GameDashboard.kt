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
import android.content.Context
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.UpcomingGame
import com.example.data.model.getPitcherImageUrl
import com.example.data.model.getTeamLogoUrl
import com.example.widget.DetroitTigersWidgetProvider
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

                    // 2. Results of the Last Game
                    val lastGame by viewModel.lastGameResult.collectAsStateWithLifecycle()
                    lastGame?.let { result ->
                        LastGameResultCard(result = result)
                    }

                    // 3. Live Standings & Playoff Race
                    StandingsAndRaceSection(viewModel = viewModel)

                    // 4. Upcoming Schedule Section (only 3 upcoming games)
                    if (games.size > 1) {
                        UpcomingScheduleSection(games = games.drop(1).take(3))
                    }

                    // 5. Widget Customization: Material You Dynamic Theming
                    WidgetThemingCard(context = context)
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
    val prefDiv by viewModel.gamesBackDivision.collectAsStateWithLifecycle()
    val prefWc by viewModel.gamesBackWildCard.collectAsStateWithLifecycle()
    val prefPlayoff by viewModel.playoffStatus.collectAsStateWithLifecycle()

    val gamesBackDivDisplay = if (prefDiv != "N/A") prefDiv else "8.0"
    val gamesBackWCDisplay = if (prefWc != "N/A") prefWc else "5.5"
    val playoffStatusDisplay = if (prefPlayoff != "UNKNOWN") prefPlayoff.uppercase() else "OUT"

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
    }
}

@Composable
fun LastGameResultCard(result: LastGameResult) {
    val isHome = result.isHomeGame
    val awayName = if (isHome) result.opponentName else "Detroit Tigers"
    val homeName = if (isHome) "Detroit Tigers" else result.opponentName
    val awayAbbr = getTeamAbbreviation(awayName)
    val homeAbbr = getTeamAbbreviation(homeName)
    val awayLogo = getTeamLogoUrl(awayName)
    val homeLogo = getTeamLogoUrl(homeName)
    val awayScore = if (isHome) result.opponentScore else result.tigersScore
    val homeScore = if (isHome) result.tigersScore else result.opponentScore

    val isTigersWin = result.isTigersWinner
    val winBadgeBg = if (isTigersWin) Color(0xFF1B5E20) else Color(0xFF7F1D1D)
    val winBadgeText = if (isTigersWin) "TIGERS WIN" else "TIGERS LOSS"
    val winTextColor = if (isTigersWin) Color(0xFF81C784) else Color(0xFFEF9A9A)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1D36)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1B365D), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Badge, Date & Status, Win/Loss Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1B365D), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "LAST GAME",
                            style = TextStyle(
                                color = Color(0xFFFA4616),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }

                    Text(
                        text = "${result.gameDate} • ${result.statusText}",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Win/Loss Pill
                Box(
                    modifier = Modifier
                        .background(winBadgeBg.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .border(1.dp, winBadgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = winBadgeText,
                        style = TextStyle(
                            color = winTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scoreboard: Away Team (Left) vs Home Team (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Away Team (Left)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = awayLogo,
                            contentDescription = awayName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column {
                        Text(
                            text = awayAbbr,
                            style = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "AWAY",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                // Center Score Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$awayScore",
                        style = TextStyle(
                            color = if (awayScore > homeScore) Color(0xFFFA4616) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp
                        )
                    )
                    Text(
                        text = "-",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.35f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "$homeScore",
                        style = TextStyle(
                            color = if (homeScore > awayScore) Color(0xFFFA4616) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp
                        )
                    )
                }

                // Home Team (Right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = homeAbbr,
                            style = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "HOME",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = homeLogo,
                            contentDescription = homeName,
                            modifier = Modifier.fillMaxSize()
                        )
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
            text = "Upcoming Schedule (Next 3 Games)",
            style = TextStyle(
                color = Color(0xFFFF823C),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(start = 2.dp)
        )

        games.take(3).forEach { game ->
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
fun WidgetThemingCard(context: Context) {
    val prefs = remember { context.getSharedPreferences("TigersWidgetPrefs", Context.MODE_PRIVATE) }
    var currentThemeIndex by remember {
        mutableStateOf(
            prefs.getInt("widget_theme_index", if (prefs.getBoolean("widget_material_you_enabled", false)) 3 else 0)
        )
    }

    val themes = listOf(
        Triple("Classic Navy", "Deep Navy & Tigers Orange", Color(0xFF0C2340) to Color(0xFFFA4616)),
        Triple("Motor City", "Charcoal Slate & Neon Orange", Color(0xFF121417) to Color(0xFFFF5722)),
        Triple("Heritage 1984", "Vintage Cream, Midnight Navy & Gold", Color(0xFF061325) to Color(0xFFF5A623)),
        Triple("MY Dynamic", "Wallpaper Primary dynamic palette", Color(0xFF1B365D) to Color(0xFF81C784)),
        Triple("MY Vibrant", "Wallpaper Tertiary pop & Accent2", Color(0xFF2B1B4D) to Color(0xFFBA68C8)),
        Triple("MY Tonal", "Wallpaper Tonal tinted container", Color(0xFF102830) to Color(0xFF4DD0E1))
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1D36)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1B365D), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🎨", fontSize = 16.sp)
                    Text(
                        text = "Widget Theme Palette",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B365D).copy(alpha = 0.6f),
                    modifier = Modifier.clickable {
                        val next = (currentThemeIndex + 1) % themes.size
                        currentThemeIndex = next
                        prefs.edit()
                            .putInt("widget_theme_index", next)
                            .putBoolean("widget_material_you_enabled", next >= 3)
                            .apply()
                        DetroitTigersWidgetProvider.triggerUpdate(context)
                    }
                ) {
                    Text(
                        text = "Cycle ↻",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = TextStyle(
                            color = Color(0xFFFF823C),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Text(
                text = "Tap 🎨 directly on the widget to cycle anytime, or select a palette below:",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            )

            // Section 1: Tigers Brand Themes
            Text(
                text = "DETROIT TIGERS PALETTES",
                style = TextStyle(
                    color = Color(0xFFFF823C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
            themes.take(3).forEachIndexed { idx, triple ->
                val (name, desc, colors) = triple
                val isSelected = currentThemeIndex == idx
                ThemeOptionRow(
                    name = name,
                    desc = desc,
                    primaryColor = colors.first,
                    accentColor = colors.second,
                    isSelected = isSelected,
                    onClick = {
                        currentThemeIndex = idx
                        prefs.edit()
                            .putInt("widget_theme_index", idx)
                            .putBoolean("widget_material_you_enabled", false)
                            .apply()
                        DetroitTigersWidgetProvider.triggerUpdate(context)
                    }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Section 2: Material You Themes
            Text(
                text = "MATERIAL YOU PALETTES (WALLPAPER DYNAMIC)",
                style = TextStyle(
                    color = Color(0xFF81C784),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
            themes.drop(3).forEachIndexed { dropIdx, triple ->
                val (name, desc, colors) = triple
                val idx = dropIdx + 3
                val isSelected = currentThemeIndex == idx
                ThemeOptionRow(
                    name = name,
                    desc = desc,
                    primaryColor = colors.first,
                    accentColor = colors.second,
                    isSelected = isSelected,
                    onClick = {
                        currentThemeIndex = idx
                        prefs.edit()
                            .putInt("widget_theme_index", idx)
                            .putBoolean("widget_material_you_enabled", true)
                            .apply()
                        DetroitTigersWidgetProvider.triggerUpdate(context)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    name: String,
    desc: String,
    primaryColor: Color,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1B365D).copy(alpha = 0.5f) else Color(0xFF081220),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFFA4616) else Color(0xFF1B365D).copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dual Color Preview Dots
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(primaryColor, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accentColor, CircleShape)
                    )
                }

                Column {
                    Text(
                        text = name,
                        style = TextStyle(
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        )
                    )
                    Text(
                        text = desc,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFA4616).copy(alpha = 0.2f),
                    modifier = Modifier.border(1.dp, Color(0xFFFA4616), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "ACTIVE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = TextStyle(
                            color = Color(0xFFFF823C),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

