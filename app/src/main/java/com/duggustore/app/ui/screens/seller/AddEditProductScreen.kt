package com.duggustore.app.ui.screens.seller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.AuthField
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
    val shownError = localError ?: error

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        Surface(color = Teal.copy(alpha = 0.92f)) {
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
                Text(
                    text = if (editing) "Edit product" else "Add product",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ImagePreview(imageUrl = imageUrl)

            Spacer(Modifier.height(16.dp))

            AuthField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = "Image URL",
                placeholder = "Optional — paste a link to a photo"
            )

            Spacer(Modifier.height(16.dp))

            AuthField(
                value = name,
                onValueChange = { name = it; localError = null },
                label = "Product name",
                placeholder = "What are you selling?"
            )

            Spacer(Modifier.height(16.dp))

            AuthField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Optional"
            )

            Spacer(Modifier.height(16.dp))

            CategoryDropdown(
                categories = categories,
                selected = selectedCategory,
                onSelect = { categoryId = it.id; localError = null }
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    AuthField(
                        value = price,
                        onValueChange = { price = it; localError = null },
                        label = "Price (₹)",
                        placeholder = "0",
                        keyboardType = KeyboardType.Decimal
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AuthField(
                        value = discountPrice,
                        onValueChange = { discountPrice = it; localError = null },
                        label = "Sale price",
                        placeholder = "Optional",
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    AuthField(
                        value = stock,
                        onValueChange = { stock = it; localError = null },
                        label = "Stock",
                        placeholder = "0",
                        keyboardType = KeyboardType.Number
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AuthField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = "Unit",
                        placeholder = "kg, pcs…"
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Visible to customers",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isActive) "Listed in the store" else "Hidden from the store",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Teal
                        )
                    )
                }
            }

            if (shownError != null) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CoralSurface
                ) {
                    Text(
                        text = shownError,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        color = CoralDark,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            shadowElevation = 18.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
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
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal,
                        disabledContainerColor = BorderGray
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (editing) "Save changes" else "Add product",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** Shows the image the URL points at, so a wrong link is obvious before saving. */
@Composable
private fun ImagePreview(imageUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceMuted),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("No image", fontSize = 12.sp, color = TextLight)
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Product image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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
        Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = SurfaceMuted,
            border = if (selected == null) BorderStroke(1.dp, BorderGray) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected?.name ?: "Select a category",
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    color = if (selected == null) TextLight else TextPrimary
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (categories.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No categories available", color = TextSecondary) },
                    onClick = { expanded = false }
                )
            } else {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = category.name,
                                fontWeight = if (category.id == selected?.id) FontWeight.Bold
                                             else FontWeight.Normal,
                                color = if (category.id == selected?.id) Teal else TextPrimary
                            )
                        },
                        onClick = { onSelect(category); expanded = false }
                    )
                }
            }
        }
    }
}
