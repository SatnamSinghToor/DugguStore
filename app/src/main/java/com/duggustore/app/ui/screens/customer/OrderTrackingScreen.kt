package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.duggustore.app.data.model.DeliveryTracking
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderIssue
import com.duggustore.app.data.model.OrderItem
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.data.model.Review
import com.duggustore.app.ui.components.ErrorRetryBlock
import com.duggustore.app.ui.components.RiderLocationCard
import com.duggustore.app.ui.components.StatusBadge
import com.duggustore.app.ui.components.trimAmount
import com.duggustore.app.ui.theme.*

private val ISSUE_REASONS = listOf("Missing item", "Damaged item", "Wrong item", "Quality issue", "Other")

private val ACTIVE_STATUSES = setOf(
    OrderStatus.PENDING.value,
    OrderStatus.CONFIRMED.value,
    OrderStatus.PREPARING.value,
    OrderStatus.READY_FOR_PICKUP.value,
    OrderStatus.OUT_FOR_DELIVERY.value
)

private val ORDER_STATUS_FILTERS = listOf("All", "Active", "Delivered", "Cancelled")

private fun Order.matchesStatusFilter(filter: String): Boolean = when (filter) {
    "Active" -> status in ACTIVE_STATUSES
    "Delivered" -> status == OrderStatus.DELIVERED.value
    "Cancelled" -> status == OrderStatus.CANCELLED.value
    else -> true
}

