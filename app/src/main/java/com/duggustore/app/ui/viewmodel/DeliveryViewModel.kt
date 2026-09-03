package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.repository.OrderRepository
import com.duggustore.app.data.repository.TrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DeliveryState(
    val isLoading: Boolean = false,
    val activeOrders: List<Order> = emptyList(),
    val completedOrders: List<Order> = emptyList(),
    val totalEarnings: Double = 0.0,
    val totalDeliveries: Int = 0,
    val error: String? = null,
    /** Whether the rider is publishing their position for their active orders. */
    val sharingLocation: Boolean = false,
    val lastSharedAt: String? = null,
    val sharingError: String? = null
)

class DeliveryViewModel : ViewModel() {
    private val orderRepo = OrderRepository()
    private val trackingRepo = TrackingRepository()

    private val _state = MutableStateFlow(DeliveryState())
    val state: StateFlow<DeliveryState> = _state

    fun loadDeliveryData(deliveryId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = orderRepo.getDeliveryOrders(deliveryId)
            result.onSuccess { orders ->
                val active = orders.filter { it.status in listOf("out_for_delivery", "confirmed", "preparing") }
                val completed = orders.filter { it.status == "delivered" }
                _state.value = _state.value.copy(
                    activeOrders = active,
                    completedOrders = completed,
                    totalDeliveries = completed.size,
                    totalEarnings = completed.sumOf { it.deliveryFee },
                    isLoading = false
                )
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun updateDeliveryStatus(orderId: String, status: String, deliveryId: String) {
        viewModelScope.launch {
            val result = orderRepo.updateOrderStatus(orderId, status)
            result.onSuccess { loadDeliveryData(deliveryId) }
            result.onFailure {
                _state.value = _state.value.copy(error = it.message)
            }
        }
    }

    fun markDelivered(orderId: String, deliveryId: String) {
        updateDeliveryStatus(orderId, "delivered", deliveryId)
    }

    fun setSharingLocation(on: Boolean) {
        _state.value = _state.value.copy(
            sharingLocation = on,
            sharingError = if (on) null else _state.value.sharingError
        )
    }

    /**
     * Writes one fix for every order the rider is currently carrying. A rider on
     * a run holds several orders at once and each is tracked separately by the
     * customer watching it, so one position fans out to all of them.
     */
    fun publishLocation(deliveryId: String, latitude: Double, longitude: Double) {
        val orders = _state.value.activeOrders
        if (orders.isEmpty()) return

        viewModelScope.launch {
            var failure: String? = null
            orders.forEach { order ->
                trackingRepo.publishLocation(
                    orderId = order.id,
                    deliveryId = deliveryId,
                    latitude = latitude,
                    longitude = longitude,
                    status = order.status
                ).onFailure { failure = it.message }
            }
            _state.value = _state.value.copy(
                lastSharedAt = System.currentTimeMillis().toString(),
                sharingError = failure
            )
        }
    }
}
