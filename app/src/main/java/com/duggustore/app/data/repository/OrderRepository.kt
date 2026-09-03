package com.duggustore.app.data.repository

import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderItem
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class OrderRepository {

    private fun token(): String? = SessionManager.getAccessToken()

    suspend fun createOrder(order: Order, items: List<OrderItem>): Result<String> {
        return try {
            val token = token()
            // Writable columns only: serializing Order would also send id="", created_at=""
            // and the nested `items` list, none of which the orders table accepts.
            val orderBody = buildJsonObject {
                put("customer_id", order.customerId)
                put("seller_id", order.sellerId)
                order.deliveryId?.let { put("delivery_id", it) }
                put("status", order.status)
                put("total_amount", order.totalAmount)
                put("delivery_fee", order.deliveryFee)
                put("delivery_address", order.deliveryAddress)
            }.toString()

            val created = SupabaseService.insert("orders", orderBody, token)
            val result = json.decodeFromString(Order.serializer(), created.toString())
            if (result.id.isBlank()) throw Exception("Order was created but no ID was returned.")

            items.forEach { item ->
                val itemBody = buildJsonObject {
                    put("order_id", result.id)
                    put("product_id", item.productId)
                    put("quantity", item.quantity)
                    put("price_at_purchase", item.priceAtPurchase)
                }.toString()
                SupabaseService.insert("order_items", itemBody, token)
            }

            Result.success(result.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeOrders(list: List<JsonObject>): List<Order> =
        list.map { json.decodeFromString(Order.serializer(), it.toString()) }
            .sortedByDescending { it.createdAt }

    suspend fun getCustomerOrders(customerId: String): Result<List<Order>> {
        return try {
            Result.success(decodeOrders(SupabaseService.select("orders", token(), mapOf("customer_id" to customerId))))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSellerOrders(sellerId: String): Result<List<Order>> {
        return try {
            Result.success(decodeOrders(SupabaseService.select("orders", token(), mapOf("seller_id" to sellerId))))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeliveryOrders(deliveryId: String): Result<List<Order>> {
        return try {
            Result.success(decodeOrders(SupabaseService.select("orders", token(), mapOf("delivery_id" to deliveryId))))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOrders(): Result<List<Order>> {
        return try {
            Result.success(decodeOrders(SupabaseService.selectAll("orders", token())))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrder(orderId: String): Result<Order?> {
        return try {
            val list = SupabaseService.select("orders", token(), mapOf("id" to orderId))
            val order = list.firstOrNull()?.let { json.decodeFromString(Order.serializer(), it.toString()) }
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            // Built as JSON rather than interpolated into a string literal, so a value
            // containing a quote cannot break out and corrupt the request body.
            val body = buildJsonObject { put("status", status) }.toString()
            SupabaseService.update("orders", orderId, body, token())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignDelivery(orderId: String, deliveryId: String): Result<Unit> {
        return try {
            val body = buildJsonObject {
                put("delivery_id", deliveryId)
                put("status", "out_for_delivery")
            }.toString()
            SupabaseService.update("orders", orderId, body, token())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderItems(orderId: String): Result<List<OrderItem>> {
        return try {
            val list = SupabaseService.select("order_items", token(), mapOf("order_id" to orderId))
            Result.success(list.map { json.decodeFromString(OrderItem.serializer(), it.toString()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> = updateOrderStatus(orderId, "cancelled")
}
