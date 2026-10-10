package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.data.Lyric
import dev.brahmkshatriya.echo.extension.data.LyricsResult
import dev.brahmkshatriya.echo.extension.data.RichsyncLine
import dev.brahmkshatriya.echo.extension.data.SearchResult
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

class MusixmatchExtension : ExtensionClient, LyricsClient, LyricsSearchClient {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val tokenManager = TokenManager(client, json)

    override suspend fun onExtensionSelected() {}

    private var setting: Settings? = null

    override suspend fun getSettingItems(): List<Setting> {
        return listOf(
            SettingTextInput(
                title = "Musixmatch User Token",
                key = TokenManager.KEY_USER_TOKEN,
                summary = "Optional: Custom Musixmatch user token if dynamic token is rate-limited",
                defaultValue = ""
            )
        )
    }

    override fun setSettings(settings: Settings) {
        setting = settings
        tokenManager.settings = settings
    }

    override suspend fun searchTrackLyrics(
        clientId: String,
        track: Track
    ): Feed<Lyrics> {
        val cleanTitle = cleanTrackTitle(track.title)
        val artist = track.artists.firstOrNull()?.name?.trim().orEmpty()

        // 1. Try search with title and artist
        val query = if (artist.isNotBlank()) "$cleanTitle $artist" else cleanTitle
        val list = toLyricsList(query, title = cleanTitle, artist = artist)
        if (list.isNotEmpty()) {
            return list.toFeed()
        }

        // 2. Fallback to raw title
        if (cleanTitle != track.title) {
            val fallbackList = toLyricsList(track.title, title = track.title, artist = artist)
            if (fallbackList.isNotEmpty()) {
                return fallbackList.toFeed()
            }
        }

        return emptyList<Lyrics>().toFeed()
    }

    override suspend fun searchLyrics(query: String): Feed<Lyrics> {
        return toLyricsList(query.trim()).toFeed()
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics {
        return loadLyricsInternal(lyrics, retry = true)
    }

    private suspend fun loadLyricsInternal(lyrics: Lyrics, retry: Boolean): Lyrics {
        val trackId = lyrics.id
        val url = getLyricsQuery(trackId)

        val request = Request.Builder()
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Cookie", "AWSELBCORS=0; AWSELB=0")
            .url(url)
            .build()

        val response = client.newCall(request).await()
        val bodyString = response.body.string()
        val result = json.decodeFromString<LyricsResult>(bodyString)

        val statusCode = result.message?.header?.statusCode ?: response.code
        if (statusCode == 401 || result.message?.header?.hint == "renew") {
            if (retry) {
                tokenManager.invalidateToken()
                return loadLyricsInternal(lyrics, retry = false)
            }
        }

        val macro = result.message?.body?.macroCalls
            ?: throw RuntimeException("Failed to load lyrics: macro calls body missing")

        // 1. Try RichSync (Word-by-word)
        val richsyncBody = macro.trackRichsyncGet?.message?.body?.richsync?.richsyncBody
        if (!richsyncBody.isNullOrBlank()) {
            val wordByWord = parseRichsyncLyrics(richsyncBody)
            if (wordByWord != null) {
                return lyrics.copy(lyrics = wordByWord)
            }
        }

        // 2. Try Synced Subtitles (Line-by-line)
        val subtitlesGet = macro.trackSubtitlesGet
        val subtitleList = subtitlesGet?.message?.body?.subtitleList
        if (!subtitleList.isNullOrEmpty()) {
            val sub = subtitleList.firstOrNull()?.subtitle?.subtitleBody
            if (!sub.isNullOrBlank()) {
                val timed = parseSyncedLyrics(sub)
                if (timed != null) {
                    return lyrics.copy(lyrics = timed)
                }
            }
        }

        // 3. Try Plain Lyrics
        val lyricsGet = macro.trackLyricsGet
        val lyricsPayload = lyricsGet?.message?.body?.lyrics
        if (lyricsPayload != null) {
            if (lyricsPayload.instrumental == 1 || lyricsPayload.lyricsBody?.trim()?.equals("Instrumental", ignoreCase = true) == true) {
                return lyrics.copy(lyrics = Lyrics.Simple("♪ Instrumental ♪"))
            }

            val body = cleanLyricsBody(lyricsPayload.lyricsBody.orEmpty())
            if (body.isNotBlank() && !isHoneypot(body)) {
                return lyrics.copy(lyrics = Lyrics.Simple(body))
            }
        }

        throw RuntimeException("Lyrics not available for this track")
    }

    private suspend fun toLyricsList(query: String, title: String? = null, artist: String? = null, retry: Boolean = true): List<Lyrics> {
        val request = Request.Builder()
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Cookie", "AWSELBCORS=0; AWSELB=0")
            .url(getSearchQuery(query, title, artist))
            .build()

        val response = client.newCall(request).await()
        val bodyString = response.body.string()
        val searchResult = json.decodeFromString<SearchResult>(bodyString)

        val statusCode = searchResult.message?.header?.statusCode ?: response.code
        if (statusCode == 401 || searchResult.message?.header?.hint == "renew") {
            if (retry) {
                tokenManager.invalidateToken()
                return toLyricsList(query, title, artist, retry = false)
            }
        }

        val tracks = searchResult.message?.body?.trackList.orEmpty()

        val list = mutableListOf<Lyrics>()
        for (item in tracks) {
            val t = item.track ?: continue
            val trackId = t.trackID ?: continue
            val trackName = t.trackName.orEmpty()
            val artistName = t.artistName.orEmpty()

            // Filter out honeypot / dummy tracks unless explicitly searched
            if (isHoneypotTrack(trackName, artistName) && !query.contains("casual", ignoreCase = true) && !query.contains("doja", ignoreCase = true)) {
                continue
            }

            list.add(
                Lyrics(
                    id = trackId.toString(),
                    title = trackName,
                    subtitle = artistName
                )
            )
        }

        return list
    }

    private fun getSearchQuery(query: String, title: String? = null, artist: String? = null): String {
        val builder = (API_URL + "track.search").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("usertoken", tokenManager.token)
            ?.addQueryParameter("app_id", APP_ID)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("page_size", "20")
            ?.addQueryParameter("s_track_rating", "desc")
            ?: throw RuntimeException("Cannot build search URL")

        if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            builder.addQueryParameter("q_track", title.trim())
            builder.addQueryParameter("q_artist", artist.trim())
        } else {
            builder.addQueryParameter("q_track_artist", query.trim())
        }

        return builder.build().toString()
    }

