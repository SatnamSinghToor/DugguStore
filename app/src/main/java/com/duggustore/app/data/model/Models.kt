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
    // Packed and waiting for a rider — nobody is carrying it yet. Distinct from
    // OUT_FOR_DELIVERY, which means a specific rider has claimed it and it is
    // actually moving; the tracking card on the customer's order screen is
    // gated on that difference.
    @SerialName("ready_for_pickup") READY_FOR_PICKUP("ready_for_pickup"),
    @SerialName("out_for_delivery") OUT_FOR_DELIVERY("out_for_delivery"),
    @SerialName("delivered") DELIVERED("delivered"),
    @SerialName("cancelled") CANCELLED("cancelled");

    companion object {
        fun fromString(value: String): OrderStatus = when (value) {
            "confirmed" -> CONFIRMED
            "preparing" -> PREPARING
            "ready_for_pickup" -> READY_FOR_PICKUP
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
        READY_FOR_PICKUP -> "Ready for Pickup"
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
    // Only meaningful for role == "seller" — where the rider picks up from.
    @SerialName("store_address") val storeAddress: String? = null,
    @SerialName("store_latitude") val storeLatitude: Double? = null,
    @SerialName("store_longitude") val storeLongitude: Double? = null,
    @SerialName("created_at") val createdAt: String = ""
) {
    fun userRole(): UserRole = UserRole.fromString(role)
    fun hasStoreFix(): Boolean = storeLatitude != null && storeLongitude != null
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
    // Added alongside image_url rather than replacing it — every reader that
    // only knows about a single photo (dashboards, order rows) keeps working
    // off image_url, which is always kept as the first entry here.
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    val stock: Int = 0,
    val unit: String = "pcs",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = ""
) {
    fun effectivePrice(): Double = discountPrice ?: price
    fun hasDiscount(): Boolean = discountPrice != null && discountPrice < price
    fun savingsAmount(): Double = if (hasDiscount()) price - discountPrice!! else 0.0

    /** All of the product's photos, falling back to the single legacy field for older rows. */
    fun images(): List<String> = imageUrls.ifEmpty { listOfNotNull(imageUrl?.takeIf { it.isNotBlank() }) }
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
    // Captured from the customer's selected address at checkout, when that
    // address itself carries a fix — null on any order placed before this,
    // or against an address that was only ever typed in by hand.
    @SerialName("delivery_latitude") val deliveryLatitude: Double? = null,
    @SerialName("delivery_longitude") val deliveryLongitude: Double? = null,
    @SerialName("created_at") val createdAt: String = "",
    val items: List<OrderItem> = emptyList(),
    // Embedded via a PostgREST select on the matching FK; each is only
    // populated by the order queries that actually ask for it — the seller's
    // store for a rider's pickup, the customer's phone for a rider or
    // seller to call, the rider's phone for a customer to call once one is
    // assigned.
    val seller: SellerStore? = null,
    val customer: ContactInfo? = null,
    val delivery: ContactInfo? = null
) {
    fun orderStatus(): OrderStatus = OrderStatus.fromString(status)
    fun hasDeliveryFix(): Boolean = deliveryLatitude != null && deliveryLongitude != null
}

@Serializable
data class SellerStore(
    @SerialName("full_name") val fullName: String = "",
    val phone: String = "",
    @SerialName("store_address") val storeAddress: String? = null,
    @SerialName("store_latitude") val storeLatitude: Double? = null,
    @SerialName("store_longitude") val storeLongitude: Double? = null
) {
    fun hasFix(): Boolean = storeLatitude != null && storeLongitude != null
}

/** A person on the other end of an order worth calling — the customer, or the rider once one has claimed it. */
@Serializable
data class ContactInfo(
    @SerialName("full_name") val fullName: String = "",
    val phone: String = ""
)

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
data class Review(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("product_id") val productId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    @SerialName("created_at") val createdAt: String = ""
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
) {
    /** 0,0 is the column default, so it means "never reported" rather than a place. */
    fun hasFix(): Boolean = latitude != 0.0 || longitude != 0.0
}

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
data class Coupon(
    val id: String = "",
    val code: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("discount_percent") val discountPercent: Int = 0,
    @SerialName("max_discount") val maxDiscount: Int = 0,
    @SerialName("min_order_value") val minOrderValue: Int = 0,
    @SerialName("expiry_label") val expiryLabel: String = "",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class ProductResponse(val products: List<Product> = emptyList())

@Serializable
data class CartResponse(val cart_items: List<CartItem> = emptyList())

@Serializable
data class OrderResponse(val orders: List<Order> = emptyList())

@Serializable
data class FavoriteResponse(val favorites: List<Favorite> = emptyList())
