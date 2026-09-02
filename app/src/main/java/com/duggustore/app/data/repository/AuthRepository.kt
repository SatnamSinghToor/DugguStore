package com.duggustore.app.data.repository

import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.remote.SupabaseClient
import com.duggustore.app.data.remote.SupabaseClient.client
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.eq
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    suspend fun signUp(email: String, password: String, fullName: String, phone: String, role: String): Result<UserProfile> {
        return try {
            val result = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("full_name", fullName)
                    put("phone", phone)
                    put("role", role)
                }
            }

            val userId = result.user?.id ?: throw Exception("User creation failed")

            val profile = UserProfile(
                id = userId,
                fullName = fullName,
                phone = phone,
                role = role
            )

            client.from("profiles").insert(profile)

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        return try {
            val result = client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = result.user?.id ?: throw Exception("Login failed")

            val profile = client.from("profiles")
                .select { eq("id", userId) }
                .decodeSingle<UserProfile>()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<UserProfile?> {
        return try {
            val session = client.auth.currentSessionOrNull()
            val userId = session?.user?.id ?: return Result.success(null)

            val profile = client.from("profiles")
                .select { eq("id", userId) }
                .decodeSingle<UserProfile>()

            Result.success(profile)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<UserProfile> {
        return try {
            client.from("profiles")
                .update(profile) { eq("id", profile.id) }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<UserProfile>> {
        return try {
            val profiles = client.from("profiles")
                .select()
                .decodeList<UserProfile>()
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, role: String): Result<Unit> {
        return try {
            val updates = buildJsonObject { put("role", role) }
            client.from("profiles")
                .update(updates) { eq("id", userId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
