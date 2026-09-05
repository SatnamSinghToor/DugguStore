package com.duggustore.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Coupon
import com.duggustore.app.ui.theme.*
import kotlinx.coroutines.delay

/** How long each offer holds before the carousel moves on. */
private const val AUTO_ADVANCE_MS = 4000L

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
    modifier: Modifier = Modifier
) {
    if (offers.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { offers.size })

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

@Composable
private fun OfferCard(
    coupon: Coupon,
    color: Color,
    modifier: Modifier = Modifier,
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
        // the shorter card.
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

        Column(
            modifier = Modifier
                .fillMaxSize()
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
