package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.CartItem
import com.duggustore.app.data.model.OrderItem
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.remote.SessionManager
import com.duggustore.app.data.remote.SupabaseService
import com.duggustore.app.data.remote.SupabaseException
import com.duggustore.app.data.repository.CartRepository
import com.duggustore.app.data.repository.OrderRepository
import com.duggustore.app.data.repository.ProductRepository
import com.duggustore.app.data.repository.WalletRepository
import com.duggustore.app.data.repository.walletBalance
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CartState(
    val isLoading: Boolean = false,
    val cartItems: List<CartItem> = emptyList(),
    val customerId: String = "",
    val error: String? = null,
    val orderPlaced: Boolean = false,
    val couponCode: String = "",
    val couponApplied: Boolean = false,
    val couponDiscount: Double = 0.0,
    val couponError: String? = null,
    val isCartOpen: Boolean = false,
    val walletBalance: Int = 0
) {
    companion object {
        /** Below this, an order isn't worth a seller packing and a rider carrying. */
        const val MIN_ORDER_VALUE = 99.0

        /** At or above this subtotal, delivery is free instead of the flat fee. */
        const val FREE_DELIVERY_THRESHOLD = 299.0
        const val BASE_DELIVERY_FEE = 29.0
    }

    val subtotal: Double
        get() = cartItems.sumOf { it.product?.effectivePrice()?.times(it.quantity) ?: 0.0 }

    val deliveryFee: Double
        get() = when {
            subtotal <= 0 -> 0.0
            subtotal >= FREE_DELIVERY_THRESHOLD -> 0.0
            else -> BASE_DELIVERY_FEE
        }

    val isBelowMinimumOrder: Boolean
        get() = subtotal > 0 && subtotal < MIN_ORDER_VALUE

    val total: Double
        get() = subtotal + deliveryFee - couponDiscount

    val savings: Double
        get() = cartItems.sumOf {
            it.product?.let { p ->
                if (p.hasDiscount()) (p.price - p.effectivePrice()) * it.quantity else 0.0
            } ?: 0.0
        }

    val itemCount: Int
        get() = cartItems.sumOf { it.quantity }
}

class CartViewModel : ViewModel() {
    private val cartRepo = CartRepository()
    private val orderRepo = OrderRepository()
    private val walletRepo = WalletRepository()
    private val productRepo = ProductRepository()

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state

    fun setCustomer(customerId: String) {
        if (_state.value.customerId != customerId) {
            _state.value = _state.value.copy(customerId = customerId)
            loadCart()
            loadWalletBalance(customerId)
        }
    }

    private fun loadWalletBalance(customerId: String) {
        viewModelScope.launch {
            walletRepo.getTransactions(customerId).onSuccess { txns ->
                _state.value = _state.value.copy(walletBalance = txns.walletBalance())
            }
        }
    }

