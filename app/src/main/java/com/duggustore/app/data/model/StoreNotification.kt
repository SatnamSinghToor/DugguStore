package com.duggustore.app.data.model

/**
 * There is no notifications table, and inventing one would need a writer on the
 * server side to fill it. What the customer actually wants to be told about is
 * their orders, and those are already loaded, so a notification is derived from
 * an order's current status rather than stored.
 */
data class StoreNotification(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: String,
    val orderId: String,
    val kind: Kind
) {
    enum class Kind { Placed, Confirmed, Preparing, OutForDelivery, Delivered, Cancelled }
}

fun Order.toNotification(): StoreNotification {
    val short = "#${id.takeLast(8).uppercase()}"
    val (kind, title, body) = when (orderStatus()) {
        OrderStatus.PENDING -> Triple(
            StoreNotification.Kind.Placed,
            "Order placed",
            "$short is with the seller and waiting to be accepted."
        )
        OrderStatus.CONFIRMED -> Triple(
            StoreNotification.Kind.Confirmed,
            "Order confirmed",
            "$short has been accepted and will be prepared shortly."
        )
        OrderStatus.PREPARING -> Triple(
            StoreNotification.Kind.Preparing,
            "Being prepared",
            "$short is being packed for you."
        )
        OrderStatus.OUT_FOR_DELIVERY -> Triple(
            StoreNotification.Kind.OutForDelivery,
            "Out for delivery",
            "$short is on its way to you."
        )
        OrderStatus.DELIVERED -> Triple(
            StoreNotification.Kind.Delivered,
            "Delivered",
            "$short was delivered. Thanks for shopping with us."
        )
        OrderStatus.CANCELLED -> Triple(
            StoreNotification.Kind.Cancelled,
            "Order cancelled",
            "$short was cancelled and will not be delivered."
        )
    }
    return StoreNotification(
        // The status is part of the id so a status change reads as a new entry.
        id = "$id-$status",
        title = title,
        body = body,
        timestamp = createdAt,
        orderId = id,
        kind = kind
    )
}
