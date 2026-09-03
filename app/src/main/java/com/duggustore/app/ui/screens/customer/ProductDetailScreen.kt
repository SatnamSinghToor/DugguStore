package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.DugguButton
import com.duggustore.app.ui.components.DugguTopBar
import com.duggustore.app.ui.theme.*

@Composable
fun ProductDetailScreen(
    product: Product?,
    isFavorite: Boolean,
    onAddToCart: (Product, Int) -> Unit,
    onToggleFavorite: (Product) -> Unit,
    onBack: () -> Unit
) {
    if (product == null) {
        Column(modifier = Modifier.fillMaxSize().background(Background)) {
            DugguTopBar(title = "Product", onBackClick = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        }
        return
    }

    var quantity by remember(product.id) { mutableStateOf(1) }
    val inStock = product.stock > 0

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        DugguTopBar(
            title = "Product Details",
            onBackClick = onBack,
            actions = {
                IconButton(onClick = { onToggleFavorite(product) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) AccentRed else Color.White
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNullOrBlank()) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(84.dp),
                        tint = TextLight
                    )
                } else {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                if (product.hasDiscount()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = AccentOrange
                    ) {
                        Text(
                            text = "SAVE ₹${"%.0f".format(product.savingsAmount())}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = product.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Per ${product.unit}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${"%.2f".format(product.effectivePrice())}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    if (product.hasDiscount()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "₹${"%.2f".format(product.price)}",
                            fontSize = 16.sp,
                            color = TextLight,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (inStock) SuccessGreen.copy(alpha = 0.12f) else AccentRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (inStock) "In stock · ${product.stock} left" else "Out of stock",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = if (inStock) SuccessGreen else AccentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Description",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 21.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Quantity",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    QuantityStepper(
                        quantity = quantity,
                        // Never let the user add more than the seller actually has.
                        canIncrease = inStock && quantity < product.stock,
                        onDecrease = { if (quantity > 1) quantity-- },
                        onIncrease = { quantity++ }
                    )
                }
            }
        }

        Surface(color = Color.White, shadowElevation = 12.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Total", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        text = "₹${"%.2f".format(product.effectivePrice() * quantity)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                DugguButton(
                    text = if (inStock) "Add to Cart" else "Out of Stock",
                    onClick = { onAddToCart(product, quantity) },
                    modifier = Modifier.weight(1.4f),
                    enabled = inStock
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(enabled = quantity > 1, onClick = onDecrease) {
            Icon(Icons.Default.Remove, "Decrease", modifier = Modifier.size(18.dp), tint = PrimaryGreen)
        }
        Text(
            text = "$quantity",
            modifier = Modifier.padding(horizontal = 18.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        StepperButton(enabled = canIncrease, onClick = onIncrease) {
            Icon(Icons.Default.Add, "Increase", modifier = Modifier.size(18.dp), tint = PrimaryGreen)
        }
    }
}

@Composable
private fun StepperButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(34.dp).clip(CircleShape),
        color = if (enabled) PrimaryGreen.copy(alpha = 0.12f) else BorderGray.copy(alpha = 0.5f)
    ) {
        IconButton(onClick = onClick, enabled = enabled) { content() }
    }
}
