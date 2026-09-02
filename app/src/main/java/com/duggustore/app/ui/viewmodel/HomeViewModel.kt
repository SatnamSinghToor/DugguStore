package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.repository.CategoryRepository
import com.duggustore.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val categoryRepo = CategoryRepository()
    private val productRepo = ProductRepository()

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val categories = categoryRepo.getAllCategories()
            val products = productRepo.getAllProducts()

            categories.onSuccess { cats ->
                _state.value = _state.value.copy(categories = cats)
            }
            products.onSuccess { prods ->
                _state.value = _state.value.copy(
                    products = prods.filter { it.isActive },
                    filteredProducts = prods.filter { it.isActive }
                )
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun selectCategory(categoryId: String?) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
        filterProducts()
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        filterProducts()
    }

    private fun filterProducts() {
        val state = _state.value
        var filtered = state.products

        if (state.selectedCategoryId != null) {
            filtered = filtered.filter { it.categoryId == state.selectedCategoryId }
        }
        if (state.searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.description.contains(state.searchQuery, ignoreCase = true)
            }
        }
        _state.value = _state.value.copy(filteredProducts = filtered)
    }
}
