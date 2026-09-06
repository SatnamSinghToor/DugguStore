package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Coupon
import com.duggustore.app.data.model.UserProfile
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.repository.AuthRepository
import com.duggustore.app.data.repository.CategoryRepository
import com.duggustore.app.data.repository.OfferRepository
import com.duggustore.app.data.repository.OrderRepository
import com.duggustore.app.data.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminState(
    val isLoading: Boolean = false,
    val users: List<UserProfile> = emptyList(),
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val coupons: List<Coupon> = emptyList(),
    val totalUsers: Int = 0,
    val totalOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalDeliveries: Int = 0,
    val isSavingCatalog: Boolean = false,
    val catalogError: String? = null,
    val error: String? = null
)

class AdminViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val orderRepo = OrderRepository()
    private val productRepo = ProductRepository()
    private val categoryRepo = CategoryRepository()
    private val offerRepo = OfferRepository()

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val usersDeferred = async { authRepo.getAllUsers() }
                val ordersDeferred = async { orderRepo.getAllOrders() }
                val productsDeferred = async { productRepo.getAllProducts() }
                val categoriesDeferred = async { categoryRepo.getAllCategories() }
                val couponsDeferred = async { offerRepo.getAllCoupons() }

                val users = usersDeferred.await().getOrThrow()
                val orders = ordersDeferred.await().getOrThrow()
                val products = productsDeferred.await().getOrThrow()
                val categories = categoriesDeferred.await().getOrThrow()
                val coupons = couponsDeferred.await().getOrThrow()

                _state.value = _state.value.copy(
                    isLoading = false,
                    users = users,
                    totalUsers = users.size,
                    orders = orders,
                    totalOrders = orders.size,
                    totalRevenue = orders.filter { it.status == "delivered" }.sumOf { it.totalAmount },
                    totalDeliveries = orders.count { it.status == "delivered" },
                    products = products,
                    categories = categories,
                    coupons = coupons
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load dashboard")
            }
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

    fun toggleProductActive(product: Product) {
        viewModelScope.launch {
            val result = productRepo.updateProduct(product.copy(isActive = !product.isActive))
            result.onSuccess {
                _state.value = _state.value.copy(
                    products = _state.value.products.map { if (it.id == product.id) it.copy(isActive = !product.isActive) else it }
                )
            }
            result.onFailure { _state.value = _state.value.copy(catalogError = it.message) }
        }
    }

    fun saveCategory(category: Category, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingCatalog = true, catalogError = null)
            val result = if (category.id.isBlank()) {
                categoryRepo.createCategory(category)
            } else {
                categoryRepo.updateCategory(category)
            }
            result.onSuccess {
                categoryRepo.getAllCategories().onSuccess { categories ->
                    _state.value = _state.value.copy(categories = categories)
                }
                onDone()
            }
            result.onFailure { _state.value = _state.value.copy(catalogError = it.message) }
            _state.value = _state.value.copy(isSavingCatalog = false)
        }
    }

    fun toggleCategoryActive(category: Category) {
        viewModelScope.launch {
            val result = categoryRepo.updateCategory(category.copy(isActive = !category.isActive))
            result.onSuccess {
                _state.value = _state.value.copy(
                    categories = _state.value.categories.map { if (it.id == category.id) it.copy(isActive = !category.isActive) else it }
                )
            }
            result.onFailure { _state.value = _state.value.copy(catalogError = it.message) }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            val result = categoryRepo.deleteCategory(id)
            result.onSuccess {
                _state.value = _state.value.copy(categories = _state.value.categories.filter { it.id != id })
            }
            result.onFailure { _state.value = _state.value.copy(catalogError = it.message) }
        }
    }

    fun saveCoupon(coupon: Coupon, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingCatalog = true, catalogError = null)
            val result = if (coupon.id.isBlank()) {
                offerRepo.createCoupon(coupon)
            } else {
                offerRepo.updateCoupon(coupon)
            }
            result.onSuccess {
                offerRepo.getAllCoupons().onSuccess { coupons ->
                    _state.value = _state.value.copy(coupons = coupons)
                }
                onDone()
            }
            result.onFailure { _state.value = _state.value.copy(catalogError = it.message) }
            _state.value = _state.value.copy(isSavingCatalog = false)
        }
    }

    fun toggleCouponActive(coupon: Coupon) {
        viewModelScope.launch {
            val result = offerRepo.updateCoupon(coupon.copy(isActive = !coupon.isActive))
            result.onSuccess {
                _state.value = _state.value.copy(
                    coupons = _state.value.coupons.map { if (it.id == coupon.id) it.copy(isActive = !coupon.isActive) else it }
                )
            }
            result.onFailure { _state.value = _state.value.copy(catalogError = it.message) }
        }
    }

    fun deleteCoupon(id: String) {
        viewModelScope.launch {
            val result = offerRepo.deleteCoupon(id)
            result.onSuccess {
                _state.value = _state.value.copy(coupons = _state.value.coupons.filter { it.id != id })
            }
            result.onFailure { _state.value = _state.value.copy(catalogError = it.message) }
        }
    }

    fun clearCatalogError() {
        _state.value = _state.value.copy(catalogError = null)
    }
}
