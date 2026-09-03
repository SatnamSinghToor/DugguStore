package com.duggustore.app.data.remote

import com.duggustore.app.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SupabaseService {
    private const val BASE_URL = BuildConfig.SUPABASE_URL
    private const val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

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

    private fun executeRequest(request: Request): String {
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $body")
        }
        return body
    }

    private fun parseJsonArray(response: String): List<JsonObject> {
        val element = json.parseToJsonElement(response)
        return (element as? JsonArray)?.filterIsInstance<JsonObject>() ?: emptyList()
    }

    private fun parseJsonObject(response: String): JsonObject {
        return json.parseToJsonElement(response) as JsonObject
    }

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

    suspend fun resendVerification(email: String) {
        val body = buildJsonObject {
            put("email", email)
            put("type", "signup")
        }
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/verify")
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
        val qs = if (params.isNotEmpty()) "?${params.entries.joinToString("&") { "${it.key}=eq.${it.value}" }}" else ""
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
        return parseJsonObject(executeRequest(request))
    }

    suspend fun update(table: String, id: String, body: String, token: String? = null): List<JsonObject> {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?id=eq.$id")
            .patch(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request))
    }

    suspend fun delete(table: String, id: String, token: String? = null) {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?id=eq.$id")
            .delete()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        executeRequest(request)
    }

    suspend fun deleteWhere(table: String, column: String, value: String, token: String? = null) {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?$column=eq.$value")
            .delete()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        executeRequest(request)
    }

    suspend fun updateWhere(table: String, column: String, value: String, body: String, token: String? = null): List<JsonObject> {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?$column=eq.$value")
            .patch(body.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return parseJsonArray(executeRequest(request))
    }
}
