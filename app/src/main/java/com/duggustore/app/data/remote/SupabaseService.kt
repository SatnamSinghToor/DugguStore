package com.duggustore.app.data.remote

import com.duggustore.app.BuildConfig
import kotlinx.serialization.json.Json
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

    suspend fun signUp(email: String, password: String, data: JsonObject? = null): JsonObject {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            data?.let { put("data", it) }
        }
        val requestBody = body.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/signup")
            .post(requestBody)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        val response = executeRequest(request)
        return json.parseToJsonElement(response) as JsonObject
    }

    suspend fun signIn(email: String, password: String): JsonObject {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }
        val requestBody = body.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/token?grant_type=password")
            .post(requestBody)
            .apply { headers().forEach { (k, v) -> addHeader(k, v) } }
            .build()
        val response = executeRequest(request)
        return json.parseToJsonElement(response) as JsonObject
    }

    suspend fun signOut(token: String) {
        val requestBody = "".toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/auth/v1/logout")
            .post(requestBody)
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
        val response = executeRequest(request)
        return json.parseToJsonElement(response) as JsonObject
    }

    suspend fun select(table: String, token: String? = null, params: Map<String, String> = emptyMap()): List<JsonObject> {
        val queryString = if (params.isNotEmpty()) {
            "?${params.entries.joinToString("&") { "${it.key}=eq.${it.value}" }}"
        } else ""
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table$queryString")
            .get()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        val response = executeRequest(request)
        return json.parseToJsonElement(response).let { element ->
            kotlinx.serialization.json.jsonArray(element) ?: emptyList()
        }
    }

    suspend fun selectAll(table: String, token: String? = null): List<JsonObject> {
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?select=*")
            .get()
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        val response = executeRequest(request)
        return json.parseToJsonElement(response).let { element ->
            kotlinx.serialization.json.jsonArray(element) ?: emptyList()
        }
    }

    suspend fun insert(table: String, body: String, token: String? = null): JsonObject {
        val requestBody = body.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table")
            .post(requestBody)
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        val response = executeRequest(request)
        return json.parseToJsonElement(response) as JsonObject
    }

    suspend fun update(table: String, id: String, body: String, token: String? = null): List<JsonObject> {
        val requestBody = body.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?id=eq.$id")
            .patch(requestBody)
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        val response = executeRequest(request)
        return json.parseToJsonElement(response).let { element ->
            kotlinx.serialization.json.jsonArray(element) ?: emptyList()
        }
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
        val requestBody = body.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$BASE_URL/rest/v1/$table?$column=eq.$value")
            .patch(requestBody)
            .apply { headers(token).forEach { (k, v) -> addHeader(k, v) } }
            .build()
        val response = executeRequest(request)
        return json.parseToJsonElement(response).let { element ->
            kotlinx.serialization.json.jsonArray(element) ?: emptyList()
        }
    }

    private fun jsonArray(element: kotlinx.serialization.json.JsonElement): List<JsonObject>? {
        return (element as? kotlinx.serialization.json.JsonArray)?.map { it as JsonObject }
    }
}
