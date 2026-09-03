package com.duggustore.app.ui.screens.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun DeliveryDashboard(
    activeOrders: List<Order>,
    completedOrders: List<Order>,
    totalEarnings: Double,
    totalDeliveries: Int,
    onMarkDelivered: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

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

        DashboardTabs(
            tabs = listOf("Active (${activeOrders.size})", "Completed (${completedOrders.size})"),
            selected = selectedTab,
            onSelect = { selectedTab = it }
        )

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                if (activeOrders.isEmpty()) {
                    DashboardEmpty(
                        icon = Icons.Default.LocalShipping,
                        title = "No active deliveries",
                        subtitle = "New delivery requests will appear here"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeOrders, key = { it.id }) { order ->
                            DeliveryOrderCard(
                                order = order,
                                onMarkDelivered = { onMarkDelivered(order.id) }
                            )
                        }
                    }
                }
            } else {
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

@Composable
fun DeliveryOrderCard(
    order: Order,
    onMarkDelivered: () -> Unit,
    isCompleted: Boolean = false
) {
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
                icon = { Icon(Icons.Default.LocationOn, null, tint = Coral, modifier = Modifier.size(16.dp)) },
                text = order.deliveryAddress.ifBlank { "No address on this order" },
                color = TextSecondary
            )

            Spacer(Modifier.height(4.dp))

            InfoLine(
                icon = { Icon(Icons.Default.CalendarToday, null, tint = TextLight, modifier = Modifier.size(14.dp)) },
                text = order.createdAt.take(10),
                color = TextLight
            )

            if (!isCompleted && order.status != OrderStatus.DELIVERED.value) {
                Spacer(Modifier.height(14.dp))
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
    color: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 2.dp)) { icon() }
        Spacer(Modifier.width(6.dp))
        Text(text = text, fontSize = 12.sp, color = color, maxLines = 2, lineHeight = 17.sp)
    }
}
