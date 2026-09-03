package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onOfferClick: (Coupon) -> Unit = {}
) {
    // Both sheets are owned here so the header can stay a plain row of
    // controls and the sheets sit above the whole screen.
    var showLocationSheet by remember { mutableStateOf(false) }
    val detected = rememberDeviceLocation()
    val voice = rememberVoiceSearchController { onSearchQueryChange(it) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Pinned. The wordmark, the location strip and the search field were
            // the first item of the list, so they scrolled away with the
            // products — searching meant scrolling back to the top first.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .statusBarsPadding()
                    // 20dp, not the usual 16 — this is what lines the search
                    // bar's edges up with the offer cards below it, whose
                    // width comes from the pager's own 20dp contentPadding.
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            }

            LazyColumn(
                // weight, not fillMaxSize: the header above has already taken
                // its height, and fillMaxSize here would ask for the whole
                // screen again and push the list off the bottom.
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

            if (offers.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    OfferCarousel(
                        offers = offers,
                        onOfferClick = onOfferClick
                    )
                }
            }

            if (categories.isNotEmpty()) {
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
