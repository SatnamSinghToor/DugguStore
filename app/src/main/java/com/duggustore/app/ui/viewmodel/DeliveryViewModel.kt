package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DeliveryState(
    val isLoading: Boolean = false,
    val activeOrders: List<Order> = emptyList(),
    val completedOrders: List<Order> = emptyList(),
    val totalEarnings: Double = 0.0,
    val totalDeliveries: Int = 0,
    val error: String? = null
)

class DeliveryViewModel : ViewModel() {
    private val orderRepo = OrderRepository()

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
}
