package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.CartItem
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.repository.CartRepository
import com.duggustore.app.data.repository.OfferRepository
import com.duggustore.app.data.repository.OrderRepository
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
    val isCartOpen: Boolean = false
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.product?.effectivePrice()?.times(it.quantity) ?: 0.0 }

    val deliveryFee: Double
        get() = if (subtotal > 0) maxOf(0.0, 29.0) else 0.0

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
    private val offerRepo = OfferRepository()

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state

    fun setCustomer(customerId: String) {
        if (_state.value.customerId != customerId) {
            _state.value = _state.value.copy(customerId = customerId)
            loadCart()
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
     * Checks the typed code against the store's real coupons (the same rows
     * the home carousel shows) instead of a fixed local list, so a code that
     * was never actually issued — or one that has since been turned off —
     * is rejected rather than silently accepted.
     */
    fun applyCoupon(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val coupons = offerRepo.getOffers().getOrElse {
                _state.value = _state.value.copy(
                    couponApplied = false,
                    couponDiscount = 0.0,
                    couponError = "Couldn't check that code — try again"
                )
                return@launch
            }
            val match = coupons.firstOrNull { it.code.equals(trimmed, ignoreCase = true) }
            val subtotal = _state.value.subtotal

            _state.value = when {
                match == null || !match.isActive -> _state.value.copy(
                    couponCode = trimmed,
                    couponApplied = false,
                    couponDiscount = 0.0,
                    couponError = "That code isn't valid"
                )
                subtotal < match.minOrderValue -> _state.value.copy(
                    couponCode = trimmed,
                    couponApplied = false,
                    couponDiscount = 0.0,
                    couponError = "Minimum order of ₹${match.minOrderValue} needed for this code"
                )
                else -> _state.value.copy(
                    couponCode = trimmed,
                    couponApplied = true,
                    couponDiscount = minOf(subtotal * match.discountPercent / 100.0, match.maxDiscount.toDouble()),
                    couponError = null
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
    fun placeOrder(deliveryAddress: String, latitude: Double? = null, longitude: Double? = null) {
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

            val order = com.duggustore.app.data.model.Order(
                customerId = customerId,
                sellerId = sellerId,
                status = "pending",
                totalAmount = state.total,
                deliveryFee = state.deliveryFee,
                deliveryAddress = deliveryAddress,
                deliveryLatitude = latitude,
                deliveryLongitude = longitude
            )

            val orderItems = state.cartItems.map { item ->
                com.duggustore.app.data.model.OrderItem(
                    productId = item.productId,
                    quantity = item.quantity,
                    priceAtPurchase = item.product?.effectivePrice() ?: 0.0
                )
            }

            val result = orderRepo.createOrder(order, orderItems)
            result.onSuccess {
                cartRepo.clearCart(customerId)
                _state.value = _state.value.copy(
                    isLoading = false,
                    orderPlaced = true,
                    cartItems = emptyList(),
                    isCartOpen = false,
                    couponApplied = false,
                    couponDiscount = 0.0,
                    couponError = null
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
}
