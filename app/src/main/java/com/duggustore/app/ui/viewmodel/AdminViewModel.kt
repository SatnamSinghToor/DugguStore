package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.repository.AuthRepository
import com.duggustore.app.data.repository.OrderRepository
import com.duggustore.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminState(
    val isLoading: Boolean = false,
    val users: List<UserProfile> = emptyList(),
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val totalUsers: Int = 0,
    val totalOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalDeliveries: Int = 0,
    val error: String? = null
)

class AdminViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val orderRepo = OrderRepository()
    private val productRepo = ProductRepository()

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val usersResult = authRepo.getAllUsers()
            val ordersResult = orderRepo.getAllOrders()
            val productsResult = productRepo.getAllProducts()

            usersResult.onSuccess { users ->
                _state.value = _state.value.copy(users = users, totalUsers = users.size)
            }
            ordersResult.onSuccess { orders ->
                _state.value = _state.value.copy(
                    orders = orders,
                    totalOrders = orders.size,
                    totalRevenue = orders.filter { it.status == "delivered" }.sumOf { it.totalAmount },
                    totalDeliveries = orders.count { it.status == "delivered" }
                )
            }
            productsResult.onSuccess { products ->
                _state.value = _state.value.copy(products = products)
            }

            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun updateUserRole(userId: String, role: String) {
        viewModelScope.launch {
            val result = authRepo.updateUserRole(userId, role)
            result.onSuccess { loadDashboard() }
            result.onFailure {
                _state.value = _state.value.copy(error = it.message)
            }
        }
    }
}
