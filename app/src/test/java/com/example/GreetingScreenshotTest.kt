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
  fun last_game_result_preview_screenshot() {
    val sampleLastGame = com.example.ui.LastGameResult(
      opponentName = "Minnesota Twins",
      tigersScore = 5,
      opponentScore = 3,
      isTigersWinner = true,
      isHomeGame = true,
      gameDate = "Wed, Sep 2",
      statusText = "Final"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.foundation.layout.Box(
          modifier = Modifier.padding(16.dp)
        ) {
          com.example.ui.LastGameResultCard(result = sampleLastGame)
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/last_game_preview.png")
  }
}

