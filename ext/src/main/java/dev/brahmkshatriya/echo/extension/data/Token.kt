package dev.brahmkshatriya.echo.extension.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Token(
    val message: TokenMessage? = null
)

@Serializable
data class TokenMessage(
    val header: TokenHeader? = null,
    @Serializable(with = TokenBodySerializer::class)
    val body: TokenBody? = null
)

@Serializable
data class TokenHeader(
    @SerialName("status_code")
    val statusCode: Int? = null,
    val hint: String? = null
)

@Serializable
data class TokenBody(
    @SerialName("user_token")
    val userToken: String? = null
)

object TokenBodySerializer : EmptyArrayAsObjectSerializer<TokenBody>(TokenBody.serializer())
