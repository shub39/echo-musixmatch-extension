package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult (
    val message: Message
)

@Serializable
data class Message (
    val body: Body
)

@Serializable
data class Body (
    @SerialName("track_list")
    val trackList: List<TrackList>
)

@Serializable
data class TrackList (
    val track: TrackClass
)

@Serializable
data class TrackClass (
    @SerialName("track_id")
    val trackID: Long,

    @SerialName("track_name")
    val trackName: String,

    @SerialName("artist_name")
    val artistName: String,
)