package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Token (
    val message: Message
) {
    @Serializable
    data class Message (
        val header: Header,
        val body: Body
    )

    @Serializable
    data class Header(
        @SerialName("status_code")
        val statusCode: Int,
        val hint: String? = null
    )

    @Serializable
    data class Body (
        @SerialName("user_token")
        val userToken: String,
    )
}