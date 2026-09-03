package com.duggustore.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Product
import com.duggustore.app.R
import com.duggustore.app.ui.theme.*

/** Wordmark with the two-tone split from the design. */
@Composable
fun StoreWordmark(first: String = "Duggu", second: String = "Store", size: Int = 22) {
    Row {
        Text(first, fontSize = size.sp, fontWeight = FontWeight.ExtraBold, color = Orange)
        Text(second, fontSize = size.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
    }
}

/** Location strip: teal pin, city over address, chevron. */
@Composable
fun LocationBar(
    city: String,
    address: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Re-detects the device location. Null leaves the crosshair out. */
    onLocateClick: (() -> Unit)? = null,
    locating: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(Teal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(city, fontSize = 13.sp, color = TextSecondary)
            Text(
                text = address,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onLocateClick != null) {
            if (locating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Teal,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onLocateClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.location_use_current),
                        tint = Teal,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
    }
}

/** Pill search field with a trailing mic separated by a divider. */
@Composable
fun StoreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search Anything...",
    onMicClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceMuted
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(placeholder, fontSize = 15.sp, color = TextLight)
                }
                BasicSearchField(query, onQueryChange)
            }
            if (onMicClick != null) {
                Box(Modifier.width(1.dp).height(22.dp).background(BorderGray))
                IconButton(onClick = onMicClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Mic,
                        stringResource(R.string.home_voice_search),
                        tint = Teal,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicSearchField(query: String, onQueryChange: (String) -> Unit) {
    androidx.compose.foundation.text.BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextPrimary),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Teal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
    )
}

/** "Categories ›" style heading. */
@Composable
fun RowHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        if (onSeeAll != null) {
            IconButton(onClick = onSeeAll, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, "See all", tint = TextSecondary)
            }
        }
    }
}

/** Solid colour tile with a white glyph and the category name inside. */
@Composable
fun CategoryTile(
    category: Category,
    color: Color,
    onClick: () -> Unit,
    // The size lives in the default rather than inside, so the categories grid can
    // hand it a width-driven square instead of a fixed 104dp that would overflow
    // three-across on a narrow phone.
    modifier: Modifier = Modifier.size(104.dp)
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = iconForCategory(category.name),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = category.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Best-effort glyph per category name, so tiles are not all the same icon. */
fun iconForCategory(name: String): ImageVector = when {
    name.contains("groc", true) -> Icons.Default.LocalGroceryStore
    name.contains("veg", true) -> Icons.Default.Eco
    // No fruit glyph in this icon set; a basket is the closest that resolves.
    name.contains("fruit", true) -> Icons.Default.ShoppingBasket
    name.contains("snack", true) -> Icons.Default.Cookie
    name.contains("choc", true) -> Icons.Default.Cake
    name.contains("bread", true) || name.contains("baker", true) -> Icons.Default.BakeryDining
    name.contains("shampoo", true) || name.contains("beauty", true) -> Icons.Default.Spa
    name.contains("clean", true) -> Icons.Default.CleaningServices
    name.contains("baby", true) -> Icons.Default.ChildCare
    name.contains("drink", true) || name.contains("cold", true) -> Icons.Default.LocalDrink
    name.contains("meat", true) -> Icons.Default.SetMeal
    name.contains("dairy", true) || name.contains("milk", true) -> Icons.Default.LocalCafe
    name.contains("frozen", true) -> Icons.Default.AcUnit
    name.contains("fashion", true) || name.contains("cloth", true) -> Icons.Default.Checkroom
    name.contains("appliance", true) || name.contains("electr", true) -> Icons.Default.Kitchen
    name.contains("furni", true) -> Icons.Default.Chair
    else -> Icons.Default.Category
}

/**
 * Product card from the reference: heart top-left, discount ribbon top-right,
 * image, name, price, and either an outlined "Add to cart" or a quantity
 * stepper once the item is in the cart.
 */
@Composable
fun StoreProductCard(
    product: Product,
    quantityInCart: Int,
    isFavorite: Boolean,
    onAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(SurfaceWhite)
            ) {
                if (product.imageUrl.isNullOrBlank()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, null, tint = TextLight, modifier = Modifier.size(44.dp))
                    }
                } else {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopStart).size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                        tint = if (isFavorite) Coral else TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (product.hasDiscount()) {
                    DiscountRibbon(
                        percent = discountPercent(product),
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${trimAmount(product.effectivePrice())}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    if (product.hasDiscount()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "₹${trimAmount(product.price)}",
                            fontSize = 12.sp,
                            color = TextLight,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    // The reference shows a rating here; this app has no review
                    // data yet, so the unit is shown rather than a made-up score.
                    Text(
                        text = "/${product.unit}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(Modifier.height(10.dp))

                when {
                    product.stock <= 0 -> Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceMuted
                    ) {
                        Text(
                            "Out of stock",
                            modifier = Modifier.padding(vertical = 10.dp),
                            fontSize = 13.sp,
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                    }
                    quantityInCart > 0 -> QuantityStepperRow(quantityInCart, onDecrease, onIncrease)
                    else -> AddToCartButton(onAdd)
                }
            }
        }
    }
}

@Composable
private fun AddToCartButton(onAdd: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onAdd() }
            .border(1.5.dp, Orange, RoundedCornerShape(10.dp)),
        color = Color.Transparent
    ) {
        Text(
            text = "Add to cart",
            modifier = Modifier.padding(vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Orange,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QuantityStepperRow(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StepperSquare(Icons.Default.Remove, Coral, "Decrease", onDecrease)
        Text(
            text = "$quantity",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Teal
        )
        StepperSquare(Icons.Default.Add, Teal, "Increase", onIncrease)
    }
}

@Composable
private fun StepperSquare(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

/** Angled corner flag used for the "5% OFF" mark. */
@Composable
fun DiscountRibbon(percent: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .rotate(45f)
                .offset(x = 18.dp, y = (-10).dp)
                .background(Coral)
                .padding(horizontal = 22.dp, vertical = 3.dp)
        ) {
            Text("$percent% OFF", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun discountPercent(product: Product): Int {
    if (!product.hasDiscount() || product.price <= 0.0) return 0
    return (((product.price - product.effectivePrice()) / product.price) * 100).toInt()
}

/** Drops the ".00" so prices read like the reference (₹10, not ₹10.00). */
fun trimAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

/** Rounded promo banner with a headline, sub-line and artwork. */
@Composable
fun PromoBanner(
    title: String,
    highlight: String,
    caption: String,
    imageUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(18.dp),
        color = TealSurface
    ) {
        Box {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.fillMaxHeight().padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(title, fontSize = 16.sp, color = TextPrimary)
                Text(highlight, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(caption, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
