package com.duggustore.app.data.repository

import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseException
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

data class SignUpResult(
    val profile: UserProfile?,
    val requiresVerification: Boolean = false,
    val email: String = ""
)

class AuthRepository {

    // Safe casts throughout: the shape of an auth response varies by endpoint and by
    // whether email confirmation is on, and `.jsonObject` / `.jsonPrimitive` throw on a
    // mismatch, which would turn a perfectly good login into a failure.
    private fun obj(element: JsonElement?): JsonObject? = element as? JsonObject

    private fun str(element: JsonElement?): String? =
        (element as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }

    private fun extractToken(resp: JsonObject): String? =
        str(resp["access_token"]) ?: str(obj(resp["session"])?.get("access_token"))

    private fun extractRefreshToken(resp: JsonObject): String =
        str(resp["refresh_token"]) ?: str(obj(resp["session"])?.get("refresh_token")) ?: ""

    private fun extractUserId(resp: JsonObject): String? =
        str(resp["id"]) ?: str(obj(resp["user"])?.get("id"))

    /** The signup metadata Supabase echoes back, used as a fallback when the profile row is unreadable. */
    private fun extractUserMetadata(resp: JsonObject): JsonObject? {
        val user = obj(resp["user"]) ?: obj(obj(resp["session"])?.get("user")) ?: resp
        return obj(user["user_metadata"])
    }

    private fun metadataString(metadata: JsonObject?, key: String, default: String): String =
        str(metadata?.get(key)) ?: default

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    /**
     * Reads the caller's own profile row. The token must be passed through: with only the
     * anon key the "Users can view own profile" policy never matches and the query comes
     * back empty, which used to silently downgrade every seller/admin to "customer".
     */
    private suspend fun fetchProfile(userId: String, token: String): UserProfile? {
        val profiles = SupabaseService.select("profiles", token, mapOf("id" to userId))
        return profiles.firstOrNull()?.let {
            json.decodeFromString(UserProfile.serializer(), it.toString())
        }
    }

