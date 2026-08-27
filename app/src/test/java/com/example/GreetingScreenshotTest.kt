package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UpcomingGame
import com.example.ui.SleekCountdownHeroCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun dodgers_matchup_screenshot() {
    val sampleDodgersGame = UpcomingGame(
      gameId = 9991199,
      gameTimeMillis = System.currentTimeMillis() + 7200000L,
      opponentName = "Los Angeles Dodgers",
      stadiumName = "Comerica Park",
      stadiumSize = 41083,
      pitcherName = "Tarik Skubal",
      pitcherStatsWins = 11,
      pitcherStatsLosses = 2,
      pitcherStatsEra = 2.35,
      pitcherStatsStrikeouts = 145,
      pitcherStatsWhip = 0.91,
      isHomeGame = true,
      isSimulated = true,
      seasonType = "Interleague",
      winProbability = 62,
      pitcherAge = 29,
      tigersStanding = "2nd in AL Central • Playoff Spot: IN (WC #1)",
      headToHeadRecord = "Record: 2-1 vs LAD",
      pitcherId = 669373,
      pitcherLastIp = 7.0,
      pitcherLastSo = 9,
      pitcherHand = "L"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
          SleekCountdownHeroCard(
            game = sampleDodgersGame,
            countdownText = "02h 15m 30s",
            isLive = false,
            weatherText = "72°F • Clear Skies",
            isWeatherLoading = false
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dodgers_matchup_preview.png")
  }

  @Test
  fun roster_preview_screenshot() {
    val sampleRoster = listOf(
      com.example.data.api.MlbRosterEntry(com.example.data.api.MlbPersonInfo(669373, "Tarik Skubal"), "29", com.example.data.api.MlbPositionInfo(type = "Pitcher", abbreviation = "LHP")),
      com.example.data.api.MlbRosterEntry(com.example.data.api.MlbPersonInfo(681857, "Reese Olson"), "45", com.example.data.api.MlbPositionInfo(type = "Pitcher", abbreviation = "RHP")),
      com.example.data.api.MlbRosterEntry(com.example.data.api.MlbPersonInfo(663554, "Casey Mize"), "12", com.example.data.api.MlbPositionInfo(type = "Pitcher", abbreviation = "RHP")),
      com.example.data.api.MlbRosterEntry(com.example.data.api.MlbPersonInfo(695549, "Jackson Jobe"), "21", com.example.data.api.MlbPositionInfo(type = "Pitcher", abbreviation = "RHP")),
      com.example.data.api.MlbRosterEntry(com.example.data.api.MlbPersonInfo(682998, "Riley Greene"), "31", com.example.data.api.MlbPositionInfo(type = "Outfielder", abbreviation = "LF")),
      com.example.data.api.MlbRosterEntry(com.example.data.api.MlbPersonInfo(681481, "Kerry Carpenter"), "30", com.example.data.api.MlbPositionInfo(type = "Outfielder", abbreviation = "RF")),
      com.example.data.api.MlbRosterEntry(com.example.data.api.MlbPersonInfo(690993, "Colt Keith"), "33", com.example.data.api.MlbPositionInfo(type = "Infielder", abbreviation = "2B")),
      com.example.data.api.MlbRosterEntry(com.example.data.api.MlbPersonInfo(650402, "Gleyber Torres"), "25", com.example.data.api.MlbPositionInfo(type = "Infielder", abbreviation = "2B"))
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.foundation.layout.Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
          androidx.compose.material3.Text(
            text = "DETROIT TIGERS SQUAD (SAMPLE)",
            style = androidx.compose.ui.text.TextStyle(
              color = androidx.compose.ui.graphics.Color(0xFFFFC107),
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
              fontSize = 12.sp
            )
          )
          com.example.ui.FlowRowLayout(items = sampleRoster)
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/roster_preview.png")
  }
}

