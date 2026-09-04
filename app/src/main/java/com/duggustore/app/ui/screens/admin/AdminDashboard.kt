package com.duggustore.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Coupon
import com.duggustore.app.data.model.DeliveryPartner
import com.duggustore.app.data.model.DeliveryPartnerDocument
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderIssue
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.model.Seller
import com.duggustore.app.data.model.SellerDocument
import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.screens.seller.IssuesList
import com.duggustore.app.ui.theme.*

private val ROLES = listOf("customer", "seller", "delivery", "admin")

@Composable
fun AdminDashboard(
    /** Driven by the bottom bar, which is the only tab control now. */
    selectedTab: Int,
    users: List<UserProfile>,
    orders: List<Order>,
    products: List<Product>,
    totalUsers: Int,
    totalOrders: Int,
    totalRevenue: Double,
    totalDeliveries: Int,
    onUpdateUserRole: (String, String) -> Unit,
    /** Unfiltered — [AdminApprovalsScreen] narrows to the ones actually awaiting a decision. */
    allSellers: List<Seller>,
    sellerDocuments: Map<String, List<SellerDocument>>,
    sellerDocumentUrls: Map<String, String>,
    loadingSellerDocsFor: String?,
    onLoadSellerDocuments: (String) -> Unit,
    onReviewSeller: (String, Boolean, String) -> Unit,
    reviewingSellerId: String?,
    sellerReviewError: String?,
    onClearSellerReviewError: () -> Unit,
    allPartners: List<DeliveryPartner>,
    partnerDocuments: Map<String, List<DeliveryPartnerDocument>>,
    partnerDocumentUrls: Map<String, String>,
    loadingPartnerDocsFor: String?,
    onLoadPartnerDocuments: (String) -> Unit,
    onReviewPartner: (String, Boolean, String) -> Unit,
    reviewingPartnerId: String?,
    partnerReviewError: String?,
    onClearPartnerReviewError: () -> Unit,
    issues: List<OrderIssue>,
    onResolveIssue: (issueId: String, approve: Boolean, refundAmount: Int) -> Unit,
    categories: List<Category>,
    coupons: List<Coupon>,
    isSavingCatalog: Boolean,
    catalogError: String?,
    onClearCatalogError: () -> Unit,
    onToggleProductActive: (Product) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onToggleCategoryActive: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveCoupon: (Coupon) -> Unit,
    onToggleCouponActive: (Coupon) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        DashboardHeader(
            title = "Admin",
            subtitle = "Everything across the store",
            stats = listOf(
                "Revenue" to "₹${trimAmount(totalRevenue)}",
                "Orders" to "$totalOrders",
                "Users" to "$totalUsers",
                "Delivered" to "$totalDeliveries"
            ),
            onSignOut = onSignOut
        )

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> OverviewTab(orders = orders, products = products)
                1 -> UsersTab(users = users, onUpdateUserRole = onUpdateUserRole)
                2 -> OrdersTab(orders = orders, issues = issues, onResolveIssue = onResolveIssue)
                3 -> AdminCatalogScreen(
                    products = products,
                    categories = categories,
                    coupons = coupons,
                    isSaving = isSavingCatalog,
                    catalogError = catalogError,
                    onClearError = onClearCatalogError,
                    onToggleProductActive = onToggleProductActive,
                    onSaveCategory = onSaveCategory,
                    onToggleCategoryActive = onToggleCategoryActive,
                    onDeleteCategory = onDeleteCategory,
                    onSaveCoupon = onSaveCoupon,
                    onToggleCouponActive = onToggleCouponActive,
                    onDeleteCoupon = onDeleteCoupon
                )
                else -> AdminApprovalsScreen(
                    sellers = allSellers,
                    sellerDocuments = sellerDocuments,
                    sellerDocumentUrls = sellerDocumentUrls,
                    loadingSellerDocsFor = loadingSellerDocsFor,
                    onLoadSellerDocuments = onLoadSellerDocuments,
                    onReviewSeller = onReviewSeller,
                    reviewingSellerId = reviewingSellerId,
                    sellerReviewError = sellerReviewError,
                    onClearSellerReviewError = onClearSellerReviewError,
                    partners = allPartners,
                    partnerDocuments = partnerDocuments,
                    partnerDocumentUrls = partnerDocumentUrls,
                    loadingPartnerDocsFor = loadingPartnerDocsFor,
                    onLoadPartnerDocuments = onLoadPartnerDocuments,
                    onReviewPartner = onReviewPartner,
                    reviewingPartnerId = reviewingPartnerId,
                    partnerReviewError = partnerReviewError,
                    onClearPartnerReviewError = onClearPartnerReviewError
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(orders: List<Order>, products: List<Product>) {
    if (orders.isEmpty() && products.isEmpty()) {
        DashboardEmpty(
            icon = Icons.Default.Inventory,
            title = "Nothing to show yet",
            subtitle = "Orders and products will appear here as the store is used"
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (orders.isNotEmpty()) {
            item { GroupTitle("Recent orders") }
            items(orders.take(5), key = { "recent-${it.id}" }) { OrderRow(it) }
        }
        if (products.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                GroupTitle("Products")
            }
            items(products.take(5), key = { "top-${it.id}" }) { ProductRow(it) }
        }
    }
}

@Composable
private fun UsersTab(users: List<UserProfile>, onUpdateUserRole: (String, String) -> Unit) {
    if (users.isEmpty()) {
        DashboardEmpty(
            icon = Icons.Default.Person,
            title = "No users",
            subtitle = "Accounts will appear here as people sign up"
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(users, key = { it.id }) { user ->
            UserManagementCard(
                user = user,
                onUpdateRole = { role -> onUpdateUserRole(user.id, role) }
            )
        }
    }
}

@Composable
private fun OrdersTab(
    orders: List<Order>,
    issues: List<OrderIssue>,
    onResolveIssue: (issueId: String, approve: Boolean, refundAmount: Int) -> Unit
) {
    var showIssues by rememberSaveable { mutableStateOf(false) }
    val openIssueCount = issues.count { it.status == "open" }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OrdersTabChip("All orders (${orders.size})", !showIssues) { showIssues = false }
            OrdersTabChip(
                if (openIssueCount > 0) "Issues ($openIssueCount)" else "Issues",
                showIssues
            ) { showIssues = true }
        }

        if (!showIssues) {
            if (orders.isEmpty()) {
                DashboardEmpty(
                    icon = Icons.Default.Receipt,
                    title = "No orders",
                    subtitle = "Orders placed in the store will appear here"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders, key = { it.id }) { OrderRow(order = it, showDate = true) }
                }
            }
        } else {
            IssuesList(issues = issues, onResolve = onResolveIssue, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun OrdersTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Teal else SurfaceMuted,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 2.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

@Composable
private fun OrderRow(order: Order, showDate: Boolean = false) {
    DashboardPanel {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${order.id.takeLast(8).uppercase()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "₹${trimAmount(order.totalAmount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Teal
                )
                if (showDate) {
                    Text(order.createdAt.take(10), fontSize = 11.sp, color = TextLight)
                }
            }
            StatusBadge(status = order.status)
        }
    }
}

@Composable
private fun ProductRow(product: Product) {
    DashboardPanel {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNullOrBlank()) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        null,
                        tint = TextLight,
                        modifier = Modifier.size(21.dp)
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "₹${trimAmount(product.effectivePrice())} · stock ${product.stock}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            if (!product.isActive) {
                Surface(shape = RoundedCornerShape(7.dp), color = CoralSurface) {
                    Text(
                        text = "INACTIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralDark
                    )
                }
            }
        }
    }
}

