package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Product
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
    onAddressClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceWhite)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoreWordmark()
                        Spacer(Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Eng", fontSize = 14.sp, color = TextPrimary)
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.size(40.dp).background(SurfaceMuted, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                "Notifications",
                                tint = Coral,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    LocationBar(
                        city = if (userName.isBlank()) "Deliver to" else "Hi $userName",
                        address = deliveryAddress,
                        onClick = onAddressClick
                    )

                    Spacer(Modifier.height(12.dp))

                    StoreSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onMicClick = {}
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                PromoBanner(
                    title = "Happy Weekend",
                    highlight = "25% OFF",
                    caption = "*on selected items",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (categories.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(22.dp))
                    RowHeader("Categories", Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
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
                    modifier = Modifier.padding(horizontal = 16.dp)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
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
