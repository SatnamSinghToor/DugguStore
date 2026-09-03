package com.duggustore.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.duggustore.app.ui.screens.auth.VerifyEmailScreen
import com.duggustore.app.ui.screens.customer.*
import com.duggustore.app.ui.screens.seller.SellerDashboard
import com.duggustore.app.ui.screens.delivery.DeliveryDashboard
import com.duggustore.app.ui.screens.admin.AdminDashboard
import com.duggustore.app.ui.viewmodel.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object VerifyEmail : Screen("verify_email/{email}") {
        fun createRoute(email: String) = "verify_email/$email"
    }
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

    // Navigate to verify email when signup requires verification
    var hasNavigatedToVerify by remember { mutableStateOf(false) }
    LaunchedEffect(authState.requiresEmailVerification, authState.pendingVerificationEmail) {
        if (authState.requiresEmailVerification && authState.pendingVerificationEmail.isNotEmpty() && !hasNavigatedToVerify) {
            hasNavigatedToVerify = true
            navController.navigate(Screen.VerifyEmail.createRoute(authState.pendingVerificationEmail)) {
                popUpTo(Screen.Register.route) { inclusive = true }
            }
        }
        if (!authState.requiresEmailVerification) {
            hasNavigatedToVerify = false
        }
    }

    // Navigate to role-based dashboard after successful login/signup
    var hasNavigatedToDashboard by remember { mutableStateOf(false) }
    LaunchedEffect(authState.isLoggedIn, authState.user) {
        if (authState.isLoggedIn && authState.user != null && !hasNavigatedToDashboard) {
            hasNavigatedToDashboard = true
            when (authState.user?.userRole()) {
                UserRole.SELLER -> navController.navigate(Screen.SellerDashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                UserRole.DELIVERY -> navController.navigate(Screen.DeliveryDashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                else -> navController.navigate(Screen.CustomerHome.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
        if (!authState.isLoggedIn) {
            hasNavigatedToDashboard = false
        }
    }

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
        composable(Screen.Login.route) {
            LoginScreen(
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

        composable(
            Screen.VerifyEmail.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: authState.pendingVerificationEmail

            VerifyEmailScreen(
                email = email,
                onResendEmail = { authViewModel.resendVerificationEmail() },
                onBackToLogin = {
                    authViewModel.resetVerificationState()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                isLoading = authState.isLoading,
                error = authState.error,
                verificationResent = authState.verificationResent,
                onClearError = { authViewModel.clearError() }
            )
        }

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
                onAccountClick = { navController.navigate(Screen.CustomerAccount.route) }
            )

            LaunchedEffect(Unit) {
                homeViewModel.loadData()
            }
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
                onIncrementQuantity = { itemId, qty -> cartViewModel.updateQuantity(itemId, qty + 1) },
                onDecrementQuantity = { itemId, qty -> cartViewModel.updateQuantity(itemId, qty - 1) },
                onRemoveItem = { cartViewModel.removeItem(it) },
                onApplyCoupon = { cartViewModel.applyCoupon(it) },
                onPlaceOrder = { cartViewModel.placeOrder(authState.user?.id ?: "", "") },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(Unit) {
                cartViewModel.loadCart()
            }

            if (cartState.orderPlaced) {
                AlertDialog(
                    onDismissRequest = { cartViewModel.resetOrderPlaced() },
                    title = { Text("Order Placed!") },
                    text = { Text("Your order has been placed successfully. You can track it in Orders.") },
                    confirmButton = {
                        TextButton(onClick = {
                            cartViewModel.resetOrderPlaced()
                            navController.navigate(Screen.CustomerOrders.route)
                        }) {
                            Text("View Orders")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { cartViewModel.resetOrderPlaced() }) {
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
                onAddressesClick = { },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SellerDashboard.route) {
            SellerDashboard(
                products = sellerState.products,
                orders = sellerState.orders,
                totalRevenue = sellerState.totalRevenue,
                totalOrders = sellerState.totalOrders,
                onAddProduct = { },
                onEditProduct = { },
                onDeleteProduct = { sellerViewModel.deleteProduct(it, authState.user?.id ?: "") },
                onUpdateOrderStatus = { orderId, status -> orderViewModel.updateOrderStatus(orderId, status) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
            LaunchedEffect(authState.user) {
                authState.user?.let { sellerViewModel.loadSellerData(it.id) }
            }
        }

        composable(Screen.DeliveryDashboard.route) {
            DeliveryDashboard(
                activeOrders = deliveryState.activeOrders,
                completedOrders = deliveryState.completedOrders,
                totalEarnings = deliveryState.totalEarnings,
                totalDeliveries = deliveryState.totalDeliveries,
                onMarkDelivered = { deliveryViewModel.markDelivered(it, authState.user?.id ?: "") },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
            LaunchedEffect(authState.user) {
                authState.user?.let { deliveryViewModel.loadDeliveryData(it.id) }
            }
        }

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
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
            LaunchedEffect(Unit) {
                adminViewModel.loadDashboard()
            }
        }
    }
}
