package com.example

import com.example.data.model.getTeamLogoUrl
import com.example.ui.getTeamAbbreviation
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDodgersVsAngelsLogoResolution() {
    // Crucial bug fix verification: "Los Angeles Dodgers" must map to "lad", NOT "laa"
    val dodgersFull = getTeamLogoUrl("Los Angeles Dodgers")
    assertTrue("Expected lad.png for Los Angeles Dodgers, got: $dodgersFull", dodgersFull.endsWith("lad.png"))

    val dodgersShort = getTeamLogoUrl("LA Dodgers")
    assertTrue("Expected lad.png for LA Dodgers, got: $dodgersShort", dodgersShort.endsWith("lad.png"))

    val dodgersNickname = getTeamLogoUrl("Dodgers")
    assertTrue("Expected lad.png for Dodgers, got: $dodgersNickname", dodgersNickname.endsWith("lad.png"))

    val dodgersId = getTeamLogoUrl("Unknown Team", 119)
    assertTrue("Expected lad.png for Dodgers teamId 119, got: $dodgersId", dodgersId.endsWith("lad.png"))

    // Angels
    val angelsFull = getTeamLogoUrl("Los Angeles Angels")
    assertTrue("Expected laa.png for Los Angeles Angels, got: $angelsFull", angelsFull.endsWith("laa.png"))

    val angelsShort = getTeamLogoUrl("LA Angels")
    assertTrue("Expected laa.png for LA Angels, got: $angelsShort", angelsShort.endsWith("laa.png"))

    val angelsNickname = getTeamLogoUrl("Angels")
    assertTrue("Expected laa.png for Angels, got: $angelsNickname", angelsNickname.endsWith("laa.png"))

    val angelsId = getTeamLogoUrl("Unknown Team", 108)
    assertTrue("Expected laa.png for Angels teamId 108, got: $angelsId", angelsId.endsWith("laa.png"))
  }

  @Test
  fun testDodgersVsAngelsAbbreviation() {
    assertEquals("LAD", getTeamAbbreviation("Los Angeles Dodgers"))
    assertEquals("LAD", getTeamAbbreviation("LA Dodgers"))
    assertEquals("LAD", getTeamAbbreviation("Dodgers"))

    assertEquals("LAA", getTeamAbbreviation("Los Angeles Angels"))
    assertEquals("LAA", getTeamAbbreviation("LA Angels"))
    assertEquals("LAA", getTeamAbbreviation("Angels"))
  }

  @Test
  fun testAll30MlbTeamLogos() {
    val mlbTeams = mapOf(
      "Detroit Tigers" to "det",
      "Cleveland Guardians" to "cle",
      "Minnesota Twins" to "min",
      "Kansas City Royals" to "kc",
      "Chicago White Sox" to "chw",
      "New York Yankees" to "nyy",
      "Boston Red Sox" to "bos",
      "Baltimore Orioles" to "bal",
      "Tampa Bay Rays" to "tb",
      "Toronto Blue Jays" to "tor",
      "Houston Astros" to "hou",
      "Seattle Mariners" to "sea",
      "Texas Rangers" to "tex",
      "Oakland Athletics" to "oak",
      "Los Angeles Angels" to "laa",
      "Los Angeles Dodgers" to "lad",
      "San Francisco Giants" to "sf",
      "San Diego Padres" to "sd",
      "Arizona Diamondbacks" to "ari",
      "Colorado Rockies" to "col",
      "Chicago Cubs" to "chc",
      "Cincinnati Reds" to "cin",
      "Milwaukee Brewers" to "mil",
      "Pittsburgh Pirates" to "pit",
      "St. Louis Cardinals" to "stl",
      "Atlanta Braves" to "atl",
      "Miami Marlins" to "mia",
      "New York Mets" to "nym",
      "Philadelphia Phillies" to "phi",
      "Washington Nationals" to "wsh"
    )

    mlbTeams.forEach { (teamName, expectedCode) ->
      val logoUrl = getTeamLogoUrl(teamName)
      assertTrue(
        "For '$teamName', expected code '$expectedCode', but got '$logoUrl'",
        logoUrl.endsWith("$expectedCode.png")
      )
    }
  }
}
