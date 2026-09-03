package com.duggustore.app.data.repository

import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class AuthRepository {

    private fun extractToken(resp: JsonObject): String? {
        // Try direct access_token
        resp["access_token"]?.jsonPrimitive?.content?.let { return it }
        // Try nested session object
        resp["session"]?.jsonObject?.get("access_token")?.jsonPrimitive?.content?.let { return it }
        // Supabase v1 signup returns user object without token - need to sign in after
        return null
    }

    private fun extractUserId(resp: JsonObject): String? {
        resp["id"]?.jsonPrimitive?.content?.let { return it }
        resp["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content?.let { return it }
        return null
    }

    suspend fun signUp(email: String, password: String, fullName: String, phone: String, role: String): Result<UserProfile> {
        return try {
            val resp = SupabaseService.signUp(email, password, buildJsonObject {
                put("full_name", fullName)
                put("phone", phone)
                put("role", role)
            })

            // Signup might return user object without token (email confirmation enabled)
            // or full session with token (email confirmation disabled)
            val token = extractToken(resp)

            if (token != null) {
                // Email confirmation disabled - we have the token
                val userId = extractUserId(resp) ?: throw Exception("No user ID")
                SessionManager.saveSession(token, "", userId, email)
                val profile = UserProfile(id = userId, fullName = fullName, phone = phone, role = role)
                SupabaseService.insert("profiles", json.encodeToString(UserProfile.serializer(), profile))
                return Result.success(profile)
            }

            // Email confirmation might be enabled - try signing in anyway
            val userId = extractUserId(resp)
            if (userId != null) {
                // Try auto sign-in
                try {
                    val signInResp = SupabaseService.signIn(email, password)
                    val signInToken = extractToken(signInResp)
                    if (signInToken != null) {
                        SessionManager.saveSession(signInToken, "", userId, email)
                        val profiles = SupabaseService.select("profiles", params = mapOf("id" to userId))
                        val profile = profiles.firstOrNull()?.let {
                            json.decodeFromString(UserProfile.serializer(), it.toString())
                        } ?: UserProfile(id = userId, fullName = fullName, phone = phone, role = role)
                        return Result.success(profile)
                    }
                } catch (_: Exception) { }
            }

            // Check if it's an email confirmation error
            val errorMsg = resp["msg"]?.jsonPrimitive?.content ?: ""
            val errorCode = resp["error_code"]?.jsonPrimitive?.content ?: ""

            if (errorCode == "email_not_confirmed" || errorMsg.contains("confirm")) {
                return Result.failure(Exception("Please verify your email first. Check your inbox."))
            }

            // Signup succeeded but can't login yet
            Result.failure(Exception("Account created! Please verify your email, then sign in."))
        } catch (e: Exception) {
            // Parse error response from HTTP exceptions
            val msg = e.message ?: ""
            when {
                msg.contains("email_not_confirmed") -> Result.failure(Exception("Please verify your email first."))
                msg.contains("User already registered") -> Result.failure(Exception("This email is already registered. Try signing in."))
                msg.contains("Password should be") -> Result.failure(Exception("Password must be at least 6 characters."))
                msg.contains("Signup requires") -> Result.failure(Exception("Please fill all required fields."))
                msg.contains("HTTP 4") -> Result.failure(Exception("Invalid input. Please check your details."))
                else -> Result.failure(Exception("Signup failed: $msg"))
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return try {
            val resp = SupabaseService.signIn(email, password)
            val token = extractToken(resp)
                ?: throw Exception("No access token received")

            val userId = extractUserId(resp) ?: throw Exception("No user ID")
            SessionManager.saveSession(token, "", userId, email)

            val profiles = SupabaseService.select("profiles", params = mapOf("id" to userId))
            val profile = profiles.firstOrNull()?.let {
                json.decodeFromString(UserProfile.serializer(), it.toString())
            } ?: UserProfile(id = userId, fullName = "", phone = "", role = "customer")

            Result.success(profile)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            when {
                msg.contains("email_not_confirmed") -> Result.failure(Exception("Please verify your email first."))
                msg.contains("Invalid login") -> Result.failure(Exception("Invalid email or password."))
                msg.contains("HTTP 400") -> Result.failure(Exception("Invalid email or password."))
                else -> Result.failure(Exception("Login failed: $msg"))
            }
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
