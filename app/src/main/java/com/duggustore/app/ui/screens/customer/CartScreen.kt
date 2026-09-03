package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.duggustore.app.data.model.CartItem
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.QuantityStepperRow
import com.duggustore.app.ui.components.trimAmount
import com.duggustore.app.R
import com.duggustore.app.ui.theme.*

@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    subtotal: Double,
    deliveryFee: Double,
    total: Double,
    savings: Double,
    couponApplied: Boolean,
    couponDiscount: Double,
    isLoading: Boolean,
    onIncrementQuantity: (String, Int) -> Unit,
    onDecrementQuantity: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onApplyCoupon: (String) -> Unit,
    onPlaceOrder: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CartHeader(itemCount = cartItems.size, onBack = onBack)

            if (cartItems.isEmpty()) {
                EmptyCart()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        // Clears the summary sheet floating over the list.
                        bottom = 300.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems, key = { it.id }) { item ->
                        item.product?.let { product ->
                            CartRow(
                                product = product,
                                quantity = item.quantity,
                                onIncrement = { onIncrementQuantity(item.id, item.quantity + 1) },
                                onDecrement = {
                                    if (item.quantity <= 1) onRemoveItem(item.id)
                                    else onDecrementQuantity(item.id, item.quantity - 1)
                                },
                                onRemove = { onRemoveItem(item.id) }
                            )
                        }
                    }

                    item { CouponCard(onApplyCoupon = onApplyCoupon, applied = couponApplied) }
                }
            }
        }

        if (cartItems.isNotEmpty()) {
            SummarySheet(
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                total = total,
                savings = savings,
                couponApplied = couponApplied,
                couponDiscount = couponDiscount,
                isLoading = isLoading,
                onPlaceOrder = onPlaceOrder,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** Teal band with the back arrow, matching the other detail screens. */
@Composable
private fun CartHeader(itemCount: Int, onBack: () -> Unit) {
    Surface(color = Teal.copy(alpha = 0.92f), shadowElevation = 0.dp) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cart_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (itemCount == 1) stringResource(R.string.cart_one_item)
                           else stringResource(R.string.cart_item_count, itemCount),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun CartRow(
    product: Product,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNullOrBlank()) {
                    Icon(
                        Icons.Default.ShoppingBasket,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(30.dp)
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
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = product.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove ${product.name}",
                        tint = TextLight,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onRemove() }
                    )
                }

                Text(
                    text = product.unit,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "₹${trimAmount(product.effectivePrice() * quantity)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal
                        )
                        if (product.hasDiscount()) {
                            Text(
                                text = "₹${trimAmount(product.price * quantity)}",
                                fontSize = 12.sp,
                                color = TextLight,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                    QuantityStepperRow(
                        quantity = quantity,
                        onDecrease = onDecrement,
                        onIncrease = onIncrement,
                        modifier = Modifier.width(130.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CouponCard(onApplyCoupon: (String) -> Unit, applied: Boolean) {
    var code by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = OrangeSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalOffer,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            if (applied) {
                Text(
                    text = stringResource(R.string.cart_coupon_applied),
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeDark
                )
            } else {
                TextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.cart_coupon_hint),
                            fontSize = 13.sp,
                            color = TextLight
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Orange,
                    modifier = Modifier.clickable { onApplyCoupon(code) }
                ) {
                    Text(
                        text = stringResource(R.string.cart_apply),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SummarySheet(
    subtotal: Double,
    deliveryFee: Double,
    total: Double,
    savings: Double,
    couponApplied: Boolean,
    couponDiscount: Double,
    isLoading: Boolean,
    onPlaceOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = SurfaceWhite.copy(alpha = 0.92f),
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = stringResource(R.string.cart_bill_details),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))

            PriceRow(stringResource(R.string.cart_subtotal), "₹${trimAmount(subtotal)}")
            PriceRow(
                label = stringResource(R.string.cart_delivery_fee),
                value = if (deliveryFee <= 0.0) stringResource(R.string.cart_free)
                        else "₹${trimAmount(deliveryFee)}",
                color = if (deliveryFee <= 0.0) SuccessGreen else TextPrimary
            )
            if (couponApplied) {
                PriceRow(
                    stringResource(R.string.cart_coupon_discount),
                    "-₹${trimAmount(couponDiscount)}",
                    color = SuccessGreen
                )
            }
            if (savings > 0) {
                PriceRow(
                    stringResource(R.string.cart_you_save),
                    "-₹${trimAmount(savings)}",
                    color = SuccessGreen
                )
            }

            Divider(color = BorderGray, modifier = Modifier.padding(vertical = 10.dp))

            PriceRow(stringResource(R.string.cart_total), "₹${trimAmount(total)}", isBold = true)

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onPlaceOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
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
                        text = stringResource(R.string.cart_checkout),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyCart() {
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
                Icons.Outlined.ShoppingCart,
                contentDescription = null,
                tint = Teal,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.cart_empty_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.cart_empty_subtitle), fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
fun PriceRow(
    label: String,
    value: String,
    color: Color = TextPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (isBold) 16.sp else 14.sp,
            color = if (isBold) TextPrimary else TextSecondary,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = if (isBold) 19.sp else 14.sp,
            color = color,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
