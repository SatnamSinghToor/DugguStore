package com.duggustore.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.duggustore.app.data.model.Campaign
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Coupon
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.DashboardEmpty
import com.duggustore.app.ui.components.DashboardPanel
import com.duggustore.app.ui.components.trimAmount
import com.duggustore.app.ui.theme.*

/**
 * Everything that shapes what customers browse: the product catalog itself
 * (moderation only — sellers own creation/editing), the categories it's
 * organised under, the coupons that discount it, and the seasonal campaigns
 * that point at a category without a discount attached. Four segments
 * rather than four bottom-bar tabs, same pattern as Approvals' seller/
 * delivery split.
 */
@Composable
fun AdminCatalogScreen(
    products: List<Product>,
    categories: List<Category>,
    coupons: List<Coupon>,
    campaigns: List<Campaign>,
    isSaving: Boolean,
    catalogError: String?,
    onClearError: () -> Unit,
    onToggleProductActive: (Product) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onToggleCategoryActive: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveCoupon: (Coupon) -> Unit,
    onToggleCouponActive: (Coupon) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onSaveCampaign: (Campaign, Int) -> Unit,
    onToggleCampaignActive: (Campaign) -> Unit,
    onDeleteCampaign: (String) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryForm by remember { mutableStateOf(false) }
    var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
    var showCouponForm by remember { mutableStateOf(false) }
    var editingCampaign by remember { mutableStateOf<Campaign?>(null) }
    var showCampaignForm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CatalogTabChip("Products (${products.size})", tab == 0) { tab = 0 }
            CatalogTabChip("Categories (${categories.size})", tab == 1) { tab = 1 }
            CatalogTabChip("Coupons (${coupons.size})", tab == 2) { tab = 2 }
            CatalogTabChip("Campaigns (${campaigns.size})", tab == 3) { tab = 3 }
        }

        if (catalogError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = CoralSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(catalogError, modifier = Modifier.weight(1f), color = CoralDark, fontSize = 13.sp)
                    Text(
                        "Dismiss",
                        color = CoralDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp).clickable { onClearError() }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        when (tab) {
            0 -> {
                if (products.isEmpty()) {
                    DashboardEmpty(
                        icon = Icons.Default.ShoppingBag,
                        title = "No products",
                        subtitle = "Products added by sellers will appear here"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            AdminProductRow(product = product, onToggleActive = { onToggleProductActive(product) })
                        }
                    }
                }
            }
            1 -> {
                Box(modifier = Modifier.weight(1f)) {
                    if (categories.isEmpty()) {
                        DashboardEmpty(
                            icon = Icons.Default.LocalOffer,
                            title = "No categories",
                            subtitle = "Add one to start organising the catalog"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(categories.sortedBy { it.sortOrder }, key = { it.id }) { category ->
                                AdminCategoryRow(
                                    category = category,
                                    onEdit = { editingCategory = category; showCategoryForm = true },
                                    onToggleActive = { onToggleCategoryActive(category) },
                                    onDelete = { onDeleteCategory(category.id) }
                                )
                            }
                        }
                    }
                    AddFab(onClick = { editingCategory = null; showCategoryForm = true })
                }
            }
            2 -> {
                Box(modifier = Modifier.weight(1f)) {
                    if (coupons.isEmpty()) {
                        DashboardEmpty(
                            icon = Icons.Default.LocalOffer,
                            title = "No coupons",
                            subtitle = "Add one to start offering discounts"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(coupons, key = { it.id }) { coupon ->
                                AdminCouponRow(
                                    coupon = coupon,
                                    onEdit = { editingCoupon = coupon; showCouponForm = true },
                                    onToggleActive = { onToggleCouponActive(coupon) },
                                    onDelete = { onDeleteCoupon(coupon.id) }
                                )
                            }
                        }
                    }
                    AddFab(onClick = { editingCoupon = null; showCouponForm = true })
                }
            }
            else -> {
                Box(modifier = Modifier.weight(1f)) {
                    if (campaigns.isEmpty()) {
                        DashboardEmpty(
                            icon = Icons.Default.LocalOffer,
                            title = "No campaigns",
                            subtitle = "Add a seasonal push to point customers at a category"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(campaigns, key = { it.id }) { campaign ->
                                AdminCampaignRow(
                                    campaign = campaign,
                                    categoryName = categories.firstOrNull { it.id == campaign.categoryId }?.name,
                                    onEdit = { editingCampaign = campaign; showCampaignForm = true },
                                    onToggleActive = { onToggleCampaignActive(campaign) },
                                    onDelete = { onDeleteCampaign(campaign.id) }
                                )
                            }
                        }
                    }
                    AddFab(onClick = { editingCampaign = null; showCampaignForm = true })
                }
            }
        }
    }

    if (showCategoryForm) {
        CategoryFormDialog(
            existing = editingCategory,
            isSaving = isSaving,
            onDismiss = { showCategoryForm = false },
            onSave = { category ->
                onSaveCategory(category)
                showCategoryForm = false
            }
        )
    }

    if (showCouponForm) {
        CouponFormDialog(
            existing = editingCoupon,
            isSaving = isSaving,
            onDismiss = { showCouponForm = false },
            onSave = { coupon ->
                onSaveCoupon(coupon)
                showCouponForm = false
            }
        )
    }

    if (showCampaignForm) {
        CampaignFormDialog(
            existing = editingCampaign,
            categories = categories,
            isSaving = isSaving,
            onDismiss = { showCampaignForm = false },
            onSave = { campaign, durationDays ->
                onSaveCampaign(campaign, durationDays)
                showCampaignForm = false
            }
        )
    }
}

