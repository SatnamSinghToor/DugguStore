package com.duggustore.app.data.repository

import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class AuthRepository {

    suspend fun signUp(email: String, password: String, fullName: String, phone: String, role: String): Result<UserProfile> {
        return try {
            val resp = SupabaseService.signUp(email, password, buildJsonObject {
                put("full_name", fullName)
                put("phone", phone)
                put("role", role)
            })

            val accessToken = resp["access_token"]?.toString()?.trim('"')
                ?: resp["session"]?.toString()?.let {
                    json.parseToJsonElement(it).let { s ->
                        (s as? kotlinx.serialization.json.JsonObject)?.get("access_token")?.toString()?.trim('"')
                    }
                }
                ?: throw Exception("No access token received")

            val refreshToken = resp["refresh_token"]?.toString()?.trim('"') ?: ""

            val userResp = SupabaseService.getUser(accessToken)
            val userId = userResp["id"]?.toString()?.trim('"') ?: throw Exception("User creation failed")
            val userEmail = userResp["email"]?.toString()?.trim('"') ?: email

            SessionManager.saveSession(accessToken, refreshToken, userId, userEmail)

            val profile = UserProfile(id = userId, fullName = fullName, phone = phone, role = role)
            SupabaseService.insert("profiles", json.encodeToString(UserProfile.serializer(), profile))

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return try {
            val resp = SupabaseService.signIn(email, password)
            val accessToken = resp["access_token"]?.toString()?.trim('"')
                ?: resp["session"]?.toString()?.let {
                    json.parseToJsonElement(it).let { s ->
                        (s as? kotlinx.serialization.json.JsonObject)?.get("access_token")?.toString()?.trim('"')
                    }
                }
                ?: throw Exception("No access token received")

            val refreshToken = resp["refresh_token"]?.toString()?.trim('"') ?: ""

            val userResp = SupabaseService.getUser(accessToken)
            val userId = userResp["id"]?.toString()?.trim('"') ?: throw Exception("Login failed")

            SessionManager.saveSession(accessToken, refreshToken, userId, email)

            val profiles = SupabaseService.select("profiles", params = mapOf("id" to userId))
            val profile = profiles.firstOrNull()?.let {
                json.decodeFromString(UserProfile.serializer(), it.toString())
            } ?: UserProfile(id = userId, fullName = "", phone = "", role = "customer")

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            SessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<UserProfile?> {
        return try {
            val token = SessionManager.getAccessToken() ?: return Result.success(null)
            val userId = SessionManager.getUserId() ?: return Result.success(null)

            val profiles = SupabaseService.select("profiles", token, mapOf("id" to userId))
            val profile = profiles.firstOrNull()?.let {
                json.decodeFromString(UserProfile.serializer(), it.toString())
            }

            Result.success(profile)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<UserProfile> {
        return try {
            val token = SessionManager.getAccessToken()
            SupabaseService.update("profiles", profile.id, json.encodeToString(UserProfile.serializer(), profile), token)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<UserProfile>> {
        return try {
            val token = SessionManager.getAccessToken()
            val list = SupabaseService.selectAll("profiles", token)
            Result.success(list.map { json.decodeFromString(UserProfile.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, role: String): Result<Unit> {
        return try {
            val token = SessionManager.getAccessToken()
            val body = buildJsonObject { put("role", role) }.toString()
            SupabaseService.update("profiles", userId, body, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
