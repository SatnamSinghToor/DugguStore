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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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

    /**
     * Sends the password-reset mail. Supabase returns 200 even for an unknown address.
     * redirect_to brings the user back into the app instead of the project's Site URL,
     * which on a fresh project is still http://localhost:3000 and dead on a phone.
     */
    suspend fun sendPasswordReset(email: String) {
        val body = buildJsonObject { put("email", email) }
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/recover?redirect_to=${encode(AUTH_REDIRECT_URL)}")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        executeRequest(request)
    }

    /** Sets a new password for the session in [token]. */
    suspend fun updatePassword(token: String, newPassword: String): JsonObject {
        val body = buildJsonObject { put("password", newPassword) }
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/user")
            .put(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonObject(executeRequest(request))
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

    /**
     * [select] is passed straight through to PostgREST, so a caller can pull an
     * embedded resource in one round trip, e.g. "*,product:products(*)" to hydrate
     * a cart row with its product instead of leaving the field null.
     */
    suspend fun select(
        table: String,
        token: String? = null,
        params: Map<String, String> = emptyMap(),
        select: String = "*"
    ): List<JsonObject> {
        val filters = params.entries.joinToString("&") { "${it.key}=eq.${encode(it.value)}" }
        val qs = if (filters.isEmpty()) "?select=${encode(select)}" else "?select=${encode(select)}&$filters"
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table$qs")
            .get()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request))
    }

    suspend fun selectAll(table: String, token: String? = null, select: String = "*"): List<JsonObject> {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?select=${encode(select)}")
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

    /**
     * Insert, or overwrite the existing row when it collides on [onConflict].
     *
     * The rider's position is one row per order that is rewritten every few
     * seconds; a plain insert would append a row per update and leave the
     * customer sorting a history to find the current fix.
     */
    suspend fun upsert(
        table: String,
        body: String,
        onConflict: String,
        token: String? = null
    ): JsonObject {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?on_conflict=${encode(onConflict)}")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                // Overrides rather than appends: headers() already sets Prefer to
                // return=representation, and two Prefer headers would leave
                // PostgREST reading only one of them.
                headers(token).forEach { (k, v) -> addHeader(k, v) }
                header("Prefer", "return=representation,resolution=merge-duplicates")
            }
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

    /**
     * Uploads raw bytes to Supabase Storage and hands back the object's
     * public URL. [path] is expected to start with the uploader's own user
     * id — the bucket's RLS policies key off that first path segment to
     * decide who is allowed to write there.
     */
    suspend fun uploadFile(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String,
        token: String?
    ): String {
        val mediaType = contentType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()
        val encodedPath = path.split("/").joinToString("/") { encode(it) }
        val request = Request.Builder()
            .url("$BASE_URL/storage/v1/object/$bucket/$encodedPath")
            .post(bytes.toRequestBody(mediaType))
            .apply {
                addHeader("apikey", ANON_KEY)
                addHeader("Authorization", "Bearer ${token ?: ANON_KEY}")
                // Overwrite rather than reject if the same path is ever
                // uploaded to twice.
                addHeader("x-upsert", "true")
            }
            .build()
        executeRequest(request)
        return "$BASE_URL/storage/v1/object/public/$bucket/$encodedPath"
    }

    /**
     * Calls a Postgres function via PostgREST's /rpc/ endpoint. Used for
     * SECURITY DEFINER functions that enforce their own authorization
     * server-side (e.g. resolving a refund) rather than a plain RLS-scoped
     * table write.
     */
    suspend fun rpc(fn: String, body: String = "{}", token: String? = null): String {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/rpc/$fn")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return executeRequest(request)
    }

    suspend fun deleteWhere(table: String, column: String, value: String, token: String? = null) {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?$column=eq.${encode(value)}")
            .delete()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        executeRequest(request)
    }

    /**
     * PATCH a row only while a named column is still NULL — built for
     * claim-style writes, where a client should only win if nobody has beaten
     * it there. Postgres evaluates the WHERE clause per row inside one UPDATE
     * statement, so of two concurrent claims on the same row only the first
     * PATCH still finds it matching; the second finds nothing and this returns
     * an empty list rather than overwriting the first caller's write.
     */
    suspend fun updateIfColumnNull(
        table: String,
        id: String,
        nullColumn: String,
        body: String,
        token: String? = null
    ): List<JsonObject> {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?id=eq.${encode(id)}&$nullColumn=is.null")
            .patch(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request))
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