@Composable
private fun BoxScope.AddFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        containerColor = Teal,
        contentColor = Color.White
    ) {
        Icon(Icons.Default.Add, "Add")
    }
}

@Composable
private fun AdminProductRow(product: Product, onToggleActive: () -> Unit) {
    DashboardPanel {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNullOrBlank()) {
                    Icon(Icons.Default.ShoppingBag, null, tint = TextLight, modifier = Modifier.size(21.dp))
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
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (product.isActive) "Active" else "Inactive",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (product.isActive) SuccessGreen else TextLight
            )
            Switch(
                checked = product.isActive,
                onCheckedChange = { onToggleActive() },
                colors = SwitchDefaults.colors(checkedTrackColor = Teal)
            )
        }
    }
}

@Composable
private fun AdminCategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    DashboardPanel {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = runCatching { Color(android.graphics.Color.parseColor(category.colorHex)) }.getOrDefault(Teal)
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (!category.iconUrl.isNullOrBlank()) {
                    AsyncImage(model = category.iconUrl, contentDescription = category.name, modifier = Modifier.size(24.dp))
                } else {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(tint))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Order ${category.sortOrder}", fontSize = 11.sp, color = TextLight)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Coral, modifier = Modifier.size(19.dp))
            }
            Switch(
                checked = category.isActive,
                onCheckedChange = { onToggleActive() },
                colors = SwitchDefaults.colors(checkedTrackColor = Teal)
            )
        }
    }
}

@Composable
private fun AdminCouponRow(
    coupon: Coupon,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    DashboardPanel {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(coupon.code, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Teal)
                    Text(coupon.title, fontSize = 13.sp, color = TextPrimary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Coral, modifier = Modifier.size(19.dp))
                }
                Switch(
                    checked = coupon.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Teal)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${coupon.discountPercent}% off, up to ₹${coupon.maxDiscount} · min order ₹${coupon.minOrderValue}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            if (coupon.expiryLabel.isNotBlank()) {
                Text(coupon.expiryLabel, fontSize = 11.sp, color = TextLight)
            }
        }
    }
}

@Composable
private fun AdminCampaignRow(
    campaign: Campaign,
    categoryName: String?,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    DashboardPanel {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val tint = runCatching { Color(android.graphics.Color.parseColor(campaign.tintHex)) }.getOrDefault(Orange)
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tint))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(campaign.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(categoryName ?: "No category linked", fontSize = 12.sp, color = TextSecondary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Coral, modifier = Modifier.size(19.dp))
                }
                Switch(
                    checked = campaign.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Teal)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "\"${campaign.ctaLabel}\" · runs ${campaign.startsAt.take(10)} to ${campaign.endsAt.take(10)}",
                fontSize = 12.sp,
                color = TextLight
            )
        }
    }
}

