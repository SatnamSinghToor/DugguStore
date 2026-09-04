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
import com.duggustore.app.ui.components.DashboardEmpty
import com.duggustore.app.ui.components.iconForCategory
import com.duggustore.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * The categories tab, laid out the way Blinkit's own category browser
 * is: a narrow icon rail on the left for the aisles, a scrolling grid
 * of that aisle's categories on the right. The two stay in sync both
 * ways — tapping a rail item scrolls the grid to it, and scrolling the
 * grid moves the rail's highlight — rather than the rail being a static
 * jump-menu.
 *
 * There's no parent/subcategory column in the schema, so "aisle" here
 * is the same name-based grouping the old flat list used; it just now
 * doubles as the rail's sections instead of being plain section headers.
 */
@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit
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
                    text = "Everything the store carries, aisle by aisle",
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

        // The colour index is taken over the whole list rather than per aisle,
        // so a category keeps the same colour it has on the home rail.
        val colourOf = remember(categories) {
            categories.withIndex().associate { (index, category) ->
                category.id to CategoryColors[index.mod(CategoryColors.size)]
            }
        }

        val aisles = remember(categories) { aislesFor(categories) }
        val gridItems = remember(aisles) { buildGridItems(aisles) }
        val headerItemIndex = remember(gridItems) {
            gridItems.withIndex()
                .filter { it.value is GridItem.Header }
                .associate { (index, item) -> (item as GridItem.Header).aisleIndex to index }
        }

        val rightListState = rememberLazyListState()
        val leftListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        // Whichever aisle owns the item currently at the top of the grid —
        // recomputed as a derivedStateOf so scrolling doesn't recompose the
        // whole screen on every pixel, only when the answer actually changes.
        val selectedAisleIndex by remember {
            derivedStateOf {
                val topIndex = rightListState.firstVisibleItemIndex.coerceIn(0, gridItems.lastIndex.coerceAtLeast(0))
                gridItems.getOrNull(topIndex)?.aisleIndex ?: 0
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
                itemsIndexed(aisles, key = { _, aisle -> aisle.title }) { index, aisle ->
                    AisleRailTile(
                        title = aisle.title,
                        icon = iconForCategory(aisle.categories.firstOrNull()?.name.orEmpty()),
                        selected = index == selectedAisleIndex,
                        onClick = {
                            val target = headerItemIndex[index] ?: return@AisleRailTile
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
                        is GridItem.Row -> Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            item.categories.forEach { category ->
                                CategoryGridTile(
                                    category = category,
                                    color = colourOf[category.id] ?: Teal,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCategoryClick(category) }
                                )
                            }
                            repeat(GRID_COLUMNS - item.categories.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Keeps the rail's own highlighted item scrolled into view as the
        // grid moves past long aisles — otherwise the highlight could land
        // on a rail item currently scrolled off-screen.
        LaunchedEffect(selectedAisleIndex) {
            leftListState.animateScrollToItem(selectedAisleIndex.coerceIn(0, aisles.lastIndex))
        }
    }
}

private const val GRID_COLUMNS = 3

/** One titled group and the categories inside it. */
private data class Aisle(val title: String, val categories: List<Category>)

/** A flattened row of the right-hand grid — either an aisle's title or one row of its tiles. */
private sealed class GridItem(val aisleIndex: Int, val key: String) {
    class Header(aisleIndex: Int, val title: String) : GridItem(aisleIndex, "header_$aisleIndex")
    class Row(aisleIndex: Int, rowIndex: Int, val categories: List<Category>) :
        GridItem(aisleIndex, "row_${aisleIndex}_$rowIndex")
}

private fun buildGridItems(aisles: List<Aisle>): List<GridItem> = buildList {
    aisles.forEachIndexed { aisleIndex, aisle ->
        add(GridItem.Header(aisleIndex, aisle.title))
        aisle.categories.chunked(GRID_COLUMNS).forEachIndexed { rowIndex, row ->
            add(GridItem.Row(aisleIndex, rowIndex, row))
        }
    }
}

/**
 * Sorts the store's categories into aisles by name.
 *
 * The categories table is flat — there is no parent column to group on — so the
 * grouping is derived from the name. Anything the mapping does not recognise
 * falls into "More", which means a category added later still appears rather
 * than vanishing from this screen.
 */
private fun aislesFor(categories: List<Category>): List<Aisle> {
    fun matching(vararg keys: String) = categories.filter { category ->
        keys.any { category.name.contains(it, ignoreCase = true) }
    }

    val kitchen = matching("veg", "fruit", "dairy", "milk", "bread", "baker", "groc", "meat", "egg")
    val snacks = matching("snack", "choc", "drink", "cold", "frozen", "ice", "sweet", "biscuit")
    val personal = matching("shampoo", "beauty", "baby", "care", "hygiene", "soap")
    val household = matching("clean", "home", "household", "kitchenware", "util")

    val placed = (kitchen + snacks + personal + household).map { it.id }.toSet()
    val rest = categories.filter { it.id !in placed }

    return listOf(
        Aisle("Grocery & Kitchen", kitchen),
        Aisle("Snacks & Drinks", snacks),
        Aisle("Beauty & Personal Care", personal),
        Aisle("Household Essentials", household),
        Aisle("More", rest)
    ).filter { it.categories.isNotEmpty() }
}

/** Left rail tile — its background merges into the grid's when selected, the way Blinkit's does. */
@Composable
private fun AisleRailTile(
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

/** The right pane's card — bigger and more "product tile" than the old flat grid cell. */
@Composable
private fun CategoryGridTile(
    category: Category,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable { onClick() },
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.62f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForCategory(category.name),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = category.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
