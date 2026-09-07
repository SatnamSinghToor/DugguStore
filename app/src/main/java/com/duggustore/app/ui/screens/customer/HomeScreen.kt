package com.duggustore.app.ui.screens.customer

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.duggustore.app.data.local.AppPrefs
import com.duggustore.app.data.model.Address
import com.duggustore.app.data.model.Campaign
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Coupon
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.model.SponsoredSlot
import androidx.compose.ui.res.stringResource
import com.duggustore.app.R
import com.duggustore.app.platform.LocationState
import com.duggustore.app.platform.rememberDeviceLocation
import com.duggustore.app.platform.rememberVoiceSearchController
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    categories: List<Category>,
    selectedCategoryId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Product) -> Unit = {},
    userName: String = "",
    deliveryAddress: String = "Set your delivery address",
    cartQuantities: Map<String, Int> = emptyMap(),
    favoriteIds: Set<String> = emptySet(),
    onIncrease: (Product) -> Unit = {},
    onDecrease: (Product) -> Unit = {},
    onToggleFavorite: (Product) -> Unit = {},
    onAddressClick: () -> Unit = {},
    notificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {},
    savedAddresses: List<Address> = emptyList(),
    onSelectAddress: (Address) -> Unit = {},
    onSaveDetectedAddress: (String, Double, Double) -> Unit = { _, _, _ -> },
    offers: List<Coupon> = emptyList(),
    onOfferClick: (Coupon) -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    // Whichever view is active — the default browse feed, a category, or a
    // search — pages through this same list; there's no separate in-memory
    // "filtered" list any more, since filtering now happens server-side.
    feedProducts: List<Product> = emptyList(),
    hasMoreFeed: Boolean = false,
    isLoadingMoreFeed: Boolean = false,
    onLoadMoreFeed: () -> Unit = {},
    // The full catalogue, separate from feedProducts above — used only to
    // let the offer carousel feature a matching product on its own card.
    allProducts: List<Product> = emptyList(),
    // Feed the wallet-reminder and referral-invite banners on the same rail
    // as the coupon cards — 0 / blank simply omits that banner.
    walletBalance: Int = 0,
    referralCode: String = "",
    onWalletBannerClick: () -> Unit = {},
    // Already date-windowed (campaigns) / approved-and-live (sponsored
    // slots) server-side — nothing here needs to check dates again.
    campaigns: List<Campaign> = emptyList(),
    sponsoredSlots: List<SponsoredSlot> = emptyList()
) {
    // Both sheets are owned here so the header can stay a plain row of
    // controls and the sheets sit above the whole screen.
    var showLocationSheet by remember { mutableStateOf(false) }
    val detected = rememberDeviceLocation()
    val voice = rememberVoiceSearchController { onSearchQueryChange(it) }
    val pullRefreshState = rememberPullRefreshState(refreshing = isLoading, onRefresh = onRefresh)

    // The location strip folds away as soon as the feed moves, leaving the
    // search field and the category tabs pinned. derivedStateOf so this only
    // recomposes on the two transitions, not on every scrolled pixel.
    val listState = rememberLazyListState()
    val addressExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10
        }
    }

    // Home is the bottom of the back stack, so with no search active the
    // system back button already does the right thing (exits to the
    // launcher). With a search active it did that too — the whole app
    // closed instead of just backing out of the search, which is what
    // actually looks like a crash from the outside.
    BackHandler(enabled = searchQuery.isNotBlank()) {
        onSearchQueryChange("")
    }

    val context = LocalContext.current
    var recentSearches by remember { mutableStateOf(AppPrefs.recentSearches(context)) }
    // Saved once typing pauses rather than on every keystroke, so the list
    // doesn't fill up with "k", "ku", "kur" for a single search.
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) return@LaunchedEffect
        delay(1000)
        AppPrefs.addRecentSearch(context, searchQuery)
        recentSearches = AppPrefs.recentSearches(context)
    }

    // The coupon cards plus whatever else earns a slot on the same rail —
    // a spotlight for whatever was added to the catalogue most recently,
    // a reminder of wallet money the customer already has, an invite to
    // refer a friend. Each is a normal outcome to omit, not a fallback:
    // no new product, no balance, or no code yet just means one fewer card.
    val promoBanners = remember(offers, allProducts, walletBalance, referralCode, campaigns, sponsoredSlots) {
        buildList {
            addAll(buildDiscountBanners(offers, allProducts, onOfferClick))

            // A seasonal push toward one category — already restricted to
            // ones actually running right now by the server, so every
            // campaign here is live by definition.
            campaigns.forEach { campaign ->
                add(
                    PromoBanner(
                        id = "campaign:${campaign.id}",
                        tint = runCatching { Color(android.graphics.Color.parseColor(campaign.tintHex)) }.getOrDefault(Orange),
                        eyebrowIcon = Icons.Default.Campaign,
                        eyebrow = "Limited time",
                        headline = campaign.label,
                        subtitle = categories.firstOrNull { it.id == campaign.categoryId }?.name
                            ?: "Handpicked for you",
                        chipLabel = campaign.ctaLabel,
                        onClick = { onCategorySelected(campaign.categoryId) }
                    )
                )
            }

            // A seller paid to feature this exact product — the one they
            // picked when requesting the slot, embedded on the row itself.
            // Skipped if that product is no longer around to show (deleted
            // after the slot was approved) rather than guessing at another one.
            sponsoredSlots.forEach { slot ->
                val product = slot.product ?: return@forEach
                add(
                    PromoBanner(
                        id = "sponsored:${slot.id}",
                        tint = TextSecondary,
                        eyebrowIcon = Icons.Default.Campaign,
                        eyebrow = "",
                        headline = product.name,
                        subtitle = slot.headline.ifBlank { product.description },
                        cornerTag = "SPONSORED",
                        featuredProduct = product,
                        onClick = { onProductClick(product) }
                    )
                )
            }

            allProducts.filter { it.isActive }.maxByOrNull { it.createdAt }?.let { product ->
                add(
                    PromoBanner(
                        id = "new-arrival:${product.id}",
                        tint = Violet,
                        // The "NEW" corner tag already says why this card is
                        // here — a separate eyebrow row would just repeat it.
                        eyebrowIcon = Icons.Default.FiberNew,
                        eyebrow = "",
                        headline = "Just landed",
                        subtitle = product.name,
                        cornerTag = "NEW",
                        featuredProduct = product,
                        onClick = { onProductClick(product) }
                    )
                )
            }

            if (walletBalance > 0) {
                add(
                    PromoBanner(
                        id = "wallet",
                        tint = Teal,
                        eyebrowIcon = Icons.Default.AccountBalanceWallet,
                        eyebrow = "In your wallet",
                        headline = "₹$walletBalance cashback",
                        subtitle = "Waiting to be used on your next order",
                        chipLabel = "USE NOW",
                        onClick = onWalletBannerClick
                    )
                )
            }

            if (referralCode.isNotBlank()) {
                add(
                    PromoBanner(
                        id = "referral",
                        tint = Color(0xFF5AA9E6),
                        eyebrowIcon = Icons.Default.Share,
                        eyebrow = "Invite a friend",
                        headline = "Give ₹50, get ₹50",
                        subtitle = "Both of you get wallet credit on their first order",
                        chipLabel = "SHARE MY CODE",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Use my Duggu Store referral code $referralCode and we both get ₹50 wallet credit!"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, "Share referral code"))
                        }
                    )
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Pinned. The wordmark, the location strip and the search field were
            // the first item of the list, so they scrolled away with the
            // products — searching meant scrolling back to the top first.
            // A soft gradient wash (the same TealSurface used in the auth
            // screens' header) rather than flat white, so the top of the
            // page reads as considered rather than bare.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
                    .background(Brush.verticalGradient(listOf(TealSurface, SurfaceWhite)))
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
            Column(
                // 20dp, not the usual 16 — this is what lines the search
                // bar's edges up with the offer cards below it, whose
                // width comes from the pager's own 20dp contentPadding.
                // The tab row below sits outside it, so its baseline can
                // run the full width of the screen.
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                // The strip showed only the saved default address. It now
                // leads with the detected one and falls back to the saved
                // address when location is off or refused.
                val locationState = detected.state
                AnimatedVisibility(visible = addressExpanded) {
                    Column {
                        LocationBar(
                            city = if (locationState is LocationState.Locating) {
                                stringResource(R.string.location_finding)
                            } else {
                                stringResource(R.string.home_deliver_to)
                            },
                            address = when (locationState) {
                                is LocationState.Found -> locationState.address
                                is LocationState.Locating ->
                                    stringResource(R.string.location_wait)
                                is LocationState.Unavailable ->
                                    stringResource(locationState.messageRes)
                                LocationState.Idle -> deliveryAddress
                            },
                            onClick = { showLocationSheet = true },
                            trailing = { StoreWordmarkBadge() }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StoreSearchBar(
                        query = searchQuery,
                        placeholder = stringResource(R.string.home_search_hint),
                        onQueryChange = onSearchQueryChange,
                        // Null when the device has no speech recogniser, which
                        // leaves the mic out rather than showing a dead button.
                        onMicClick = voice?.let { { it.open() } },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    LanguagePicker()
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceMuted)
                                .dugguClickable { onNotificationsClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                stringResource(R.string.home_notifications),
                                tint = Coral,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        if (notificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                    .clip(CircleShape)
                                    .background(Coral),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (notificationCount > 9) "9+" else "$notificationCount",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    RecentSearchesRow(
                        terms = recentSearches,
                        onTermClick = onSearchQueryChange,
                        onClear = {
                            AppPrefs.clearRecentSearches(context)
                            recentSearches = emptyList()
                        }
                    )
                }
            }

            // Pinned with the search field rather than scrolling with the
            // feed: the tabs filter what is in that feed, so losing them
            // the moment you scroll into it is the wrong way round.
            if (categories.isNotEmpty() && searchQuery.isBlank()) {
                CurvedCategoryTabs(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = onCategorySelected
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
            }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pullRefresh(pullRefreshState)
            ) {
            if (error != null && !isLoading && categories.isEmpty() && feedProducts.isEmpty()) {
                ErrorRetryBlock(message = error, onRetry = onRetry)
            } else if (isLoading && categories.isEmpty() && feedProducts.isEmpty()) {
                // Fills the same weight(1f) area the list below would, so
                // there is no jump in the page's overall height once real
                // content replaces it. Shaped like the grid it's standing in
                // for, rather than a bare spinner, so the first frame already
                // reads as "a product grid is coming" instead of a blank
                // page with a wait icon in the middle of it.
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ProductGridSkeleton()
                }
            } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

            // Search takes over the whole page — the carousel is a browsing
            // aid, and showing it above a set of search results made it look
            // like the results were mixed in with it instead of being their
            // own list.
            val isSearching = searchQuery.isNotBlank()

            if (promoBanners.isNotEmpty() && !isSearching) {
                item {
                    Spacer(Modifier.height(14.dp))
                    OfferCarousel(banners = promoBanners)
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                RowHeader(
                    title = when {
                        searchQuery.isNotBlank() -> "Results"
                        selectedCategoryId != null ->
                            categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Products"
                        else -> "Popular Deals"
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            if (feedProducts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SearchOff, null, tint = TextLight, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Nothing here yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Products will show up once a seller adds them"
                                   else "Try a different search",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // Two per row, built manually so the whole page stays one scrolling
                // LazyColumn rather than nesting a grid inside it. Keyed on the
                // pair's own product ids — without a key, Compose can only
                // diff this list by position, so an insert/removal/reorder
                // anywhere in a 50-100 product feed reuses every row after it
                // for the wrong pair instead of recomposing just the one that
                // actually changed.
                items(feedProducts.chunked(2), key = { pair -> pair.joinToString("-") { it.id } }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { product ->
                            StoreProductCard(
                                product = product,
                                quantityInCart = cartQuantities[product.id] ?: 0,
                                isFavorite = favoriteIds.contains(product.id),
                                onAdd = { onAddToCart(product) },
                                onIncrease = { onIncrease(product) },
                                onDecrease = { onDecrease(product) },
                                onToggleFavorite = { onToggleFavorite(product) },
                                onClick = { onProductClick(product) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                // A plain item at the tail of the feed rather than a scroll
                // listener — LazyColumn only composes what's near the
                // viewport, so this only enters composition (and fires) once
                // the user has actually scrolled close to the end of what's
                // loaded so far.
                if (hasMoreFeed) {
                    item {
                        LaunchedEffect(feedProducts.size) { onLoadMoreFeed() }
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMoreFeed) {
                                CircularProgressIndicator(color = Teal, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
            }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = Teal
            )
            }
        }

        LocationSheet(
            visible = showLocationSheet,
            locationState = detected.state,
            addresses = savedAddresses,
            onDetectLocation = detected.refresh,
            onUseDetected = { address, lat, lng ->
                onSaveDetectedAddress(address, lat, lng)
                showLocationSheet = false
            },
            onSelectAddress = { address ->
                onSelectAddress(address)
                showLocationSheet = false
            },
            onAddNewAddress = {
                showLocationSheet = false
                onAddressClick()
            },
            onDismiss = { showLocationSheet = false }
        )

        voice?.let { VoiceSearchSheet(controller = it) }
    }
}

@Composable
private fun RecentSearchesRow(
    terms: List<String>,
    onTermClick: (String) -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent searches", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Text(
                text = "Clear",
                fontSize = 12.sp,
                color = Teal,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.dugguClickable { onClear() }
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(terms, key = { it }) { term ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceMuted,
                    onClick = { onTermClick(term) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, null, tint = TextLight, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(term, fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

/**
 * "All" plus every category as a scrollable row of text tabs, the selected
 * one riding a raised curve rather than a flat highlight — each tab draws
 * its own slice of one continuous baseline, so the row reads as a single
 * connected shape (unselected tabs contribute a flat segment, the selected
 * one bulges) without needing to measure any other tab's position.
 */
@Composable
private fun CurvedCategoryTabs(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp)) {
        item {
            CurvedTab(
                label = "All",
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(categories, key = { it.id }) { category ->
            CurvedTab(
                label = category.name,
                selected = selectedCategoryId == category.id,
                onClick = {
                    onCategorySelected(if (selectedCategoryId == category.id) null else category.id)
                }
            )
        }
    }
}

@Composable
private fun CurvedTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                // Curve and label are both inset from the row's edges by fixed
                // amounts (8dp here, the padding below), so the raised segment
                // clears the top of the text instead of cutting through it,
                // and stays clear at any font scale — which a height fraction
                // wouldn't.
                val bumpTopY = 8.dp.toPx()
                val baselineY = size.height - 8.dp.toPx()
                val curveRun = (size.width * 0.4f).coerceAtMost(30.dp.toPx())
                val path = Path().apply {
                    if (selected) {
                        moveTo(0f, baselineY)
                        cubicTo(
                            curveRun * 0.66f, baselineY,
                            curveRun * 0.34f, bumpTopY,
                            curveRun, bumpTopY
                        )
                        lineTo(size.width - curveRun, bumpTopY)
                        cubicTo(
                            size.width - curveRun * 0.34f, bumpTopY,
                            size.width - curveRun * 0.66f, baselineY,
                            size.width, baselineY
                        )
                    } else {
                        moveTo(0f, baselineY)
                        lineTo(size.width, baselineY)
                    }
                }
                drawPath(
                    path = path,
                    color = Teal,
                    // HairlineWidth is one physical pixel at any density —
                    // the thinnest line that still renders.
                    style = Stroke(width = Stroke.HairlineWidth, cap = StrokeCap.Round)
                )
            }
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            color = if (selected) Teal else TextSecondary
        )
    }
}
