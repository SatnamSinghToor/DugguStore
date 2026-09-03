package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.QuantityStepperRow
import com.duggustore.app.ui.components.discountPercent
import com.duggustore.app.ui.components.trimAmount
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
        Box(
            modifier = Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }

    var quantity by remember(product.id) { mutableStateOf(1) }
    val inStock = product.stock > 0

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ProductHero(product = product)
                ProductSheet(
                    product = product,
                    inStock = inStock,
                    quantity = quantity,
                    onDecrease = { if (quantity > 1) quantity-- },
                    onIncrease = { if (quantity < product.stock) quantity++ }
                )
            }

            // Floating controls sit over the image rather than in a top bar,
            // so the artwork runs to the top of the screen.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleAction(
                    icon = Icons.Default.ArrowBack,
                    label = "Back",
                    tint = TextPrimary,
                    onClick = onBack
                )
                CircleAction(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (isFavorite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavorite) Coral else TextSecondary,
                    onClick = { onToggleFavorite(product) }
                )
            }
        }

        BuyBar(
            product = product,
            quantity = quantity,
            inStock = inStock,
            onAddToCart = { onAddToCart(product, quantity) }
        )
    }
}

@Composable
private fun ProductHero(product: Product) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .background(TealSurface),
        contentAlignment = Alignment.Center
    ) {
        if (product.imageUrl.isNullOrBlank()) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = TealLight
            )
        } else {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize().padding(top = 40.dp, bottom = 40.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ProductSheet(
    product: Product,
    inStock: Boolean,
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-24).dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = SurfaceWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = product.name,
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (product.hasDiscount()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Coral) {
                        Text(
                            text = "${discountPercent(product)}% OFF",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(text = "Per ${product.unit}", fontSize = 13.sp, color = TextSecondary)

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "₹${trimAmount(product.effectivePrice())}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Teal
                )
                if (product.hasDiscount()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "₹${trimAmount(product.price)}",
                        modifier = Modifier.padding(bottom = 4.dp),
                        fontSize = 16.sp,
                        color = TextLight,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (inStock) TealSurface else CoralSurface
                ) {
                    Text(
                        text = if (inStock) "In stock · ${product.stock} left" else "Out of stock",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (inStock) TealDark else CoralDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))
                QuantityStepperRow(
                    quantity = quantity,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease,
                    modifier = Modifier.width(140.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Perk(Icons.Default.LocalShipping, "Fast delivery", Modifier.weight(1f))
                Perk(Icons.Default.CheckCircle, "Quality checked", Modifier.weight(1f))
            }

            if (product.description.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Description",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = product.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 21.sp
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Perk(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Teal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BuyBar(
    product: Product,
    quantity: Int,
    inStock: Boolean,
    onAddToCart: () -> Unit
) {
    Surface(color = SurfaceWhite, shadowElevation = 18.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = "₹${trimAmount(product.effectivePrice() * quantity)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = onAddToCart,
                modifier = Modifier
                    .weight(1.5f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    disabledContainerColor = BorderGray
                ),
                enabled = inStock
            ) {
                Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (inStock) "Add to cart" else "Out of stock",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CircleAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = SurfaceWhite,
        shadowElevation = 4.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(21.dp))
        }
    }
}
