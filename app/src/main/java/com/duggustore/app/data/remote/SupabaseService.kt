package com.duggustore.app.data.remote

import com.duggustore.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thrown for any non-2xx response from Supabase. [errorCode] carries Supabase's
 * machine readable code (e.g. "email_not_confirmed") when the API supplies one,
 * so callers can branch on it instead of pattern matching english strings.
 */
class SupabaseException(
    val statusCode: Int,
    val errorCode: String,
    message: String
) : Exception(message)

object SupabaseService {
    private val BASE_URL = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY.trim()

    /** True when the app was built without real Supabase credentials in local.properties. */
    val isConfigured: Boolean
        get() = BASE_URL.isNotEmpty() &&
            ANON_KEY.isNotEmpty() &&
            !BASE_URL.contains("your-project") &&
            ANON_KEY != "your-anon-key"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun headers(token: String? = null): Map<String, String> = mapOf(
        "apikey" to ANON_KEY,
        "Authorization" to "Bearer ${token ?: ANON_KEY}",
        "Content-Type" to "application/json",
        "Prefer" to "return=representation"
    )

    /**
     * Runs the call off the main thread. OkHttp's execute() blocks, and every caller
     * reaches this from viewModelScope (Dispatchers.Main), so without this switch
     * Android raises NetworkOnMainThreadException on every request.
     */
    private suspend fun executeRequest(request: Request): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw SupabaseException(
                0,
                "not_configured",
                "App is not connected to Supabase. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties and rebuild."
            )
        }
        val (code, body) = try {
            client.newCall(request).execute().use { response ->
                response.code to (response.body?.string() ?: "")
            }
        } catch (e: IOException) {
            throw SupabaseException(0, "network_error", "No internet connection. Please check your network and try again.")
        }
        if (code !in 200..299) {
            throw SupabaseException(code, errorCodeOf(body, code), errorMessageOf(body, code))
        }
        body
    }

    private fun parseOrNull(body: String): JsonObject? = try {
        json.parseToJsonElement(body) as? JsonObject
    } catch (_: Exception) {
        null
    }

    private fun stringField(obj: JsonObject?, vararg keys: String): String? {
        obj ?: return null
        for (key in keys) {
            val value = (obj[key] as? JsonPrimitive) ?: continue
            if (value.isString || value.content.isNotBlank()) {
                val content = value.content
                if (content.isNotBlank() && content != "null") return content
            }
        }
        return null
    }

    /** Supabase spreads its error code across `error_code`, `code` and `error` depending on the endpoint. */
    private fun errorCodeOf(body: String, statusCode: Int): String {
        val obj = parseOrNull(body)
        return stringField(obj, "error_code", "code", "error") ?: statusCode.toString()
    }

    /** Likewise the human readable text lives under `msg`, `message`, `error_description` or `error`. */
    private fun errorMessageOf(body: String, statusCode: Int): String {
        val obj = parseOrNull(body)
        return stringField(obj, "msg", "message", "error_description", "error", "hint")
            ?: if (body.isBlank()) "HTTP $statusCode" else "HTTP $statusCode: $body"
    }

    private fun parseJsonArray(response: String): List<JsonObject> {
        if (response.isBlank()) return emptyList()
        val element = json.parseToJsonElement(response)
        return (element as? JsonArray)?.filterIsInstance<JsonObject>()
            ?: (element as? JsonObject)?.let { listOf(it) }
            ?: emptyList()
    }

    private fun parseJsonObject(response: String): JsonObject {
        if (response.isBlank()) return buildJsonObject { }
        return json.parseToJsonElement(response) as? JsonObject ?: buildJsonObject { }
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    suspend fun signUp(email: String, password: String, data: JsonObject? = null): JsonObject {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            data?.let { put("data", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/signup")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonObject(executeRequest(request))
    }

    suspend fun signIn(email: String, password: String): JsonObject {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/token?grant_type=password")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonObject(executeRequest(request))
    }

    /** Exchanges a stored refresh token for a fresh access token. */
    suspend fun refreshSession(refreshToken: String): JsonObject {
        val body = buildJsonObject { put("refresh_token", refreshToken) }
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/token?grant_type=refresh_token")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonObject(executeRequest(request))
    }

    /**
     * Exchanges the emailed one-time code for a session. This is the same
     * /auth/v1/verify endpoint that a confirmation link hits, but driven from the
     * app with the code the user typed, so no link has to be opened.
     *
     * `type` must match how the code was issued: "signup" for the confirmation
     * mail, "recovery" for a password reset, "email_change" for an address change.
     */
    suspend fun verifyOtp(email: String, code: String, type: String = "signup"): JsonObject {
        val body = buildJsonObject {
            put("type", type)
            put("email", email)
            put("token", code)
        }
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/verify")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonObject(executeRequest(request))
    }

    /**
     * Re-sends the signup confirmation mail. This is /auth/v1/resend — /auth/v1/verify
     * consumes an existing token instead of issuing a new one.
     */
    suspend fun resendVerification(email: String) {
        val body = buildJsonObject {
            put("type", "signup")
            put("email", email)
        }
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/resend")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        executeRequest(request)
    }

    suspend fun signOut(token: String) {
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/logout")
            .post("".toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        executeRequest(request)
    }

    suspend fun getUser(token: String): JsonObject {
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/user")
            .get()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonObject(executeRequest(request))
    }

    suspend fun select(table: String, token: String? = null, params: Map<String, String> = emptyMap()): List<JsonObject> {
        val filters = params.entries.joinToString("&") { "${it.key}=eq.${encode(it.value)}" }
        val qs = if (filters.isEmpty()) "?select=*" else "?select=*&$filters"
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table$qs")
            .get()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request))
    }

    suspend fun selectAll(table: String, token: String? = null): List<JsonObject> {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?select=*")
            .get()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request))
    }

    suspend fun insert(table: String, body: String, token: String? = null): JsonObject {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request)).firstOrNull() ?: buildJsonObject { }
    }

    suspend fun update(table: String, id: String, body: String, token: String? = null): List<JsonObject> {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?id=eq.${encode(id)}")
            .patch(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request))
    }

    suspend fun delete(table: String, id: String, token: String? = null) {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?id=eq.${encode(id)}")
            .delete()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        executeRequest(request)
    }

    suspend fun deleteWhere(table: String, column: String, value: String, token: String? = null) {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?$column=eq.${encode(value)}")
            .delete()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        executeRequest(request)
    }

    suspend fun updateWhere(table: String, column: String, value: String, body: String, token: String? = null): List<JsonObject> {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?$column=eq.${encode(value)}")
            .patch(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request))
    }
}
