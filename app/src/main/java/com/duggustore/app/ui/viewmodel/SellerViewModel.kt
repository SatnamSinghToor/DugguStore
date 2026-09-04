package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.repository.ProductRepository
import com.duggustore.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SellerState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val orders: List<Order> = emptyList(),
    val totalRevenue: Double = 0.0,
    val totalOrders: Int = 0,
    val error: String? = null,
    val productSaved: Boolean = false
)

class SellerViewModel : ViewModel() {
    private val productRepo = ProductRepository()
    private val orderRepo = OrderRepository()

    private val _state = MutableStateFlow(SellerState())
    val state: StateFlow<SellerState> = _state

    fun loadSellerData(sellerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val products = productRepo.getProductsBySeller(sellerId)
            val orders = orderRepo.getSellerOrders(sellerId)

            products.onSuccess { prods ->
                _state.value = _state.value.copy(products = prods)
            }
            orders.onSuccess { ords ->
                _state.value = _state.value.copy(
                    orders = ords,
                    totalOrders = ords.size,
                    totalRevenue = ords.filter { it.status == "delivered" }.sumOf { it.totalAmount }
                )
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            val result = if (product.id.isEmpty()) {
                productRepo.createProduct(product)
            } else {
                productRepo.updateProduct(product)
            }
            result.onSuccess {
                _state.value = _state.value.copy(productSaved = true)
            }
            result.onFailure {
                _state.value = _state.value.copy(error = it.message)
            }
        }
    }

    fun deleteProduct(productId: String, sellerId: String) {
        viewModelScope.launch {
            val result = productRepo.deleteProduct(productId)
            result.onSuccess { loadSellerData(sellerId) }
            result.onFailure {
                _state.value = _state.value.copy(error = it.message)
            }
        }
    }

    /**
     * The seller's Accept/Reject/Start-preparing buttons all end up here.
     *
     * SellerDashboard is fed from this view model's own `orders`, not from
     * OrderViewModel's — the dashboard's Accept button used to call through
     * OrderViewModel.updateOrderStatus, which wrote the status correctly but
     * then refreshed only OrderViewModel's own state. The seller's list never
     * reloaded, so the button looked like it did nothing even though the order
     * had, in fact, been accepted. Reloading here, on the view model that
     * actually owns the list the dashboard renders, is what makes the card
     * disappear from "pending" and the next action appear.
     */
    fun updateOrderStatus(orderId: String, status: OrderStatus, sellerId: String) {
        viewModelScope.launch {
            val result = orderRepo.updateOrderStatus(orderId, status.value)
            result.onSuccess { loadSellerData(sellerId) }
            result.onFailure {
                _state.value = _state.value.copy(error = it.message)
            }
        }
    }

    fun resetProductSaved() {
        _state.value = _state.value.copy(productSaved = false)
    }
}
