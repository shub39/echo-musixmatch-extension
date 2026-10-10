package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val message: SearchMessage? = null
)

@Serializable
data class SearchMessage(
    val header: SearchHeader? = null,
    val body: SearchBody? = null
)

@Serializable
data class SearchHeader(
    @SerialName("status_code")
    val statusCode: Int? = null,
    val hint: String? = null
)

@Serializable
data class SearchBody(
    @SerialName("track_list")
    val trackList: List<TrackListWrapper>? = null
)

@Serializable
data class TrackListWrapper(
    val track: TrackItem? = null
)

@Serializable
data class TrackItem(
    @SerialName("track_id")
    val trackID: Long? = null,

    @SerialName("track_name")
    val trackName: String? = null,

    @SerialName("artist_name")
    val artistName: String? = null,

    @SerialName("album_name")
    val albumName: String? = null,

    @SerialName("has_lyrics")
    val hasLyrics: Int? = null,

    @SerialName("has_subtitles")
    val hasSubtitles: Int? = null,

    @SerialName("has_richsync")
    val hasRichsync: Int? = null,

    @SerialName("instrumental")
    val instrumental: Int? = null,

    @SerialName("restricted")
    val restricted: Int? = null
)
