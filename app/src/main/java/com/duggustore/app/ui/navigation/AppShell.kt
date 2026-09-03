package com.duggustore.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Person
import com.duggustore.app.data.model.UserRole
import com.duggustore.app.ui.components.BottomNavItem

/**
 * The bottom bar's destinations for each role. Customers navigate between routes;
 * the other three roles switch tabs inside their one dashboard, so their keys are
 * tab indices rather than routes.
 */
object BottomNav {

    val CART_KEY = "cart"

    fun itemsFor(role: UserRole): List<BottomNavItem> = when (role) {
        UserRole.CUSTOMER -> listOf(
            BottomNavItem(Screen.CustomerHome.route, "Home", Icons.Outlined.Home),
            BottomNavItem(Screen.CustomerCategories.route, "Categories", Icons.Outlined.GridView),
            BottomNavItem(Screen.CustomerFavorites.route, "Favourites", Icons.Outlined.FavoriteBorder),
            BottomNavItem(Screen.CustomerAccount.route, "Account", Icons.Outlined.Person)
        )
        UserRole.SELLER -> listOf(
            BottomNavItem("0", "Products", Icons.Default.Inventory),
            BottomNavItem("1", "Orders", Icons.Default.Receipt)
        )
        UserRole.DELIVERY -> listOf(
            BottomNavItem("0", "Active", Icons.Default.LocalShipping),
            BottomNavItem("1", "Completed", Icons.Default.Receipt)
        )
        UserRole.ADMIN -> listOf(
            BottomNavItem("0", "Overview", Icons.Outlined.GridView),
            BottomNavItem("1", "Users", Icons.Outlined.Person),
            BottomNavItem("2", "Orders", Icons.Default.Receipt),
            BottomNavItem("3", "Products", Icons.Default.Inventory)
        )
    }

    /**
     * Routes that carry the bar. Detail, form and checkout screens are pushes on
     * top of these — a tab bar there would offer to navigate away mid-task — so
     * they keep their back arrow instead.
     */
    private val CUSTOMER_TOP_LEVEL = setOf(
        Screen.CustomerHome.route,
        Screen.CustomerCategories.route,
        Screen.CustomerFavorites.route,
        Screen.CustomerAccount.route,
        Screen.CustomerOrders.route,
        Screen.CustomerCart.route
    )

    private val DASHBOARD_ROUTES = setOf(
        Screen.SellerDashboard.route,
        Screen.DeliveryDashboard.route,
        Screen.AdminDashboard.route
    )

    fun showsBar(route: String?, role: UserRole): Boolean = when (role) {
        UserRole.CUSTOMER -> route in CUSTOMER_TOP_LEVEL
        else -> route in DASHBOARD_ROUTES
    }

    /**
     * Which item reads as selected. Orders and Cart are reached from elsewhere and
     * are not themselves tabs, so Orders highlights Account (where it is reached
     * from) and Cart highlights nothing but the circle.
     */
    fun selectedKeyFor(route: String?): String? = when (route) {
        Screen.CustomerOrders.route -> Screen.CustomerAccount.route
        Screen.CustomerCart.route -> null
        else -> route
    }
}
