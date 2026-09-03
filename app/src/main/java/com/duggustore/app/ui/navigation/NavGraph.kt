package com.duggustore.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay
import com.duggustore.app.platform.RiderLocationPublisher
import com.duggustore.app.platform.rememberRiderPosition
import com.duggustore.app.ui.components.BottomBarCentre
import com.duggustore.app.ui.components.StoreBottomBar
import com.duggustore.app.ui.components.StoreBottomBarHeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.res.stringResource
import com.duggustore.app.R
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.data.model.UserRole
import com.duggustore.app.data.model.toNotification
import com.duggustore.app.ui.screens.auth.ForgotPasswordScreen
import com.duggustore.app.ui.screens.auth.LoginScreen
import com.duggustore.app.ui.screens.auth.RegisterScreen
import com.duggustore.app.ui.screens.auth.ResetPasswordScreen
import com.duggustore.app.ui.screens.auth.SplashScreen
import com.duggustore.app.ui.screens.auth.VerifyEmailScreen
import com.duggustore.app.ui.screens.customer.*
import com.duggustore.app.ui.screens.seller.AddEditProductScreen
import com.duggustore.app.ui.screens.seller.SellerDashboard
import com.duggustore.app.ui.screens.delivery.DeliveryDashboard
import com.duggustore.app.ui.screens.admin.AdminDashboard
import com.duggustore.app.ui.viewmodel.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")
    object Register : Screen("register")
    object VerifyEmail : Screen("verify_email/{email}") {
        fun createRoute(email: String) = "verify_email/$email"
    }
    object CustomerHome : Screen("customer_home")
    object CustomerCart : Screen("customer_cart")
    object CustomerOrders : Screen("customer_orders")
    object CustomerOrderTracking : Screen("customer_order_tracking/{orderId}") {
        fun createRoute(orderId: String) = "customer_order_tracking/$orderId"
    }
    object CustomerCategories : Screen("customer_categories")
    object CustomerNotifications : Screen("customer_notifications")
    object CustomerFavorites : Screen("customer_favorites")
    object CustomerAccount : Screen("customer_account")
    object CustomerAddresses : Screen("customer_addresses")
    object CustomerCheckout : Screen("customer_checkout")
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    object SellerProductForm : Screen("seller_product_form?productId={productId}") {
        fun createRoute(productId: String = "") = "seller_product_form?productId=$productId"
    }
    object SellerDashboard : Screen("seller_dashboard")
    object DeliveryDashboard : Screen("delivery_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
}

