package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Address
import com.duggustore.app.data.model.CartItem
import com.duggustore.app.ui.components.appPatternOverlay
import com.duggustore.app.ui.components.trimAmount
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
    onPlaceOrder: (deliveryAddress: String, latitude: Double?, longitude: Double?) -> Unit,
    onBack: () -> Unit
) {
    // Preselect the default address so the common case is a single tap.
    var selectedId by remember(addresses) {
        mutableStateOf(
            addresses.firstOrNull { it.isDefault }?.id ?: addresses.firstOrNull()?.id ?: ""
        )
    }
    val selected = addresses.firstOrNull { it.id == selectedId }

    Column(modifier = Modifier.fillMaxSize().background(Background).appPatternOverlay()) {
        CheckoutHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle(Icons.Default.LocationOn, "Delivery address", Teal)
            Spacer(Modifier.height(10.dp))

            if (addresses.isEmpty()) {
                Panel(modifier = Modifier.clickable { onManageAddresses() }) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(Icons.Default.AddLocationAlt, Teal)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Add a delivery address",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                "Required before placing an order",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
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
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    text = "Manage addresses",
                    modifier = Modifier.clickable { onManageAddresses() },
                    color = Teal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(22.dp))

            SectionTitle(Icons.Default.Payments, "Payment", Orange)
            Spacer(Modifier.height(10.dp))

            Panel {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTile(Icons.Default.Payments, Orange)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Cash on delivery",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            "Pay when your order arrives",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            SectionTitle(Icons.Default.Receipt, "Order summary", Coral)
            Spacer(Modifier.height(10.dp))

            Panel {
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
                                text = "₹${trimAmount((item.product?.effectivePrice() ?: 0.0) * item.quantity)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }

                    Divider(Modifier.padding(vertical = 10.dp), color = BorderGray)

                    SummaryRow("Subtotal", "₹${trimAmount(subtotal)}")
                    SummaryRow(
                        label = "Delivery fee",
                        value = if (deliveryFee <= 0.0) "FREE" else "₹${trimAmount(deliveryFee)}",
                        valueColor = if (deliveryFee <= 0.0) SuccessGreen else TextPrimary
                    )
                    if (savings > 0) {
                        SummaryRow("You save", "-₹${trimAmount(savings)}", SuccessGreen)
                    }

                    Divider(Modifier.padding(vertical = 10.dp), color = BorderGray)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Total",
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "₹${trimAmount(total)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CoralSurface
                ) {
                    Text(
                        text = error,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (selected == null && addresses.isNotEmpty()) {
                    Text(
                        text = "Select a delivery address to continue",
                        fontSize = 12.sp,
                        color = Coral
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        selected?.let {
                            // 0.0 is the column default for an address that was
                            // typed by hand rather than detected — treated as
                            // "no fix" rather than a real coordinate near
                            // (0°, 0°).
                            val lat = it.latitude.takeIf { v -> v != 0.0 }
                            val lng = it.longitude.takeIf { v -> v != 0.0 }
                            onPlaceOrder(it.fullAddress, lat, lng)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal,
                        disabledContainerColor = BorderGray
                    ),
                    // An order with no address is not deliverable, so the button
                    // stays disabled until one is picked.
                    enabled = !isLoading && selected != null && cartItems.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Place order · ₹${trimAmount(total)}",
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

@Composable
private fun CheckoutHeader(onBack: () -> Unit) {
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
            Text(
                text = "Checkout",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp,
        content = content
    )
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun AddressOption(address: Address, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) TealSurface else SurfaceWhite,
        shadowElevation = if (selected) 0.dp else 2.dp,
        border = if (selected) BorderStroke(1.5.dp, Teal) else null
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle
                              else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) Teal else TextLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(address.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(address.fullAddress, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}
