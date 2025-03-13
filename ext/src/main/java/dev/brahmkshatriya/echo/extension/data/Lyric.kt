package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.Serializable

@Serializable
data class Lyric(
    val text: String,
    val time: Time
)

@Serializable
data class Time(
    val total: Double,
    val minutes: Int,
    val seconds: Int,
    val hundredths: Int
)
