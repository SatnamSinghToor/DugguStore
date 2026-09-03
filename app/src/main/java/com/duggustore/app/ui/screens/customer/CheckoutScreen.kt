package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Address
import com.duggustore.app.data.model.CartItem
import com.duggustore.app.ui.components.DugguButton
import com.duggustore.app.ui.components.DugguTopBar
import com.duggustore.app.ui.theme.*

@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    addresses: List<Address>,
    subtotal: Double,
    deliveryFee: Double,
    total: Double,
    savings: Double,
    isLoading: Boolean,
    error: String? = null,
    onManageAddresses: () -> Unit,
    onPlaceOrder: (deliveryAddress: String) -> Unit,
    onBack: () -> Unit
) {
    // Preselect the default address so the common case is a single tap.
    var selectedId by remember(addresses) {
        mutableStateOf(addresses.firstOrNull { it.isDefault }?.id ?: addresses.firstOrNull()?.id ?: "")
    }
    val selected = addresses.firstOrNull { it.id == selectedId }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        DugguTopBar(title = "Checkout", onBackClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle(icon = Icons.Default.LocationOn, text = "Delivery Address")

            Spacer(modifier = Modifier.height(10.dp))

            if (addresses.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onManageAddresses() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddLocationAlt, null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Add a delivery address", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Required before placing an order", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                addresses.forEach { address ->
                    AddressOption(
                        address = address,
                        selected = address.id == selectedId,
                        onSelect = { selectedId = address.id }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TextButton(onClick = onManageAddresses) {
                    Text("Manage addresses", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(icon = Icons.Default.Payments, text = "Payment")

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Cash on Delivery", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Pay when your order arrives", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Order Summary",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cartItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                text = "${item.quantity} × ${item.product?.name ?: "Item"}",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₹${"%.2f".format((item.product?.effectivePrice() ?: 0.0) * item.quantity)}",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderGray)

                    SummaryRow("Subtotal", subtotal)
                    SummaryRow("Delivery fee", deliveryFee)
                    if (savings > 0) SummaryRow("Savings", -savings, valueColor = SuccessGreen)

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderGray)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text(
                            text = "₹${"%.2f".format(total)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            error?.let { err ->
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                if (selected == null && addresses.isNotEmpty()) {
                    Text(
                        text = "Select a delivery address to continue",
                        fontSize = 12.sp,
                        color = AccentRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                DugguButton(
                    text = "Place Order · ₹${"%.2f".format(total)}",
                    onClick = { selected?.let { onPlaceOrder(it.fullAddress) } },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                    // An order with no address is not deliverable, so the button stays
                    // disabled until one is picked.
                    enabled = !isLoading && selected != null && cartItems.isNotEmpty()
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun AddressOption(address: Address, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryGreen.copy(alpha = 0.07f) else Color.White
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen) else null
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) PrimaryGreen else TextLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(address.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(address.fullAddress, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, valueColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text("₹${"%.2f".format(amount)}", fontSize = 13.sp, color = valueColor)
    }
}
