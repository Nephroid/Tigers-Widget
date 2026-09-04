package com.example.data.model

private val WHITESPACE_REGEX = Regex("\\s+")

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
    val words = upper.replace(".", " ").split(WHITESPACE_REGEX).toSet()
    val code = when {
        upper.contains("DODGER") || upper.contains("LA DODGER") || upper.contains("LOS ANGELES DODGER") || "LAD" in words -> "lad"
        upper.contains("ANGEL") || upper.contains("LA ANGEL") || upper.contains("LOS ANGELES ANGEL") || upper.contains("ANAHEIM") || "LAA" in words -> "laa"
        upper.contains("TIGER") || "DET" in words -> "det"
        upper.contains("GUARD") || upper.contains("INDIAN") || "CLE" in words -> "cle"
        upper.contains("TWIN") || "MIN" in words -> "min"
        upper.contains("ROYAL") || "KC" in words || "KCR" in words || upper.contains("KANSAS") -> "kc"
        upper.contains("WHITE SOX") || "CHW" in words || "CWS" in words || upper.contains("CHICAGO WHITE") -> "chw"
        upper.contains("RED SOX") || "BOS" in words || upper.contains("BOSTON") -> "bos"
        upper.contains("BLUE JAY") || "TOR" in words || upper.contains("TORONTO") -> "tor"
        upper.contains("YANKE") || "NYY" in words -> "nyy"
        upper.contains("MET") || "NYM" in words -> "nym"
        upper.contains("ORIOLE") || "BAL" in words || upper.contains("BALTIMORE") -> "bal"
        upper.contains("RAY") || "TB" in words || "TBR" in words || upper.contains("TAMPA") -> "tb"
        upper.contains("ASTRO") || "HOU" in words || upper.contains("HOUSTON") -> "hou"
        upper.contains("ATHLET") || "OAK" in words || "ATH" in words || upper.contains("OAKLAND") || "A'S" in words || "AS" in words -> "oak"
        upper.contains("MARINER") || "SEA" in words || upper.contains("SEATTLE") -> "sea"
        upper.contains("RANGER") || "TEX" in words || upper.contains("TEXAS") -> "tex"
        upper.contains("BRAVE") || "ATL" in words || upper.contains("ATLANTA") -> "atl"
        upper.contains("MARLIN") || "MIA" in words || upper.contains("MIAMI") -> "mia"
        upper.contains("PHILLI") || "PHI" in words || upper.contains("PHILADELPHIA") -> "phi"
        upper.contains("NATIONAL") || "WSH" in words || "WAS" in words || upper.contains("WASHINGTON") -> "wsh"
        upper.contains("CUB") || "CHC" in words || upper.contains("CHICAGO CUBS") -> "chc"
        upper.contains("RED") || "CIN" in words || upper.contains("CINCINNATI") -> "cin"
        upper.contains("BREWER") || "MIL" in words || upper.contains("MILWAUKEE") -> "mil"
        upper.contains("PIRATE") || "PIT" in words || upper.contains("PITTSBURGH") -> "pit"
        upper.contains("CARDINAL") || "STL" in words || upper.contains("ST. LOUIS") || upper.contains("ST LOUIS") -> "stl"
        upper.contains("DIAMONDBACK") || upper.contains("D-BACK") || "ARI" in words || upper.contains("ARIZONA") -> "ari"
        upper.contains("ROCKIE") || "COL" in words || upper.contains("COLORADO") -> "col"
        upper.contains("PADRE") || "SD" in words || "SDP" in words || upper.contains("SAN DIEGO") -> "sd"
        upper.contains("GIANT") || "SF" in words || "SFG" in words || upper.contains("SAN FRANCISCO") -> "sf"
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
