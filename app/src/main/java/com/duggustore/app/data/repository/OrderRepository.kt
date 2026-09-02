package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderItem
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class OrderRepository {

    suspend fun createOrder(order: Order, items: List<OrderItem>): Result<String> {
        return try {
            val resultStr = SupabaseService.insert("orders", json.encodeToString(Order.serializer(), order))
            val result = json.decodeFromString(Order.serializer(), resultStr.toString())

            items.forEach { item ->
                val orderItem = item.copy(orderId = result.id)
                SupabaseService.insert("order_items", json.encodeToString(OrderItem.serializer(), orderItem))
            }

            Result.success(result.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerOrders(customerId: String): Result<List<Order>> {
        return try {
            val list = SupabaseService.select("orders", params = mapOf("customer_id" to customerId))
            Result.success(list.map { json.decodeFromString(Order.serializer(), it.toString()) }.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSellerOrders(sellerId: String): Result<List<Order>> {
        return try {
            val list = SupabaseService.select("orders", params = mapOf("seller_id" to sellerId))
            Result.success(list.map { json.decodeFromString(Order.serializer(), it.toString()) }.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeliveryOrders(deliveryId: String): Result<List<Order>> {
        return try {
            val list = SupabaseService.select("orders", params = mapOf("delivery_id" to deliveryId))
            Result.success(list.map { json.decodeFromString(Order.serializer(), it.toString()) }.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOrders(): Result<List<Order>> {
        return try {
            val list = SupabaseService.selectAll("orders")
            Result.success(list.map { json.decodeFromString(Order.serializer(), it.toString()) }.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrder(orderId: String): Result<Order?> {
        return try {
            val list = SupabaseService.select("orders", params = mapOf("id" to orderId))
            val order = list.firstOrNull()?.let { json.decodeFromString(Order.serializer(), it.toString()) }
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            SupabaseService.update("orders", orderId, """{"status":"$status"}""")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignDelivery(orderId: String, deliveryId: String): Result<Unit> {
        return try {
            SupabaseService.update("orders", orderId, """{"delivery_id":"$deliveryId","status":"out_for_delivery"}""")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderItems(orderId: String): Result<List<OrderItem>> {
        return try {
            val list = SupabaseService.select("order_items", params = mapOf("order_id" to orderId))
            Result.success(list.map { json.decodeFromString(OrderItem.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> = updateOrderStatus(orderId, "cancelled")
}
