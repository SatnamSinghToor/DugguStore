package com.duggustore.app.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.data.model.Product
import com.duggustore.app.platform.LocationState
import com.duggustore.app.platform.rememberDeviceLocation
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun SellerDashboard(
    /** Driven by the bottom bar, which is the only tab control now. */
    selectedTab: Int,
    products: List<Product>,
    orders: List<Order>,
    totalRevenue: Double,
    totalOrders: Int,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit,
    hasStoreLocation: Boolean = true,
    onSaveStoreLocation: (String, Double, Double) -> Unit = { _, _, _ -> },
    onSignOut: () -> Unit
) {
    val pendingCount = orders.count { it.status == OrderStatus.PENDING.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        DashboardHeader(
            title = "Seller dashboard",
            subtitle = if (pendingCount == 0) "No orders waiting on you"
                       else if (pendingCount == 1) "1 order waiting on you"
                       else "$pendingCount orders waiting on you",
            stats = listOf(
                "Revenue" to "₹${trimAmount(totalRevenue)}",
                "Orders" to "$totalOrders",
                "Products" to "${products.size}"
            ),
            onSignOut = onSignOut
        )

        // Without this, delivery_id gets set on their orders but no rider has
        // any idea where the store actually is — nothing in the app ever
        // asked. Stays up until it's saved once.
        if (!hasStoreLocation) {
            StoreLocationBanner(onSave = onSaveStoreLocation)
        }

        when (selectedTab) {
            0 -> Box(modifier = Modifier.weight(1f)) {
                if (products.isEmpty()) {
                    DashboardEmpty(
                        icon = Icons.Default.Inventory,
                        title = "No products yet",
                        subtitle = "Add your first product to start selling"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductManagementCard(
                                product = product,
                                onEdit = { onEditProduct(product.id) },
                                onDelete = { onDeleteProduct(product.id) }
                            )
                        }
                    }
                }

                ExtendedFloatingActionButton(
                    onClick = onAddProduct,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    containerColor = Orange,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add product", fontWeight = FontWeight.Bold)
                }
            }

            else -> Box(modifier = Modifier.weight(1f)) {
                if (orders.isEmpty()) {
                    DashboardEmpty(
                        icon = Icons.Default.Receipt,
                        title = "No orders yet",
                        subtitle = "Orders from customers will appear here"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders, key = { it.id }) { order ->
                            OrderManagementCard(
                                order = order,
                                onUpdateStatus = { status -> onUpdateOrderStatus(order.id, status) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductManagementCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    DashboardPanel {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(SurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                // The seller uploads an image; showing a placeholder for every
                // product regardless made the list useless for telling them apart.
                if (product.imageUrl.isNullOrBlank()) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        null,
                        tint = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "₹${trimAmount(product.effectivePrice())} · per ${product.unit}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(5.dp))
                StockPill(stock = product.stock)
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit ${product.name}", tint = Teal)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete ${product.name}", tint = Coral)
            }
        }
    }
}

@Composable
private fun StockPill(stock: Int) {
    val (bg, fg, label) = when {
        stock <= 0 -> Triple(CoralSurface, CoralDark, "Out of stock")
        stock < 10 -> Triple(OrangeSurface, OrangeDark, "Low stock · $stock")
        else -> Triple(TealSurface, TealDark, "In stock · $stock")
    }
    Surface(shape = RoundedCornerShape(7.dp), color = bg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

/**
 * Prompt the seller taps once to record where their store actually is — a
 * device GPS fix, reverse-geocoded to something readable. Every rider who
 * later claims one of their orders reads this back to navigate to pickup.
 */
@Composable
private fun StoreLocationBanner(onSave: (String, Double, Double) -> Unit) {
    val detected = rememberDeviceLocation(autoStart = false)
    val found = detected.state as? LocationState.Found
    val busy = detected.state is LocationState.Locating

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = OrangeSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy) {
                    if (found != null) onSave(found.address, found.latitude, found.longitude)
                    else detected.refresh()
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = OrangeDark, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Set your store location",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeDark
                )
                Text(
                    text = when {
                        busy -> "Finding you…"
                        found != null -> "Tap to save: ${found.address}"
                        else -> "So a rider knows where to pick up your orders"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OrangeDark, strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
fun OrderManagementCard(
    order: Order,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    DashboardPanel {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.id.takeLast(8).uppercase()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                StatusBadge(status = order.status)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "₹${trimAmount(order.totalAmount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Teal
            )

            if (order.deliveryAddress.isNotBlank()) {
                Text(
                    text = order.deliveryAddress,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Before this, only a pending order had actions, so an order the
            // seller had accepted could never be moved on to preparing or
            // handed to delivery — it sat as "confirmed" forever.
            val next = when (order.status) {
                OrderStatus.CONFIRMED.value -> OrderStatus.PREPARING to "Start preparing"
                OrderStatus.PREPARING.value -> OrderStatus.READY_FOR_PICKUP to "Mark ready for pickup"
                else -> null
            }

            if (order.status == OrderStatus.PENDING.value) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardAction(
                        text = "Accept",
                        color = Teal,
                        onClick = { onUpdateStatus(OrderStatus.CONFIRMED) }
                    )
                    DashboardAction(
                        text = "Reject",
                        color = Coral,
                        filled = false,
                        onClick = { onUpdateStatus(OrderStatus.CANCELLED) }
                    )
                }
            } else if (next != null) {
                Spacer(Modifier.height(12.dp))
                DashboardAction(
                    text = next.second,
                    color = Orange,
                    onClick = { onUpdateStatus(next.first) }
                )
            }
        }
    }
}
