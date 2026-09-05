package com.duggustore.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.delay

/** Wordmark with the two-tone split from the design. */
@Composable
fun StoreWordmark(first: String = "Duggu", second: String = "Store", size: Int = 22) {
    Row {
        Text(first, fontSize = size.sp, fontWeight = FontWeight.ExtraBold, color = Orange)
        Text(second, fontSize = size.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
    }
}

/** Location strip: teal pin, city over address, reload. */
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
            .padding(vertical = 6.dp)
            // Matches the search bar's own end inset directly below, so the
            // reload icon here and the mic icon there land on the same
            // vertical line instead of one sitting flush to the edge and
            // the other tucked in from it.
            .padding(end = 8.dp),
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
            // Fixed-size box for both states, so the reload icon and the
            // loading spinner it swaps with sit at the same spot rather
            // than the row reflowing between them.
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (locating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Orange,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onLocateClick, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.location_use_current),
                            // Same colour as the search bar's lens icon
                            // below, not teal — the pin above already
                            // carries teal, and this is the one icon in
                            // the strip that should read as its own action.
                            tint = Orange,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
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
            // Left inset kept small on purpose: the location pin right above
            // this bar sits flush with the screen margin, and the old boxed
            // icon (with its own 8dp of padding before it) started noticeably
            // further right than the pin — the two controls didn't line up.
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plain tinted glyph rather than a filled tile: a solid orange
            // square here was just as heavy as the teal one it replaced, and
            // this bar doesn't need its own colour block to read as a search
            // field.
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                BasicSearchField(query, onQueryChange, placeholder)
            }
            if (onMicClick != null) {
                Spacer(Modifier.width(8.dp))
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
private fun BasicSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    androidx.compose.foundation.text.BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextPrimary),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Teal),
        modifier = Modifier.fillMaxWidth(),
        // The placeholder used to be a sibling Text with no padding while the
        // field carried 14dp of its own, so the hint sat higher than the text
        // that replaced it. Drawing it inside the decoration puts both on the
        // same baseline.
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(placeholder, fontSize = 15.sp, color = TextLight)
                }
                innerTextField()
            }
        }
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
            // Translucent rather than a solid or lightened fill, so
            // whatever sits behind the tile shows through a little.
            .background(color.copy(alpha = 0.55f))
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = iconForCategory(category.name),
            contentDescription = null,
            // Translucent too, but less than the card behind it, so the
            // glyph still reads clearly against it.
            tint = Color.White.copy(alpha = 0.85f),
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
 * A product's photos, auto-advancing on their own — a shopper scanning a
 * grid never swipes an individual card by hand, so a second or third photo
 * would otherwise go unseen. Falls back to a single static image (or the
 * placeholder icon) when there is nothing to cycle through.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductImageCarousel(
    images: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    imageModifier: Modifier = Modifier
) {
    if (images.size <= 1) {
        Box(modifier = modifier) {
            val url = images.firstOrNull()
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    modifier = imageModifier.fillMaxSize(),
                    contentScale = contentScale
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, null, tint = TextLight, modifier = Modifier.size(44.dp))
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { images.size })

    LaunchedEffect(pagerState, images) {
        while (true) {
            delay(2600L)
            val next = (pagerState.currentPage + 1) % images.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = contentDescription,
                modifier = imageModifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) Color.White
                            else Color.White.copy(alpha = 0.5f)
                        )
                )
            }
        }
    }
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
        // Slightly translucent rather than flat white, with a hairline no
        // heavier than a chat input box's — a boundary, not a shadow.
        color = SurfaceWhite.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.6f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(SurfaceWhite)
            ) {
                val outOfStock = product.stock <= 0
                ProductImageCarousel(
                    images = product.images(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    // Faded rather than full colour once it's unavailable, so
                    // the image itself signals "can't buy this" at a glance
                    // instead of only the text underneath. Tighter padding
                    // than before — the photo is the point of the card, not
                    // the white margin around it.
                    imageModifier = Modifier.padding(6.dp).alpha(if (outOfStock) 0.35f else 1f)
                )

                if (outOfStock) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).rotate(-8f),
                        shape = RoundedCornerShape(6.dp),
                        color = TextPrimary.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "OUT OF STOCK",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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

            // A Material3 Text without its own lineHeight keeps bodyLarge's
            // 24sp regardless of the fontSize set on it — every line below
            // was rendering several dp taller than its own glyphs, which
            // added up across the card into real, visible dead space. Every
            // one now sets a lineHeight that actually matches its fontSize.
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${trimAmount(product.effectivePrice())}",
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    if (product.hasDiscount()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "₹${trimAmount(product.price)}",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
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
                        lineHeight = 15.sp,
                        color = TextSecondary
                    )
                }

                Spacer(Modifier.height(8.dp))

                when {
                    product.stock <= 0 -> Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceMuted
                    ) {
                        Text(
                            "Out of stock",
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
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
            modifier = Modifier.padding(vertical = 9.dp),
            fontSize = 14.sp,
            lineHeight = 17.sp,
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

/**
 * Small straight "5% OFF" chip for the product card's top-right corner. Used
 * to be a diagonal ribbon cutting across the corner at 45°, which read as
 * heavier and less tidy than the rest of the card's flat, rounded style.
 */
@Composable
fun DiscountRibbon(percent: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(6.dp),
        shape = RoundedCornerShape(6.dp),
        color = Coral
    ) {
        Text(
            text = "$percent% OFF",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun discountPercent(product: Product): Int {
    if (!product.hasDiscount() || product.price <= 0.0) return 0
    return (((product.price - product.effectivePrice()) / product.price) * 100).toInt()
}

/** Drops the ".00" so prices read like the reference (₹10, not ₹10.00). */
fun trimAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

/**
 * A flat estimate rather than a real routing calculation — there's no live
 * distance/traffic API wired in, so this mirrors the fixed "X-Y mins" promise
 * every quick-commerce app shows before an address and rider even exist.
 */
fun estimatedDeliveryWindow(): String = "15-20 mins"

/** Small pill shown on cart/checkout so the delivery promise is visible before placing the order. */
@Composable
fun DeliveryEtaBanner(modifier: Modifier = Modifier, etaText: String = estimatedDeliveryWindow()) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = TealSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Teal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Delivery in $etaText",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

