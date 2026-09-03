package com.duggustore.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.duggustore.app.ui.theme.Orange
import com.duggustore.app.ui.theme.Teal

/**
 * The scattered grocery-icon motif from the app's logo art, redrawn very
 * faint and tiled behind a screen's content — a nod to the branding without
 * a raster asset to ship or scroll out of sync with the page. Chain this
 * right after `.background(Background)`: it only draws the pattern, not a
 * base colour, so it stays legible over whatever the screen's own
 * background is.
 *
 * Painted with `drawWithCache`/`onDrawBehind` on the screen's own root
 * modifier — not inside the scrollable content — so it holds still as a
 * backdrop while a LazyColumn or scroll state moves over it.
 */
@Composable
fun Modifier.appPatternOverlay(alpha: Float = 0.05f): Modifier {
    val cartPainter = rememberVectorPainter(Icons.Default.ShoppingCart)
    val bagPainter = rememberVectorPainter(Icons.Default.ShoppingBag)
    val basketPainter = rememberVectorPainter(Icons.Default.ShoppingBasket)
    val bottlePainter = rememberVectorPainter(Icons.Default.LocalDrink)
    val leafPainter = rememberVectorPainter(Icons.Default.Eco)

    val painters = remember(cartPainter, bagPainter, basketPainter, bottlePainter, leafPainter) {
        listOf(cartPainter, bagPainter, basketPainter, bottlePainter, leafPainter)
    }
    val tints = remember { listOf(Teal, Orange) }

    return this.drawWithCache {
        val cell = 84.dp.toPx()
        val iconSize = Size(22.dp.toPx(), 22.dp.toPx())
        val cols = (size.width / cell).toInt() + 2
        val rows = (size.height / cell).toInt() + 2

        onDrawBehind {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    // A fixed but non-repeating-looking pick per cell, not
                    // real randomness — the pattern must draw identically
                    // every frame, or it would shimmer as it recomposes.
                    val pick = row * 7 + col * 5
                    val painter = painters[pick % painters.size]
                    val tint = tints[pick % tints.size]
                    // Offsetting alternate rows is what keeps this from
                    // reading as a rigid grid, closer to the loose scatter
                    // in the reference art.
                    val rowOffset = if (row % 2 == 0) 0f else cell / 2f
                    val x = col * cell + rowOffset - iconSize.width / 2f
                    val y = row * cell - iconSize.height / 2f

                    translate(left = x, top = y) {
                        with(painter) {
                            draw(size = iconSize, alpha = alpha, colorFilter = ColorFilter.tint(tint))
                        }
                    }
                }
            }
        }
    }
}
