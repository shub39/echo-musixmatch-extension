package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

open class EmptyArrayAsObjectSerializer<T : Any>(serializer: KSerializer<T>) :
    JsonTransformingSerializer<T>(serializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return if (element is JsonArray) JsonObject(emptyMap()) else element
    }
}

@Serializable
data class LyricsResult(
    val message: LyricsMessage? = null
)

@Serializable
data class LyricsMessage(
    val header: MessageHeader? = null,
    @Serializable(with = MacroBodySerializer::class)
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

object MacroBodySerializer : EmptyArrayAsObjectSerializer<MacroBody>(MacroBody.serializer())

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
    @Serializable(with = MatcherTrackBodySerializer::class)
    val body: MatcherTrackBody? = null
)

@Serializable
data class MatcherTrackBody(
    @Serializable(with = TrackItemSerializer::class)
    val track: TrackItem? = null
)

object MatcherTrackBodySerializer : EmptyArrayAsObjectSerializer<MatcherTrackBody>(MatcherTrackBody.serializer())

@Serializable
data class TrackLyricsGet(
    val message: TrackLyricsGetMessage? = null
)

@Serializable
data class TrackLyricsGetMessage(
    val header: MessageHeader? = null,
    @Serializable(with = TrackLyricsGetBodySerializer::class)
    val body: TrackLyricsGetBody? = null
)

@Serializable
data class TrackLyricsGetBody(
    @Serializable(with = LyricsPayloadSerializer::class)
    val lyrics: LyricsPayload? = null
)

object TrackLyricsGetBodySerializer : EmptyArrayAsObjectSerializer<TrackLyricsGetBody>(TrackLyricsGetBody.serializer())

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

object LyricsPayloadSerializer : EmptyArrayAsObjectSerializer<LyricsPayload>(LyricsPayload.serializer())

@Serializable
data class TrackSubtitlesGet(
    val message: TrackSubtitlesGetMessage? = null
)

@Serializable
data class TrackSubtitlesGetMessage(
    val header: SubtitleHeader? = null,
    @Serializable(with = TrackSubtitlesGetBodySerializer::class)
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

object TrackSubtitlesGetBodySerializer : EmptyArrayAsObjectSerializer<TrackSubtitlesGetBody>(TrackSubtitlesGetBody.serializer())

@Serializable
data class SubtitleWrapper(
    @Serializable(with = SubtitlePayloadSerializer::class)
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

object SubtitlePayloadSerializer : EmptyArrayAsObjectSerializer<SubtitlePayload>(SubtitlePayload.serializer())

@Serializable
data class TrackRichsyncGet(
    val message: TrackRichsyncGetMessage? = null
)

@Serializable
data class TrackRichsyncGetMessage(
    val header: MessageHeader? = null,
    @Serializable(with = TrackRichsyncGetBodySerializer::class)
    val body: TrackRichsyncGetBody? = null
)

@Serializable
data class TrackRichsyncGetBody(
    @Serializable(with = RichsyncPayloadSerializer::class)
    val richsync: RichsyncPayload? = null
)

object TrackRichsyncGetBodySerializer : EmptyArrayAsObjectSerializer<TrackRichsyncGetBody>(TrackRichsyncGetBody.serializer())

@Serializable
data class RichsyncPayload(
    @SerialName("richsync_id")
    val richsyncID: Long? = null,

    @SerialName("richsync_body")
    val richsyncBody: String? = null,

    @SerialName("richsync_language")
    val richsyncLanguage: String? = null
)

object RichsyncPayloadSerializer : EmptyArrayAsObjectSerializer<RichsyncPayload>(RichsyncPayload.serializer())
