package com.example.data.model

fun getTeamLogoUrl(teamName: String, teamId: Int? = null): String {
    if (teamId != null && teamId > 0) {
        val codeById = when (teamId) {
            110 -> "bal"
            111 -> "bos"
            147 -> "nyy"
            139 -> "tb"
            141 -> "tor"
            145 -> "chw"
            114 -> "cle"
            116 -> "det"
            118 -> "kc"
            142 -> "min"
            117 -> "hou"
            108 -> "laa"
            133 -> "oak"
            136 -> "sea"
            140 -> "tex"
            144 -> "atl"
            146 -> "mia"
            121 -> "nym"
            143 -> "phi"
            120 -> "wsh"
            112 -> "chc"
            113 -> "cin"
            158 -> "mil"
            134 -> "pit"
            138 -> "stl"
            109 -> "ari"
            115 -> "col"
            119 -> "lad"
            135 -> "sd"
            137 -> "sf"
            else -> null
        }
        if (codeById != null) {
            return "https://a.espncdn.com/i/teamlogos/mlb/500/$codeById.png"
        }
    }

    val upper = teamName.uppercase().trim()
    val code = when {
        upper.contains("TIGER") || upper.contains("DET") -> "det"
        upper.contains("GUARD") || upper.contains("CLE") || upper.contains("INDIAN") -> "cle"
        upper.contains("TWIN") || upper.contains("MIN") -> "min"
        upper.contains("ROYAL") || upper.contains("KANSAS") || upper.contains("KC") -> "kc"
        upper.contains("WHITE SOX") || upper.contains("CHW") || upper.contains("CWS") -> "chw"
        upper.contains("RED SOX") || upper.contains("BOS") || upper.contains("BOSTON") -> "bos"
        upper.contains("BLUE JAY") || upper.contains("TOR") || upper.contains("TORONTO") -> "tor"
        upper.contains("YANKE") || upper.contains("NYY") -> "nyy"
        upper.contains("MET") || upper.contains("NYM") -> "nym"
        upper.contains("ORIOLE") || upper.contains("BAL") || upper.contains("BALTIMORE") -> "bal"
        upper.contains("RAY") || upper.contains("TAMPA") || upper.contains("TB") -> "tb"
        upper.contains("ASTRO") || upper.contains("HOU") || upper.contains("HOUSTON") -> "hou"
        upper.contains("ATHLET") || upper.contains("OAK") || upper.contains("ATH") -> "oak"
        upper.contains("ANGEL") || upper.contains("LAA") || upper.contains("ANAHEIM") -> "laa"
        upper.contains("MARINER") || upper.contains("SEA") || upper.contains("SEATTLE") -> "sea"
        upper.contains("RANGER") || upper.contains("TEX") || upper.contains("TEXAS") -> "tex"
        upper.contains("BRAVE") || upper.contains("ATL") || upper.contains("ATLANTA") -> "atl"
        upper.contains("MARLIN") || upper.contains("MIA") || upper.contains("MIAMI") -> "mia"
        upper.contains("PHILLI") || upper.contains("PHI") || upper.contains("PHILADELPHIA") -> "phi"
        upper.contains("NATIONAL") || upper.contains("WSH") || upper.contains("WAS") -> "wsh"
        upper.contains("CUB") || upper.contains("CHC") || upper.contains("CHICAGO CUBS") -> "chc"
        upper.contains("RED") || upper.contains("CIN") || upper.contains("CINCINNATI") -> "cin"
        upper.contains("BREWER") || upper.contains("MIL") || upper.contains("MILWAUKEE") -> "mil"
        upper.contains("PIRATE") || upper.contains("PIT") || upper.contains("PITTSBURGH") -> "pit"
        upper.contains("CARDINAL") || upper.contains("STL") || upper.contains("ST. LOUIS") -> "stl"
        upper.contains("DIAMONDBACK") || upper.contains("D-BACK") || upper.contains("ARI") || upper.contains("ARIZONA") -> "ari"
        upper.contains("ROCKIE") || upper.contains("COL") || upper.contains("COLORADO") -> "col"
        upper.contains("DODGER") || upper.contains("LAD") || upper.contains("LOS ANGELES DODGERS") -> "lad"
        upper.contains("PADRE") || upper.contains("SD") || upper.contains("SAN DIEGO") -> "sd"
        upper.contains("GIANT") || upper.contains("SF") || upper.contains("SAN FRANCISCO") -> "sf"
        else -> "det" // Default to Tigers logo
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
