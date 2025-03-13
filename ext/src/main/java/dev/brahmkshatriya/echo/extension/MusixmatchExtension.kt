package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.data.Lyric
import dev.brahmkshatriya.echo.extension.data.LyricsResult
import dev.brahmkshatriya.echo.extension.data.SearchResult
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class MusixmatchExtension : ExtensionClient, LyricsClient, LyricsSearchClient {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenManager = TokenManager(client, json)

    override suspend fun onExtensionSelected() {}

    override val settingItems: List<Setting> = emptyList()

    private lateinit var setting: Settings
    override fun setSettings(settings: Settings) {
        setting = settings
    }

    override fun searchTrackLyrics(
        clientId: String,
        track: Track
    ): PagedData<Lyrics> = PagedData.Single {
        return@Single toLyricsList("${track.title} ${track.artists.firstOrNull()?.name ?: ""}".trim())
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics {
        val request = Request.Builder()
            .addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            .addHeader("Cookie", "mxm_bab=AB")
            .url(getLyricsQuery(lyrics.id))
            .build()
        val response = client.newCall(request).await()
        val result = json.decodeFromString<LyricsResult>(response.body.string())

        return lyrics.copy(
            lyrics = if (result.message.body.macroCalls.trackSubtitlesGet.message.header.available == 1L) {
                parseSyncedLyrics(result.message.body.macroCalls.trackSubtitlesGet.message.body.subtitleList.first().subtitle.subtitleBody)
            } else {
                Lyrics.Simple(result.message.body.macroCalls.trackLyricsGet.message.body.lyrics.lyricsBody)
            }
        )
    }

    override fun searchLyrics(query: String): PagedData<Lyrics> = PagedData.Single {
        return@Single toLyricsList(query)
    }

    private suspend fun toLyricsList(query: String): List<Lyrics> {
        val request = Request.Builder()
            .addHeader(
                "user-agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
            )
            .addHeader("Cookie", "mxm_bab=AB")
            .url(getSearchQuery(query))
            .build()
        val response = client.newCall(request).await()
        val tracks = json.decodeFromString<SearchResult>(response.body.string())

        return tracks.message.body.trackList.map {
            Lyrics(
                id = it.track.trackID.toString(),
                title = it.track.trackName,
                subtitle = it.track.artistName
            )
        }
    }

    private fun getSearchQuery(query: String): String {
        return (API_URL + "track.search").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("usertoken", tokenManager.token)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("subtitle_format", "mxm")
            ?.addQueryParameter("namespace", "lyrics_richsynched")
            ?.addQueryParameter("app_id", "web-desktop-app-v1.0")
            ?.addQueryParameter("q", query.trim())
            ?.build()
            ?.toString()
            ?: throw RuntimeException("Cant build URL")
    }

    private fun getLyricsQuery(trackId: String): String {
        return (API_URL + "macro.subtitles.get").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("track_id", trackId)
            ?.addQueryParameter("q_track", "*")
            ?.addQueryParameter("usertoken", tokenManager.token)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("subtitle_format", "mxm")
            ?.addQueryParameter("namespace", "lyrics_richsynched")
            ?.addQueryParameter("app_id", "web-desktop-app-v1.0")
            ?.toString()
            ?: throw RuntimeException("Cant build URL")
    }

    private fun parseSyncedLyrics(jsonString: String): Lyrics.Timed {
        val lyrics: List<Lyric> = Json.decodeFromString(jsonString)

        val items = mutableListOf<Lyrics.Item>()

        for (i in lyrics.indices) {
            val currentLyric = lyrics[i]
            val startTime = (currentLyric.time.total * 1000).toLong()

            val endTime = if (i < lyrics.size - 1) {
                (lyrics[i + 1].time.total * 1000).toLong()
            } else {
                startTime + 3000
            }

            items.add(Lyrics.Item(currentLyric.text, startTime, endTime))
        }

        return Lyrics.Timed(items)
    }

    companion object {
        private const val API_URL = "https://apic-desktop.musixmatch.com/ws/1.1/"
    }
}