@Composable
private fun CategoryFormDialog(
    existing: Category?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: "#7C3AED") }
    var iconUrl by remember { mutableStateOf(existing?.iconUrl.orEmpty()) }
    var sortOrder by remember { mutableStateOf((existing?.sortOrder ?: 0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add category" else "Edit category") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text("Color hex, e.g. #7C3AED") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Icon URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortOrder,
                    onValueChange = { input -> if (input.all { it.isDigit() }) sortOrder = input },
                    label = { Text("Sort order") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !isSaving,
                onClick = {
                    onSave(
                        Category(
                            id = existing?.id.orEmpty(),
                            name = name.trim(),
                            iconUrl = iconUrl.trim().ifBlank { null },
                            colorHex = colorHex.trim().ifBlank { "#7C3AED" },
                            sortOrder = sortOrder.toIntOrNull() ?: 0,
                            isActive = existing?.isActive ?: true
                        )
                    )
                }
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CouponFormDialog(
    existing: Coupon?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Coupon) -> Unit
) {
    var code by remember { mutableStateOf(existing?.code.orEmpty()) }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var discountPercent by remember { mutableStateOf((existing?.discountPercent ?: 0).toString()) }
    var maxDiscount by remember { mutableStateOf((existing?.maxDiscount ?: 0).toString()) }
    var minOrderValue by remember { mutableStateOf((existing?.minOrderValue ?: 0).toString()) }
    var expiryLabel by remember { mutableStateOf(existing?.expiryLabel.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add coupon" else "Edit coupon") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Code, e.g. FIRST50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountPercent,
                        onValueChange = { input -> if (input.all { it.isDigit() }) discountPercent = input },
                        label = { Text("Discount %") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxDiscount,
                        onValueChange = { input -> if (input.all { it.isDigit() }) maxDiscount = input },
                        label = { Text("Max ₹") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = minOrderValue,
                    onValueChange = { input -> if (input.all { it.isDigit() }) minOrderValue = input },
                    label = { Text("Minimum order value ₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expiryLabel,
                    onValueChange = { expiryLabel = it },
                    label = { Text("Expiry label, e.g. \"Valid till 30 Sep\"") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank() && title.isNotBlank() && !isSaving,
                onClick = {
                    onSave(
                        Coupon(
                            id = existing?.id.orEmpty(),
                            code = code.trim(),
                            title = title.trim(),
                            description = description.trim(),
                            discountPercent = discountPercent.toIntOrNull() ?: 0,
                            maxDiscount = maxDiscount.toIntOrNull() ?: 0,
                            minOrderValue = minOrderValue.toIntOrNull() ?: 0,
                            expiryLabel = expiryLabel.trim(),
                            isActive = existing?.isActive ?: true
                        )
                    )
                }
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** [durationDays] always runs the campaign starting now — editing an existing one restarts its window rather than adjusting a fixed range, which keeps this form to one field instead of two date pickers the app has no component for anywhere else. */
@Composable
private fun CampaignFormDialog(
    existing: Campaign?,
    categories: List<Category>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Campaign, Int) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label.orEmpty()) }
    var tintHex by remember { mutableStateOf(existing?.tintHex ?: "#F5A623") }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var ctaLabel by remember { mutableStateOf(existing?.ctaLabel ?: "SHOP NOW") }
    var durationDays by remember { mutableStateOf("7") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add campaign" else "Renew campaign") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label, e.g. \"Diwali Dhamaka\"") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tintHex,
                    onValueChange = { tintHex = it },
                    label = { Text("Tint hex, e.g. #F5A623") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == categoryId }?.name ?: "No category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().clickable { showCategoryMenu = true }
                    )
                    // A transparent click-catcher on top of the read-only field —
                    // OutlinedTextField itself doesn't take a click when readOnly.
                    Box(modifier = Modifier.matchParentSize().clickable { showCategoryMenu = true })
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("No category") },
                            onClick = { categoryId = null; showCategoryMenu = false }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { categoryId = category.id; showCategoryMenu = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = ctaLabel,
                    onValueChange = { ctaLabel = it.uppercase() },
                    label = { Text("Button text, e.g. SHOP NOW") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durationDays,
                    onValueChange = { input -> if (input.all { it.isDigit() }) durationDays = input },
                    label = { Text("Runs for how many days, starting now") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && (durationDays.toIntOrNull() ?: 0) > 0 && !isSaving,
                onClick = {
                    onSave(
                        Campaign(
                            id = existing?.id.orEmpty(),
                            label = label.trim(),
                            tintHex = tintHex.trim().ifBlank { "#F5A623" },
                            categoryId = categoryId,
                            ctaLabel = ctaLabel.trim().ifBlank { "SHOP NOW" },
                            isActive = existing?.isActive ?: true
                        ),
                        durationDays.toIntOrNull() ?: 7
                    )
                }
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CatalogTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
