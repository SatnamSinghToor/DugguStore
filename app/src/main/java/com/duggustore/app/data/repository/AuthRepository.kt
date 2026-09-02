package com.duggustore.app.data.repository

import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.remote.SupabaseService
import com.duggustore.app.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class AuthRepository {

    suspend fun signUp(email: String, password: String, fullName: String, phone: String, role: String): Result<UserProfile> {
        return try {
            SupabaseService.signUp(email, password, buildJsonObject {
                put("full_name", fullName)
                put("phone", phone)
                put("role", role)
            })

            val token = SupabaseClient.client.auth.currentSessionOrNull()?.accessToken ?: ""

            val userResp = SupabaseService.getUser(token)
            val userId = userResp["id"]?.toString()?.trim('"') ?: throw Exception("User creation failed")

            val profile = UserProfile(id = userId, fullName = fullName, phone = phone, role = role)
            SupabaseService.insert("profiles", json.encodeToString(UserProfile.serializer(), profile), token)

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return try {
            val resp = SupabaseService.signIn(email, password)
            val token = resp["access_token"]?.toString()?.trim('"') ?: throw Exception("Login failed")

            val userResp = SupabaseService.getUser(token)
            val userId = userResp["id"]?.toString()?.trim('"') ?: throw Exception("Login failed")

            val profiles = SupabaseService.select("profiles", token, mapOf("id" to userId))
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
            SupabaseClient.client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<UserProfile?> {
        return try {
            val session = SupabaseClient.client.auth.currentSessionOrNull()
            val token = session?.accessToken ?: return Result.success(null)
            val userResp = SupabaseService.getUser(token)
            val userId = userResp["id"]?.toString()?.trim('"') ?: return Result.success(null)

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
            SupabaseService.update("profiles", profile.id, json.encodeToString(UserProfile.serializer(), profile))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<UserProfile>> {
        return try {
            val list = SupabaseService.selectAll("profiles")
            Result.success(list.map { json.decodeFromString(UserProfile.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, role: String): Result<Unit> {
        return try {
            val body = buildJsonObject { put("role", role) }.toString()
            SupabaseService.update("profiles", userId, body)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
