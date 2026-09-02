package com.duggustore.app.ui.screens.admin

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
import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun AdminDashboard(
    users: List<UserProfile>,
    orders: List<Order>,
    products: List<Product>,
    totalUsers: Int,
    totalOrders: Int,
    totalRevenue: Double,
    totalDeliveries: Int,
    onUpdateUserRole: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Users", "Orders", "Products")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Surface(color = PrimaryGreenDark) {
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
                        text = "Admin Dashboard",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, "Sign Out", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Analytics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsCard("Total Delivery", "$totalDeliveries", Modifier.weight(1f))
                    AnalyticsCard("Total Ordered", "$totalOrders", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsCard("Total Users", "$totalUsers", Modifier.weight(1f))
                    AnalyticsCard("Revenue", "₹${"%.0f".format(totalRevenue)}", Modifier.weight(1f))
                }
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PrimaryGreen
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Overview
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        SectionHeader(title = "Recent Orders")
                    }
                    items(orders.take(5)) { order ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Order #${order.id.takeLast(8).uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("₹${"%.1f".format(order.totalAmount)}", fontSize = 14.sp, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                                }
                                StatusBadge(status = order.status)
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Top Products")
                    }
                    items(products.take(5)) { product ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ShoppingBag, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("Stock: ${product.stock}", fontSize = 12.sp, color = TextSecondary)
                                }
                                Text("₹${product.effectivePrice()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                    }
                }
            }
            1 -> {
                // Users
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users) { user ->
                        UserManagementCard(user = user, onUpdateRole = { role -> onUpdateUserRole(user.id, role) })
                    }
                }
            }
            2 -> {
                // Orders
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orders) { order ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Order #${order.id.takeLast(8).uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("₹${"%.1f".format(order.totalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                    Text(order.createdAt, fontSize = 11.sp, color = TextLight)
                                }
                                StatusBadge(status = order.status)
                            }
                        }
                    }
                }
            }
            3 -> {
                // Products
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products) { product ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ShoppingBag, null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Row {
                                        Text("₹${product.effectivePrice()}", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Stock: ${product.stock}", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                                if (!product.isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentRed.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            "Inactive",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            color = AccentRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementCard(
    user: UserProfile,
    onUpdateRole: (String) -> Unit
) {
    var showRoleMenu by remember { mutableStateOf(false) }

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
            // Avatar
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = PrimaryGreen.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.fullName.firstOrNull()?.toString() ?: "U",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(user.phone, fontSize = 12.sp, color = TextSecondary)
            }

            // Role Selector
            Box {
                Surface(
                    onClick = { showRoleMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    color = when (user.role) {
                        "admin" -> AccentRed.copy(alpha = 0.1f)
                        "seller" -> PrimaryGreen.copy(alpha = 0.1f)
                        "delivery" -> InfoBlue.copy(alpha = 0.1f)
                        else -> WarningYellow.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = user.role.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (user.role) {
                            "admin" -> AccentRed
                            "seller" -> PrimaryGreen
                            "delivery" -> InfoBlue
                            else -> WarningYellow
                        }
                    )
                }

                DropdownMenu(
                    expanded = showRoleMenu,
                    onDismissRequest = { showRoleMenu = false }
                ) {
                    listOf("customer", "seller", "delivery", "admin").forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.uppercase()) },
                            onClick = {
                                onUpdateRole(role)
                                showRoleMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
