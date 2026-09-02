package com.duggustore.app.data.remote

import com.duggustore.app.BuildConfig
import com.duggustore.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SupabaseService {
    private const val BASE_URL = BuildConfig.SUPABASE_URL
    private const val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        expectSuccess = true
    }

    private fun headers(token: String? = null) = buildMap {
        put("apikey", ANON_KEY)
        put("Authorization", "Bearer ${token ?: ANON_KEY}")
        put("Content-Type", "application/json")
        put("Prefer", "return=representation")
    }

    // Auth endpoints
    suspend fun signUp(email: String, password: String, data: JsonObject? = null): JsonObject {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            data?.let { put("data", it) }
        }
        return client.post("$BASE_URL/auth/v1/signup") {
            headers().forEach { (k, v) -> header(k, v) }
            setBody(body.toString())
            contentType(ContentType.Application.Json)
        }.body()
    }

    suspend fun signIn(email: String, password: String): JsonObject {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }
        return client.post("$BASE_URL/auth/v1/token?grant_type=password") {
            headers().forEach { (k, v) -> header(k, v) }
            setBody(body.toString())
            contentType(ContentType.Application.Json)
        }.body()
    }

    suspend fun signOut(token: String) {
        client.post("$BASE_URL/auth/v1/logout") {
            headers(token)
        }
    }

    suspend fun getUser(token: String): JsonObject {
        return client.get("$BASE_URL/auth/v1/user") {
            headers(token)
        }.body()
    }

    // Postgrest helpers
    suspend fun select(table: String, token: String? = null, params: Map<String, String> = emptyMap()): List<JsonObject> {
        val url = buildString {
            append("$BASE_URL/rest/v1/$table")
            if (params.isNotEmpty()) {
                append("?")
                append(params.entries.joinToString("&") { "${it.key}=eq.${it.value}" })
            }
        }
        return client.get(url) {
            headers(token).forEach { (k, v) -> header(k, v) }
        }.body()
    }

    suspend fun selectAll(table: String, token: String? = null): List<JsonObject> {
        return client.get("$BASE_URL/rest/v1/$table?select=*") {
            headers(token).forEach { (k, v) -> header(k, v) }
        }.body()
    }

    suspend fun insert(table: String, body: String, token: String? = null): JsonObject {
        return client.post("$BASE_URL/rest/v1/$table") {
            headers(token).forEach { (k, v) -> header(k, v) }
            setBody(body)
            contentType(ContentType.Application.Json)
        }.body()
    }

    suspend fun update(table: String, id: String, body: String, token: String? = null): List<JsonObject> {
        return client.patch("$BASE_URL/rest/v1/$table?id=eq.$id") {
            headers(token).forEach { (k, v) -> header(k, v) }
            setBody(body)
            contentType(ContentType.Application.Json)
        }.body()
    }

    suspend fun delete(table: String, id: String, token: String? = null) {
        client.delete("$BASE_URL/rest/v1/$table?id=eq.$id") {
            headers(token).forEach { (k, v) -> header(k, v) }
        }
    }

    suspend fun deleteWhere(table: String, column: String, value: String, token: String? = null) {
        client.delete("$BASE_URL/rest/v1/$table?$column=eq.$value") {
            headers(token).forEach { (k, v) -> header(k, v) }
        }
    }

    suspend fun updateWhere(table: String, column: String, value: String, body: String, token: String? = null): List<JsonObject> {
        return client.patch("$BASE_URL/rest/v1/$table?$column=eq.$value") {
            headers(token).forEach { (k, v) -> header(k, v) }
            setBody(body)
            contentType(ContentType.Application.Json)
        }.body()
    }
}