// consumeWindowInsets is still marked experimental in this Compose version.
@OptIn(ExperimentalLayoutApi::class)
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
    adminViewModel: AdminViewModel = viewModel(),
    addressViewModel: AddressViewModel = viewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val homeState by homeViewModel.state.collectAsState()
    val cartState by cartViewModel.state.collectAsState()
    val orderState by orderViewModel.state.collectAsState()
    val favState by favoriteViewModel.state.collectAsState()
    val sellerState by sellerViewModel.state.collectAsState()
    val deliveryState by deliveryViewModel.state.collectAsState()
    val adminState by adminViewModel.state.collectAsState()
    val addressState by addressViewModel.state.collectAsState()

    // Hold on the splash until the stored session has been checked, otherwise the app
    // shows the login screen for a moment and then jumps to a dashboard.
    if (authState.isRestoringSession) {
        SplashScreen()
        return
    }

    // A password-reset deep link arrives with a recovery session attached and has to be
    // finished before anything else, so it takes over ahead of the nav graph.
    if (authState.awaitingNewPassword) {
        ResetPasswordScreen(
            onSubmit = { pass, confirm -> authViewModel.updatePassword(pass, confirm) },
            onCancel = { authViewModel.cancelPasswordRecovery() },
            isLoading = authState.isLoading,
            error = authState.error,
            onClearError = { authViewModel.clearError() }
        )
        return
    }

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
            addressViewModel.setUser(user.id)
        }
    }

    // The bar is part of the shell rather than of any one screen, so every
    // top-level destination gets it and the selected item always matches where
    // the user actually is.
    val role = authState.user?.userRole() ?: UserRole.CUSTOMER
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBar = BottomNav.showsBar(currentRoute, role)
    // The three dashboards are one screen each with tabs inside them, so the bar
    // drives that index instead of navigating.
    var dashboardTab by rememberSaveable { mutableStateOf(0) }

    // The bar sits above the system navigation inset, so the space it occupies is
    // its own height plus that inset. Consuming the same amount stops screens with
    // their own pinned bottoms (the cart's bill sheet) adding the inset a second
    // time underneath it.
    val barSpace = if (showBar) {
        StoreBottomBarHeight +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {

    NavHost(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = barSpace)
            .consumeWindowInsets(PaddingValues(bottom = barSpace)),
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
                onForgotPassword = {
                    authViewModel.resetPasswordResetState()
                    navController.navigate(Screen.ForgotPassword.route)
                },
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
                onVerifyCode = { code -> authViewModel.verifyEmailCode(email, code) },
                onResendEmail = { authViewModel.resendVerificationEmail(email) },
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

        composable(Screen.CustomerNotifications.route) {
            NotificationsScreen(
                notifications = orderState.customerOrders.map { it.toNotification() },
                onNotificationClick = { notification ->
                    navController.navigate(
                        Screen.CustomerOrderTracking.createRoute(notification.orderId)
                    )
                },
                onBack = { navController.popBackStack() }
            )
            LaunchedEffect(authState.user) {
                authState.user?.let { orderViewModel.loadCustomerOrders(it.id) }
            }
        }

        composable(Screen.CustomerCategories.route) {
            CategoriesScreen(
                categories = homeState.categories,
                onCategoryClick = { category ->
                    homeViewModel.selectCategory(category.id)
                    navController.navigate(Screen.CustomerHome.route) {
                        popUpTo(Screen.CustomerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CustomerHome.route) {
            HomeScreen(
                categories = homeState.categories,
                filteredProducts = homeState.filteredProducts,
                selectedCategoryId = homeState.selectedCategoryId,
                searchQuery = homeState.searchQuery,
                onSearchQueryChange = { homeViewModel.search(it) },
                onCategorySelected = { homeViewModel.selectCategory(it) },
                onAddToCart = { cartViewModel.addToCart(it) },
                onProductClick = { navController.navigate(Screen.ProductDetail.createRoute(it.id)) },
                userName = authState.user?.fullName?.substringBefore(' ').orEmpty(),
                deliveryAddress = addressState.defaultAddress?.fullAddress
                    ?: stringResource(R.string.home_set_address),
                // Lets a card show a stepper instead of "Add to cart" once the
                // product is already in the cart, as in the design.
                cartQuantities = cartState.cartItems.associate { it.productId to it.quantity },
                favoriteIds = favState.favorites.map { it.productId }.toSet(),
                onIncrease = { cartViewModel.addToCart(it) },
                onDecrease = { product ->
                    cartState.cartItems.firstOrNull { it.productId == product.id }?.let { item ->
                        cartViewModel.updateQuantity(item.id, item.quantity - 1)
                    }
                },
                onToggleFavorite = { product ->
                    authState.user?.let { favoriteViewModel.toggleFavorite(it.id, product.id) }
                },
                onAddressClick = { navController.navigate(Screen.CustomerAddresses.route) },
                // Orders still in flight are the ones worth a badge; delivered
                // and cancelled ones are not news.
                notificationCount = orderState.customerOrders.count {
                    it.status != OrderStatus.DELIVERED.value &&
                        it.status != OrderStatus.CANCELLED.value
                },
                onNotificationsClick = { navController.navigate(Screen.CustomerNotifications.route) },
                savedAddresses = addressState.addresses,
                onSelectAddress = { addressViewModel.setDefault(it.id) },
                // A detected address is only useful if it survives the session,
                // so picking it saves it as the new default rather than holding
                // it in memory until the next launch.
                onSaveDetectedAddress = { detected, lat, lng ->
                    addressViewModel.saveAddress(
                        label = "Current location",
                        fullAddress = detected,
                        isDefault = true,
                        latitude = lat,
                        longitude = lng
                    )
                },
                offers = homeState.offers,
                // The card carries a real coupon code, so tapping it puts the
                // customer where they can spend it.
                onOfferClick = { navController.navigate(Screen.CustomerCart.route) }
            )

            LaunchedEffect(authState.user) {
                authState.user?.let { orderViewModel.loadCustomerOrders(it.id) }
                authState.user?.let { favoriteViewModel.loadFavorites(it.id) }
                cartViewModel.loadCart()
            }

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
                // The screen passes the quantity it wants, already adjusted.
                onIncrementQuantity = { itemId, qty -> cartViewModel.updateQuantity(itemId, qty) },
                onDecrementQuantity = { itemId, qty -> cartViewModel.updateQuantity(itemId, qty) },
                onRemoveItem = { cartViewModel.removeItem(it) },
                onApplyCoupon = { cartViewModel.applyCoupon(it) },
                onPlaceOrder = { navController.navigate(Screen.CustomerCheckout.route) },
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
                    navController.navigate(Screen.CustomerOrderTracking.createRoute(orderId))
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
                val riderPosition = rememberRiderPosition(
                    tracking = orderState.tracking,
                    deliveryAddress = it.deliveryAddress
                )

                OrderTrackingDetailScreen(
                    order = it,
                    onCancelOrder = { orderViewModel.cancelOrder(orderId) },
                    onBack = { navController.popBackStack() },
                    tracking = orderState.tracking,
                    riderDistanceMetres = riderPosition.distanceMetres,
                    riderFixAgeMinutes = riderPosition.ageMinutes
                )

                // Polled only while this screen is on top and the order is
                // actually on the road; the effect is cancelled when either
                // stops being true.
                LaunchedEffect(orderId, it.status) {
                    if (it.status != OrderStatus.OUT_FOR_DELIVERY.value) {
                        orderViewModel.clearTracking()
                        return@LaunchedEffect
                    }
                    while (true) {
                        orderViewModel.loadTracking(orderId)
                        delay(15_000L)
                    }
                }
            }
        }

        composable(Screen.CustomerFavorites.route) {
            FavoritesScreen(
                favoriteProducts = favState.favoriteProducts,
                onAddToCart = { cartViewModel.addToCart(it) },
                onBack = { navController.popBackStack() },
                onProductClick = { navController.navigate(Screen.ProductDetail.createRoute(it.id)) },
                cartQuantities = cartState.cartItems.associate { it.productId to it.quantity },
                onIncrease = { cartViewModel.addToCart(it) },
                onDecrease = { product ->
                    cartState.cartItems.firstOrNull { it.productId == product.id }?.let { item ->
                        cartViewModel.updateQuantity(item.id, item.quantity - 1)
                    }
                },
                onRemoveFavorite = { product ->
                    authState.user?.let { favoriteViewModel.toggleFavorite(it.id, product.id) }
                }
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
                onAddressesClick = { navController.navigate(Screen.CustomerAddresses.route) },
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
                selectedTab = dashboardTab,
                products = sellerState.products,
                orders = sellerState.orders,
                totalRevenue = sellerState.totalRevenue,
                totalOrders = sellerState.totalOrders,
                onAddProduct = { navController.navigate(Screen.SellerProductForm.createRoute()) },
                onEditProduct = { navController.navigate(Screen.SellerProductForm.createRoute(it)) },
                onDeleteProduct = { sellerViewModel.deleteProduct(it, authState.user?.id ?: "") },
                onUpdateOrderStatus = { orderId, status ->
                    sellerViewModel.updateOrderStatus(orderId, status, authState.user?.id ?: "")
                },
                hasStoreLocation = authState.user?.storeLatitude != null,
                onSaveStoreLocation = { address, lat, lng ->
                    authViewModel.updateStoreLocation(address, lat, lng)
                },
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
                selectedTab = dashboardTab,
                availableOrders = deliveryState.availableOrders,
                activeOrders = deliveryState.activeOrders,
                completedOrders = deliveryState.completedOrders,
                totalEarnings = deliveryState.totalEarnings,
                totalDeliveries = deliveryState.totalDeliveries,
                onMarkDelivered = { deliveryViewModel.markDelivered(it, authState.user?.id ?: "") },
                onClaimOrder = { orderId ->
                    deliveryViewModel.claimOrder(orderId, authState.user?.id ?: "")
                },
                claimError = deliveryState.claimError,
                onDismissClaimError = { deliveryViewModel.clearClaimError() },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                sharingLocation = deliveryState.sharingLocation,
                sharingError = deliveryState.sharingError,
                onSharingChange = { deliveryViewModel.setSharingLocation(it) }
            )

            // Runs only while the dashboard is on screen and the rider has the
            // switch on; leaving the screen removes the location listener.
            RiderLocationPublisher(
                enabled = deliveryState.sharingLocation,
                onFix = { location ->
                    authState.user?.let { user ->
                        deliveryViewModel.publishLocation(
                            deliveryId = user.id,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }
                }
            )

            LaunchedEffect(authState.user) {
                authState.user?.let { deliveryViewModel.loadDeliveryData(it.id) }
            }

            // Polled only while the Available tab is actually showing, same
            // pattern as the customer's rider-position poll: other riders can
            // claim a pool order at any time, so a one-time load would go stale.
            LaunchedEffect(dashboardTab) {
                if (dashboardTab != 0) return@LaunchedEffect
                while (true) {
                    deliveryViewModel.loadAvailableOrders()
                    delay(15_000L)
                }
            }
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboard(
                selectedTab = dashboardTab,
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

        composable(Screen.Splash.route) {
            SplashScreen()
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onSendReset = { authViewModel.sendPasswordReset(it) },
                onBackToLogin = {
                    authViewModel.resetPasswordResetState()
                    navController.popBackStack()
                },
                isLoading = authState.isLoading,
                error = authState.error,
                resetSent = authState.passwordResetSent,
                onClearError = { authViewModel.clearError() }
            )
        }

        composable(
            Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            // Look in the catalogue first, then favourites, so the screen still resolves
            // when it is opened from the favourites grid.
            val product = homeState.products.firstOrNull { it.id == productId }
                ?: favState.favoriteProducts.firstOrNull { it.id == productId }

            ProductDetailScreen(
                product = product,
                isFavorite = favState.favorites.any { it.productId == productId },
                onAddToCart = { p, qty ->
                    repeat(qty) { cartViewModel.addToCart(p) }
                    navController.popBackStack()
                },
                onToggleFavorite = { p ->
                    authState.user?.let { favoriteViewModel.toggleFavorite(it.id, p.id) }
                },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(authState.user) {
                authState.user?.let { favoriteViewModel.loadFavorites(it.id) }
            }
        }

        composable(Screen.CustomerAddresses.route) {
            AddressesScreen(
                addresses = addressState.addresses,
                isLoading = addressState.isLoading,
                onSaveAddress = { label, full, isDefault, existingId ->
                    addressViewModel.saveAddress(label, full, isDefault, existingId)
                },
                onDeleteAddress = { addressViewModel.deleteAddress(it) },
                onSetDefault = { addressViewModel.setDefault(it) },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(Unit) { addressViewModel.loadAddresses() }
        }

        composable(Screen.CustomerCheckout.route) {
            CheckoutScreen(
                cartItems = cartState.cartItems,
                addresses = addressState.addresses,
                subtotal = cartState.subtotal,
                deliveryFee = cartState.deliveryFee,
                total = cartState.total,
                savings = cartState.savings,
                isLoading = cartState.isLoading,
                error = cartState.error,
                onManageAddresses = { navController.navigate(Screen.CustomerAddresses.route) },
                onPlaceOrder = { address, lat, lng -> cartViewModel.placeOrder(address, lat, lng) },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(Unit) { addressViewModel.loadAddresses() }

            if (cartState.orderPlaced) {
                AlertDialog(
                    onDismissRequest = { cartViewModel.resetOrderPlaced() },
                    title = { Text("Order Placed!") },
                    text = { Text("Your order has been placed successfully. You can track it in Orders.") },
                    confirmButton = {
                        TextButton(onClick = {
                            cartViewModel.resetOrderPlaced()
                            navController.navigate(Screen.CustomerOrders.route) {
                                popUpTo(Screen.CustomerHome.route)
                            }
                        }) {
                            Text("View Orders")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            cartViewModel.resetOrderPlaced()
                            navController.navigate(Screen.CustomerHome.route) {
                                popUpTo(Screen.CustomerHome.route) { inclusive = true }
                            }
                        }) {
                            Text("Continue Shopping")
                        }
                    }
                )
            }
        }

        composable(
            Screen.SellerProductForm.route,
            arguments = listOf(navArgument("productId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            val existing = sellerState.products.firstOrNull { it.id == productId }

            AddEditProductScreen(
                product = existing,
                categories = homeState.categories,
                sellerId = authState.user?.id ?: "",
                isLoading = sellerState.isLoading,
                error = sellerState.error,
                onSave = { sellerViewModel.saveProduct(it) },
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(sellerState.productSaved) {
                if (sellerState.productSaved) {
                    sellerViewModel.resetProductSaved()
                    authState.user?.let { sellerViewModel.loadSellerData(it.id) }
                    navController.popBackStack()
                }
            }
        }
    }

    if (showBar) {
        StoreBottomBar(
            items = BottomNav.itemsFor(role),
            selectedKey = if (role == UserRole.CUSTOMER) {
                BottomNav.selectedKeyFor(currentRoute)
            } else {
                dashboardTab.toString()
            },
            onSelect = { key ->
                if (role == UserRole.CUSTOMER) {
                    if (key != currentRoute) {
                        navController.navigate(key) {
                            // Tabs re-enter rather than stack, so tapping around
                            // the bar does not build up a back stack of them.
                            popUpTo(Screen.CustomerHome.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                } else {
                    dashboardTab = key.toIntOrNull() ?: 0
                }
            },
            centre = if (role == UserRole.CUSTOMER) {
                BottomBarCentre(
                    count = cartState.itemCount,
                    selected = currentRoute == Screen.CustomerCart.route,
                    onClick = {
                        if (currentRoute != Screen.CustomerCart.route) {
                            navController.navigate(Screen.CustomerCart.route)
                        }
                    }
                )
            } else null,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    }
}
