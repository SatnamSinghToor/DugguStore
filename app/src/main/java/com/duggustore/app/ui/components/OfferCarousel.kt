package com.duggustore.app.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import coil.request.ImageRequest
import com.duggustore.app.data.model.Coupon
import com.duggustore.app.data.model.Product
import com.duggustore.app.platform.BackgroundRemover
import com.duggustore.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** How long each offer holds before the carousel moves on. */
private const val AUTO_ADVANCE_MS = 4000L

/**
 * How many percentage points a product's own discount may differ from a
 * coupon's headline figure and still count as that coupon's product — just
 * enough slack to absorb [discountPercent]'s truncation to an Int, not loose
 * enough to let one steeply-discounted item satisfy every coupon on the rail.
 */
private const val MATCH_TOLERANCE = 2

/**
 * The offers strip on home.
 *
 * The single static "25% OFF" panel is gone. Cards sit side by side with the
 * neighbours peeking in from both edges, so it is obvious the strip moves, and
 * it advances on its own like an ad rail. Cards take their colours from the
 * same palette as the category tiles, so the two rails read as one family.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OfferCarousel(
    offers: List<Coupon>,
    onOfferClick: (Coupon) -> Unit,
    modifier: Modifier = Modifier,
    // The full catalogue, not just whatever page of it Home currently has
    // loaded — a discount worth featuring shouldn't depend on how far the
    // customer happens to have scrolled.
    products: List<Product> = emptyList()
) {
    if (offers.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { offers.size })

    // Each coupon is paired with at most one product, and each product with
    // at most one coupon — closest percentage match wins — so a single
    // steeply-discounted item can't end up "featured" on every card just
    // because it also happens to clear every other card's lower threshold.
    val featuredByOffer = remember(offers, products) { assignFeaturedProducts(offers, products) }

    // Only advances while the user is not touching it — a card that slides away
    // mid-tap is worse than one that waits — and never with a single card,
    // where it would animate to the page it is already on.
    LaunchedEffect(pagerState, offers.size) {
        if (offers.size < 2) return@LaunchedEffect
        while (true) {
            delay(AUTO_ADVANCE_MS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % offers.size)
            }
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            // Wider than before, so more of the previous and next card
            // shows at each edge, and a tighter pageSpacing means that
            // extra peek is actual neighbouring card rather than gap.
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val selected = page == pagerState.currentPage
            // Neighbours sit back a little so the front card is clearly the one
            // being read.
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.92f,
                animationSpec = tween(220),
                label = "offerScale"
            )

            OfferCard(
                coupon = offers[page],
                color = CategoryColors[page.mod(CategoryColors.size)],
                featuredProduct = featuredByOffer[offers[page].id],
                modifier = Modifier.scale(scale),
                onClick = { onOfferClick(offers[page]) }
            )
        }

        if (offers.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(offers.size) { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            // The current dot stretches rather than only
                            // changing colour, so the position reads at a glance.
                            .width(if (active) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) CategoryColors[index.mod(CategoryColors.size)]
                                else BorderGray
                            )
                    )
                }
            }
        }
    }
}

/**
 * Greedy one-to-one pairing between coupons and products: every
 * (coupon, product) combination within [MATCH_TOLERANCE] points of each
 * other, closest first, each side claimed only once it's matched. A coupon
 * with nothing close enough, or a product with no discount at all, simply
 * gets no match — that's a normal outcome, not a fallback to guess at.
 */
private fun assignFeaturedProducts(offers: List<Coupon>, products: List<Product>): Map<String, Product> {
    val candidates = products.filter { it.hasDiscount() }
    if (candidates.isEmpty()) return emptyMap()

    val pairs = offers.flatMap { offer ->
        candidates.map { product -> Triple(offer, product, abs(discountPercent(product) - offer.discountPercent)) }
    }.filter { it.third <= MATCH_TOLERANCE }.sortedBy { it.third }

    val claimedProducts = mutableSetOf<String>()
    val result = mutableMapOf<String, Product>()
    for ((offer, product, _) in pairs) {
        if (result.containsKey(offer.id) || product.id in claimedProducts) continue
        result[offer.id] = product
        claimedProducts += product.id
    }
    return result
}

@Composable
private fun OfferCard(
    coupon: Coupon,
    color: Color,
    modifier: Modifier = Modifier,
    featuredProduct: Product? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Sized to the card's own content (icon+label row, title,
            // two-line description, code chip, plus its padding) rather than
            // matched to the product card's height — that left a visibly
            // empty band under the text instead of a snug banner.
            .height(186.dp)
            .clip(RoundedCornerShape(20.dp))
            // A light wash of the card's colour rather than the colour itself:
            // the reference banner is a pale tint with dark type on it, and a
            // fully saturated panel reads far heavier than the rest of the page.
            .background(color.copy(alpha = 0.14f))
            .clickable { onClick() }
    ) {
        // Two soft discs bleeding off the right edge, so a flat tint still has
        // some depth without needing artwork per offer. Scaled down to match
        // the shorter card. Doubles as the backdrop a featured product sits
        // on below, standing in for the plain white card it would otherwise
        // show up on.
        Box(
            modifier = Modifier
                .size(112.dp)
                .offset(x = 188.dp, y = (-50).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f))
        )
        Box(
            modifier = Modifier
                .size(82.dp)
                .offset(x = 212.dp, y = 91.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
        )

        featuredProduct?.images()?.firstOrNull()?.let { imageUrl ->
            CutoutProductImage(
                imageUrl = imageUrl,
                contentDescription = featuredProduct.name,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 4.dp)
                    .size(128.dp)
            )
        }

        Column(
            modifier = Modifier
                // Narrowed only when a featured product is actually taking
                // up the right side — otherwise the text keeps the full
                // width it always had.
                .fillMaxWidth(if (featuredProduct != null) 0.62f else 1f)
                .fillMaxHeight()
                .padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = coupon.expiryLabel.ifBlank { "Limited time" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = coupon.title.ifBlank { "${coupon.discountPercent}% OFF" },
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = coupon.description,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // The code is the point of the card, so it is set apart rather than
            // buried in the description.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "CODE  ${coupon.code}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Keeps a cutout from being re-segmented every time its card scrolls back
 * into view — segmentation is on-device but not free, and the result for a
 * given photo never changes for the lifetime of the app process.
 */
private val cutoutCache = mutableMapOf<String, Bitmap?>()

/**
 * A product photo with its own background cut out on-device (the same ML
 * Kit subject segmenter used at upload time), so it sits directly on the
 * offer card's tinted background instead of carrying a visible white
 * rectangle from the original photo. Shows nothing while the cutout is
 * being computed, and nothing at all if segmentation fails — a mismatched
 * white box behind the photo is worse than no photo.
 */
@Composable
private fun CutoutProductImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cutout by produceState<Bitmap?>(initialValue = cutoutCache[imageUrl], key1 = imageUrl) {
        if (cutoutCache.containsKey(imageUrl)) return@produceState
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val drawable = context.imageLoader.execute(request).drawable
                (drawable as? BitmapDrawable)?.bitmap?.let { BackgroundRemover.removeBackground(it) }
            }.getOrNull()
        }
        cutoutCache[imageUrl] = result
        value = result
    }

    cutout?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}
