package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderItem
import com.duggustore.app.data.remote.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.filter.eq

class OrderRepository {

    suspend fun createOrder(order: Order, items: List<OrderItem>): Result<String> {
        return try {
            val result = client.from("orders")
                .insert(order)
                .decodeSingle<Order>()

            items.forEach { item ->
                val orderItem = item.copy(orderId = result.id)
                client.from("order_items").insert(orderItem)
            }

            Result.success(result.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerOrders(customerId: String): Result<List<Order>> {
        return try {
            val orders = client.from("orders")
                .select { eq("customer_id", customerId) }
                .decodeList<Order>()
            Result.success(orders.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSellerOrders(sellerId: String): Result<List<Order>> {
        return try {
            val orders = client.from("orders")
                .select { eq("seller_id", sellerId) }
                .decodeList<Order>()
            Result.success(orders.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeliveryOrders(deliveryId: String): Result<List<Order>> {
        return try {
            val orders = client.from("orders")
                .select { eq("delivery_id", deliveryId) }
                .decodeList<Order>()
            Result.success(orders.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOrders(): Result<List<Order>> {
        return try {
            val orders = client.from("orders")
                .select()
                .decodeList<Order>()
            Result.success(orders.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrder(orderId: String): Result<Order?> {
        return try {
            val order = client.from("orders")
                .select { eq("id", orderId) }
                .decodeSingle<Order>()
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            client.from("orders")
                .update(mapOf("status" to status)) { eq("id", orderId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignDelivery(orderId: String, deliveryId: String): Result<Unit> {
        return try {
            client.from("orders")
                .update(mapOf(
                    "delivery_id" to deliveryId,
                    "status" to "out_for_delivery"
                )) { eq("id", orderId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderItems(orderId: String): Result<List<OrderItem>> {
        return try {
            val items = client.from("order_items")
                .select { eq("order_id", orderId) }
                .decodeList<OrderItem>()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> {
        return updateOrderStatus(orderId, "cancelled")
    }
}
