package com.duggustore.app.ui.screens.customer

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
import com.duggustore.app.data.model.CartItem
import com.duggustore.app.ui.components.*
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        DugguTopBar(
            title = "My Cart (${cartItems.size})",
            onBackClick = onBack
        )

        if (cartItems.isEmpty()) {
            EmptyState(
                icon = Icons.Default.ShoppingCart,
                title = "Your cart is empty",
                subtitle = "Add items to get started"
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                // Cart Items List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(cartItems) { item ->
                        item.product?.let { product ->
                            CartItemRow(
                                product = product,
                                quantity = item.quantity,
                                onIncrement = { onIncrementQuantity(item.id, item.quantity + 1) },
                                onDecrement = { onDecrementQuantity(item.id, item.quantity - 1) },
                                onRemove = { onRemoveItem(item.id) }
                            )
                            Divider(color = BorderGray)
                        }
                    }

                    // Coupon Section
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalOffer, null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                var couponInput by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = { couponInput = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Do You Have a Coupon?", fontSize = 13.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryGreen,
                                        unfocusedBorderColor = BorderGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onApplyCoupon(couponInput) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text("Apply", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Bottom Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Price Breakdown
                        PriceRow("Subtotal", "₹${"%.1f".format(subtotal)}")
                        PriceRow("Delivery Fee", "₹${"%.1f".format(deliveryFee)}")
                        if (couponApplied) {
                            PriceRow("Coupon Discount", "-₹${"%.1f".format(couponDiscount)}", color = DeliveredGreen)
                        }
                        if (savings > 0) {
                            PriceRow("You Save", "-₹${"%.1f".format(savings)}", color = SuccessGreen)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        PriceRow("Total Bill", "₹${"%.1f".format(total)}", isBold = true)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onPlaceOrder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Place Order ₹${"%.1f".format(total)}",
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = if (isBold) 18.sp else 14.sp,
            color = color,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
