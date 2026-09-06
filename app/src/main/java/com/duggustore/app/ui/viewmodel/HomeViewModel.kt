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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    /** The store's active coupons, shown on the home carousel. */
    val offers: List<Coupon> = emptyList(),
    /**
     * The full active catalogue — kept only for Categories' scroll-spy view
     * (which genuinely needs every product across every category at once,
     * a different job from anything paginated) and as a fallback for
     * resolving a product by id from somewhere that isn't [feedProducts].
     * Home's own grid never renders this directly.
     */
    val products: List<Product> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    /**
     * Whichever view is active — the default browse feed, a selected
     * category, or a search — pages through this same list, loaded and
     * filtered server-side rather than sliced out of [products] in memory.
     */
    val feedProducts: List<Product> = emptyList(),
    val hasMoreFeed: Boolean = true,
    val isLoadingMoreFeed: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val categoryRepo = CategoryRepository()
    private val productRepo = ProductRepository()
    private val offerRepo = OfferRepository()

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    /** Tracked here rather than in HomeState — advancing it isn't itself
     *  something the UI needs to recompose over. */
    private var feedPage = 0

    /** Debounced separately from HomeState.searchQuery itself, so the text
     *  field stays responsive to every keystroke while the network re-query
     *  it drives waits until typing actually pauses. */
    private val searchQueryChanges = MutableStateFlow("")

    init {
        loadData()
        viewModelScope.launch {
            searchQueryChanges
                .debounce(350)
                .distinctUntilChanged()
                .collect { refreshFeed() }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Fired together rather than awaited one after another — four
            // sequential round trips left the screen sitting on its empty
            // state for roughly their combined latency, and then had the
            // carousel, categories and the whole product grid all pop in
            // at once, right below the search bar. Started concurrently,
            // the wait is only as long as the slowest of the four.
            val categoriesDeferred = async { categoryRepo.getAllCategories() }
            val productsDeferred = async { productRepo.getAllProducts() }
            val offersDeferred = async { offerRepo.getOffers() }
            val feedDeferred = async {
                productRepo.getProductsPage(
                    page = 0,
                    pageSize = FEED_PAGE_SIZE,
                    categoryId = _state.value.selectedCategoryId,
                    search = _state.value.searchQuery.takeIf { it.isNotBlank() }
                )
            }

            val categories = categoriesDeferred.await().getOrNull()
            val products = productsDeferred.await().getOrNull()?.filter { it.isActive }
            // A store with no coupons is a normal state, not an error worth
            // showing; the carousel simply does not render.
            val offers = offersDeferred.await().getOrNull()
            val feed = feedDeferred.await().getOrNull()

            feedPage = 0
            _state.value = _state.value.copy(
                categories = categories ?: _state.value.categories,
                products = products ?: _state.value.products,
                offers = offers ?: _state.value.offers,
                feedProducts = feed ?: _state.value.feedProducts,
                hasMoreFeed = feed?.let { it.size == FEED_PAGE_SIZE } ?: _state.value.hasMoreFeed,
                isLoading = false
            )
        }
    }

    /**
     * Appends the next page of whichever view is currently active. A no-op
     * while a page is already in flight or the last one came back short of
     * a full page (nothing further to ask for) — the caller (a LazyColumn
     * item entering composition near the end of the list) can call this
     * freely without its own guard.
     */
    fun loadMoreFeed() {
        val current = _state.value
        if (current.isLoadingMoreFeed || !current.hasMoreFeed) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMoreFeed = true)
            val nextPage = feedPage + 1
            productRepo.getProductsPage(
                page = nextPage,
                pageSize = FEED_PAGE_SIZE,
                categoryId = current.selectedCategoryId,
                search = current.searchQuery.takeIf { it.isNotBlank() }
            ).onSuccess { page ->
                feedPage = nextPage
                _state.value = _state.value.copy(
                    feedProducts = _state.value.feedProducts + page,
                    hasMoreFeed = page.size == FEED_PAGE_SIZE,
                    isLoadingMoreFeed = false
                )
            }.onFailure {
                _state.value = _state.value.copy(isLoadingMoreFeed = false)
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
        // Not a rapid-fire event like typing, so no debounce needed —
        // refetch as soon as the tap lands.
        viewModelScope.launch { refreshFeed() }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        searchQueryChanges.value = query
    }

    /** Replaces the feed from page 0 for whatever filter (category/search) is now current. */
    private suspend fun refreshFeed() {
        val state = _state.value
        feedPage = 0
        _state.value = state.copy(isLoadingMoreFeed = false)
        productRepo.getProductsPage(
            page = 0,
            pageSize = FEED_PAGE_SIZE,
            categoryId = state.selectedCategoryId,
            search = state.searchQuery.takeIf { it.isNotBlank() }
        ).onSuccess { page ->
            _state.value = _state.value.copy(
                feedProducts = page,
                hasMoreFeed = page.size == FEED_PAGE_SIZE
            )
        }
    }

    private companion object {
        const val FEED_PAGE_SIZE = 20
    }
}
