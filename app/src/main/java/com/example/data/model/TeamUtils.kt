package com.example.data.model

fun getTeamLogoUrl(teamName: String): String {
    val upper = teamName.uppercase()
    val code = when {
        upper.contains("TIGER") || upper.contains("DET") -> "det"
        upper.contains("GUARD") || upper.contains("CLE") -> "cle"
        upper.contains("TWIN") || upper.contains("MIN") -> "min"
        upper.contains("ROYAL") || upper.contains("KANSAS") || upper.contains("KC") -> "kc"
        upper.contains("WHITE SOX") || upper.contains("CHW") || upper.contains("CHICAGO WHITE") -> "chw"
        upper.contains("RED SOX") || upper.contains("BOS") -> "bos"
        upper.contains("YANKEES") || upper.contains("NYY") || upper.contains("YORK") -> "nyy"
        upper.contains("BLUE JAYS") || upper.contains("TOR") -> "tor"
        upper.contains("ORIOLE") || upper.contains("BAL") -> "bal"
        upper.contains("RAY") || upper.contains("TAMPA") || upper.contains("TB") -> "tb"
        upper.contains("ASTRO") || upper.contains("HOU") -> "hou"
        upper.contains("ATHLET") || upper.contains("OAK") -> "oak"
        upper.contains("ANGEL") || upper.contains("LAA") -> "laa"
        upper.contains("MARINER") || upper.contains("SEA") -> "sea"
        upper.contains("RANGER") || upper.contains("TEX") -> "tex"
        upper.contains("BRAVE") || upper.contains("ATL") -> "atl"
        upper.contains("MARLIN") || upper.contains("MIA") -> "mia"
        upper.contains("MET") || upper.contains("NYM") -> "nym"
        upper.contains("PHILLI") || upper.contains("PHI") -> "phi"
        upper.contains("NATIONAL") || upper.contains("WSH") -> "wsh"
        upper.contains("CUB") || upper.contains("CHC") -> "chc"
        upper.contains("RED") || upper.contains("CIN") -> "cin"
        upper.contains("BREWER") || upper.contains("MIL") -> "mil"
        upper.contains("PIRATE") || upper.contains("PIT") -> "pit"
        upper.contains("CARDINAL") || upper.contains("STL") -> "stl"
        upper.contains("DIAMONDBACK") || upper.contains("ARI") -> "ari"
        upper.contains("ROCKIE") || upper.contains("COL") -> "col"
        upper.contains("DODGER") || upper.contains("LAD") -> "lad"
        upper.contains("PADRE") || upper.contains("SD") -> "sd"
        upper.contains("GIANT") || upper.contains("SF") -> "sf"
        else -> "mlb" // Fallback to generic MLB logo
    }
    return "https://a.espncdn.com/i/teamlogos/mlb/500/$code.png"
}

fun getPitcherImageUrl(pitcherId: Int?): String? {
    return if (pitcherId != null && pitcherId > 0) {
        "https://img.mlbstatic.com/mlb-photos/image/upload/d_people:generic:headshot:67:current.png/w_426,q_auto:best/v1/people/$pitcherId/headshot/67/current"
    } else {
        null
    }
}