    /**
     * Returns the profile row for a freshly authenticated user. The `on_auth_user_created`
     * trigger normally creates it; if that trigger is missing we create it here, and if the
     * row is unreadable we fall back to the signup metadata so the role is still correct.
     */
    private suspend fun resolveProfile(
        userId: String,
        token: String,
        fallback: UserProfile
    ): UserProfile {
        val existing = try {
            fetchProfile(userId, token)
        } catch (e: Exception) {
            return fallback
        }
        if (existing != null) return existing

        return try {
            val body = buildJsonObject {
                put("id", userId)
                put("full_name", fallback.fullName)
                put("phone", fallback.phone)
                put("role", fallback.role)
            }.toString()
            SupabaseService.insert("profiles", body, token)
            fallback
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String, phone: String, role: String): Result<SignUpResult> {
        val cleanEmail = normalizeEmail(email)
        return try {
            val resp = SupabaseService.signUp(cleanEmail, password, buildJsonObject {
                put("full_name", fullName)
                put("phone", phone)
                put("role", role)
            })

            val token = extractToken(resp)

            // No session in the response means email confirmation is enabled on the project:
            // the account exists but cannot be used until the link is clicked.
            if (token == null) {
                return Result.success(
                    SignUpResult(profile = null, requiresVerification = true, email = cleanEmail)
                )
            }

            val userId = extractUserId(resp)
                ?: return Result.failure(Exception("Signup succeeded but no user ID was returned."))

            SessionManager.saveSession(token, extractRefreshToken(resp), userId, cleanEmail)

            val profile = resolveProfile(
                userId,
                token,
                UserProfile(id = userId, fullName = fullName, phone = phone, role = role)
            )
            Result.success(SignUpResult(profile = profile))
        } catch (e: Exception) {
            Result.failure(Exception(signUpErrorMessage(e)))
        }
    }

    private fun signUpErrorMessage(e: Exception): String {
        val code = (e as? SupabaseException)?.errorCode ?: ""
        val msg = e.message ?: ""
        return when {
            code == "not_configured" || code == "network_error" -> msg
            code == "user_already_exists" || code == "email_exists" ||
                msg.contains("already registered", ignoreCase = true) ->
                "This email is already registered. Try signing in."
            code == "weak_password" || msg.contains("Password should be", ignoreCase = true) ->
                "Password must be at least 6 characters."
            code == "email_address_invalid" ||
                (msg.contains("invalid", ignoreCase = true) && msg.contains("email", ignoreCase = true)) ->
                "Please enter a valid email address."
            code == "signup_disabled" -> "Sign ups are currently disabled for this app."
            code == "over_email_send_rate_limit" || code == "429" ->
                "Too many attempts. Please wait a moment and try again."
            msg.isBlank() -> "Signup failed. Please try again."
            else -> msg
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        val cleanEmail = normalizeEmail(email)
        return try {
            val resp = SupabaseService.signIn(cleanEmail, password)
            val token = extractToken(resp)
                ?: return Result.failure(Exception("Login failed: no access token received."))

            val userId = extractUserId(resp)
                ?: return Result.failure(Exception("Login failed: no user ID received."))

            SessionManager.saveSession(token, extractRefreshToken(resp), userId, cleanEmail)

            val metadata = extractUserMetadata(resp)
            val fallback = UserProfile(
                id = userId,
                fullName = metadataString(metadata, "full_name", ""),
                phone = metadataString(metadata, "phone", ""),
                role = metadataString(metadata, "role", "customer")
            )

            Result.success(resolveProfile(userId, token, fallback))
        } catch (e: Exception) {
            Result.failure(Exception(signInErrorMessage(e)))
        }
    }

    private fun signInErrorMessage(e: Exception): String {
        val code = (e as? SupabaseException)?.errorCode ?: ""
        val msg = e.message ?: ""
        return when {
            code == "not_configured" || code == "network_error" -> msg
            code == "email_not_confirmed" || msg.contains("not confirmed", ignoreCase = true) ->
                "Please verify your email first. Check your inbox for the verification link."
            code == "invalid_credentials" || code == "invalid_grant" ||
                msg.contains("Invalid login", ignoreCase = true) ->
                "Invalid email or password."
            code == "over_request_rate_limit" || code == "429" ->
                "Too many attempts. Please wait a moment and try again."
            msg.isBlank() -> "Login failed. Please try again."
            else -> msg
        }
    }

    /**
     * Verifies the 6-digit code from the signup mail and signs the user in.
     * Supabase returns a full session here, so there is no second login step.
     */
    suspend fun verifyEmailCode(email: String, code: String): Result<UserProfile> {
        val cleanEmail = normalizeEmail(email)
        val cleanCode = code.trim()
        return try {
            val resp = SupabaseService.verifyOtp(cleanEmail, cleanCode)
            val token = extractToken(resp)
                ?: return Result.failure(Exception("Verification failed: no session was returned."))
            val userId = extractUserId(resp)
                ?: return Result.failure(Exception("Verification failed: no user ID was returned."))

            SessionManager.saveSession(token, extractRefreshToken(resp), userId, cleanEmail)

            val metadata = extractUserMetadata(resp)
            val fallback = UserProfile(
                id = userId,
                fullName = metadataString(metadata, "full_name", ""),
                phone = metadataString(metadata, "phone", ""),
                role = metadataString(metadata, "role", "customer")
            )
            Result.success(resolveProfile(userId, token, fallback))
        } catch (e: Exception) {
            Result.failure(Exception(verifyErrorMessage(e)))
        }
    }

    private fun verifyErrorMessage(e: Exception): String {
        val code = (e as? SupabaseException)?.errorCode ?: ""
        val msg = e.message ?: ""
        return when {
            code == "not_configured" || code == "network_error" -> msg
            code == "otp_expired" || msg.contains("expired", ignoreCase = true) ->
                "That code has expired. Tap resend to get a new one."
            code == "otp_disabled" ->
                "Email codes are not enabled for this project yet."
            msg.contains("Token has expired or is invalid", ignoreCase = true) ||
                msg.contains("invalid", ignoreCase = true) ->
                "That code is not correct. Please check and try again."
            code == "over_email_send_rate_limit" || code == "429" ->
                "Too many attempts. Please wait a moment and try again."
            msg.isBlank() -> "Verification failed. Please try again."
            else -> msg
        }
    }

    suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return try {
            SupabaseService.resendVerification(normalizeEmail(email))
            Result.success(Unit)
        } catch (e: Exception) {
            val code = (e as? SupabaseException)?.errorCode ?: ""
            val msg = e.message ?: ""
            when {
                msg.contains("already confirmed", ignoreCase = true) ->
                    Result.failure(Exception("This email is already verified. Try signing in."))
                code == "over_email_send_rate_limit" || code == "429" ->
                    Result.failure(Exception("Too many requests. Please wait a moment and try again."))
                msg.isBlank() -> Result.failure(Exception("Failed to resend verification email."))
                else -> Result.failure(Exception(msg))
            }
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            SessionManager.getAccessToken()?.let {
                try {
                    SupabaseService.signOut(it)
                } catch (_: Exception) {
                    // Revoking server side is best effort; the local session is cleared either way.
                }
            }
            SessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            SessionManager.clearSession()
            Result.success(Unit)
        }
    }

    /**
     * Restores the profile for a stored session, refreshing the access token first when the
     * stored one has expired (Supabase access tokens last about an hour).
     */
    suspend fun getCurrentUserProfile(): Result<UserProfile?> {
        return try {
            val token = SessionManager.getAccessToken() ?: return Result.success(null)
            val userId = SessionManager.getUserId() ?: return Result.success(null)

            try {
                Result.success(fetchProfile(userId, token))
            } catch (e: SupabaseException) {
                if (e.statusCode != 401) throw e
                val refreshed = refreshAccessToken() ?: return Result.success(null)
                Result.success(fetchProfile(userId, refreshed))
            }
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    /** Swaps the stored refresh token for a new access token, returning null if it is no longer valid. */
    private suspend fun refreshAccessToken(): String? {
        val refreshToken = SessionManager.getRefreshToken()?.takeIf { it.isNotBlank() } ?: run {
            SessionManager.clearSession()
            return null
        }
        return try {
            val resp = SupabaseService.refreshSession(refreshToken)
            val token = extractToken(resp) ?: return null
            val userId = extractUserId(resp) ?: SessionManager.getUserId() ?: return null
            SessionManager.saveSession(
                token,
                extractRefreshToken(resp),
                userId,
                SessionManager.getEmail() ?: ""
            )
            token
        } catch (e: Exception) {
            SessionManager.clearSession()
            null
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<UserProfile> {
        return try {
            val token = SessionManager.getAccessToken()
            val body = buildJsonObject {
                put("full_name", profile.fullName)
                put("phone", profile.phone)
                profile.avatarUrl?.let { put("avatar_url", it) }
            }.toString()
            SupabaseService.update("profiles", profile.id, body, token)
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
