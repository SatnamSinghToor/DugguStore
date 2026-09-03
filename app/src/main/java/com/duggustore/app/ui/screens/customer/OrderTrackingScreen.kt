package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.DeliveryTracking
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.ui.components.RiderLocationCard
import com.duggustore.app.ui.components.StatusBadge
import com.duggustore.app.ui.components.trimAmount
import com.duggustore.app.ui.theme.*

@Composable
fun OrderListScreen(
    orders: List<Order>,
    onOrderClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        OrderTopBar(
            title = "My orders",
            caption = if (orders.size == 1) "1 order" else "${orders.size} orders",
            onBack = onBack
        )

        if (orders.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(TealSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.size(46.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text("No orders yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your orders will show up here once you place one",
                    modifier = Modifier.padding(horizontal = 40.dp),
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderCard(order = order, onClick = { onOrderClick(order.id) })
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.id.takeLast(8).uppercase()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                StatusBadge(status = order.status)
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "₹${trimAmount(order.totalAmount)}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal
                    )
                    if (order.deliveryAddress.isNotBlank()) {
                        Text(
                            text = order.deliveryAddress,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = order.createdAt.take(10),
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextLight)
            }
        }
    }
}

@Composable
fun OrderTrackingDetailScreen(
    order: Order,
    onCancelOrder: () -> Unit,
    onBack: () -> Unit,
    tracking: DeliveryTracking? = null,
    /** Straight-line metres from the rider to the delivery address. */
    riderDistanceMetres: Float? = null,
    riderFixAgeMinutes: Long? = null
) {
    val cancelled = order.status == OrderStatus.CANCELLED.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        OrderTopBar(
            title = "Order #${order.id.takeLast(8).uppercase()}",
            caption = order.createdAt.take(10),
            onBack = onBack
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Panel {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status", fontSize = 14.sp, color = TextSecondary)
                            StatusBadge(status = order.status)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", fontSize = 14.sp, color = TextSecondary)
                            Text(
                                text = "₹${trimAmount(order.totalAmount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Teal
                            )
                        }
                        if (order.deliveryAddress.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Delivering to", fontSize = 14.sp, color = TextSecondary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = order.deliveryAddress,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Only while it is actually on the road: before that there is no
            // rider, and afterwards where they are stopped being the customer's
            // business.
            if (order.status == OrderStatus.OUT_FOR_DELIVERY.value) {
                item {
                    RiderLocationCard(
                        tracking = tracking,
                        distanceMetres = riderDistanceMetres,
                        ageMinutes = riderFixAgeMinutes
                    )
                }
            }

            item {
                Panel {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Progress", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(16.dp))

                        if (cancelled) {
                            // "cancelled" is not a point on the timeline, so
                            // drawing the timeline for it would show an order
                            // that never started rather than one that stopped.
                            CancelledNotice()
                        } else {
                            val steps = listOf(
                                OrderStatus.PENDING to "Order placed",
                                OrderStatus.CONFIRMED to "Order confirmed",
                                OrderStatus.PREPARING to "Being prepared",
                                OrderStatus.READY_FOR_PICKUP to "Ready for pickup",
                                OrderStatus.OUT_FOR_DELIVERY to "Out for delivery",
                                OrderStatus.DELIVERED to "Delivered"
                            )
                            val currentIdx = steps.indexOfFirst { it.first.value == order.status }

                            steps.forEachIndexed { index, (_, label) ->
                                TimelineStep(
                                    label = label,
                                    // created_at is the only timestamp the order
                                    // carries, so it belongs on the first step
                                    // alone rather than on every reached step.
                                    time = if (index == 0) order.createdAt.take(10) else "",
                                    isActive = index <= currentIdx,
                                    isCurrent = index == currentIdx,
                                    isLast = index == steps.lastIndex
                                )
                            }
                        }
                    }
                }
            }

            if (order.status in listOf(OrderStatus.PENDING.value, OrderStatus.CONFIRMED.value)) {
                item {
                    OutlinedButton(
                        onClick = onCancelOrder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel order", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelledNotice() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CoralSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, null, tint = Coral, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Order cancelled", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("This order will not be delivered", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun OrderTopBar(title: String, caption: String, onBack: () -> Unit) {
    Surface(color = Teal) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Column {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (caption.isNotBlank()) {
                    Text(caption, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp,
        content = content
    )
}

@Composable
fun TimelineStep(
    label: String,
    time: String,
    isActive: Boolean,
    isLast: Boolean,
    isCurrent: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Teal else BorderGray),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(if (isActive) Teal else BorderGray)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold
                             else if (isActive) FontWeight.SemiBold
                             else FontWeight.Normal,
                color = if (isActive) TextPrimary else TextLight
            )
            if (time.isNotEmpty()) {
                Text(text = time, fontSize = 12.sp, color = if (isActive) Teal else TextLight)
            }
        }
    }
}
