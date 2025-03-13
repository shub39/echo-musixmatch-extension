package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LyricsResult (
    val message: TrackMessage
)

@Serializable
data class TrackMessage (
    val body: PurpleBody
)

@Serializable
data class PurpleBody (
    @SerialName("macro_calls")
    val macroCalls: MacroCalls
)

@Serializable
data class MacroCalls (
    @SerialName("track.lyrics.get")
    val trackLyricsGet: TrackLyricsGet,

    @SerialName("track.subtitles.get")
    val trackSubtitlesGet: TrackSubtitlesGet,
)

@Serializable
data class TrackLyricsGet (
    val message: TrackLyricsGetMessage
)

@Serializable
data class TrackLyricsGetMessage (
    val body: TentacledBody
)

@Serializable
data class TentacledBody (
    val lyrics: Lyrics
)

@Serializable
data class Lyrics (
    @SerialName("lyrics_id")
    val lyricsID: Long,

    @SerialName("lyrics_body")
    val lyricsBody: String,
)

@Serializable
data class TrackSubtitlesGet (
    val message: TrackSubtitlesGetMessage
)

@Serializable
data class TrackSubtitlesGetMessage (
    val header: FluffyHeader,
    val body: IndigoBody
)

@Serializable
data class IndigoBody (
    @SerialName("subtitle_list")
    val subtitleList: List<SubtitleList>
)

@Serializable
data class SubtitleList (
    val subtitle: Subtitle
)

@Serializable
data class Subtitle (
    @SerialName("subtitle_id")
    val subtitleID: Long,

    @SerialName("subtitle_body")
    val subtitleBody: String,
)

@Serializable
data class FluffyHeader (
    @SerialName("status_code")
    val statusCode: Long,

    val available: Long,

    @SerialName("execute_time")
    val executeTime: Double,

    val instrumental: Long
)