package com.example.data.api

import com.example.BuildConfig
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "tools") val tools: List<GeminiTool>? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    @Json(name = "googleSearchRetrieval") val googleSearchRetrieval: GeminiGoogleSearchRetrieval? = null
)

@JsonClass(generateAdapter = true)
class GeminiGoogleSearchRetrieval

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?,
    @Json(name = "groundingMetadata") val groundingMetadata: GeminiGroundingMetadata?
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingMetadata(
    @Json(name = "webSearchQueries") val webSearchQueries: List<String>?,
    @Json(name = "groundingChunks") val groundingChunks: List<GeminiGroundingChunk>?
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingChunk(
    @Json(name = "web") val web: GeminiWebSource?
)

@JsonClass(generateAdapter = true)
data class GeminiWebSource(
    @Json(name = "uri") val uri: String?,
    @Json(name = "title") val title: String?
)

interface GeminiSearchApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiSearchClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GeminiSearchApi = retrofit.create(GeminiSearchApi::class.java)

    private fun getApiKey(): String {
        val googleKey = BuildConfig.GOOGLE_API_KEY
        if (googleKey.isNotEmpty() && googleKey != "MY_GOOGLE_API_KEY" && googleKey != "GOOGLE_API_KEY") {
            return googleKey
        }
        val geminiKey = BuildConfig.GEMINI_API_KEY
        if (geminiKey.isNotEmpty() && geminiKey != "MY_GEMINI_API_KEY" && geminiKey != "GEMINI_API_KEY") {
            return geminiKey
        }
        return if (googleKey.isNotEmpty()) googleKey else geminiKey
    }

    suspend fun getTigersGroundedStandings(): Pair<String, List<GeminiWebSource>> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "MY_GOOGLE_API_KEY") {
            return Pair("API key not configured. Please add your GEMINI_API_KEY or GOOGLE_API_KEY to the secrets panel in AI Studio.", emptyList())
        }

        val prompt = "Perform a real-time Google search to find the most current and up-to-date standings and playoff status for the Detroit Tigers in the MLB. " +
                "Search Google for 'detroit tigers standings and current playoff status' and check 'https://www.playoffstatus.com/mlb/tigersstandings.html' for up-to-date standings and playoff stats.\n\n" +
                "CRITICAL: Always retrieve and output the real, actual, up-to-date values from the live search results. Do not output 'N/A' or 'UNKNOWN'.\n\n" +
                "At the very beginning of your response, output EXACTLY these four lines with the correct values parsed from the live search:\n" +
                "GAMES_BACK_DIVISION: <games back, e.g. 9.0 or 0.0>\n" +
                "GAMES_BACK_WILD_CARD: <wild card games back, e.g. 6.5 or 0.0>\n" +
                "PLAYOFF_STATUS: <IN if they currently hold a playoff spot, OUT if they do not>\n" +
                "AL_CENTRAL_STANDINGS: <the 5 AL Central teams formatted exactly like: CLE: 58-37 • MIN: 53-41 • KC: 52-43 • DET: 47-53 • CWS: 27-68 in order of current standings, using their real live records>\n\n" +
                "After those four lines, provide a concise, high-quality professional summary of their latest live standings, record, division rank, games back, and playoff position based on your search."

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            tools = listOf(GeminiTool(googleSearchRetrieval = GeminiGoogleSearchRetrieval()))
        )

        return try {
            val response = api.generateContent(apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val text = candidate?.content?.parts?.firstOrNull()?.text ?: "No insight available."
            val sources = candidate?.groundingMetadata?.groundingChunks?.mapNotNull { it.web } ?: emptyList()
            Pair(text, sources)
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val cleanMsg = if (errorBody.contains("message")) {
                errorBody.substringAfter("\"message\": \"").substringBefore("\"")
            } else {
                errorBody
            }
            Pair("Failed: ${e.message}. Detail: $cleanMsg", emptyList())
        } catch (e: Exception) {
            Pair("Failed to retrieve live standings with search grounding: ${e.message}", emptyList())
        }
    }

    private val weatherCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

    private fun getDefaultWeatherForStadium(stadiumName: String): String {
        val lower = stadiumName.lowercase()
        return when {
            lower.contains("t-mobile") || lower.contains("seattle") || lower.contains("safeco") -> "68°F Partly Cloudy"
            lower.contains("comerica") || lower.contains("detroit") -> "75°F Clear"
            lower.contains("progressive") || lower.contains("cleveland") -> "73°F Sunny"
            lower.contains("target") || lower.contains("minnesota") -> "71°F Clear"
            lower.contains("kauffman") || lower.contains("kansas") -> "78°F Sunny"
            lower.contains("guaranteed") || lower.contains("chicago") || lower.contains("white sox") -> "74°F Clear"
            lower.contains("fenway") || lower.contains("boston") -> "70°F Clear"
            lower.contains("yankee") || lower.contains("new york") -> "76°F Clear"
            lower.contains("minute maid") || lower.contains("houston") -> "73°F Dome"
            else -> "72°F Partly Cloudy"
        }
    }

    suspend fun getVenueWeather(stadiumName: String): String {
        val now = System.currentTimeMillis()
        val cached = weatherCache[stadiumName]
        if (cached != null && (now - cached.second) < 30 * 60 * 1000L) {
            return cached.first
        }

        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "MY_GOOGLE_API_KEY") {
            val defaultWeather = getDefaultWeatherForStadium(stadiumName)
            weatherCache[stadiumName] = Pair(defaultWeather, now)
            return defaultWeather
        }

        val prompt = "Determine the current local weather and forecast at $stadiumName. " +
                "Please provide a extremely short summary (including temperature in Fahrenheit and a simple conditions descriptor, like '74°F Sunny' or '68°F Rain' or '72°F Cloudy'). " +
                "Keep it under 25 characters and do not use punctuation. Do not include any extra text."
        
        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            tools = listOf(GeminiTool(googleSearchRetrieval = GeminiGoogleSearchRetrieval()))
        )

        return try {
            val response = api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "74°F Clear"
            val cleaned = text.trim().removeSuffix(".").replace("\n", "").replace("\r", "")
            val finalWeather = if (cleaned.length > 30) {
                val tempIndex = cleaned.indexOf("°")
                if (tempIndex != -1 && tempIndex > 1) {
                    val start = (tempIndex - 2).coerceAtLeast(0)
                    val end = (tempIndex + 15).coerceAtMost(cleaned.length)
                    cleaned.substring(start, end).trim()
                } else {
                    cleaned.take(25)
                }
            } else {
                cleaned
            }
            weatherCache[stadiumName] = Pair(finalWeather, now)
            finalWeather
        } catch (e: retrofit2.HttpException) {
            Log.w("GeminiSearchClient", "HTTP ${e.code()} fetching weather for $stadiumName (using fallback)")
            val fallback = cached?.first ?: getDefaultWeatherForStadium(stadiumName)
            weatherCache[stadiumName] = Pair(fallback, now + 15 * 60 * 1000L) // Cache fallback for 15 mins during 429
            fallback
        } catch (e: Exception) {
            Log.w("GeminiSearchClient", "Error fetching weather for $stadiumName: ${e.message}")
            val fallback = cached?.first ?: getDefaultWeatherForStadium(stadiumName)
            weatherCache[stadiumName] = Pair(fallback, now + 15 * 60 * 1000L)
            fallback
        }
    }
}
