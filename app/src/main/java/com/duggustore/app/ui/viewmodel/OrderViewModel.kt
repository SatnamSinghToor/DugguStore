package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.data.repository.OrderRepository
import com.duggustore.app.data.repository.TrackingRepository
import com.duggustore.app.data.model.DeliveryTracking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OrderState(
    val isLoading: Boolean = false,
    /** The rider's last reported position for the order being watched. */
    val tracking: DeliveryTracking? = null,
    val customerOrders: List<Order> = emptyList(),
    val sellerOrders: List<Order> = emptyList(),
    val deliveryOrders: List<Order> = emptyList(),
    val allOrders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val error: String? = null
)

class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val trackingRepo = TrackingRepository()

    private val _state = MutableStateFlow(OrderState())
    val state: StateFlow<OrderState> = _state

    fun loadCustomerOrders(customerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.getCustomerOrders(customerId)
            result.onSuccess { orders ->
                _state.value = _state.value.copy(customerOrders = orders, isLoading = false)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun loadSellerOrders(sellerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.getSellerOrders(sellerId)
            result.onSuccess { orders ->
                _state.value = _state.value.copy(sellerOrders = orders, isLoading = false)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun loadDeliveryOrders(deliveryId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.getDeliveryOrders(deliveryId)
            result.onSuccess { orders ->
                _state.value = _state.value.copy(deliveryOrders = orders, isLoading = false)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun loadAllOrders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.getAllOrders()
            result.onSuccess { orders ->
                _state.value = _state.value.copy(allOrders = orders, isLoading = false)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            val result = repository.updateOrderStatus(orderId, status.value)
            result.onSuccess {
                refreshOrders()
            }
            result.onFailure {
                _state.value = _state.value.copy(error = it.message)
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            val result = repository.cancelOrder(orderId)
            result.onSuccess { refreshOrders() }
            result.onFailure {
                _state.value = _state.value.copy(error = it.message)
            }
        }
    }

    private fun refreshOrders() {
        val state = _state.value
        // Re-trigger loads based on what was loaded
        if (state.customerOrders.isNotEmpty()) {
            loadCustomerOrders(state.customerOrders.first().customerId)
        }
        if (state.sellerOrders.isNotEmpty()) {
            loadSellerOrders(state.sellerOrders.first().sellerId)
        }
        if (state.allOrders.isNotEmpty()) {
            loadAllOrders()
        }
    }

    /**
     * Reads the rider's last position for an order.
     *
     * The screen calls this on a timer rather than the app holding a socket
     * open: the row changes every few seconds at most, and a poll while the
     * tracking screen is on top costs less than a live subscription that has to
     * be torn down and rebuilt on every navigation.
     */
    fun loadTracking(orderId: String) {
        viewModelScope.launch {
            trackingRepo.getTracking(orderId)
                .onSuccess { _state.value = _state.value.copy(tracking = it) }
            // A failure here is not worth an error banner: the tracking card
            // reads "not sharing" and the rest of the screen is unaffected.
        }
    }

    fun clearTracking() {
        _state.value = _state.value.copy(tracking = null)
    }
}
