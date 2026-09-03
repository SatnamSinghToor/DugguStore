package com.duggustore.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.duggustore.app.R

/**
 * The scattered grocery-icon artwork supplied for the app's background —
 * used as-is, not redrawn. Chain this right after `.background(Background)`
 * on a screen's own root modifier (not inside its scrollable content), so
 * it fills behind whatever the screen draws and stays still as a backdrop
 * rather than scrolling with a LazyColumn or scroll state.
 */
@Composable
fun Modifier.appPatternOverlay(): Modifier {
    val painter = painterResource(R.drawable.bg_pattern)
    return this.paint(painter = painter, contentScale = ContentScale.Crop)
}
