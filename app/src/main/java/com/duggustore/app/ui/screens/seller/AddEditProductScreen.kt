package com.duggustore.app.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.DugguButton
import com.duggustore.app.ui.components.DugguTextField
import com.duggustore.app.ui.components.DugguTopBar
import com.duggustore.app.ui.theme.*

@Composable
fun AddEditProductScreen(
    product: Product?,
    categories: List<Category>,
    sellerId: String,
    isLoading: Boolean,
    error: String? = null,
    onSave: (Product) -> Unit,
    onBack: () -> Unit
) {
    val editing = product != null

    var name by remember(product) { mutableStateOf(product?.name ?: "") }
    var description by remember(product) { mutableStateOf(product?.description ?: "") }
    var price by remember(product) { mutableStateOf(product?.price?.takeIf { it > 0 }?.toString() ?: "") }
    var discountPrice by remember(product) { mutableStateOf(product?.discountPrice?.toString() ?: "") }
    var stock by remember(product) { mutableStateOf(product?.stock?.toString() ?: "") }
    var unit by remember(product) { mutableStateOf(product?.unit ?: "pcs") }
    var imageUrl by remember(product) { mutableStateOf(product?.imageUrl ?: "") }
    var categoryId by remember(product) { mutableStateOf(product?.categoryId ?: "") }
    var isActive by remember(product) { mutableStateOf(product?.isActive ?: true) }
    var localError by remember { mutableStateOf<String?>(null) }

    val priceValue = price.toDoubleOrNull()
    val discountValue = discountPrice.takeIf { it.isNotBlank() }?.toDoubleOrNull()
    val selectedCategory = categories.firstOrNull { it.id == categoryId }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        DugguTopBar(
            title = if (editing) "Edit Product" else "Add Product",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DugguTextField(value = name, onValueChange = { name = it; localError = null }, label = "Product name")

            Spacer(modifier = Modifier.height(12.dp))

            DugguTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                singleLine = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            CategoryDropdown(
                categories = categories,
                selected = selectedCategory,
                onSelect = { categoryId = it.id; localError = null }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                Box(modifier = Modifier.weight(1f)) {
                    DugguTextField(
                        value = price,
                        onValueChange = { price = it; localError = null },
                        label = "Price (₹)",
                        keyboardType = KeyboardType.Decimal
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    DugguTextField(
                        value = discountPrice,
                        onValueChange = { discountPrice = it; localError = null },
                        label = "Sale price",
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                Box(modifier = Modifier.weight(1f)) {
                    DugguTextField(
                        value = stock,
                        onValueChange = { stock = it; localError = null },
                        label = "Stock",
                        keyboardType = KeyboardType.Number
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    DugguTextField(value = unit, onValueChange = { unit = it }, label = "Unit (kg, pcs…)")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            DugguTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = "Image URL (optional)")

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Visible to customers", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        text = if (isActive) "Listed in the store" else "Hidden from the store",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            (localError ?: error)?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = AccentRed.copy(alpha = 0.1f)
                ) {
                    Text(err, modifier = Modifier.padding(12.dp), color = AccentRed, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Surface(color = Color.White, shadowElevation = 12.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                DugguButton(
                    text = if (editing) "Save Changes" else "Add Product",
                    onClick = {
                        localError = when {
                            name.isBlank() -> "Enter a product name"
                            categoryId.isBlank() -> "Pick a category"
                            priceValue == null || priceValue <= 0 -> "Enter a valid price"
                            // The products table enforces discount_price < price, so a bad
                            // value would come back as an opaque database error.
                            discountValue != null && discountValue >= priceValue ->
                                "Sale price must be lower than the price"
                            stock.toIntOrNull() == null || stock.toInt() < 0 -> "Enter a valid stock count"
                            else -> null
                        }
                        if (localError == null) {
                            onSave(
                                Product(
                                    id = product?.id ?: "",
                                    sellerId = product?.sellerId?.takeIf { it.isNotBlank() } ?: sellerId,
                                    categoryId = categoryId,
                                    name = name.trim(),
                                    description = description.trim(),
                                    price = priceValue ?: 0.0,
                                    discountPrice = discountValue,
                                    imageUrl = imageUrl.trim().takeIf { it.isNotBlank() },
                                    stock = stock.toIntOrNull() ?: 0,
                                    unit = unit.trim().ifBlank { "pcs" },
                                    isActive = isActive
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                    enabled = !isLoading
                )
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Category", fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected?.name ?: "Select a category",
                    fontSize = 15.sp,
                    color = if (selected == null) TextLight else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = { onSelect(category); expanded = false }
                )
            }
        }
    }
}
