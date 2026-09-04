package com.duggustore.app.ui.screens.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderItem
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.platform.openDialer
import com.duggustore.app.platform.openNavigation
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun DeliveryDashboard(
    /** Driven by the bottom bar, which is the only tab control now. 0=Available, 1=Active, 2=Completed */
    selectedTab: Int,
    availableOrders: List<Order> = emptyList(),
    activeOrders: List<Order>,
    completedOrders: List<Order>,
    totalEarnings: Double,
    totalDeliveries: Int,
    onMarkDelivered: (String) -> Unit,
    onClaimOrder: (String) -> Unit = {},
    claimError: String? = null,
    onDismissClaimError: () -> Unit = {},
    // In-app map when the destination has a real fix; the caller falls back
    // to an external maps app when it doesn't (openNavigation's text-search
    // fallback needs no coordinates at all).
    onNavigateToPickup: (Order) -> Unit = {},
    onNavigateToDrop: (Order) -> Unit = {},
    onSignOut: () -> Unit,
    sharingLocation: Boolean = false,
    sharingError: String? = null,
    onSharingChange: (Boolean) -> Unit = {},
    orderItemsByOrderId: Map<String, List<OrderItem>> = emptyMap(),
    onExpandOrderItems: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        DashboardHeader(
            title = "Deliveries",
            subtitle = if (activeOrders.isEmpty()) "Nothing on your route right now"
                       else if (activeOrders.size == 1) "1 delivery on your route"
                       else "${activeOrders.size} deliveries on your route",
            stats = listOf(
                "Earnings" to "₹${trimAmount(totalEarnings)}",
                "Delivered" to "$totalDeliveries"
            ),
            onSignOut = onSignOut
        )

        if (activeOrders.isNotEmpty() || sharingLocation) {
            LocationSharingRow(
                sharing = sharingLocation,
                error = sharingError,
                orderCount = activeOrders.size,
                onChange = onSharingChange
            )
        }

        // A lost claim is not an error the rider caused — someone else was
        // faster — so it surfaces as a dismissible banner, not a blocking dialog.
        if (claimError != null) {
            Surface(color = OrangeSurface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(claimError, fontSize = 12.sp, color = OrangeDark, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissClaimError) { Text("Dismiss", fontSize = 12.sp) }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    if (availableOrders.isEmpty()) {
                        DashboardEmpty(
                            icon = Icons.Default.ShoppingBag,
                            title = "No orders waiting",
                            subtitle = "Orders ready for pickup will show up here"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(availableOrders, key = { it.id }) { order ->
                                AvailableOrderCard(
                                    order = order,
                                    items = orderItemsByOrderId[order.id] ?: emptyList(),
                                    onClaim = { onClaimOrder(order.id) },
                                    onNavigateToPickup = { onNavigateToPickup(order) },
                                    onExpandItems = { onExpandOrderItems(order.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (activeOrders.isEmpty()) {
                        DashboardEmpty(
                            icon = Icons.Default.LocalShipping,
                            title = "No active deliveries",
                            subtitle = "Accept an order from Available to start your route"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeOrders, key = { it.id }) { order ->
                                DeliveryOrderCard(
                                    order = order,
                                    items = orderItemsByOrderId[order.id] ?: emptyList(),
                                    onMarkDelivered = { onMarkDelivered(order.id) },
                                    onNavigateToPickup = { onNavigateToPickup(order) },
                                    onNavigateToDrop = { onNavigateToDrop(order) },
                                    onExpandItems = { onExpandOrderItems(order.id) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (completedOrders.isEmpty()) {
                        DashboardEmpty(
                            icon = Icons.Default.CheckCircle,
                            title = "No completed deliveries",
                            subtitle = "Your delivery history will appear here"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(completedOrders, key = { it.id }) { order ->
                                DeliveryOrderCard(
                                    order = order,
                                    onMarkDelivered = {},
                                    isCompleted = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tap to fetch and reveal the order's line items — collapsed by default so a list of many orders stays scannable. */
@Composable
private fun OrderItemsSection(items: List<OrderItem>, onExpandItems: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                    if (expanded) onExpandItems()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "Hide items" else "View items",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Teal
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Teal,
                modifier = Modifier.size(18.dp)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            if (items.isEmpty()) {
                Text("Loading items…", fontSize = 12.sp, color = TextLight)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEach { item ->
                        Text(
                            text = "${item.product?.name ?: "Item"} ×${item.quantity}",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AvailableOrderCard(
    order: Order,
    items: List<OrderItem> = emptyList(),
    onClaim: () -> Unit,
    onNavigateToPickup: () -> Unit = {},
    onExpandItems: () -> Unit = {}
) {
    val context = LocalContext.current
    DashboardPanel {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "#${order.id.takeLast(8).uppercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "₹${trimAmount(order.totalAmount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal
                    )
                }
                StatusBadge(status = order.status)
            }

            Spacer(Modifier.height(10.dp))

            InfoLine(
                icon = { Icon(Icons.Default.ShoppingBag, null, tint = Orange, modifier = Modifier.size(16.dp)) },
                text = order.seller?.storeAddress?.takeIf { it.isNotBlank() }
                    ?: "Pickup address not set by seller",
                color = TextSecondary,
                onCallClick = order.seller?.phone?.takeIf { it.isNotBlank() }
                    ?.let { phone -> { openDialer(context, phone) } }
            )

            Spacer(Modifier.height(4.dp))

            InfoLine(
                icon = { Icon(Icons.Default.LocationOn, null, tint = Coral, modifier = Modifier.size(16.dp)) },
                text = order.deliveryAddress.ifBlank { "No address on this order" },
                color = TextSecondary,
                onCallClick = order.customer?.phone?.takeIf { it.isNotBlank() }
                    ?.let { phone -> { openDialer(context, phone) } }
            )

            Spacer(Modifier.height(8.dp))
            OrderItemsSection(items = items, onExpandItems = onExpandItems)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardAction(
                    text = "Navigate to store",
                    color = Teal,
                    filled = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val lat = order.seller?.storeLatitude
                        val lng = order.seller?.storeLongitude
                        if (lat != null && lng != null) {
                            onNavigateToPickup()
                        } else {
                            openNavigation(context, order.seller?.storeAddress.orEmpty(), lat, lng)
                        }
                    }
                )
                DashboardAction(
                    text = "Accept delivery",
                    color = Teal,
                    modifier = Modifier.weight(1f),
                    onClick = onClaim
                )
            }
        }
    }
}

@Composable
fun DeliveryOrderCard(
    order: Order,
    items: List<OrderItem> = emptyList(),
    onMarkDelivered: () -> Unit,
    isCompleted: Boolean = false,
    onNavigateToPickup: () -> Unit = {},
    onNavigateToDrop: () -> Unit = {},
    onExpandItems: () -> Unit = {}
) {
    val context = LocalContext.current
    val active = !isCompleted && order.status != OrderStatus.DELIVERED.value

    DashboardPanel {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "#${order.id.takeLast(8).uppercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "₹${trimAmount(order.totalAmount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal
                    )
                }
                StatusBadge(status = order.status)
            }

            Spacer(Modifier.height(10.dp))

            // The claim never marks a distinct "picked up" step, so an active
            // order might still need collecting from the store even though its
            // status already reads out_for_delivery — both stops stay visible
            // for the whole trip rather than the pickup one disappearing early.
            if (active) {
                InfoLine(
                    icon = { Icon(Icons.Default.ShoppingBag, null, tint = Orange, modifier = Modifier.size(16.dp)) },
                    text = order.seller?.storeAddress?.takeIf { it.isNotBlank() }
                        ?: "Pickup address not set by seller",
                    color = TextSecondary,
                    onCallClick = order.seller?.phone?.takeIf { it.isNotBlank() }
                        ?.let { phone -> { openDialer(context, phone) } }
                )
                Spacer(Modifier.height(4.dp))
            }

            InfoLine(
                icon = { Icon(Icons.Default.LocationOn, null, tint = Coral, modifier = Modifier.size(16.dp)) },
                text = order.deliveryAddress.ifBlank { "No address on this order" },
                color = TextSecondary,
                onCallClick = order.customer?.phone?.takeIf { it.isNotBlank() }
                    ?.let { phone -> { openDialer(context, phone) } }
            )

            Spacer(Modifier.height(4.dp))

            InfoLine(
                icon = { Icon(Icons.Default.CalendarToday, null, tint = TextLight, modifier = Modifier.size(14.dp)) },
                text = order.createdAt.take(10),
                color = TextLight
            )

            if (active) {
                Spacer(Modifier.height(6.dp))
                OrderItemsSection(items = items, onExpandItems = onExpandItems)

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardAction(
                        text = "Navigate to pickup",
                        color = Orange,
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val lat = order.seller?.storeLatitude
                            val lng = order.seller?.storeLongitude
                            if (lat != null && lng != null) {
                                onNavigateToPickup()
                            } else {
                                openNavigation(context, order.seller?.storeAddress.orEmpty(), lat, lng)
                            }
                        }
                    )
                    DashboardAction(
                        text = "Navigate to drop",
                        color = Coral,
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (order.deliveryLatitude != null && order.deliveryLongitude != null) {
                                onNavigateToDrop()
                            } else {
                                openNavigation(context, order.deliveryAddress, order.deliveryLatitude, order.deliveryLongitude)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))
                DashboardAction(
                    text = "Mark as delivered",
                    color = Teal,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMarkDelivered
                )
            }
        }
    }
}

@Composable
private fun InfoLine(
    icon: @Composable () -> Unit,
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onCallClick: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.padding(top = 2.dp).align(Alignment.Top)) { icon() }
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = color,
            maxLines = 2,
            lineHeight = 17.sp
        )
        if (onCallClick != null) {
            IconButton(onClick = onCallClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Call, "Call", tint = Teal, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * The rider decides when their position is visible. Nothing is published until
 * this is on, and it goes off with the screen.
 */
@Composable
private fun LocationSharingRow(
    sharing: Boolean,
    error: String?,
    orderCount: Int,
    onChange: (Boolean) -> Unit
) {
    Surface(color = if (sharing) TealSurface else OrangeSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (sharing) Icons.Default.LocationOn else Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = if (sharing) TealDark else OrangeDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (sharing) "Sharing your location" else "Location not shared",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sharing) TealDark else OrangeDark
                    )
                    Text(
                        text = when {
                            !sharing -> "Turn on so customers can follow their order"
                            orderCount == 1 -> "1 customer can see where you are"
                            else -> "$orderCount customers can see where you are"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = sharing,
                    onCheckedChange = onChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Teal
                    )
                )
            }

            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error, fontSize = 12.sp, color = CoralDark)
            }
        }
    }
}
