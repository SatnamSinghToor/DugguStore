package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Product
import com.duggustore.app.ui.components.DashboardEmpty
import com.duggustore.app.ui.components.StoreProductCard
import com.duggustore.app.ui.components.iconForCategory
import com.duggustore.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * The categories tab: a narrow icon rail on the left, one per real category,
 * and the matching products for whichever one is selected on the right —
 * not another layer of category tiles to tap through. The two stay in sync
 * both ways, the same as the aisle rail this replaced: tapping a rail item
 * scrolls the grid to it, and scrolling the grid moves the rail's highlight.
 */
@Composable
fun CategoriesScreen(
    categories: List<Category>,
    products: List<Product>,
    cartQuantities: Map<String, Int> = emptyMap(),
    favoriteIds: Set<String> = emptySet(),
    onAddToCart: (Product) -> Unit = {},
    onIncrease: (Product) -> Unit = {},
    onDecrease: (Product) -> Unit = {},
    onToggleFavorite: (Product) -> Unit = {},
    onProductClick: (Product) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Surface(color = Teal) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "Categories",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Everything the store carries, category by category",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
        }

        if (categories.isEmpty()) {
            DashboardEmpty(
                icon = Icons.Default.Category,
                title = "No categories yet",
                subtitle = "Categories appear here once the store has some"
            )
            return@Column
        }

        val colourOf = remember(categories) {
            categories.withIndex().associate { (index, category) ->
                category.id to CategoryColors[index.mod(CategoryColors.size)]
            }
        }

        val productsByCategory = remember(products) {
            products.filter { it.isActive }.groupBy { it.categoryId }
        }
        val gridItems = remember(categories, productsByCategory) {
            buildGridItems(categories, productsByCategory)
        }
        val headerItemIndex = remember(gridItems) {
            gridItems.withIndex()
                .filter { it.value is GridItem.Header }
                .associate { (index, item) -> (item as GridItem.Header).categoryIndex to index }
        }

        val rightListState = rememberLazyListState()
        val leftListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        // Whichever category owns the item currently at the top of the grid.
        val selectedCategoryIndex by remember {
            derivedStateOf {
                val topIndex = rightListState.firstVisibleItemIndex.coerceIn(0, gridItems.lastIndex.coerceAtLeast(0))
                gridItems.getOrNull(topIndex)?.categoryIndex ?: 0
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .width(84.dp)
                    .fillMaxHeight()
                    .background(SurfaceMuted),
                state = leftListState
            ) {
                itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                    CategoryRailTile(
                        title = category.name,
                        icon = iconForCategory(category.name),
                        selected = index == selectedCategoryIndex,
                        onClick = {
                            val target = headerItemIndex[index] ?: return@CategoryRailTile
                            coroutineScope.launch { rightListState.animateScrollToItem(target) }
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                state = rightListState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(gridItems, key = { it.key }) { item ->
                    when (item) {
                        is GridItem.Header -> Text(
                            text = item.title,
                            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        is GridItem.Empty -> Text(
                            text = "Nothing here yet",
                            modifier = Modifier.padding(bottom = 12.dp),
                            fontSize = 13.sp,
                            color = TextLight
                        )
                        is GridItem.ProductRow -> Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item.products.forEach { product ->
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
                            if (item.products.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Keeps the rail's own highlighted item scrolled into view as the
        // grid moves past long categories.
        LaunchedEffect(selectedCategoryIndex) {
            leftListState.animateScrollToItem(selectedCategoryIndex.coerceIn(0, categories.lastIndex))
        }
    }
}

private const val PRODUCT_ROW_COLUMNS = 2

/** A flattened row of the right-hand list — a category's title, one row of its products, or "nothing here". */
private sealed class GridItem(val categoryIndex: Int, val key: String) {
    class Header(categoryIndex: Int, val title: String) : GridItem(categoryIndex, "header_$categoryIndex")
    class Empty(categoryIndex: Int) : GridItem(categoryIndex, "empty_$categoryIndex")
    class ProductRow(categoryIndex: Int, rowIndex: Int, val products: List<Product>) :
        GridItem(categoryIndex, "row_${categoryIndex}_$rowIndex")
}

private fun buildGridItems(
    categories: List<Category>,
    productsByCategory: Map<String, List<Product>>
): List<GridItem> = buildList {
    categories.forEachIndexed { categoryIndex, category ->
        add(GridItem.Header(categoryIndex, category.name))
        val items = productsByCategory[category.id].orEmpty()
        if (items.isEmpty()) {
            add(GridItem.Empty(categoryIndex))
        } else {
            items.chunked(PRODUCT_ROW_COLUMNS).forEachIndexed { rowIndex, row ->
                add(GridItem.ProductRow(categoryIndex, rowIndex, row))
            }
        }
    }
}

/** Left rail tile — its background merges into the grid's when selected, the way Blinkit's does. */
@Composable
private fun CategoryRailTile(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(78.dp)) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (selected) Teal else Color.Transparent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (selected) Background else Color.Transparent)
                .clickable { onClick() }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) TealSurface else SurfaceWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Teal else TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Teal else TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