    fun loadCart() {
        viewModelScope.launch {
            val customerId = _state.value.customerId
            if (customerId.isEmpty()) return@launch
            _state.value = _state.value.copy(isLoading = true)
            val result = cartRepo.getCartItems(customerId)
            result.onSuccess { items ->
                _state.value = _state.value.copy(cartItems = items, isLoading = false)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            val customerId = _state.value.customerId
            if (customerId.isEmpty()) return@launch
            val result = cartRepo.addToCart(customerId, product.id)
            result.onSuccess { loadCart() }
        }
    }

    fun updateQuantity(itemId: String, quantity: Int) {
        viewModelScope.launch {
            val result = cartRepo.updateQuantity(itemId, quantity)
            result.onSuccess { loadCart() }
        }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch {
            val result = cartRepo.removeFromCart(itemId)
            result.onSuccess { loadCart() }
        }
    }

    fun toggleCart() {
        _state.value = _state.value.copy(isCartOpen = !_state.value.isCartOpen)
    }

    /**
     * Validates the coupon code via the `use_coupon` Postgres RPC which atomically:
     *  1. Checks the code exists and is active
     *  2. Checks the subtotal meets the minimum order value
     *  3. Checks this customer hasn't already used the code
     *  4. Inserts a row in `coupon_usages` to lock the redemption
     *
     * The RPC raises typed exceptions (`INVALID_COUPON`, `MIN_ORDER:<value>`,
     * `ALREADY_USED`) that are parsed here into friendly error strings.
     * The discount is calculated client-side from the values the RPC returns.
     *
     * Note: the RPC records the usage immediately on Apply, before the order is
     * placed. If the user closes the app without placing the order the usage row
     * stays — this is intentional and mirrors how most real coupon systems work.
     */
    fun applyCoupon(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return

        val customerId = _state.value.customerId
        if (customerId.isEmpty()) {
            _state.value = _state.value.copy(couponError = "Sign in to apply a coupon")
            return
        }

        viewModelScope.launch {
            val subtotal = _state.value.subtotal

            try {
                val body = kotlinx.serialization.json.buildJsonObject {
                    put("p_coupon_code", trimmed)
                    put("p_user_id", customerId)
                    put("p_subtotal", subtotal)
                }.toString()

                val raw = SupabaseService.rpc("use_coupon", body, SessionManager.getAccessToken())

                // RPC returns [{coupon_id, discount_percent, max_discount}]
                val row = Json.parseToJsonElement(raw).jsonArray.firstOrNull()?.jsonObject
                    ?: throw Exception("Empty response from coupon validation")

                val discountPercent = row["discount_percent"]?.jsonPrimitive?.int ?: 0
                val maxDiscount = row["max_discount"]?.jsonPrimitive?.int ?: 0

                val discount = minOf(subtotal * discountPercent / 100.0, maxDiscount.toDouble())

                _state.value = _state.value.copy(
                    couponCode = trimmed,
                    couponApplied = true,
                    couponDiscount = discount,
                    couponError = null
                )
            } catch (e: SupabaseException) {
                val msg = e.message ?: ""
                val friendlyError = when {
                    msg.contains("INVALID_COUPON", ignoreCase = true) ->
                        "That code isn't valid"
                    msg.contains("MIN_ORDER:", ignoreCase = true) -> {
                        val minVal = msg.substringAfter("MIN_ORDER:").trim().split(" ").firstOrNull() ?: ""
                        "Minimum order of ₹$minVal needed for this code"
                    }
                    msg.contains("ALREADY_USED", ignoreCase = true) ->
                        "You've already used this code"
                    else -> "Couldn't apply that code — try again"
                }
                _state.value = _state.value.copy(
                    couponCode = trimmed,
                    couponApplied = false,
                    couponDiscount = 0.0,
                    couponError = friendlyError
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    couponCode = trimmed,
                    couponApplied = false,
                    couponDiscount = 0.0,
                    couponError = "Couldn't check that code — try again"
                )
            }
        }
    }

    /**
     * [deliveryAddress] comes from the checkout screen. The seller is taken from the
     * products in the cart rather than passed in: the caller used to hand over the
     * signed-in customer's own id, which stored every order with
     * seller_id = customer_id and left it invisible to the seller who has to fulfil it.
     */
    fun placeOrder(
        deliveryAddress: String,
        latitude: Double? = null,
        longitude: Double? = null,
        walletAmount: Int = 0
    ) {
        viewModelScope.launch {
            val state = _state.value
            val customerId = state.customerId
            if (customerId.isEmpty() || state.cartItems.isEmpty()) return@launch

            val sellerId = state.cartItems.firstNotNullOfOrNull { it.product?.sellerId }
            if (sellerId.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Could not work out which seller these items belong to."
                )
                return@launch
            }
            if (deliveryAddress.isBlank()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Choose a delivery address first."
                )
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, error = null)

            // Clamped again here rather than trusted from the caller — the
            // balance can move between the screen composing this call and
            // the request actually landing.
            val walletUsed = walletAmount.coerceIn(0, minOf(state.walletBalance, state.total.toInt()))

            val order = com.duggustore.app.data.model.Order(
                customerId = customerId,
                sellerId = sellerId,
                status = "pending",
                totalAmount = state.total - walletUsed,
                deliveryFee = state.deliveryFee,
                deliveryAddress = deliveryAddress,
                deliveryLatitude = latitude,
                deliveryLongitude = longitude,
                paymentMethod = "cod",
                walletUsed = walletUsed.toDouble()
            )

            val orderItems = state.cartItems.map { item ->
                com.duggustore.app.data.model.OrderItem(
                    productId = item.productId,
                    quantity = item.quantity,
                    priceAtPurchase = item.product?.effectivePrice() ?: 0.0
                )
            }

            val result = orderRepo.createOrder(order, orderItems)
            result.onSuccess { orderId ->
                cartRepo.clearCart(customerId)
                if (walletUsed > 0) {
                    walletRepo.debit(customerId, walletUsed, "Used on order #${orderId.takeLast(8).uppercase()}")
                }
                // Atomically reduce stock for each purchased item via the
                // decrement_stock RPC. Failure is intentionally swallowed:
                // the order is already placed and the seller can correct
                // stock manually — no need to surface a confusing error.
                val stockItems = orderItems.map { it.productId to it.quantity }
                productRepo.decrementStock(stockItems)
                _state.value = _state.value.copy(
                    isLoading = false,
                    orderPlaced = true,
                    cartItems = emptyList(),
                    isCartOpen = false,
                    couponApplied = false,
                    couponDiscount = 0.0,
                    couponError = null,
                    walletBalance = state.walletBalance - walletUsed
                )
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun resetOrderPlaced() {
        _state.value = _state.value.copy(orderPlaced = false)
    }

    /** "Buy again" from a past order — re-adds each line at its original quantity. */
    fun reorderItems(items: List<OrderItem>) {
        val customerId = _state.value.customerId
        if (customerId.isEmpty() || items.isEmpty()) return
        viewModelScope.launch {
            items.forEach { item ->
                cartRepo.addToCart(customerId, item.productId, item.quantity)
            }
            loadCart()
        }
    }
}
