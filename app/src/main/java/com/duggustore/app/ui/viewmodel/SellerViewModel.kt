package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun resetProductSaved() {
        _state.value = _state.value.copy(productSaved = false)
    }
}
