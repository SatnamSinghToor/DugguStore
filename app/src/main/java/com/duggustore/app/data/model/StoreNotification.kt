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
    enum class Kind { Placed, Confirmed, Preparing, ReadyForPickup, OutForDelivery, Delivered, Cancelled }
}

/**
 * [items] is optional because it's fetched separately from the order itself
 * (see OrderViewModel.loadOrderItems) and may not have arrived yet — the
 * order number is what a notification falls back to until then, same as an
 * OrderCard falls back to "1 item" / "Item" before its own items load.
 */
fun Order.toNotification(items: List<OrderItem> = emptyList()): StoreNotification {
    val short = "#${id.takeLast(8).uppercase()}"
    val subject = when {
        items.isEmpty() -> short
        items.size == 1 -> items[0].product?.name ?: short
        else -> "${items[0].product?.name ?: short} +${items.size - 1} more"
    }
    val (kind, title, body) = when (orderStatus()) {
        OrderStatus.PENDING -> Triple(
            StoreNotification.Kind.Placed,
            "Order placed",
            "$subject is with the seller and waiting to be accepted."
        )
        OrderStatus.CONFIRMED -> Triple(
            StoreNotification.Kind.Confirmed,
            "Order confirmed",
            "$subject has been accepted and will be prepared shortly."
        )
        OrderStatus.PREPARING -> Triple(
            StoreNotification.Kind.Preparing,
            "Being prepared",
            "$subject is being packed for you."
        )
        OrderStatus.READY_FOR_PICKUP -> Triple(
            StoreNotification.Kind.ReadyForPickup,
            "Ready for pickup",
            "$subject is packed and waiting for a rider to pick it up."
        )
        OrderStatus.OUT_FOR_DELIVERY -> Triple(
            StoreNotification.Kind.OutForDelivery,
            "Out for delivery",
            "$subject is on its way to you."
        )
        OrderStatus.DELIVERED -> Triple(
            StoreNotification.Kind.Delivered,
            "Delivered",
            "$subject was delivered. Thanks for shopping with us."
        )
        OrderStatus.CANCELLED -> Triple(
            StoreNotification.Kind.Cancelled,
            "Order cancelled",
            "$subject was cancelled and will not be delivered."
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
