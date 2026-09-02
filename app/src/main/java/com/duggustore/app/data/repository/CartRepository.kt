package com.duggustore.app.data.repository

import com.duggustore.app.data.model.CartItem
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class CartRepository {

    suspend fun getCartItems(customerId: String): Result<List<CartItem>> {
        return try {
            val list = SupabaseService.select("cart_items", params = mapOf("customer_id" to customerId))
            Result.success(list.map { json.decodeFromString(CartItem.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToCart(customerId: String, productId: String, quantity: Int = 1): Result<Unit> {
        return try {
            val existing = SupabaseService.select("cart_items", params = mapOf("customer_id" to customerId))
                .map { json.decodeFromString(CartItem.serializer(), it.toString()) }
                .find { it.productId == productId }

            if (existing != null) {
                val newQty = existing.quantity + quantity
                SupabaseService.update("cart_items", existing.id, """{"quantity":$newQty}""")
            } else {
                val cartItem = CartItem(customerId = customerId, productId = productId, quantity = quantity)
                SupabaseService.insert("cart_items", json.encodeToString(CartItem.serializer(), cartItem))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuantity(itemId: String, quantity: Int): Result<Unit> {
        return try {
            if (quantity <= 0) {
                SupabaseService.delete("cart_items", itemId)
            } else {
                SupabaseService.update("cart_items", itemId, """{"quantity":$quantity}""")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(itemId: String): Result<Unit> {
        return try {
            SupabaseService.delete("cart_items", itemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCart(customerId: String): Result<Unit> {
        return try {
            SupabaseService.deleteWhere("cart_items", "customer_id", customerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
