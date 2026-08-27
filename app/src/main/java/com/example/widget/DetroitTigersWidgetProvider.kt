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
                applyResponsiveLayout(views, minWidth, minHeight)

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
        views.setTextViewText(R.id.widget_standing_h2h, "${game.tigersStanding} • ${game.headToHeadRecord}")
        
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

    private data class TeamStandingsInfo(val name: String, val wins: Int, val losses: Int, val rawText: String) {
        val winPct: Double
            get() = if (wins + losses > 0) wins.toDouble() / (wins + losses) else 0.0
    }

    private fun parseTeamInfo(rawItem: String): TeamStandingsInfo {
        val clean = rawItem.replace(Regex("^\\d+[\\.\\s]\\s*"), "").trim()
        val match = Regex("([A-Za-z]+):?\\s*(\\d+)[\\-\\s]+(\\d+)").find(clean)
        return if (match != null) {
            val name = match.groupValues[1].uppercase()
            val w = match.groupValues[2].toIntOrNull() ?: 0
            val l = match.groupValues[3].toIntOrNull() ?: 0
            TeamStandingsInfo(name, w, l, "$name: $w-$l")
        } else {
            TeamStandingsInfo(clean, 0, 0, clean)
        }
    }

    private fun bindStandingsStats(context: Context, views: RemoteViews) {
        val prefs = context.getSharedPreferences("TigersWidgetPrefs", Context.MODE_PRIVATE)
        val alCentralStandings = prefs.getString(
            "al_central_standings",
            "CLE: 58-37 • MIN: 53-41 • KC: 52-43 • DET: 47-53 • CWS: 27-68"
        ) ?: "CLE: 58-37 • MIN: 53-41 • KC: 52-43 • DET: 47-53 • CWS: 27-68"

        val items = alCentralStandings.split("•", ",").map { it.trim() }.filter { it.isNotEmpty() }
        val defaultTeams = listOf("CLE: 58-37", "MIN: 53-41", "KC: 52-43", "DET: 47-53", "CWS: 27-68")

        val rawList = if (items.isNotEmpty()) items else defaultTeams
        val parsedList = rawList.map { parseTeamInfo(it) }

        // Sort by win percentage descending to guarantee accurate 1-5 rankings
        val sortedList = parsedList.sortedWith(
            compareByDescending<TeamStandingsInfo> { it.winPct }
                .thenByDescending { it.wins }
        )

        val t1 = sortedList.getOrNull(0)?.rawText ?: defaultTeams[0]
        val t2 = sortedList.getOrNull(1)?.rawText ?: defaultTeams[1]
        val t3 = sortedList.getOrNull(2)?.rawText ?: defaultTeams[2]
        val t4 = sortedList.getOrNull(3)?.rawText ?: defaultTeams[3]
        val t5 = sortedList.getOrNull(4)?.rawText ?: defaultTeams[4]

        // Column 1 (ranks 1, 2)
        views.setTextViewText(R.id.widget_team_1, formatTeamText(1, t1))
        views.setTextViewText(R.id.widget_team_2, formatTeamText(2, t2))

        // Column 2 (ranks 3, 4)
        views.setTextViewText(R.id.widget_team_3, formatTeamText(3, t3))
        views.setTextViewText(R.id.widget_team_4, formatTeamText(4, t4))

        // Column 3 (rank 5 and 6th spot: Wild Card GB & Games Left in red if > 0, green if 0 or IN)
        views.setTextViewText(R.id.widget_team_5, formatTeamText(5, t5))

        val wcGbRaw = prefs.getString("games_back_wild_card", "6.0") ?: "6.0"
        val detTeamString = sortedList.firstOrNull { it.rawText.contains("DET", ignoreCase = true) }?.rawText
        val glCalculated = detTeamString?.let { raw ->
            val match = Regex("(\\d+)-(\\d+)").find(raw)
            if (match != null) {
                val w = match.groupValues[1].toIntOrNull() ?: 47
                val l = match.groupValues[2].toIntOrNull() ?: 53
                maxOf(0, 162 - (w + l))
            } else null
        } ?: prefs.getInt("tigers_games_left", 62)

        val formattedWcText = formatWcGbText(wcGbRaw, glCalculated)
        val isGreen = isWildCardInOrZero(wcGbRaw, formattedWcText)
        val colorHex = if (isGreen) "#00E676" else "#FF5252"
        val styledWc = "<b><i><font color='$colorHex'>$formattedWcText</font></i></b>"
        val htmlWc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(styledWc, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(styledWc)
        }
        views.setTextViewText(R.id.widget_team_6, htmlWc)
    }

    private fun isWildCardInOrZero(raw: String, formatted: String): Boolean {
        val trimmed = raw.trim()
        val formattedTrimmed = formatted.trim()
        if (trimmed.contains("IN", ignoreCase = true) || formattedTrimmed.contains("IN", ignoreCase = true)) {
            return true
        }
        val match = Regex("WCGB:\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(formattedTrimmed)
            ?: Regex("(\\d+(?:\\.\\d+)?)").find(trimmed)
        if (match != null) {
            val value = match.groupValues[1].toDoubleOrNull() ?: 6.0
            return value <= 0.0
        }
        return false
    }

    private fun formatWcGbText(raw: String, gamesLeft: Int): String {
        val trimmed = raw.trim()
        val wcPart = when {
            trimmed.contains("IN", ignoreCase = true) -> "WCGB: IN"
            else -> {
                val match = Regex("(\\d+(?:\\.\\d+)?)").find(trimmed)
                if (match != null) {
                    "WCGB: ${match.groupValues[1]}"
                } else if (trimmed.isNotEmpty() && trimmed != "N/A") {
                    val cleaned = trimmed.replace(Regex("^WC\\s*GB:?\\s*", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("^WCGB:?\\s*", RegexOption.IGNORE_CASE), "")
                    "WCGB: $cleaned"
                } else {
                    "WCGB: 6.0"
                }
            }
        }
        return "$wcPart, GL: $gamesLeft"
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

    private fun applyResponsiveLayout(views: RemoteViews, minWidth: Int, minHeight: Int) {
        Log.d("TigersWidget", "Applying responsive layout: width=$minWidth, height=$minHeight")

        // 1. Height-based rules
        if (minHeight < 95) {
            // Ultra-compact size - show only matchup & countdown
            views.setViewVisibility(R.id.widget_header_layout, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_divider_top, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_standings_table, android.view.View.GONE)
        } else if (minHeight < 125) {
            // Intermediate compact size - hide header/divider to save space for content
            views.setViewVisibility(R.id.widget_header_layout, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_divider_top, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_standings_table, android.view.View.VISIBLE)
        } else {
            // Full size - show all elements with optimized spacing
            views.setViewVisibility(R.id.widget_header_layout, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_divider_top, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_standings_table, android.view.View.VISIBLE)
        }

        // 2. Width-based rules
        if (minWidth < 150) {
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
