package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.duggustore.app.data.local.AppPrefs
import com.duggustore.app.data.model.Address
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Coupon
import com.duggustore.app.data.model.Product
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
    filteredProducts: List<Product>,
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
    onRetry: () -> Unit = {}
) {
    // Both sheets are owned here so the header can stay a plain row of
    // controls and the sheets sit above the whole screen.
    var showLocationSheet by remember { mutableStateOf(false) }
    val detected = rememberDeviceLocation()
    val voice = rememberVoiceSearchController { onSearchQueryChange(it) }
    val pullRefreshState = rememberPullRefreshState(refreshing = isLoading, onRefresh = onRefresh)

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

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Pinned. The wordmark, the location strip and the search field were
            // the first item of the list, so they scrolled away with the
            // products — searching meant scrolling back to the top first.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                color = SurfaceWhite
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    // 20dp, not the usual 16 — this is what lines the search
                    // bar's edges up with the offer cards below it, whose
                    // width comes from the pager's own 20dp contentPadding.
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    // Matches the 8dp end inset on the location strip's
                    // reload icon and the search bar's mic icon below, so
                    // all three land on the same vertical line instead of
                    // the bell sitting flush with the true edge.
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StoreWordmark()
                    Spacer(Modifier.weight(1f))
                    LanguagePicker()
                    Spacer(Modifier.width(12.dp))
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceMuted)
                                .clickable { onNotificationsClick() },
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

                Spacer(Modifier.height(10.dp))

                // The strip showed only the saved default address. It now
                // leads with the detected one and falls back to the saved
                // address when location is off or refused.
                val locationState = detected.state
                LocationBar(
                    city = when {
                        locationState is LocationState.Locating ->
                            stringResource(R.string.location_finding)
                        locationState is LocationState.Found ->
                            stringResource(R.string.location_current)
                        userName.isBlank() -> stringResource(R.string.home_deliver_to)
                        else -> stringResource(R.string.home_greeting, userName)
                    },
                    address = when (locationState) {
                        is LocationState.Found -> locationState.address
                        is LocationState.Locating -> stringResource(R.string.location_wait)
                        is LocationState.Unavailable ->
                            stringResource(locationState.messageRes)
                        LocationState.Idle -> deliveryAddress
                    },
                    onClick = { showLocationSheet = true },
                    onLocateClick = detected.refresh,
                    locating = locationState is LocationState.Locating
                )

                Spacer(Modifier.height(12.dp))

                StoreSearchBar(
                    query = searchQuery,
                    placeholder = stringResource(R.string.home_search_hint),
                    onQueryChange = onSearchQueryChange,
                    // Null when the device has no speech recogniser, which
                    // leaves the mic out rather than showing a dead button.
                    onMicClick = voice?.let { { it.open() } }
                )

                if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
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
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pullRefresh(pullRefreshState)
            ) {
            if (error != null && !isLoading && categories.isEmpty() && filteredProducts.isEmpty()) {
                ErrorRetryBlock(message = error, onRetry = onRetry)
            } else if (isLoading && categories.isEmpty() && filteredProducts.isEmpty()) {
                // Fills the same weight(1f) area the list below would, so
                // there is no jump in the page's overall height once real
                // content replaces it — a small "nothing here" block that
                // then suddenly grows into a full page, right below the
                // search bar, was the actual first-load stutter this
                // screen used to show.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Teal)
                }
            } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

            // Search takes over the whole page — the carousel and the
            // categories row are browsing aids, and showing them above a
            // set of search results made it look like the results were
            // mixed in with the categories instead of being their own list.
            val isSearching = searchQuery.isNotBlank()

            if (offers.isNotEmpty() && !isSearching) {
                item {
                    Spacer(Modifier.height(10.dp))
                    OfferCarousel(
                        offers = offers,
                        onOfferClick = onOfferClick
                    )
                }
            }

            if (categories.isNotEmpty() && !isSearching) {
                item {
                    Spacer(Modifier.height(22.dp))
                    RowHeader("Categories", Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AllCategoriesTile(
                                selected = selectedCategoryId == null,
                                onClick = { onCategorySelected(null) }
                            )
                        }
                        items(categories, key = { it.id }) { category ->
                            CategoryTile(
                                category = category,
                                color = CategoryColors[
                                    (categories.indexOf(category)).mod(CategoryColors.size)
                                ],
                                onClick = {
                                    onCategorySelected(
                                        if (selectedCategoryId == category.id) null else category.id
                                    )
                                }
                            )
                        }
                    }
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

            if (filteredProducts.isEmpty()) {
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
                // LazyColumn rather than nesting a grid inside it.
                items(filteredProducts.chunked(2)) { pair ->
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
                modifier = Modifier.clickable { onClear() }
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(terms) { term ->
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

@Composable
private fun AllCategoriesTile(selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(104.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) TextPrimary else SurfaceWhite,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "All",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else TextPrimary
            )
        }
    }
}
