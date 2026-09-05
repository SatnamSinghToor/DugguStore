package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.Category
import com.duggustore.app.data.model.Coupon
import com.duggustore.app.data.model.Product
import com.duggustore.app.data.repository.CategoryRepository
import com.duggustore.app.data.repository.OfferRepository
import com.duggustore.app.data.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    /** The store's active coupons, shown on the home carousel. */
    val offers: List<Coupon> = emptyList(),
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val categoryRepo = CategoryRepository()
    private val productRepo = ProductRepository()
    private val offerRepo = OfferRepository()

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Fired together rather than awaited one after another — three
            // sequential round trips left the screen sitting on its empty
            // state for roughly their combined latency, and then had the
            // carousel, categories and the whole product grid all pop in
            // at once, right below the search bar. Started concurrently,
            // the wait is only as long as the slowest of the three.
            val categoriesDeferred = async { categoryRepo.getAllCategories() }
            val productsDeferred = async { productRepo.getAllProducts() }
            val offersDeferred = async { offerRepo.getOffers() }

            val categories = categoriesDeferred.await().getOrNull()
            val products = productsDeferred.await().getOrNull()?.filter { it.isActive }
            // A store with no coupons is a normal state, not an error worth
            // showing; the carousel simply does not render.
            val offers = offersDeferred.await().getOrNull()

            _state.value = _state.value.copy(
                categories = categories ?: _state.value.categories,
                products = products ?: _state.value.products,
                filteredProducts = products ?: _state.value.filteredProducts,
                offers = offers ?: _state.value.offers,
                isLoading = false
            )
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

        // A search looks across the whole catalogue, not just the category
        // tile that happened to be selected before typing — the category
        // row is hidden while searching, so narrowing by it here silently
        // hid matches from every other category instead of showing them.
        if (state.searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.description.contains(state.searchQuery, ignoreCase = true)
            }
        } else if (state.selectedCategoryId != null) {
            filtered = filtered.filter { it.categoryId == state.selectedCategoryId }
        }
        _state.value = _state.value.copy(filteredProducts = filtered)
    }
}
