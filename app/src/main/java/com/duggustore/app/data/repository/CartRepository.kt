package com.duggustore.app.data.repository

import com.duggustore.app.data.model.CartItem
import com.duggustore.app.data.remote.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.eq

class CartRepository {

    suspend fun getCartItems(customerId: String): Result<List<CartItem>> {
        return try {
            val items = client.from("cart_items")
                .select { eq("customer_id", customerId) }
                .decodeList<CartItem>()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToCart(customerId: String, productId: String, quantity: Int = 1): Result<Unit> {
        return try {
            val existing = client.from("cart_items")
                .select()
                .decodeList<CartItem>()
                .find { it.customerId == customerId && it.productId == productId }

            if (existing != null) {
                val newQty = existing.quantity + quantity
                client.from("cart_items")
                    .update(mapOf("quantity" to newQty)) { eq("id", existing.id) }
            } else {
                val cartItem = CartItem(
                    customerId = customerId,
                    productId = productId,
                    quantity = quantity
                )
                client.from("cart_items").insert(cartItem)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuantity(itemId: String, quantity: Int): Result<Unit> {
        return try {
            if (quantity <= 0) {
                client.from("cart_items")
                    .delete { eq("id", itemId) }
            } else {
                client.from("cart_items")
                    .update(mapOf("quantity" to quantity)) { eq("id", itemId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(itemId: String): Result<Unit> {
        return try {
            client.from("cart_items")
                .delete { eq("id", itemId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCart(customerId: String): Result<Unit> {
        return try {
            client.from("cart_items")
                .delete { eq("customer_id", customerId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