@Composable
fun UserManagementCard(user: UserProfile, onUpdateRole: (String) -> Unit) {
    var showRoleMenu by remember { mutableStateOf(false) }
    val (bg, fg) = roleColors(user.role)

    DashboardPanel {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TealSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.fullName.trim().firstOrNull()?.uppercase() ?: "U",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Teal
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName.takeIf { it.isNotBlank() } ?: "Unnamed",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.phone.takeIf { it.isNotBlank() } ?: "No phone on file",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { showRoleMenu = true }
                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.role.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Change role",
                        tint = fg,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showRoleMenu,
                    onDismissRequest = { showRoleMenu = false }
                ) {
                    ROLES.forEach { role ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = role.replaceFirstChar { it.uppercase() },
                                    fontWeight = if (role == user.role) FontWeight.Bold
                                                 else FontWeight.Normal,
                                    color = if (role == user.role) Teal else TextPrimary
                                )
                            },
                            onClick = {
                                showRoleMenu = false
                                // Re-sending the role the user already has is a
                                // write for nothing.
                                if (role != user.role) onUpdateRole(role)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun roleColors(role: String): Pair<Color, Color> = when (role) {
    "admin" -> CoralSurface to CoralDark
    "seller" -> TealSurface to TealDark
    "delivery" -> Color(0xFFE3F0FD) to InfoBlue
    else -> OrangeSurface to OrangeDark
}
