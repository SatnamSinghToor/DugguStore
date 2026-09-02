package com.duggustore.app.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun SellerDashboard(
    products: List<Product>,
    orders: List<Order>,
    totalRevenue: Double,
    totalOrders: Int,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Products", "Orders")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top Bar
        Surface(color = PrimaryGreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seller Dashboard",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, "Sign Out", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Revenue", "₹${"%.0f".format(totalRevenue)}", Modifier.weight(1f))
                    StatCard("Orders", "$totalOrders", Modifier.weight(1f))
                    StatCard("Products", "${products.size}", Modifier.weight(1f))
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PrimaryGreen
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Products Tab
                Box(modifier = Modifier.weight(1f)) {
                    if (products.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Inventory,
                            title = "No products yet",
                            subtitle = "Add your first product to start selling"
                        )
                    } else {
                        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(products) { product ->
                                ProductManagementCard(
                                    product = product,
                                    onEdit = { onEditProduct(product.id) },
                                    onDelete = { onDeleteProduct(product.id) }
                                )
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = onAddProduct,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, "Add Product")
                    }
                }
            }
            1 -> {
                // Orders Tab
                if (orders.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Receipt,
                        title = "No orders yet",
                        subtitle = "Orders from customers will appear here"
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(orders) { order ->
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
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun ProductManagementCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = Background
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ShoppingBag, null, tint = TextLight, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "₹${product.effectivePrice()} • Stock: ${product.stock}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = PrimaryGreen)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = AccentRed)
            }
        }
    }
}

@Composable
fun OrderManagementCard(
    order: Order,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.id.takeLast(8).uppercase()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "₹${"%.1f".format(order.totalAmount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )

            Text(
                text = order.deliveryAddress.take(50),
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1
            )

            if (order.status == "pending") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onUpdateStatus(OrderStatus.CONFIRMED) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Accept", color = Color.White, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onUpdateStatus(OrderStatus.CANCELLED) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Reject", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