    private fun getLyricsQuery(trackId: String): String {
        return (API_URL + "macro.subtitles.get").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("track_id", trackId)
            ?.addQueryParameter("usertoken", tokenManager.token)
            ?.addQueryParameter("app_id", APP_ID)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("subtitle_format", "mxm")
            ?.addQueryParameter("namespace", "lyrics_richsynched")
            ?.build()
            ?.toString()
            ?: throw RuntimeException("Cannot build lyrics URL")
    }

    private fun parseRichsyncLyrics(jsonString: String): Lyrics.WordByWord? {
        return try {
            val lines: List<RichsyncLine> = json.decodeFromString(jsonString)
            val items = lines.mapNotNull { line ->
                val lineStart = ((line.ts ?: 0.0) * 1000).toLong()
                val lineEnd = ((line.te ?: 0.0) * 1000).toLong()
                val words = line.l.orEmpty()
                if (words.isEmpty()) {
                    if (!line.x.isNullOrBlank()) {
                        listOf(Lyrics.Item(line.x, lineStart, lineEnd))
                    } else null
                } else {
                    words.map { w ->
                        val wordStart = lineStart + ((w.o ?: 0.0) * 1000).toLong()
                        Lyrics.Item(w.c.orEmpty(), wordStart, lineEnd)
                    }
                }
            }
            if (items.isNotEmpty()) Lyrics.WordByWord(items) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSyncedLyrics(raw: String): Lyrics.Timed? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("[") && trimmed.contains("\"text\"")) {
            // MXM JSON format
            return try {
                val lyrics: List<Lyric> = json.decodeFromString(trimmed)
                val items = mutableListOf<Lyrics.Item>()
                for (i in lyrics.indices) {
                    val current = lyrics[i]
                    val totalSec = current.time?.total ?: 0.0
                    val startTime = (totalSec * 1000).toLong()
                    val endTime = if (i < lyrics.size - 1) {
                        ((lyrics[i + 1].time?.total ?: (totalSec + 3.0)) * 1000).toLong()
                    } else {
                        startTime + 3000
                    }
                    val text = current.text.orEmpty().ifBlank { "♪" }
                    items.add(Lyrics.Item(text, startTime, endTime))
                }
                if (items.isNotEmpty()) Lyrics.Timed(items) else null
            } catch (_: Exception) {
                null
            }
        }

        // Standard LRC format
        return parseLrcLyrics(trimmed)
    }

    private fun parseLrcLyrics(lrc: String): Lyrics.Timed? {
        val pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
        val lines = lrc.lines()
        val parsed = mutableListOf<Pair<Long, String>>()

        for (line in lines) {
            val matcher = pattern.matcher(line.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val frac = matcher.group(3)?.toLongOrNull() ?: 0L
                val text = matcher.group(4)?.trim().orEmpty()
                val ms = min * 60000 + sec * 1000 + if (matcher.group(3)?.length == 2) frac * 10 else frac
                parsed.add(Pair(ms, text.ifBlank { "♪" }))
            }
        }

        if (parsed.isEmpty()) return null

        val items = mutableListOf<Lyrics.Item>()
        for (i in parsed.indices) {
            val current = parsed[i]
            val startTime = current.first
            val endTime = if (i < parsed.size - 1) parsed[i + 1].first else startTime + 3000
            items.add(Lyrics.Item(current.second, startTime, endTime))
        }

        return Lyrics.Timed(items)
    }

    private fun cleanLyricsBody(body: String): String {
        return body.lines()
            .filterNot { line ->
                val l = line.trim()
                l.startsWith("*******") ||
                l.contains("NOT for Commercial use", ignoreCase = true) ||
                l.contains("Musixmatch", ignoreCase = true) && l.contains("commercial", ignoreCase = true)
            }
            .joinToString("\n")
            .trim()
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\s*[\\[(](official\\s*(video|audio|music\\s*video|lyric\\s*video)?|remastered|remaster|hd|4k|audio|lyrics)[\\])]"), "")
            .replace(Regex("(?i)\\s*(feat|ft|featuring)\\.?\\s+.*$"), "")
            .trim()
    }

    private fun isHoneypot(lyrics: String): Boolean {
        return lyrics.contains("Wob gopini den", ignoreCase = true) ||
               lyrics.contains("Tefe woxica fero", ignoreCase = true)
    }

    private fun isHoneypotTrack(trackName: String, artistName: String): Boolean {
        return (trackName.equals("Casual", ignoreCase = true) && artistName.equals("Doja Cat", ignoreCase = true)) ||
               (trackName.equals("NOKIA", ignoreCase = true) && artistName.equals("Drake", ignoreCase = true)) ||
               (artistName.contains("Calming Cat Music", ignoreCase = true)) ||
               (artistName.contains("Cat Music", ignoreCase = true))
    }

    companion object {
        private const val API_URL = "https://apic.musixmatch.com/ws/1.1/"
        private const val APP_ID = "mac-ios-v2.0"
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
