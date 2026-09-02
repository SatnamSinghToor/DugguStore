package com.duggustore.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole(val value: String) {
    @SerialName("customer") CUSTOMER("customer"),
    @SerialName("seller") SELLER("seller"),
    @SerialName("delivery") DELIVERY("delivery"),
    @SerialName("admin") ADMIN("admin");

    companion object {
        fun fromString(value: String): UserRole = when (value) {
            "seller" -> SELLER
            "delivery" -> DELIVERY
            "admin" -> ADMIN
            else -> CUSTOMER
        }
    }
}

enum class OrderStatus(val value: String) {
    @SerialName("pending") PENDING("pending"),
    @SerialName("confirmed") CONFIRMED("confirmed"),
    @SerialName("preparing") PREPARING("preparing"),
    @SerialName("out_for_delivery") OUT_FOR_DELIVERY("out_for_delivery"),
    @SerialName("delivered") DELIVERED("delivered"),
    @SerialName("cancelled") CANCELLED("cancelled");

    companion object {
        fun fromString(value: String): OrderStatus = when (value) {
            "confirmed" -> CONFIRMED
            "preparing" -> PREPARING
            "out_for_delivery" -> OUT_FOR_DELIVERY
            "delivered" -> DELIVERED
            "cancelled" -> CANCELLED
            else -> PENDING
        }
    }

    fun displayText(): String = when (this) {
        PENDING -> "Pending"
        CONFIRMED -> "Confirmed"
        PREPARING -> "Preparing"
        OUT_FOR_DELIVERY -> "Out for Delivery"
        DELIVERED -> "Delivered"
        CANCELLED -> "Cancelled"
    }
}

@Serializable
data class UserProfile(
    val id: String = "",
    @SerialName("full_name") val fullName: String = "",
    val phone: String = "",
    val role: String = "customer",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String = ""
) {
    fun userRole(): UserRole = UserRole.fromString(role)
}

@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("color_hex") val colorHex: String = "#7C3AED",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class Product(
    val id: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("category_id") val categoryId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    @SerialName("discount_price") val discountPrice: Double? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val stock: Int = 0,
    val unit: String = "pcs",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = ""
) {
    fun effectivePrice(): Double = discountPrice ?: price
    fun hasDiscount(): Boolean = discountPrice != null && discountPrice < price
    fun savingsAmount(): Double = if (hasDiscount()) price - discountPrice!! else 0.0
}

@Serializable
data class CartItem(
    val id: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("product_id") val productId: String = "",
    val quantity: Int = 1,
    val product: Product? = null
)

@Serializable
data class Order(
    val id: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("delivery_id") val deliveryId: String? = null,
    val status: String = "pending",
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("delivery_fee") val deliveryFee: Double = 0.0,
    @SerialName("delivery_address") val deliveryAddress: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val items: List<OrderItem> = emptyList()
) {
    fun orderStatus(): OrderStatus = OrderStatus.fromString(status)
}

@Serializable
data class OrderItem(
    val id: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("product_id") val productId: String = "",
    val quantity: Int = 1,
    @SerialName("price_at_purchase") val priceAtPurchase: Double = 0.0,
    val product: Product? = null
)

@Serializable
data class DeliveryTracking(
    val id: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("delivery_id") val deliveryId: String = "",
    val status: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class Address(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val label: String = "",
    @SerialName("full_address") val fullAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("is_default") val isDefault: Boolean = false
)

@Serializable
data class Favorite(
    val id: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("product_id") val productId: String = ""
)

// Wrapper classes for Supabase Postgrest
@Serializable
data class ProfileResponse(val profiles: List<UserProfile> = emptyList())

@Serializable
data class CategoryResponse(val categories: List<Category> = emptyList())

@Serializable
data class ProductResponse(val products: List<Product> = emptyList())

@Serializable
data class CartResponse(val cart_items: List<CartItem> = emptyList())

@Serializable
data class OrderResponse(val orders: List<Order> = emptyList())

@Serializable
data class FavoriteResponse(val favorites: List<Favorite> = emptyList())
