package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.extension.data.Token
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64

@Serializable
data class TokenPayload(val token: String, val expiresAt: Long)

@Suppress("NewApi")
class TokenManager(
    private val client: OkHttpClient,
    private val json: Json
) {
    private var payload: TokenPayload? = null

    val token: String
        get() {
            if (isValid()) {
                return payload!!.token
            }
            load()
            return payload!!.token
        }

    private fun isValid(): Boolean {
        return payload?.let { Instant.now().epochSecond < it.expiresAt } ?: false
    }

    private fun load() {
        val now = LocalDateTime.now()

        val params = mapOf(
            "format" to "json",
            "app_id" to APP_ID
        )

        val queryString = params.entries.joinToString("&") { (k, v) -> "$k=$v" }
        val url = "$API_URL/token.get?$queryString"

        val signature = getApiSignature(url, now)

        val fullUrl = url.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("signature", signature)
            ?.addQueryParameter("signature_protocol", "sha1")
            ?.build()
            ?.toString() ?: throw RuntimeException("Failed to build URL")

        val request = Request.Builder()
            .addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            .addHeader("Cookie", "mxm_bab=AB")
            .url(fullUrl)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body.string()

            if (!response.isSuccessful) {
                throw RuntimeException("API request failed with code: ${response.code}, body: $responseBody")
            }

            val msg = json.decodeFromString<Token>(responseBody)
            val userToken = msg.message.body.userToken
            payload = TokenPayload(userToken, Instant.now().plus(30, ChronoUnit.DAYS).epochSecond)
        }
    }

    private fun getApiSignature(apiEndpoint: String, dateTime: LocalDateTime): String {
        val key = "IEJ5E8XFaHQvIQNfs7IC"
        val formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val data = apiEndpoint + formattedDate
        val hmacSha1 = Mac.getInstance("HmacSHA1")
        val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA1")
        hmacSha1.init(secretKey)
        val signatureBytes = hmacSha1.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes)
    }

    companion object {
        const val API_URL = "https://apic-desktop.musixmatch.com/ws/1.1"
        const val APP_ID = "web-desktop-app-v1.0"
    }
}