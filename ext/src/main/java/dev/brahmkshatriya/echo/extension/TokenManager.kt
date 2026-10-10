package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.data.Token
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Serializable
data class TokenPayload(val token: String, val expiresAt: Long)

class TokenManager(
    private val client: OkHttpClient,
    private val json: Json
) {
    var settings: Settings? = null
    private var payload: TokenPayload? = null
    private val lock = Any()

    val token: String
        get() = synchronized(lock) {
            // 1. Check user custom token from extension settings
            val customToken = settings?.getString(KEY_USER_TOKEN)?.trim()
            if (!customToken.isNullOrBlank() && isValidTokenString(customToken)) {
                return customToken
            }

            // 2. Check in-memory token
            if (payload != null && isValid(payload)) {
                return payload!!.token
            }

            // 3. Check cached token from persistent settings
            val cachedToken = settings?.getString(KEY_CACHED_TOKEN)?.trim()
            val cachedExpiresAt = settings?.getString(KEY_CACHED_EXPIRES_AT)?.toLongOrNull() ?: 0L
            if (!cachedToken.isNullOrBlank() && isValidTokenString(cachedToken) && Instant.now().epochSecond < cachedExpiresAt) {
                payload = TokenPayload(cachedToken, cachedExpiresAt)
                return cachedToken
            }

            // 4. Try fetching fresh token from API
            try {
                val freshToken = fetchFreshToken()
                if (isValidTokenString(freshToken)) {
                    val expiresAt = Instant.now().plus(14, ChronoUnit.DAYS).epochSecond
                    payload = TokenPayload(freshToken, expiresAt)
                    settings?.putString(KEY_CACHED_TOKEN, freshToken)
                    settings?.putString(KEY_CACHED_EXPIRES_AT, expiresAt.toString())
                    return freshToken
                }
            } catch (_: Exception) {
                // Ignore and fall through to fallback
            }

            // 5. Fallback pool
            val fallback = FALLBACK_TOKENS.random()
            payload = TokenPayload(fallback, Instant.now().plus(7, ChronoUnit.DAYS).epochSecond)
            return fallback
        }

    fun invalidateToken() = synchronized(lock) {
        payload = null
        settings?.putString(KEY_CACHED_TOKEN, null)
        settings?.putString(KEY_CACHED_EXPIRES_AT, null)
    }

    private fun isValid(p: TokenPayload?): Boolean {
        return p != null && isValidTokenString(p.token) && Instant.now().epochSecond < p.expiresAt
    }

    private fun isValidTokenString(t: String?): Boolean {
        if (t.isNullOrBlank()) return false
        if (t.length < 10) return false
        // Check for dummy all-zero token returned when banned or blocked
        if (t.all { it == '0' }) return false
        return true
    }

    private fun fetchFreshToken(): String {
        val guid = UUID.randomUUID().toString()
        val t = System.currentTimeMillis().toString()

        val endpoints = listOf(
            "$API_URL/token.get?app_id=$APP_ID_IOS&format=json&t=$t",
            "$API_URL_DESKTOP/token.get?app_id=$APP_ID_DESKTOP&format=json&t=$t"
        )

        for (url in endpoints) {
            try {
                val request = Request.Builder()
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                    )
                    .addHeader("Cookie", "AWSELBCORS=0; AWSELB=0; x-mxm-token-guid=$guid")
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val bodyString = response.body.string()
                    val msg = json.decodeFromString<Token>(bodyString)
                    val userToken = msg.message?.body?.userToken?.trim()
                    if (isValidTokenString(userToken)) {
                        return userToken!!
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }

        throw RuntimeException("Failed to obtain a valid Musixmatch token")
    }

    companion object {
        const val KEY_USER_TOKEN = "user_token"
        private const val KEY_CACHED_TOKEN = "cached_user_token"
        private const val KEY_CACHED_EXPIRES_AT = "cached_user_token_expires_at"

        const val API_URL = "https://apic.musixmatch.com/ws/1.1"
        const val API_URL_DESKTOP = "https://apic-desktop.musixmatch.com/ws/1.1"
        const val APP_ID_IOS = "mac-ios-v2.0"
        const val APP_ID_DESKTOP = "web-desktop-app-v1.0"

        val FALLBACK_TOKENS = listOf(
            "261010c08a8e0025eb382466efefd0831581d43d120c3995302c2d",
            "240502a5a589e600570b5514f7b607612f00eb34bf774d6c7028f0"
        )
    }
}
