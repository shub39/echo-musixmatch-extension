package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.Serializable

@Serializable
data class Lyric(
    val text: String? = "",
    val time: LyricTime? = null
)

@Serializable
data class LyricTime(
    val total: Double? = 0.0,
    val minutes: Int? = 0,
    val seconds: Int? = 0,
    val hundredths: Int? = 0
)

@Serializable
data class RichsyncLine(
    val ts: Double? = 0.0,
    val te: Double? = 0.0,
    val x: String? = "",
    val l: List<RichsyncWord>? = null
)

@Serializable
data class RichsyncWord(
    val c: String? = "",
    val o: Double? = 0.0
)
