package com.duggustore.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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

enum class StoreTab { Home, Categories, Favorites, Account }

/**
 * Bottom bar with the cart lifted into a floating circle in the middle, as in
 * the design. The circle overhangs the bar, so the whole thing sits in a Box
 * and the bar itself is given top padding to leave room.
 */
@Composable
fun StoreBottomBar(
    selected: StoreTab,
    cartCount: Int,
    onSelect: (StoreTab) -> Unit,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            color = SurfaceWhite,
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Outlined.Home, "Home", selected == StoreTab.Home, Modifier.weight(1f)) {
                    onSelect(StoreTab.Home)
                }
                NavItem(Icons.Outlined.GridView, "Categories", selected == StoreTab.Categories, Modifier.weight(1f)) {
                    onSelect(StoreTab.Categories)
                }
                Spacer(Modifier.weight(1f)) // room for the floating cart
                NavItem(Icons.Outlined.FavoriteBorder, "Favourites", selected == StoreTab.Favorites, Modifier.weight(1f)) {
                    onSelect(StoreTab.Favorites)
                }
                NavItem(Icons.Outlined.Person, "Account", selected == StoreTab.Account, Modifier.weight(1f)) {
                    onSelect(StoreTab.Account)
                }
            }
        }

        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Orange)
                    .clickable { onCartClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    "Cart",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            if (cartCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, end = 2.dp)
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(CircleShape)
                        .background(Coral),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (cartCount > 99) "99+" else "$cartCount",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable { onClick() }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Teal else TextLight,
            modifier = Modifier.size(25.dp)
        )
        if (selected) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(5.dp).clip(CircleShape).background(Teal))
        }
    }
}
