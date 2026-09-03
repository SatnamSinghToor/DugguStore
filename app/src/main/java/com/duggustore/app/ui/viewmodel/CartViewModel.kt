package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.CartItem
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.repository.CartRepository
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

    fun applyCoupon(code: String) {
        val discount = when (code.uppercase()) {
            "DUGGU10" -> 10.0
            "DUGGU50" -> 50.0
            "SAVE20" -> 20.0
            else -> 0.0
        }
        _state.value = _state.value.copy(
            couponCode = code,
            couponApplied = discount > 0,
            couponDiscount = discount
        )
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
                    couponDiscount = 0.0
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