@OptIn(ExperimentalMaterialApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    orders: List<Order>,
    itemsByOrderId: Map<String, List<OrderItem>> = emptyMap(),
    onOrderClick: (String) -> Unit,
    onBack: () -> Unit,
    dueReminderOrderIds: Set<String> = emptySet(),
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedStatus: String = "All",
    onStatusSelected: (String) -> Unit = {}
) {
    val filteredOrders = remember(orders, searchQuery, selectedStatus, itemsByOrderId) {
        orders.filter { order ->
            val matchesStatus = order.matchesStatusFilter(selectedStatus)
            val matchesSearch = searchQuery.isBlank() || (itemsByOrderId[order.id] ?: emptyList()).any {
                it.product?.name?.contains(searchQuery, ignoreCase = true) == true
            }
            matchesStatus && matchesSearch
        }
    }

    val pullRefreshState = rememberPullRefreshState(refreshing = isLoading, onRefresh = onRefresh)

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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder = { Text("Search orders by product name") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = BorderGray
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ORDER_STATUS_FILTERS.forEach { filter ->
                FilterChip(
                    selected = selectedStatus == filter,
                    onClick = { onStatusSelected(filter) },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealSurface,
                        selectedLabelColor = TealDark
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Teal
            )
        }

        val dueOrder = orders.firstOrNull { it.id in dueReminderOrderIds }
        if (dueOrder != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onOrderClick(dueOrder.id) },
                shape = RoundedCornerShape(14.dp),
                color = TealSurface
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Teal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Running low? Reorder from #${dueOrder.id.takeLast(8).uppercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, null, tint = Teal)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pullRefresh(pullRefreshState)
        ) {
            when {
                error != null && !isLoading && orders.isEmpty() -> {
                    ErrorRetryBlock(message = error, onRetry = onRetry)
                }
                filteredOrders.isEmpty() -> {
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
                        Text(
                            text = if (orders.isEmpty()) "No orders yet" else "No matching orders",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (orders.isEmpty())
                                "Your orders will show up here once you place one"
                            else
                                "Try a different search or filter",
                            modifier = Modifier.padding(horizontal = 40.dp),
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                items = itemsByOrderId[order.id] ?: emptyList(),
                                onClick = { onOrderClick(order.id) }
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = Teal
            )
        }
    }
}

@Composable
fun OrderCard(order: Order, items: List<OrderItem> = emptyList(), onClick: () -> Unit) {
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

            // What was actually ordered, not just its number — the order id
            // above is for support/reference, not something a customer
            // recognises their groceries by.
            Row(verticalAlignment = Alignment.CenterVertically) {
                val firstItem = items.firstOrNull()
                val thumbnailUrl = firstItem?.product?.images()?.firstOrNull()
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailUrl != null) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Default.Receipt, null, tint = TextLight, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when {
                        items.isEmpty() -> "Loading items…"
                        items.size == 1 -> items[0].product?.name ?: "1 item"
                        else -> "${items[0].product?.name ?: "Item"} +${items.size - 1} more"
                    },
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    riderFixAgeMinutes: Long? = null,
    items: List<OrderItem> = emptyList(),
    myReviews: Map<String, Review> = emptyMap(),
    onSubmitReview: (productId: String, rating: Int, comment: String) -> Unit = { _, _, _ -> },
    onReorder: () -> Unit = {},
    myIssues: List<OrderIssue> = emptyList(),
    onReportIssue: (reason: String, description: String) -> Unit = { _, _ -> },
    hasReorderReminder: Boolean = false,
    onSetReorderReminder: () -> Unit = {}
) {
    val cancelled = order.status == OrderStatus.CANCELLED.value
    var showReportDialog by remember { mutableStateOf(false) }

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

            if (items.isNotEmpty()) {
                item {
                    Panel {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Items in this order",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                            items.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val imageUrl = item.product?.images()?.firstOrNull()
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceMuted),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (imageUrl != null) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().padding(3.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(Icons.Default.Receipt, null, tint = TextLight, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = item.product?.name ?: "Item",
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "×${item.quantity}",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "₹${trimAmount(item.priceAtPurchase * item.quantity)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                if (order.status == OrderStatus.DELIVERED.value) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val myRating = myReviews[item.productId]?.rating ?: 0
                                        Text(
                                            text = if (myRating > 0) "Your rating" else "Rate this item",
                                            fontSize = 11.sp,
                                            color = TextLight
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        ItemRatingStars(
                                            rating = myRating,
                                            onRate = { stars -> onSubmitReview(item.productId, stars, "") }
                                        )
                                    }
                                }
                                if (index != items.lastIndex) Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onReorder,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal)
                    ) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reorder these items", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                if (order.status == OrderStatus.DELIVERED.value) {
                    item {
                        if (hasReorderReminder) {
                            IssueStatusNotice(
                                text = "We'll remind you to reorder this in about a week",
                                color = Teal
                            )
                        } else {
                            TextButton(onClick = onSetReorderReminder, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp), tint = Teal)
                                Spacer(Modifier.width(6.dp))
                                Text("Remind me to reorder in a week", color = Teal, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            if (order.status == OrderStatus.DELIVERED.value) {
                item {
                    val latestIssue = myIssues.maxByOrNull { it.createdAt }
                    when {
                        latestIssue == null -> {
                            OutlinedButton(
                                onClick = { showReportDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                            ) {
                                Icon(Icons.Default.ReportProblem, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Report a problem", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        latestIssue.status == "open" -> IssueStatusNotice(
                            text = "Issue reported — the seller is reviewing it",
                            color = WarningYellow
                        )
                        latestIssue.status == "resolved" -> IssueStatusNotice(
                            text = "Resolved — ₹${latestIssue.refundAmount} credited to your wallet",
                            color = SuccessGreen
                        )
                        else -> IssueStatusNotice(text = "Your report was reviewed and rejected", color = Coral)
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
                        ageMinutes = riderFixAgeMinutes,
                        riderPhone = order.delivery?.phone
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

        if (showReportDialog) {
            ReportIssueDialog(
                onSubmit = { reason, description ->
                    onReportIssue(reason, description)
                    showReportDialog = false
                },
                onDismiss = { showReportDialog = false }
            )
        }
    }
}

@Composable
private fun IssueStatusNotice(text: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ReportProblem, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
private fun ReportIssueDialog(
    onSubmit: (reason: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf(ISSUE_REASONS.first()) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report a problem") },
        text = {
            Column {
                ISSUE_REASONS.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = Teal)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(reason, fontSize = 14.sp, color = TextPrimary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Tell us more (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selectedReason, description) }) {
                Text("Submit", color = Teal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

/** Tap a star to rate — submits immediately rather than waiting on a separate confirm step. */
@Composable
private fun ItemRatingStars(rating: Int, onRate: (Int) -> Unit) {
    Row {
        for (star in 1..5) {
            Icon(
                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Rate $star star${if (star == 1) "" else "s"}",
                tint = Orange,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onRate(star) }
            )
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
