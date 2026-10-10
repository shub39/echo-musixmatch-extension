package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LyricsResult(
    val message: LyricsMessage? = null
)

@Serializable
data class LyricsMessage(
    val header: MessageHeader? = null,
    val body: MacroBody? = null
)

@Serializable
data class MessageHeader(
    @SerialName("status_code")
    val statusCode: Int? = null,
    val hint: String? = null
)

@Serializable
data class MacroBody(
    @SerialName("macro_calls")
    val macroCalls: MacroCalls? = null
)

@Serializable
data class MacroCalls(
    @SerialName("matcher.track.get")
    val matcherTrackGet: MatcherTrackGet? = null,

    @SerialName("track.lyrics.get")
    val trackLyricsGet: TrackLyricsGet? = null,

    @SerialName("track.subtitles.get")
    val trackSubtitlesGet: TrackSubtitlesGet? = null,

    @SerialName("track.richsync.get")
    val trackRichsyncGet: TrackRichsyncGet? = null
)

@Serializable
data class MatcherTrackGet(
    val message: MatcherTrackMessage? = null
)

@Serializable
data class MatcherTrackMessage(
    val header: MessageHeader? = null,
    val body: MatcherTrackBody? = null
)

@Serializable
data class MatcherTrackBody(
    val track: TrackItem? = null
)

@Serializable
data class TrackLyricsGet(
    val message: TrackLyricsGetMessage? = null
)

@Serializable
data class TrackLyricsGetMessage(
    val header: MessageHeader? = null,
    val body: TrackLyricsGetBody? = null
)

@Serializable
data class TrackLyricsGetBody(
    val lyrics: LyricsPayload? = null
)

@Serializable
data class LyricsPayload(
    @SerialName("lyrics_id")
    val lyricsID: Long? = null,

    @SerialName("lyrics_body")
    val lyricsBody: String? = null,

    @SerialName("lyrics_language")
    val lyricsLanguage: String? = null,

    @SerialName("restricted")
    val restricted: Int? = null,

    @SerialName("instrumental")
    val instrumental: Int? = null
)

@Serializable
data class TrackSubtitlesGet(
    val message: TrackSubtitlesGetMessage? = null
)

@Serializable
data class TrackSubtitlesGetMessage(
    val header: SubtitleHeader? = null,
    val body: TrackSubtitlesGetBody? = null
)

@Serializable
data class SubtitleHeader(
    @SerialName("status_code")
    val statusCode: Long? = null,

    val available: Long? = null,

    @SerialName("execute_time")
    val executeTime: Double? = null,

    val instrumental: Long? = null
)

@Serializable
data class TrackSubtitlesGetBody(
    @SerialName("subtitle_list")
    val subtitleList: List<SubtitleWrapper>? = null
)

@Serializable
data class SubtitleWrapper(
    val subtitle: SubtitlePayload? = null
)

@Serializable
data class SubtitlePayload(
    @SerialName("subtitle_id")
    val subtitleID: Long? = null,

    @SerialName("subtitle_body")
    val subtitleBody: String? = null,

    @SerialName("subtitle_language")
    val subtitleLanguage: String? = null
)

@Serializable
data class TrackRichsyncGet(
    val message: TrackRichsyncGetMessage? = null
)

@Serializable
data class TrackRichsyncGetMessage(
    val header: MessageHeader? = null,
    val body: TrackRichsyncGetBody? = null
)

@Serializable
data class TrackRichsyncGetBody(
    val richsync: RichsyncPayload? = null
)

@Serializable
data class RichsyncPayload(
    @SerialName("richsync_id")
    val richsyncID: Long? = null,

    @SerialName("richsync_body")
    val richsyncBody: String? = null,

    @SerialName("richsync_language")
    val richsyncLanguage: String? = null
)
