package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Favorite
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class FavoriteRepository {

    suspend fun getFavorites(customerId: String): Result<List<Favorite>> {
        return try {
            val list = SupabaseService.select("favorites", params = mapOf("customer_id" to customerId))
            Result.success(list.map { json.decodeFromString(Favorite.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToFavorites(customerId: String, productId: String): Result<Unit> {
        return try {
            val favorite = Favorite(customerId = customerId, productId = productId)
            SupabaseService.insert("favorites", json.encodeToString(Favorite.serializer(), favorite))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromFavorites(customerId: String, productId: String): Result<Unit> {
        return try {
            val favs = SupabaseService.select("favorites", params = mapOf("customer_id" to customerId))
                .map { json.decodeFromString(Favorite.serializer(), it.toString()) }
            val fav = favs.find { it.productId == productId }
            fav?.let { SupabaseService.delete("favorites", it.id) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFavorite(customerId: String, productId: String): Boolean {
        return try {
            val favs = SupabaseService.select("favorites", params = mapOf("customer_id" to customerId))
                .map { json.decodeFromString(Favorite.serializer(), it.toString()) }
            favs.any { it.productId == productId }
        } catch (e: Exception) {
            false
        }
    }
}
