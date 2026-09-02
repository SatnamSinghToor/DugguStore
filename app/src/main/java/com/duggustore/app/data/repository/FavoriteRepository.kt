package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Favorite
import com.duggustore.app.data.remote.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.eq

class FavoriteRepository {

    suspend fun getFavorites(customerId: String): Result<List<Favorite>> {
        return try {
            val favorites = client.from("favorites")
                .select { eq("customer_id", customerId) }
                .decodeList<Favorite>()
            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToFavorites(customerId: String, productId: String): Result<Unit> {
        return try {
            val favorite = Favorite(
                customerId = customerId,
                productId = productId
            )
            client.from("favorites").insert(favorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromFavorites(customerId: String, productId: String): Result<Unit> {
        return try {
            client.from("favorites")
                .delete {
                    eq("customer_id", customerId)
                    eq("product_id", productId)
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFavorite(customerId: String, productId: String): Boolean {
        return try {
            val favorites = client.from("favorites")
                .select()
                .decodeList<Favorite>()
            favorites.any { it.customerId == customerId && it.productId == productId }
        } catch (e: Exception) {
            false
        }
    }
}
