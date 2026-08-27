package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.UpcomingGame
import com.example.data.model.getTeamLogoUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DetroitTigersWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("TigersWidget", "onEnabled triggered - starting auto refresh alarm")
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d("TigersWidget", "onDisabled triggered - cancelling auto refresh alarm")
        cancelUpdate(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("TigersWidget", "onUpdate triggered for widgets")
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
        scheduleNextUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d("TigersWidget", "onAppWidgetOptionsChanged triggered")
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                updateAllWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("TigersWidget", "onReceive action: ${intent.action}")
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == ACTION_AUTO_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DetroitTigersWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val pendingResult = goAsync()
                widgetScope.launch {
                    try {
                        updateAllWidgets(context, appWidgetManager, appWidgetIds)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            scheduleNextUpdate(context)
        }
    }

    private suspend fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val db = AppDatabase.getDatabase(context)
            
            // Check if games need background refreshing (throttle to at most every 15 mins)
            val prefs = context.getSharedPreferences("TigersWidgetPrefs", Context.MODE_PRIVATE)
            val lastRefresh = prefs.getLong("last_widget_refresh_ts", 0L)
            val now = System.currentTimeMillis()
            if (now - lastRefresh > 15 * 60 * 1000L) {
                try {
                    val repository = com.example.data.repository.GameRepository(db.gameDao())
                    repository.refreshGames(context, forceSimulated = false)
                    prefs.edit().putLong("last_widget_refresh_ts", now).apply()
                } catch (e: Exception) {
                    Log.e("TigersWidget", "Error refreshing games repository in widget: ${e.message}")
                }
            }

            val games = db.gameDao().getUpcomingGames().firstOrNull() ?: emptyList()
            val nextGame = games.firstOrNull()

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
                
                if (nextGame != null) {
                    bindGameData(context, views, nextGame)
                } else {
                    bindEmptyState(context, views)
                }

                // Query current widget options for responsive sizing
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 180
                val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 110
                applyResponsiveLayout(context, views, minWidth, minHeight)

                // Click pending intent to open main app
                val clickIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                
                val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    clickIntent,
                    pendingIntentFlags
                )

                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        } catch (e: Exception) {
            Log.e("TigersWidget", "Error updating widget remote views: ${e.message}", e)
        }
    }

    private suspend fun bindGameData(context: Context, views: RemoteViews, game: UpcomingGame) {
        val isHome = game.isHomeGame
        val prefix = if (isHome) "vs." else "at"
        views.setTextViewText(R.id.widget_opponent, "$prefix ${game.opponentName}")
        
        // Calculate countdown text
        val diff = game.gameTimeMillis - System.currentTimeMillis()
        val countdownText = if (diff <= 0) {
            val gameDuration = TimeUnit.HOURS.toMillis(4)
            if (diff > -gameDuration) "PLAY BALL! LIVE" else "Game Finished"
        } else {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            
            buildString {
                if (days > 0) append("${days}d ")
                append(String.format("%02dh %02dm", hours, minutes))
            }
        }

        views.setTextViewText(R.id.widget_countdown, countdownText)
        
        // Probable starting pitcher and their last game info directly underneath
        val cleanName = cleanPitcherName(game.pitcherName)
        val handSuffix = if (game.pitcherHand.isNotEmpty()) " (${game.pitcherHand})" else ""
        val rawPitcherHtml = String.format(
            "*Starting Pitcher: <b><font color='#FA4616'>%s%s</font></b><br/><font color='#98A6B8'>(Last Game: %.1f IP, %d SO)</font>",
            cleanName,
            handSuffix,
            game.pitcherLastIp,
            game.pitcherLastSo
        )
        val formattedPitcherText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(rawPitcherHtml, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(rawPitcherHtml)
        }
        views.setTextViewText(R.id.widget_stadium_pitcher_info, formattedPitcherText)

        // Standing & H2H record info
        val prefs = context.getSharedPreferences("TigersWidgetPrefs", Context.MODE_PRIVATE)
        val standingSummary = prefs.getString("tigers_standing_summary", null)
        val standingText = standingSummary ?: game.tigersStanding
        views.setTextViewText(R.id.widget_standing_h2h, "$standingText • ${game.headToHeadRecord}")
        
        // Load logos from URL asynchronously and set them to RemoteViews
        val tigersLogoUrl = "https://a.espncdn.com/i/teamlogos/mlb/500/det.png"
        val opponentLogoUrl = getTeamLogoUrl(game.opponentName)

        val tigersBitmap = loadLogoBitmap(context, tigersLogoUrl)
        if (tigersBitmap != null) {
            views.setImageViewBitmap(R.id.widget_tigers_logo, tigersBitmap)
        } else {
            views.setImageViewResource(R.id.widget_tigers_logo, R.drawable.ic_tigers_logo)
        }

        val opponentBitmap = loadLogoBitmap(context, opponentLogoUrl)
        if (opponentBitmap != null) {
            views.setImageViewBitmap(R.id.widget_opponent_logo, opponentBitmap)
        } else {
            views.setImageViewResource(R.id.widget_opponent_logo, R.drawable.ic_baseball_placeholder)
        }

        // Bind live Games Back & Playoff Spot values
        bindStandingsStats(context, views)
    }

    private fun cleanPitcherName(rawName: String): String {
        if (rawName.contains("Skubal", ignoreCase = true)) return "Tarik Skubal"
        if (rawName.contains("Olson", ignoreCase = true)) return "Reese Olson"
        if (rawName.contains("Mize", ignoreCase = true)) return "Casey Mize"
        if (rawName.contains("Manning", ignoreCase = true)) return "Matt Manning"
        if (rawName.contains("Montero", ignoreCase = true)) return "Keider Montero"
        if (rawName.contains("Flaherty", ignoreCase = true)) return "Jack Flaherty"
        if (rawName.contains("TBD", ignoreCase = true) && !rawName.contains("Likely", ignoreCase = true)) return "TBD"
        return rawName.replace(Regex("\\s*Likely\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^TBD\\s*\\(?"), "")
            .replace(Regex("\\)?$"), "")
            .trim().ifEmpty { "TBD" }
    }

    private fun bindEmptyState(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_opponent, "No Scheduled Games")
        views.setTextViewText(R.id.widget_countdown, "00d 00h 00m")
        views.setTextViewText(R.id.widget_stadium_pitcher_info, "*Starting Pitcher: TBD")
        views.setTextViewText(R.id.widget_standing_h2h, "Standings unavailable")
        views.setImageViewResource(R.id.widget_tigers_logo, R.drawable.ic_tigers_logo)
        views.setImageViewResource(R.id.widget_opponent_logo, R.drawable.ic_baseball_placeholder)

        // Bind live Games Back & Playoff Spot values
        bindStandingsStats(context, views)
    }

    private data class TeamStandingsInfo(
        val name: String,
        val wins: Int,
        val losses: Int,
        val gbString: String?,
        val rawText: String
    ) {
        val winPct: Double
            get() = if (wins + losses > 0) wins.toDouble() / (wins + losses) else 0.0
    }

    private fun parseTeamInfo(rawItem: String): TeamStandingsInfo {
        val clean = rawItem.replace(Regex("^\\d+[\\.\\s]\\s*"), "").trim()
        val match = Regex("([A-Za-z]+):?\\s*(\\d+)[\\-\\s]+(\\d+)(?:\\s*\\(([^)]+)\\))?").find(clean)
        return if (match != null) {
            val name = match.groupValues[1].uppercase()
            val w = match.groupValues[2].toIntOrNull() ?: 0
            val l = match.groupValues[3].toIntOrNull() ?: 0
            val gb = match.groupValues.getOrNull(4)?.trim()?.ifEmpty { null }
            val raw = if (gb != null) "$name: $w-$l ($gb)" else "$name: $w-$l"
            TeamStandingsInfo(name, w, l, gb, raw)
        } else {
            TeamStandingsInfo(clean, 0, 0, null, clean)
        }
    }

    private fun bindStandingsStats(context: Context, views: RemoteViews) {
        val prefs = context.getSharedPreferences("TigersWidgetPrefs", Context.MODE_PRIVATE)
        val alCentralStandings = prefs.getString(
            "al_central_standings",
            "CWS: 70-63 (-) • CLE: 68-66 (2.5) • MIN: 64-70 (6.5) • DET: 62-71 (8.0) • KC: 59-75 (11.5)"
        ) ?: "CWS: 70-63 (-) • CLE: 68-66 (2.5) • MIN: 64-70 (6.5) • DET: 62-71 (8.0) • KC: 59-75 (11.5)"

        val items = alCentralStandings.split("•", ",").map { it.trim() }.filter { it.isNotEmpty() }
        val defaultTeams = listOf(
            "CWS: 70-63 (-)",
            "CLE: 68-66 (2.5)",
            "MIN: 64-70 (6.5)",
            "DET: 62-71 (8.0)",
            "KC: 59-75 (11.5)"
        )

        val rawList = if (items.isNotEmpty()) items else defaultTeams
        val parsedList = rawList.map { parseTeamInfo(it) }

        // Sort by win percentage descending to guarantee accurate 1-5 rankings
        val sortedList = parsedList.sortedWith(
            compareByDescending<TeamStandingsInfo> { it.winPct }
                .thenByDescending { it.wins }
        )

        val leader = sortedList.firstOrNull()
        val leaderW = leader?.wins ?: 70
        val leaderL = leader?.losses ?: 63

        val formattedTeams = sortedList.mapIndexed { index, team ->
            val cleanName = team.name.uppercase()
            val w = team.wins
            val l = team.losses
            val gbStr = if (team.gbString != null && team.gbString.isNotEmpty()) {
                team.gbString
            } else if (index == 0) {
                "-"
            } else {
                val gb = ((leaderW - w) + (l - leaderL)) / 2.0
                if (gb <= 0.0) "-" else if (gb % 1.0 == 0.0) "${gb.toInt()}.0" else String.format(Locale.US, "%.1f", gb)
            }
            val raw = "$cleanName: $w-$l ($gbStr)"
            formatTeamText(index + 1, raw)
        }

        // Column 1 (ranks 1, 2)
        views.setTextViewText(R.id.widget_team_1, formattedTeams.getOrNull(0) ?: formatTeamText(1, defaultTeams[0]))
        views.setTextViewText(R.id.widget_team_2, formattedTeams.getOrNull(1) ?: formatTeamText(2, defaultTeams[1]))

        // Column 2 (ranks 3, 4)
        views.setTextViewText(R.id.widget_team_3, formattedTeams.getOrNull(2) ?: formatTeamText(3, defaultTeams[2]))
        views.setTextViewText(R.id.widget_team_4, formattedTeams.getOrNull(3) ?: formatTeamText(4, defaultTeams[3]))

        // Column 3 (rank 5 and 6th spot: Playoff Spot / Wild Card GB & Games Left)
        views.setTextViewText(R.id.widget_team_5, formattedTeams.getOrNull(4) ?: formatTeamText(5, defaultTeams[4]))

        val wcGbRaw = prefs.getString("games_back_wild_card", "5.5") ?: "5.5"
        val playoffStatusRaw = prefs.getString("playoff_status", "OUT") ?: "OUT"
        val playoffSpotInfo = prefs.getString("playoff_spot_info", null)

        val detTeam = sortedList.firstOrNull { it.name.contains("DET", ignoreCase = true) }
        val glCalculated = if (detTeam != null && detTeam.wins + detTeam.losses > 0) {
            maxOf(0, 162 - (detTeam.wins + detTeam.losses))
        } else {
            prefs.getInt("tigers_games_left", 29)
        }

        val isPlayoffIn = playoffStatusRaw.contains("IN", ignoreCase = true) ||
                wcGbRaw.contains("IN", ignoreCase = true) ||
                (sortedList.indexOfFirst { it.name.contains("DET", ignoreCase = true) } == 0)

        val formattedWcText = when {
            isPlayoffIn -> {
                if (playoffSpotInfo != null && playoffSpotInfo.contains("WC", ignoreCase = true)) {
                    val spot = if (playoffSpotInfo.contains("WC #1")) "WC1" else if (playoffSpotInfo.contains("WC #2")) "WC2" else "WC3"
                    "PLAYOFF: IN ($spot) • GL: $glCalculated"
                } else if (sortedList.indexOfFirst { it.name.contains("DET", ignoreCase = true) } == 0) {
                    "PLAYOFF: IN (ALC #1) • GL: $glCalculated"
                } else {
                    "PLAYOFF: IN • GL: $glCalculated"
                }
            }
            else -> {
                val cleanGb = wcGbRaw.replace(Regex("^WCGB:?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^WC:?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace("GB", "").trim()
                "WCGB: $cleanGb • GL: $glCalculated"
            }
        }

        val colorHex = if (isPlayoffIn) "#00E676" else "#FF5252"
        val styledWc = "<b><i><font color='$colorHex'>$formattedWcText</font></i></b>"
        val htmlWc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(styledWc, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(styledWc)
        }
        views.setTextViewText(R.id.widget_team_6, htmlWc)
    }

    private fun formatTeamText(rank: Int, rawText: String): CharSequence {
        val cleanText = rawText.replace(Regex("^\\d+[\\.\\s]\\s*"), "")
        val textWithRank = "$rank. $cleanText"
        val isTigers = cleanText.contains("DET", ignoreCase = true)
        val styled = if (isTigers) {
            "<b><font color='#FFC107'>$textWithRank</font></b>"
        } else {
            "<font color='#D0D8E4'>$textWithRank</font>"
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(styled, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(styled)
        }
    }

    private fun applyResponsiveLayout(context: Context, views: RemoteViews, minWidth: Int, minHeight: Int) {
        Log.d("TigersWidget", "Applying responsive layout: width=$minWidth, height=$minHeight")
        val density = context.resources.displayMetrics.density

        // Dynamic responsive font scaling to fill available widget space and eliminate dead space
        val heightRatio = (minHeight / 110f).coerceIn(0.8f, 2.5f)
        val widthRatio = (minWidth / 180f).coerceIn(0.8f, 2.2f)
        val scale = (heightRatio * 0.65f + widthRatio * 0.35f).coerceIn(0.85f, 2.4f)

        val titleSp = (11.5f * scale).coerceIn(10f, 18f)
        val tagSp = (9f * scale).coerceIn(8f, 13f)
        val oppSp = (13f * scale).coerceIn(11.5f, 22f)
        val countdownSp = (20f * scale).coerceIn(17f, 38f)
        val pitcherSp = (11f * scale).coerceIn(9.5f, 16f)
        val standingH2hSp = (9.5f * scale).coerceIn(8.5f, 14.5f)
        val tableSp = (9.5f * scale).coerceIn(8.5f, 14f)
        val tableWcSp = (9f * scale).coerceIn(8f, 13.5f)

        views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, titleSp)
        views.setTextViewTextSize(R.id.widget_tag, android.util.TypedValue.COMPLEX_UNIT_SP, tagSp)
        views.setTextViewTextSize(R.id.widget_opponent, android.util.TypedValue.COMPLEX_UNIT_SP, oppSp)
        views.setTextViewTextSize(R.id.widget_countdown, android.util.TypedValue.COMPLEX_UNIT_SP, countdownSp)
        views.setTextViewTextSize(R.id.widget_stadium_pitcher_info, android.util.TypedValue.COMPLEX_UNIT_SP, pitcherSp)
        views.setTextViewTextSize(R.id.widget_standing_h2h, android.util.TypedValue.COMPLEX_UNIT_SP, standingH2hSp)

        views.setTextViewTextSize(R.id.widget_team_1, android.util.TypedValue.COMPLEX_UNIT_SP, tableSp)
        views.setTextViewTextSize(R.id.widget_team_2, android.util.TypedValue.COMPLEX_UNIT_SP, tableSp)
        views.setTextViewTextSize(R.id.widget_team_3, android.util.TypedValue.COMPLEX_UNIT_SP, tableSp)
        views.setTextViewTextSize(R.id.widget_team_4, android.util.TypedValue.COMPLEX_UNIT_SP, tableSp)
        views.setTextViewTextSize(R.id.widget_team_5, android.util.TypedValue.COMPLEX_UNIT_SP, tableSp)
        views.setTextViewTextSize(R.id.widget_team_6, android.util.TypedValue.COMPLEX_UNIT_SP, tableWcSp)

        // Dynamic padding adjustment: compact for small widgets, expanding for spacious widgets
        val padH = (minOf(minWidth * 0.035f, 14f) * density).toInt().coerceAtLeast((3 * density).toInt())
        val padV = (minOf(minHeight * 0.035f, 14f) * density).toInt().coerceAtLeast((3 * density).toInt())
        views.setViewPadding(R.id.widget_root, padH, padV, padH, padV)

        // 1. Height-based rules
        if (minHeight < 90) {
            // Ultra-compact size - show only matchup & countdown
            views.setViewVisibility(R.id.widget_header_layout, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_divider_top, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_standings_table, android.view.View.GONE)
        } else if (minHeight < 120) {
            // Intermediate compact size
            views.setViewVisibility(R.id.widget_header_layout, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_divider_top, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_standings_table, android.view.View.VISIBLE)
        } else {
            // Full size - show all elements with dynamic typography filling the available area
            views.setViewVisibility(R.id.widget_header_layout, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_divider_top, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_standings_table, android.view.View.VISIBLE)
        }

        // 2. Width-based rules
        if (minWidth < 140) {
            // Narrow widget - hide team logos to prevent compressing the texts in between
            views.setViewVisibility(R.id.widget_tigers_logo, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_opponent_logo, android.view.View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_tigers_logo, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_opponent_logo, android.view.View.VISIBLE)
        }
    }

    private suspend fun loadLogoBitmap(context: Context, url: String): Bitmap? {
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // essential for widgets so bitmap can be passed across processes
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val rawBitmap = result.drawable.toBitmap()
                // Resize to max 120x120 to avoid TransactionTooLargeException in RemoteViews
                val maxDim = 120
                if (rawBitmap.width > maxDim || rawBitmap.height > maxDim) {
                    val aspectRatio = rawBitmap.width.toFloat() / rawBitmap.height.toFloat()
                    val newWidth: Int
                    val newHeight: Int
                    if (rawBitmap.width > rawBitmap.height) {
                        newWidth = maxDim
                        newHeight = (maxDim / aspectRatio).toInt().coerceAtLeast(1)
                    } else {
                        newHeight = maxDim
                        newWidth = (maxDim * aspectRatio).toInt().coerceAtLeast(1)
                    }
                    Bitmap.createScaledBitmap(rawBitmap, newWidth, newHeight, true)
                } else {
                    rawBitmap
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("TigersWidget", "Error loading logo bitmap from $url: ${e.message}")
            null
        }
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, DetroitTigersWidgetProvider::class.java).apply {
            action = ACTION_AUTO_UPDATE
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        val intervalMs = 15 * 60 * 1000L // 15-minute battery-friendly periodic refresh
        val triggerAt = android.os.SystemClock.elapsedRealtime() + intervalMs
        try {
            alarmManager.setInexactRepeating(
                android.app.AlarmManager.ELAPSED_REALTIME,
                triggerAt,
                intervalMs,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("TigersWidget", "Error scheduling auto refresh alarm: ${e.message}")
        }
    }

    private fun cancelUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, DetroitTigersWidgetProvider::class.java).apply {
            action = ACTION_AUTO_UPDATE
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val ACTION_AUTO_UPDATE = "com.example.widget.ACTION_AUTO_UPDATE"
        private const val ALARM_REQUEST_CODE = 9981

        fun triggerUpdate(context: Context) {
            Log.d("TigersWidget", "Triggering widget update intent broadcast")
            val intent = Intent(context, DetroitTigersWidgetProvider::class.java).apply {
                action = ACTION_AUTO_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
