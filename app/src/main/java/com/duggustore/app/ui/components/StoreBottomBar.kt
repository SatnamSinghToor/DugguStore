package com.duggustore.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.theme.*

/** One destination in the bottom bar. `key` is what the caller matches on. */
data class BottomNavItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

/** The floating circle in the middle of the bar. Only the customer bar has one. */
data class BottomBarCentre(
    val count: Int,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * Height of the bar itself, above the system navigation inset. Callers pad their
 * content by this so nothing ends up underneath it.
 */
val StoreBottomBarHeight = 68.dp

/**
 * Bottom navigation, driven by whatever list of destinations the caller passes, so
 * each role gets its own set. With a centre the cart is lifted into a floating
 * circle that overhangs the bar, as in the design.
 */
@Composable
fun StoreBottomBar(
    items: List<BottomNavItem>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    centre: BottomBarCentre? = null
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (centre != null) 22.dp else 0.dp),
            color = SurfaceWhite,
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(StoreBottomBarHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // With a centre the items are split either side of the gap the
                // floating circle sits in.
                val split = if (centre != null) (items.size + 1) / 2 else items.size

                items.forEachIndexed { index, item ->
                    if (centre != null && index == split) {
                        Spacer(Modifier.weight(1f))
                    }
                    NavItem(
                        item = item,
                        selected = item.key == selectedKey,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(item.key) }
                    )
                }
                // An odd number of items would otherwise leave the gap at the end.
                if (centre != null && items.size <= split) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        if (centre != null) {
            CartCircle(centre)
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Selected is a filled teal pill carrying the icon and the label; unselected is
    // a grey icon on its own. Previously only the tint changed, and since the bar
    // was hard-coded to Home it never appeared to change at all.
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) TealSurface else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (selected) Teal else TextLight,
                modifier = Modifier.size(23.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                maxLines = 1,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Teal else TextLight
            )
        }
    }
}

@Composable
private fun CartCircle(centre: BottomBarCentre) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(SurfaceWhite)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (centre.selected) TealDark else Orange)
                .clickable { centre.onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                "Cart",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        if (centre.count > 0) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 2.dp)
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(CircleShape)
                    .background(Coral),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (centre.count > 99) "99+" else "${centre.count}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )
            }
        }
    }
}
