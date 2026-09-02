package com.duggustore.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.duggustore.app.data.model.UserRole
import com.duggustore.app.ui.screens.auth.LoginScreen
import com.duggustore.app.ui.screens.auth.RegisterScreen
import com.duggustore.app.ui.screens.customer.*
import com.duggustore.app.ui.screens.seller.SellerDashboard
import com.duggustore.app.ui.screens.delivery.DeliveryDashboard
import com.duggustore.app.ui.screens.admin.AdminDashboard
import com.duggustore.app.ui.viewmodel.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object CustomerHome : Screen("customer_home")
    object CustomerCart : Screen("customer_cart")
    object CustomerOrders : Screen("customer_orders")
    object CustomerOrderTracking : Screen("customer_order_tracking/{orderId}")
    object CustomerFavorites : Screen("customer_favorites")
    object CustomerAccount : Screen("customer_account")
    object SellerDashboard : Screen("seller_dashboard")
    object DeliveryDashboard : Screen("delivery_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel(),
    favoriteViewModel: FavoriteViewModel = viewModel(),
    sellerViewModel: SellerViewModel = viewModel(),
    deliveryViewModel: DeliveryViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val homeState by homeViewModel.state.collectAsState()
    val cartState by cartViewModel.state.collectAsState()
    val orderState by orderViewModel.state.collectAsState()
    val favState by favoriteViewModel.state.collectAsState()
    val sellerState by sellerViewModel.state.collectAsState()
    val deliveryState by deliveryViewModel.state.collectAsState()
    val adminState by adminViewModel.state.collectAsState()

    // Set up initial route based on auth state
    val startDestination = remember(authState.isLoggedIn, authState.user) {
        if (!authState.isLoggedIn) Screen.Login.route
        else {
            when (authState.user?.userRole()) {
                UserRole.SELLER -> Screen.SellerDashboard.route
                UserRole.DELIVERY -> Screen.DeliveryDashboard.route
                UserRole.ADMIN -> Screen.AdminDashboard.route
                else -> Screen.CustomerHome.route
            }
        }
    }

    // Initialize cart when user is logged in
    LaunchedEffect(authState.user) {
        authState.user?.let { user ->
            cartViewModel.setCustomer(user.id)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(300)) }
    ) {
        // Auth Screens
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    authState.user?.let { user ->
                        when (user.userRole()) {
                            UserRole.SELLER -> navController.navigate(Screen.SellerDashboard.route)
                            UserRole.DELIVERY -> navController.navigate(Screen.DeliveryDashboard.route)
                            UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route)
                            else -> navController.navigate(Screen.CustomerHome.route)
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                isLoading = authState.isLoading,
                error = authState.error,
                onLogin = { email, password -> authViewModel.signIn(email, password) },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    authState.user?.let { user ->
                        when (user.userRole()) {
                            UserRole.SELLER -> navController.navigate(Screen.SellerDashboard.route)
                            UserRole.DELIVERY -> navController.navigate(Screen.DeliveryDashboard.route)
                            UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route)
                            else -> navController.navigate(Screen.CustomerHome.route)
                        }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
                isLoading = authState.isLoading,
                error = authState.error,
                successMessage = authState.successMessage,
                onRegister = { email, pass, name, phone, role ->
                    authViewModel.signUp(email, pass, name, phone, role)
                },
                onClearError = { authViewModel.clearError() },
                onClearSuccess = { authViewModel.clearSuccess() }
            )
        }

        // Customer Screens
        composable(Screen.CustomerHome.route) {
            HomeScreen(
                categories = homeState.categories,
                filteredProducts = homeState.filteredProducts,
                selectedCategoryId = homeState.selectedCategoryId,
                searchQuery = homeState.searchQuery,
                cartItemCount = cartState.itemCount,
                onSearchQueryChange = { homeViewModel.search(it) },
                onCategorySelected = { homeViewModel.selectCategory(it) },
                onAddToCart = { cartViewModel.addToCart(it) },
                onCartClick = { navController.navigate(Screen.CustomerCart.route) },
                onFavoritesClick = { navController.navigate(Screen.CustomerFavorites.route) },
                onAccountClick = { navController.navigate(Screen.CustomerAccount.route) },
                onOrdersClick = { navController.navigate(Screen.CustomerOrders.route) }
            )
        }

        composable(Screen.CustomerCart.route) {
            CartScreen(
                cartItems = cartState.cartItems,
                subtotal = cartState.subtotal,
                deliveryFee = cartState.deliveryFee,
                total = cartState.total,
                savings = cartState.savings,
                couponApplied = cartState.couponApplied,
                couponDiscount = cartState.couponDiscount,
                isLoading = cartState.isLoading,
                onIncrementQuantity = { id, qty -> cartViewModel.updateQuantity(id, qty) },
                onDecrementQuantity = { id, qty -> cartViewModel.updateQuantity(id, qty) },
                onRemoveItem = { cartViewModel.removeItem(it) },
                onApplyCoupon = { cartViewModel.applyCoupon(it) },
                onPlaceOrder = {
                    cartViewModel.placeOrder(
                        sellerId = "",
                        deliveryAddress = "Pankaj Residential, Lande Colony, Siyana Road, Ghaziabad"
                    )
                },
                onBack = { navController.popBackStack() }
            )

            // Show success dialog
            if (cartState.orderPlaced) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { cartViewModel.resetOrderPlaced() },
                    title = { Text("Order Placed! 🎉") },
                    text = { Text("Your order has been placed successfully. You can track it in My Orders.") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            cartViewModel.resetOrderPlaced()
                            navController.navigate(Screen.CustomerOrders.route) {
                                popUpTo(Screen.CustomerHome.route) { inclusive = false }
                            }
                        }) {
                            Text("View Orders")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { cartViewModel.resetOrderPlaced() }) {
                            Text("Continue Shopping")
                        }
                    }
                )
            }
        }

        composable(Screen.CustomerOrders.route) {
            OrderListScreen(
                orders = orderState.customerOrders,
                onOrderClick = { orderId ->
                    navController.navigate("customer_order_tracking/$orderId")
                },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(Unit) {
                authState.user?.let { orderViewModel.loadCustomerOrders(it.id) }
            }
        }

        composable(
            Screen.CustomerOrderTracking.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val order = orderState.customerOrders.find { it.id == orderId }

            order?.let {
                OrderTrackingDetailScreen(
                    order = it,
                    onCancelOrder = { orderViewModel.cancelOrder(orderId) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.CustomerFavorites.route) {
            FavoritesScreen(
                favoriteProducts = favState.favoriteProducts,
                onAddToCart = { cartViewModel.addToCart(it) },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(Unit) {
                authState.user?.let { favoriteViewModel.loadFavorites(it.id) }
            }
        }

        composable(Screen.CustomerAccount.route) {
            AccountScreen(
                user = authState.user,
                onOrdersClick = { navController.navigate(Screen.CustomerOrders.route) },
                onFavoritesClick = { navController.navigate(Screen.CustomerFavorites.route) },
                onAddressesClick = { /* TODO: Addresses screen */ },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Seller Dashboard
        composable(Screen.SellerDashboard.route) {
            SellerDashboard(
                products = sellerState.products,
                orders = sellerState.orders,
                totalRevenue = sellerState.totalRevenue,
                totalOrders = sellerState.totalOrders,
                onAddProduct = { /* TODO: Add Product Dialog */ },
                onEditProduct = { /* TODO: Edit Product */ },
                onDeleteProduct = { sellerViewModel.deleteProduct(it, authState.user?.id ?: "") },
                onUpdateOrderStatus = { orderId, status -> orderViewModel.updateOrderStatus(orderId, status) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )

            LaunchedEffect(authState.user) {
                authState.user?.let { sellerViewModel.loadSellerData(it.id) }
            }
        }

        // Delivery Dashboard
        composable(Screen.DeliveryDashboard.route) {
            DeliveryDashboard(
                activeOrders = deliveryState.activeOrders,
                completedOrders = deliveryState.completedOrders,
                totalEarnings = deliveryState.totalEarnings,
                totalDeliveries = deliveryState.totalDeliveries,
                onMarkDelivered = { deliveryViewModel.markDelivered(it, authState.user?.id ?: "") },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )

            LaunchedEffect(authState.user) {
                authState.user?.let { deliveryViewModel.loadDeliveryData(it.id) }
            }
        }

        // Admin Dashboard
        composable(Screen.AdminDashboard.route) {
            AdminDashboard(
                users = adminState.users,
                orders = adminState.orders,
                products = adminState.products,
                totalUsers = adminState.totalUsers,
                totalOrders = adminState.totalOrders,
                totalRevenue = adminState.totalRevenue,
                totalDeliveries = adminState.totalDeliveries,
                onUpdateUserRole = { userId, role -> adminViewModel.updateUserRole(userId, role) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )

            LaunchedEffect(Unit) {
                adminViewModel.loadDashboard()
            }
        }
    }
}
