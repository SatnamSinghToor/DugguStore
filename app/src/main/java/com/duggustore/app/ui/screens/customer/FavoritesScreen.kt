package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun FavoritesScreen(
    favoriteProducts: List<Product>,
    onAddToCart: (Product) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        DugguTopBar(title = "My Favorites", onBackClick = onBack)

        if (favoriteProducts.isEmpty()) {
            EmptyState(
                icon = Icons.Default.FavoriteBorder,
                title = "No favorites yet",
                subtitle = "Tap the heart icon on products to add them here"
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoriteProducts) { product ->
                    ProductCard(
                        product = product,
                        onAddClick = onAddToCart
                    )
                }
            }
        }
    }
}
