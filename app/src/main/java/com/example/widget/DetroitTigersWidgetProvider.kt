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
import androidx.core.content.ContextCompat
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

enum class WidgetTheme(
    val id: Int,
    val displayName: String,
    val buttonLabel: String,
    val isMaterialYou: Boolean,
    val bgDrawableRes: Int,
    val tagDrawableRes: Int,
    val titleColorRes: Int,
    val countdownColorRes: Int,
    val opponentColorRes: Int,
    val dividerColorRes: Int,
    val pitcherSubColorRes: Int,
    val pitcherHighlightHex: String?,
    val standingColorRes: Int,
    val teamColorRes: Int,
    val teamDetColorRes: Int,
    val wcgbColorRes: Int,
    val tagTextColorRes: Int
) {
    CLASSIC(
        id = 0,
        displayName = "Classic Navy",
        buttonLabel = "🎨 CLASSIC",
        isMaterialYou = false,
        bgDrawableRes = R.drawable.widget_bg_classic,
        tagDrawableRes = R.drawable.widget_tag_classic,
        titleColorRes = R.color.widget_classic_title,
        countdownColorRes = R.color.widget_classic_countdown,
        opponentColorRes = R.color.widget_classic_opponent,
        dividerColorRes = R.color.widget_classic_divider,
        pitcherSubColorRes = R.color.widget_classic_pitcher_sub,
        pitcherHighlightHex = "#FA4616",
        standingColorRes = R.color.widget_classic_standing,
        teamColorRes = R.color.widget_classic_team,
        teamDetColorRes = R.color.widget_classic_team_det,
        wcgbColorRes = R.color.widget_classic_wcgb,
        tagTextColorRes = R.color.widget_classic_tag_text
    ),
    MOTOR_CITY(
        id = 1,
        displayName = "Motor City",
        buttonLabel = "🎨 MOTOR CITY",
        isMaterialYou = false,
        bgDrawableRes = R.drawable.widget_bg_motor,
        tagDrawableRes = R.drawable.widget_tag_motor,
        titleColorRes = R.color.widget_motor_title,
        countdownColorRes = R.color.widget_motor_countdown,
        opponentColorRes = R.color.widget_motor_opponent,
        dividerColorRes = R.color.widget_motor_divider,
        pitcherSubColorRes = R.color.widget_motor_pitcher_sub,
        pitcherHighlightHex = "#FF5722",
        standingColorRes = R.color.widget_motor_standing,
        teamColorRes = R.color.widget_motor_team,
        teamDetColorRes = R.color.widget_motor_team_det,
        wcgbColorRes = R.color.widget_motor_wcgb,
        tagTextColorRes = R.color.widget_motor_tag_text
    ),
    HERITAGE(
        id = 2,
        displayName = "Heritage 1984",
        buttonLabel = "🎨 HERITAGE",
        isMaterialYou = false,
        bgDrawableRes = R.drawable.widget_bg_heritage,
        tagDrawableRes = R.drawable.widget_tag_heritage,
        titleColorRes = R.color.widget_heritage_title,
        countdownColorRes = R.color.widget_heritage_countdown,
        opponentColorRes = R.color.widget_heritage_opponent,
        dividerColorRes = R.color.widget_heritage_divider,
        pitcherSubColorRes = R.color.widget_heritage_pitcher_sub,
        pitcherHighlightHex = "#F5A623",
        standingColorRes = R.color.widget_heritage_standing,
        teamColorRes = R.color.widget_heritage_team,
        teamDetColorRes = R.color.widget_heritage_team_det,
        wcgbColorRes = R.color.widget_heritage_wcgb,
        tagTextColorRes = R.color.widget_heritage_tag_text
    ),
    MY_DYNAMIC(
        id = 3,
        displayName = "MY Dynamic",
        buttonLabel = "🎨 MY DYNAMIC",
        isMaterialYou = true,
        bgDrawableRes = R.drawable.widget_bg_my1,
        tagDrawableRes = R.drawable.widget_tag_my1,
        titleColorRes = R.color.widget_my1_title,
        countdownColorRes = R.color.widget_my1_countdown,
        opponentColorRes = R.color.widget_my1_opponent,
        dividerColorRes = R.color.widget_my1_divider,
        pitcherSubColorRes = R.color.widget_my1_pitcher_sub,
        pitcherHighlightHex = null,
        standingColorRes = R.color.widget_my1_standing,
        teamColorRes = R.color.widget_my1_team,
        teamDetColorRes = R.color.widget_my1_team_det,
        wcgbColorRes = R.color.widget_my1_wcgb,
        tagTextColorRes = R.color.widget_my1_tag_text
    ),
    MY_VIBRANT(
        id = 4,
        displayName = "MY Vibrant",
        buttonLabel = "🎨 MY VIBRANT",
        isMaterialYou = true,
        bgDrawableRes = R.drawable.widget_bg_my2,
        tagDrawableRes = R.drawable.widget_tag_my2,
        titleColorRes = R.color.widget_my2_title,
        countdownColorRes = R.color.widget_my2_countdown,
        opponentColorRes = R.color.widget_my2_opponent,
        dividerColorRes = R.color.widget_my2_divider,
        pitcherSubColorRes = R.color.widget_my2_pitcher_sub,
        pitcherHighlightHex = null,
        standingColorRes = R.color.widget_my2_standing,
        teamColorRes = R.color.widget_my2_team,
        teamDetColorRes = R.color.widget_my2_team_det,
        wcgbColorRes = R.color.widget_my2_wcgb,
        tagTextColorRes = R.color.widget_my2_tag_text
    ),
    MY_TONAL(
        id = 5,
        displayName = "MY Tonal",
        buttonLabel = "🎨 MY TONAL",
        isMaterialYou = true,
        bgDrawableRes = R.drawable.widget_bg_my3,
        tagDrawableRes = R.drawable.widget_tag_my3,
        titleColorRes = R.color.widget_my3_title,
        countdownColorRes = R.color.widget_my3_countdown,
        opponentColorRes = R.color.widget_my3_opponent,
        dividerColorRes = R.color.widget_my3_divider,
        pitcherSubColorRes = R.color.widget_my3_pitcher_sub,
        pitcherHighlightHex = null,
        standingColorRes = R.color.widget_my3_standing,
        teamColorRes = R.color.widget_my3_team,
        teamDetColorRes = R.color.widget_my3_team_det,
        wcgbColorRes = R.color.widget_my3_wcgb,
        tagTextColorRes = R.color.widget_my3_tag_text
    );

    companion object {
        fun fromIndex(index: Int): WidgetTheme {
            val validIndex = (index % values().size + values().size) % values().size
            return values()[validIndex]
        }
    }
}

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
                updateAllWidgets(context, appWidgetManager, intArrayOf(appWidgetId), newOptions)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("TigersWidget", "onReceive action: ${intent.action}")
        if (intent.action == ACTION_TOGGLE_THEME) {
            val prefs = context.getSharedPreferences("TigersWidgetPrefs", Context.MODE_PRIVATE)
            val currentIdx = prefs.getInt("widget_theme_index", if (prefs.getBoolean("widget_material_you_enabled", false)) 3 else 0)
            val nextIdx = (currentIdx + 1) % WidgetTheme.values().size
            val nextTheme = WidgetTheme.fromIndex(nextIdx)
            prefs.edit()
                .putInt("widget_theme_index", nextIdx)
                .putBoolean("widget_material_you_enabled", nextTheme.isMaterialYou)
                .apply()
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
            return
        }

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

    private suspend fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        overrideOptions: android.os.Bundle? = null
    ) {
        try {
            val db = AppDatabase.getDatabase(context)
            val prefs = context.getSharedPreferences("TigersWidgetPrefs", Context.MODE_PRIVATE)
            val lastRefresh = prefs.getLong("last_widget_refresh_ts", 0L)
            val now = System.currentTimeMillis()

            // Proactively refresh game data if stale (> 15 minutes)
            if (now - lastRefresh > 15 * 60 * 1000L) {
                try {
                    val repository = com.example.data.repository.GameRepository(db.gameDao())
                    repository.refreshGames(context, forceSimulated = false)
                    prefs.edit().putLong("last_widget_refresh_ts", now).apply()
                } catch (e: Exception) {
                    Log.e("TigersWidget", "Error refreshing games repository in widget: ${e.message}")
                }
            }

            val themeIndex = prefs.getInt("widget_theme_index", if (prefs.getBoolean("widget_material_you_enabled", false)) 3 else 0)
            val theme = WidgetTheme.fromIndex(themeIndex)

            val games = db.gameDao().getUpcomingGames().firstOrNull() ?: emptyList()
            val nextGame = games.firstOrNull()

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
                
                // Query current widget options for responsive sizing
                val options = overrideOptions ?: appWidgetManager.getAppWidgetOptions(widgetId)
                val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 180
                val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 110

                // Theme toggle click intent
                val toggleIntent = Intent(context, DetroitTigersWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_THEME
                }
                val togglePending = PendingIntent.getBroadcast(
                    context,
                    widgetId,
                    toggleIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_theme_toggle, togglePending)
                views.setTextViewText(R.id.widget_theme_toggle, theme.buttonLabel)

                // Apply the active theme colors, backgrounds, and drawables
                applyWidgetTheme(context, views, theme)

                if (nextGame != null) {
                    bindGameData(context, views, nextGame, minWidth, theme)
                } else {
                    bindEmptyState(context, views, theme)
                }

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

    private fun applyWidgetTheme(context: Context, views: RemoteViews, theme: WidgetTheme) {
        views.setInt(R.id.widget_root, "setBackgroundResource", theme.bgDrawableRes)
        views.setInt(R.id.widget_tag, "setBackgroundResource", theme.tagDrawableRes)
        views.setInt(R.id.widget_theme_toggle, "setBackgroundResource", theme.tagDrawableRes)
        views.setInt(R.id.widget_divider_top, "setBackgroundColor", ContextCompat.getColor(context, theme.dividerColorRes))

        views.setTextColor(R.id.widget_title, ContextCompat.getColor(context, theme.titleColorRes))
        views.setTextColor(R.id.widget_countdown, ContextCompat.getColor(context, theme.countdownColorRes))
        views.setTextColor(R.id.widget_opponent, ContextCompat.getColor(context, theme.opponentColorRes))
        views.setTextColor(R.id.widget_standing_h2h, ContextCompat.getColor(context, theme.standingColorRes))
        views.setTextColor(R.id.widget_stadium_pitcher_info, ContextCompat.getColor(context, theme.pitcherSubColorRes))
        views.setTextColor(R.id.widget_tag, ContextCompat.getColor(context, theme.tagTextColorRes))
    }

    private suspend fun bindGameData(context: Context, views: RemoteViews, game: UpcomingGame, minWidth: Int = 180, theme: WidgetTheme = WidgetTheme.CLASSIC) {
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
        val rawPitcherHtml = if (cleanName.equals("TBD", ignoreCase = true)) {
            if (theme.pitcherHighlightHex != null) "SP: <b><font color='${theme.pitcherHighlightHex}'>TBD</font></b>" else "SP: <b>TBD</b>"
        } else {
            if (theme.pitcherHighlightHex != null) {
                String.format(
                    "SP: <b><font color='%s'>%s%s</font></b> • (LG: %.1f IP, %d SO)",
                    theme.pitcherHighlightHex,
                    cleanName,
                    handSuffix,
                    game.pitcherLastIp,
                    game.pitcherLastSo
                )
            } else {
                String.format(
                    "SP: <b>%s%s</b> • (LG: %.1f IP, %d SO)",
                    cleanName,
                    handSuffix,
                    game.pitcherLastIp,
                    game.pitcherLastSo
                )
            }
        }
        val formattedPitcherText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(rawPitcherHtml, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(rawPitcherHtml)
        }
        views.setTextViewText(R.id.widget_stadium_pitcher_info, formattedPitcherText)
        views.setTextColor(R.id.widget_stadium_pitcher_info, ContextCompat.getColor(context, theme.pitcherSubColorRes))

        // Standing & H2H record info
        val prefs = context.getSharedPreferences("TigersWidgetPrefs", Context.MODE_PRIVATE)
        val standingSummary = prefs.getString("tigers_standing_summary", null)
        val standingText = standingSummary ?: game.tigersStanding
        views.setTextViewText(R.id.widget_standing_h2h, "$standingText • ${game.headToHeadRecord}")
        
        // Load logos from URL asynchronously and set them to RemoteViews
        val tigersLogoUrl = "https://a.espncdn.com/i/teamlogos/mlb/500/det.png"
        val opponentLogoUrl = getTeamLogoUrl(game.opponentName)

        val tigersBitmap = loadLogoBitmap(context, tigersLogoUrl)
        val opponentBitmap = loadLogoBitmap(context, opponentLogoUrl)

        // Away team goes on the LEFT (widget_away_logo), Home team goes on the RIGHT (widget_home_logo)
        val (awayBitmap, awayResFallback) = if (isHome) {
            Pair(opponentBitmap, R.drawable.ic_baseball_placeholder)
        } else {
            Pair(tigersBitmap, R.drawable.ic_tigers_logo)
        }

        val (homeBitmap, homeResFallback) = if (isHome) {
            Pair(tigersBitmap, R.drawable.ic_tigers_logo)
        } else {
            Pair(opponentBitmap, R.drawable.ic_baseball_placeholder)
        }

        if (awayBitmap != null) {
            views.setImageViewBitmap(R.id.widget_away_logo, awayBitmap)
        } else {
            views.setImageViewResource(R.id.widget_away_logo, awayResFallback)
        }

        if (homeBitmap != null) {
            views.setImageViewBitmap(R.id.widget_home_logo, homeBitmap)
        } else {
            views.setImageViewResource(R.id.widget_home_logo, homeResFallback)
        }

        // Bind live Games Back & Playoff Spot values
        bindStandingsStats(context, views, theme)
    }

    private fun cleanPitcherName(rawName: String): String {
        if (rawName.contains("Skubal", ignoreCase = true)) return "Tarik Skubal"
        if (rawName.contains("Olson", ignoreCase = true)) return "Reese Olson"
        if (rawName.contains("Mize", ignoreCase = true)) return "Casey Mize"
        if (rawName.contains("Montero", ignoreCase = true)) return "Keider Montero"
        if (rawName.contains("Jobe", ignoreCase = true)) return "Jackson Jobe"
        if (rawName.contains("Hurter", ignoreCase = true)) return "Brant Hurter"
        if (rawName.contains("Madden", ignoreCase = true)) return "Ty Madden"
        if (rawName.contains("Sears", ignoreCase = true)) return "Andrew Sears"
        if (rawName.contains("Kinley", ignoreCase = true)) return "Tyler Kinley"
        if (rawName.contains("Brieske", ignoreCase = true)) return "Beau Brieske"
        if (rawName.contains("Vest", ignoreCase = true)) return "Will Vest"
        if (rawName.contains("Holton", ignoreCase = true)) return "Tyler Holton"
        if (rawName.contains("Hanifee", ignoreCase = true)) return "Brenan Hanifee"
        if (rawName.contains("Manning", ignoreCase = true)) return "Matt Manning"
        if (rawName.contains("TBD", ignoreCase = true) && !rawName.contains("Likely", ignoreCase = true)) return "TBD"
        return rawName.replace(Regex("\\s*Likely\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^TBD\\s*\\(?"), "")
            .replace(Regex("\\)?$"), "")
            .trim().ifEmpty { "TBD" }
    }

    private fun bindEmptyState(context: Context, views: RemoteViews, theme: WidgetTheme = WidgetTheme.CLASSIC) {
        views.setTextViewText(R.id.widget_opponent, "No Scheduled Games")
        views.setTextViewText(R.id.widget_countdown, "00d 00h 00m")
        views.setTextViewText(R.id.widget_stadium_pitcher_info, "SP: TBD")
        views.setTextViewText(R.id.widget_standing_h2h, "Standings unavailable")
        views.setImageViewResource(R.id.widget_away_logo, R.drawable.ic_tigers_logo)
        views.setImageViewResource(R.id.widget_home_logo, R.drawable.ic_baseball_placeholder)

        // Bind live Games Back & Playoff Spot values
        bindStandingsStats(context, views, theme)
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

    private fun bindStandingsStats(context: Context, views: RemoteViews, theme: WidgetTheme = WidgetTheme.CLASSIC) {
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
        val t1 = formattedTeams.getOrNull(0) ?: formatTeamText(1, defaultTeams[0])
        val t2 = formattedTeams.getOrNull(1) ?: formatTeamText(2, defaultTeams[1])
        views.setTextViewText(R.id.widget_team_1, t1)
        views.setTextViewText(R.id.widget_team_2, t2)

        // Column 2 (ranks 3, 4)
        val t3 = formattedTeams.getOrNull(2) ?: formatTeamText(3, defaultTeams[2])
        val t4 = formattedTeams.getOrNull(3) ?: formatTeamText(4, defaultTeams[3])
        views.setTextViewText(R.id.widget_team_3, t3)
        views.setTextViewText(R.id.widget_team_4, t4)

        // Column 3 (rank 5 and 6th spot: Playoff Spot / Wild Card GB & Games Left)
        val t5 = formattedTeams.getOrNull(4) ?: formatTeamText(5, defaultTeams[4])
        views.setTextViewText(R.id.widget_team_5, t5)

        val wcGbRaw = prefs.getString("games_back_wild_card", "5.5") ?: "5.5"
        val playoffStatusRaw = prefs.getString("playoff_status", "OUT") ?: "OUT"
        val playoffSpotInfo = prefs.getString("playoff_spot_info", null)

        val isPlayoffIn = playoffStatusRaw.contains("IN", ignoreCase = true) ||
                wcGbRaw.contains("IN", ignoreCase = true) ||
                (sortedList.indexOfFirst { it.name.contains("DET", ignoreCase = true) } == 0)

        val formattedWcText = when {
            isPlayoffIn -> {
                if (playoffSpotInfo != null && playoffSpotInfo.contains("WC", ignoreCase = true)) {
                    val spot = if (playoffSpotInfo.contains("WC #1")) "WC1" else if (playoffSpotInfo.contains("WC #2")) "WC2" else "WC3"
                    "PLAYOFF: IN ($spot)"
                } else if (sortedList.indexOfFirst { it.name.contains("DET", ignoreCase = true) } == 0) {
                    "PLAYOFF: IN (ALC #1)"
                } else {
                    "PLAYOFF: IN"
                }
            }
            else -> {
                val cleanGb = wcGbRaw.replace(Regex("^WCGB:?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^WC:?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace("GB", "").trim()
                "WCGB: $cleanGb"
            }
        }

        // Apply theme colors to team standings
        val teamColor = ContextCompat.getColor(context, theme.teamColorRes)
        val teamDetColor = ContextCompat.getColor(context, theme.teamDetColorRes)
        val wcgbColor = ContextCompat.getColor(context, if (isPlayoffIn) theme.teamDetColorRes else theme.wcgbColorRes)

        views.setTextColor(R.id.widget_team_1, if (t1.toString().contains("DET", ignoreCase = true)) teamDetColor else teamColor)
        views.setTextColor(R.id.widget_team_2, if (t2.toString().contains("DET", ignoreCase = true)) teamDetColor else teamColor)
        views.setTextColor(R.id.widget_team_3, if (t3.toString().contains("DET", ignoreCase = true)) teamDetColor else teamColor)
        views.setTextColor(R.id.widget_team_4, if (t4.toString().contains("DET", ignoreCase = true)) teamDetColor else teamColor)
        views.setTextColor(R.id.widget_team_5, if (t5.toString().contains("DET", ignoreCase = true)) teamDetColor else teamColor)

        val styledWc = "<b><i>$formattedWcText</i></b>"
        val htmlWc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(styledWc, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(styledWc)
        }
        views.setTextViewText(R.id.widget_team_6, htmlWc)
        views.setTextColor(R.id.widget_team_6, wcgbColor)
    }

    private fun formatTeamText(rank: Int, rawText: String): CharSequence {
        val cleanText = rawText.replace(Regex("^\\d+[\\.\\s]\\s*"), "")
        val textWithRank = "$rank. $cleanText"
        val isTigers = cleanText.contains("DET", ignoreCase = true)
        return if (isTigers) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.text.Html.fromHtml("<b>$textWithRank</b>", android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml("<b>$textWithRank</b>")
            }
        } else {
            textWithRank
        }
    }

    fun applyResponsiveLayout(context: Context, views: RemoteViews, minWidth: Int, minHeight: Int) {
        applyResponsiveLayout(views, minWidth, minHeight, context)
    }

    fun applyResponsiveLayout(views: RemoteViews, minWidth: Int, minHeight: Int, context: Context? = null) {
        Log.d("TigersWidget", "Applying responsive layout: width=$minWidth, height=$minHeight")

        // ── 1. Visibility based purely on available height ────────────────────────────────────
        when {
            minHeight < 72 -> {
                // Single row — just show matchup and countdown, nothing else
                views.setViewVisibility(R.id.widget_header_layout, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_divider_top, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_standings_table, android.view.View.GONE)
            }
            minHeight < 90 -> {
                // Narrow 2-row: show header + matchup + H2H only
                views.setViewVisibility(R.id.widget_header_layout, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_divider_top, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_standings_table, android.view.View.GONE)
            }
            else -> {
                // Full 3x2+ layout — everything visible, weights handle the sizing
                views.setViewVisibility(R.id.widget_header_layout, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_divider_top, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_stadium_pitcher_info, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_standing_h2h, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_standings_table, android.view.View.VISIBLE)
            }
        }

        // ── 2. Logo visibility based on available width ───────────────────────────────────────
        val showLogos = minWidth >= 140
        views.setViewVisibility(R.id.widget_away_logo, if (showLogos) android.view.View.VISIBLE else android.view.View.GONE)
        views.setViewVisibility(R.id.widget_home_logo, if (showLogos) android.view.View.VISIBLE else android.view.View.GONE)

        // ── 3. Proportional font sizing ────────────────────────────────────────────────────────
        // Font sizes scale from available height so they ALWAYS fill the space without overflow.
        // Coefficients tuned so text fills each weight-allocated row comfortably.
        val h = minHeight.toFloat()

        // Header row (~14% of height in weights)
        val titleSp   = (h * 0.115f).coerceIn(8f, 16f)
        val tagSp     = (h * 0.095f).coerceIn(7f, 13f)

        // Matchup row (~36% of height in weights)
        val opponentSp  = (h * 0.115f).coerceIn(9f, 18f)
        val countdownSp = (h * 0.175f).coerceIn(13f, 30f)

        // SP + H2H rows (~12% + 11% of height)
        val pitcherSp    = (h * 0.095f).coerceIn(7f, 14f)
        val standingH2hSp = (h * 0.085f).coerceIn(6.5f, 13f)

        // Standings rows (~24% of height, 2 lines split equally)
        val teamSp = (h * 0.085f).coerceIn(6.5f, 12f)

        views.setTextViewTextSize(R.id.widget_title,  android.util.TypedValue.COMPLEX_UNIT_SP, titleSp)
        views.setTextViewTextSize(R.id.widget_tag,    android.util.TypedValue.COMPLEX_UNIT_SP, tagSp)
        views.setTextViewTextSize(R.id.widget_theme_toggle, android.util.TypedValue.COMPLEX_UNIT_SP, tagSp)
        views.setTextViewTextSize(R.id.widget_opponent,  android.util.TypedValue.COMPLEX_UNIT_SP, opponentSp)
        views.setTextViewTextSize(R.id.widget_countdown, android.util.TypedValue.COMPLEX_UNIT_SP, countdownSp)
        views.setTextViewTextSize(R.id.widget_stadium_pitcher_info, android.util.TypedValue.COMPLEX_UNIT_SP, pitcherSp)
        views.setTextViewTextSize(R.id.widget_standing_h2h,         android.util.TypedValue.COMPLEX_UNIT_SP, standingH2hSp)
        views.setTextViewTextSize(R.id.widget_team_1,  android.util.TypedValue.COMPLEX_UNIT_SP, teamSp)
        views.setTextViewTextSize(R.id.widget_team_2,  android.util.TypedValue.COMPLEX_UNIT_SP, teamSp)
        views.setTextViewTextSize(R.id.widget_team_3,  android.util.TypedValue.COMPLEX_UNIT_SP, teamSp)
        views.setTextViewTextSize(R.id.widget_team_4,  android.util.TypedValue.COMPLEX_UNIT_SP, teamSp)
        views.setTextViewTextSize(R.id.widget_team_5,  android.util.TypedValue.COMPLEX_UNIT_SP, teamSp)
        views.setTextViewTextSize(R.id.widget_team_6,  android.util.TypedValue.COMPLEX_UNIT_SP, teamSp)

        // ── 4. Proportional padding (scales with widget size) ────────────────────────────────
        val density = context?.resources?.displayMetrics?.density ?: 2f
        val padH = ((minWidth  * 0.015f).coerceIn(3f, 10f) * density).toInt()
        val padV = ((minHeight * 0.018f).coerceIn(2f,  8f) * density).toInt()
        views.setViewPadding(R.id.widget_root, padH, padV, padH, padV)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Ensure alarm triggers even in Doze mode without requiring exact alarm permissions
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.ELAPSED_REALTIME,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setInexactRepeating(
                    android.app.AlarmManager.ELAPSED_REALTIME,
                    triggerAt,
                    intervalMs,
                    pendingIntent
                )
            }
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
        const val ACTION_TOGGLE_THEME = "com.example.widget.ACTION_TOGGLE_THEME"
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
