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

/** How long each banner holds before the carousel moves on. */
private const val AUTO_ADVANCE_MS = 4000L

/**
 * How many percentage points a product's own discount may differ from a
 * coupon's headline figure and still count as that coupon's product — just
 * enough slack to absorb [discountPercent]'s truncation to an Int, not loose
 * enough to let one steeply-discounted item satisfy every coupon on the rail.
 */
private const val MATCH_TOLERANCE = 2

/**
 * The offers strip on home. Not every card on it is a discount coupon any
 * more — [banners] can mix in anything built by [buildDiscountBanners] or
 * assembled by the caller (a new-arrival spotlight, a wallet reminder, a
 * referral invite…), and this only knows how to page through and render
 * them. Cards take their tint from whoever built them, so a mixed rail
 * still reads as one family rather than one coupon look plus odd ones out.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OfferCarousel(
    banners: List<PromoBanner>,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })

    // Only advances while the user is not touching it — a card that slides away
    // mid-tap is worse than one that waits — and never with a single card,
    // where it would animate to the page it is already on.
    LaunchedEffect(pagerState, banners.size) {
        if (banners.size < 2) return@LaunchedEffect
        while (true) {
            delay(AUTO_ADVANCE_MS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % banners.size)
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

            OfferCard(banner = banners[page], modifier = Modifier.scale(scale))
        }

        if (banners.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(banners.size) { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            // The current dot stretches rather than only
                            // changing colour, so the position reads at a glance.
                            .width(if (active) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) banners[index].tint else BorderGray)
                    )
                }
            }
        }
    }
}

/**
 * Turns the store's active coupons into discount [PromoBanner]s, each paired
 * with at most one product and each product with at most one coupon —
 * closest percentage match wins, greedily, closest first — so a single
 * steeply-discounted item can't end up "featured" on every coupon just
 * because it also clears every other one's lower threshold. A coupon with
 * nothing close enough features no product at all, which is a normal
 * outcome, not a fallback to guess at.
 */
fun buildDiscountBanners(
    coupons: List<Coupon>,
    products: List<Product>,
    onClick: (Coupon) -> Unit
): List<PromoBanner> {
    val candidates = products.filter { it.hasDiscount() }
    val pairs = if (candidates.isEmpty()) emptyList() else coupons.flatMap { coupon ->
        candidates.map { product -> Triple(coupon, product, abs(discountPercent(product) - coupon.discountPercent)) }
    }.filter { it.third <= MATCH_TOLERANCE }.sortedBy { it.third }

    val claimedProducts = mutableSetOf<String>()
    val featuredByCoupon = mutableMapOf<String, Product>()
    for ((coupon, product, _) in pairs) {
        if (featuredByCoupon.containsKey(coupon.id) || product.id in claimedProducts) continue
        featuredByCoupon[coupon.id] = product
        claimedProducts += product.id
    }

    return coupons.mapIndexed { index, coupon ->
        PromoBanner(
            id = "coupon:${coupon.id}",
            tint = CategoryColors[index.mod(CategoryColors.size)],
            eyebrowIcon = Icons.Default.LocalOffer,
            eyebrow = coupon.expiryLabel.ifBlank { "Limited time" },
            headline = coupon.title.ifBlank { "${coupon.discountPercent}% OFF" },
            subtitle = coupon.description,
            chipLabel = "CODE  ${coupon.code}",
            featuredProduct = featuredByCoupon[coupon.id],
            onClick = { onClick(coupon) }
        )
    }
}

@Composable
private fun OfferCard(banner: PromoBanner, modifier: Modifier = Modifier) {
    val featuredImageUrl = banner.featuredProduct?.images()?.firstOrNull()

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
            .background(banner.tint.copy(alpha = 0.14f))
            .clickable { banner.onClick() }
    ) {
        if (featuredImageUrl != null) {
            // The matched product's photo, background cut out on-device so
            // it doesn't carry a visible white rectangle from the original
            // shot — sitting under the two decorative discs, not filling
            // the whole card.
            CutoutProductImage(
                imageUrl = featuredImageUrl,
                contentDescription = banner.featuredProduct?.name,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 4.dp)
                    .size(128.dp)
            )
        }

        // A cluster of soft, differently-coloured discs, in more than one
        // colour so it reads as a little collage rather than one flat tint.
        // The largest anchors the top-right corner (bleeding off both
        // edges); the other three sit lower and further left, squarely over
        // the photo rather than only grazing its top. Drawn after the photo
        // so they sit on top of it, same for every card regardless of
        // whether a product is featured.
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = 250.dp, y = (-70).dp)
                .clip(CircleShape)
                .background(banner.tint.copy(alpha = 0.20f))
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .offset(x = 215.dp, y = 70.dp)
                .clip(CircleShape)
                .background(Coral.copy(alpha = 0.24f))
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = 228.dp, y = 125.dp)
                .clip(CircleShape)
                .background(Orange.copy(alpha = 0.26f))
        )
        Box(
            modifier = Modifier
                .size(70.dp)
                .offset(x = 185.dp, y = 150.dp)
                .clip(CircleShape)
                .background(Violet.copy(alpha = 0.24f))
        )

        banner.cornerTag?.let { tag ->
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = RoundedCornerShape(7.dp),
                color = banner.tint
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                // Narrowed only when a featured product is actually taking
                // up the right side — otherwise the text keeps the full
                // width it always had.
                .fillMaxWidth(if (featuredImageUrl != null) 0.62f else 1f)
                .fillMaxHeight()
                .padding(18.dp),
            // A corner tag already occupies the top-left, so the text sits
            // at the bottom instead of centred, rather than crowding it.
            verticalArrangement = if (banner.cornerTag != null) Arrangement.Bottom else Arrangement.Center
        ) {
            if (banner.eyebrow.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        banner.eyebrowIcon,
                        contentDescription = null,
                        tint = banner.tint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = banner.eyebrow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = banner.tint
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = banner.headline,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = banner.subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis
            )

            banner.chipLabel?.let { label ->
                Spacer(Modifier.height(10.dp))
                // The point of the card, so it is set apart rather than
                // buried in the description.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(banner.tint)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
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
 * Kit subject segmenter used at upload time), so it doesn't carry a visible
 * white rectangle from the original photo when it sits directly on the
 * offer card's tint. Shows nothing while the cutout is being computed, and
 * nothing at all if segmentation fails — a mismatched white box is worse
 * than no photo.
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
