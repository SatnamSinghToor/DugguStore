package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Category
import com.duggustore.app.ui.components.DashboardEmpty
import com.duggustore.app.ui.components.iconForCategory
import com.duggustore.app.ui.theme.*

/**
 * The categories tab.
 *
 * A flat grid of thirteen tiles tells the customer nothing about how the store
 * is arranged. The rows are grouped into aisles the way a shop is — food first,
 * then snacks, then the non-food shelves — with each aisle titled and its
 * categories laid out four across underneath.
 */
@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Translucent, not solid: the background pattern is meant to show
        // through every part of the app, headers included, not just the
        // plain areas around them.
        Surface(color = Teal.copy(alpha = 0.92f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "Shop by category",
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
        val colourOf = categories.withIndex().associate { (index, category) ->
            category.id to CategoryColors[index.mod(CategoryColors.size)]
        }

        val aisles = remember(categories) { aislesFor(categories) }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(aisles, key = { it.title }) { aisle ->
                AisleBlock(
                    aisle = aisle,
                    colourOf = colourOf,
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

/** One titled group and the categories inside it. */
private data class Aisle(val title: String, val categories: List<Category>)

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

@Composable
private fun AisleBlock(
    aisle: Aisle,
    colourOf: Map<String, Color>,
    onCategoryClick: (Category) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = aisle.title,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 10.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceMuted
        ) {
            Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp)) {
                // Laid out as rows of four by hand rather than as a grid: a
                // LazyVerticalGrid cannot be nested inside the LazyColumn that
                // scrolls the aisles.
                aisle.categories.chunked(4).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { category ->
                            CategoryCell(
                                category = category,
                                color = colourOf[category.id] ?: Teal,
                                modifier = Modifier.weight(1f),
                                onClick = { onCategoryClick(category) }
                            )
                        }
                        // Keeps a short last row aligned with the one above it
                        // instead of spreading four-wide.
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: Category,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForCategory(category.name),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
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
