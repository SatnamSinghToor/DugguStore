package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.Order
import com.duggustore.app.data.model.OrderIssue
import com.duggustore.app.data.model.OrderItem
import com.duggustore.app.data.model.OrderStatus
import com.duggustore.app.data.model.Review
import com.duggustore.app.data.model.WalletTransaction
import com.duggustore.app.data.repository.OrderIssueRepository
import com.duggustore.app.data.repository.OrderRepository
import com.duggustore.app.data.repository.ReviewRepository
import com.duggustore.app.data.repository.TrackingRepository
import com.duggustore.app.data.repository.WalletRepository
import com.duggustore.app.data.repository.walletBalance
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
    val error: String? = null,
    /** Line items for whichever orders have had them fetched — on demand, not with every order list. */
    val orderItemsByOrderId: Map<String, List<OrderItem>> = emptyMap(),
    /** This customer's own reviews on a delivered order, keyed by product id, so "rate this item" shows what's already rated. */
    val myReviewsByOrderId: Map<String, Map<String, Review>> = emptyMap(),
    val walletTransactions: List<WalletTransaction> = emptyList(),
    /** This customer's own issue reports, keyed by order id, so "report a problem" can show it was already sent. */
    val myIssuesByOrderId: Map<String, List<OrderIssue>> = emptyMap(),
    /** Open issues on a seller's (or, for an admin, every) order, for the resolve screen. */
    val issuesForReview: List<OrderIssue> = emptyList()
) {
    val walletBalance: Int get() = walletTransactions.walletBalance()
}

class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val trackingRepo = TrackingRepository()
    private val reviewRepo = ReviewRepository()
    private val walletRepo = WalletRepository()
    private val issueRepo = OrderIssueRepository()

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

    /**
     * Fetched per order rather than embedded in every list query — a
     * seller or customer with dozens of orders would otherwise pull every
     * line item and product row for all of them on every list refresh, for
     * detail almost none of those rows are open at once.
     */
    fun loadOrderItems(orderId: String) {
        if (_state.value.orderItemsByOrderId.containsKey(orderId)) return
        viewModelScope.launch {
            repository.getOrderItems(orderId).onSuccess { items ->
                _state.value = _state.value.copy(
                    orderItemsByOrderId = _state.value.orderItemsByOrderId + (orderId to items)
                )
            }
        }
    }

    /** Only worth calling once an order is delivered — nothing can be rated before that. */
    fun loadMyReviews(userId: String, orderId: String) {
        if (_state.value.myReviewsByOrderId.containsKey(orderId)) return
        viewModelScope.launch {
            reviewRepo.getMyReviewsForOrder(userId, orderId).onSuccess { reviews ->
                _state.value = _state.value.copy(
                    myReviewsByOrderId = _state.value.myReviewsByOrderId +
                        (orderId to reviews.associateBy { it.productId })
                )
            }
        }
    }

    /** Upserts, so rating the same item on the same order again just updates it. */
    fun submitReview(userId: String, orderId: String, productId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            reviewRepo.submitReview(userId, orderId, productId, rating, comment).onSuccess {
                val forOrder = _state.value.myReviewsByOrderId[orderId].orEmpty() +
                    (productId to Review(
                        userId = userId,
                        orderId = orderId,
                        productId = productId,
                        rating = rating,
                        comment = comment
                    ))
                _state.value = _state.value.copy(
                    myReviewsByOrderId = _state.value.myReviewsByOrderId + (orderId to forOrder)
                )
            }
        }
    }

    fun loadWallet(userId: String) {
        viewModelScope.launch {
            walletRepo.getTransactions(userId).onSuccess { txns ->
                _state.value = _state.value.copy(walletTransactions = txns)
            }
        }
    }

    fun loadMyIssues(userId: String, orderId: String) {
        if (_state.value.myIssuesByOrderId.containsKey(orderId)) return
        viewModelScope.launch {
            issueRepo.getMyIssues(userId).onSuccess { issues ->
                _state.value = _state.value.copy(
                    myIssuesByOrderId = issues.groupBy { it.orderId }
                )
            }
        }
    }

    fun reportIssue(orderId: String, productId: String?, userId: String, reason: String, description: String) {
        viewModelScope.launch {
            issueRepo.reportIssue(orderId, productId, userId, reason, description).onSuccess {
                _state.value = _state.value.copy(myIssuesByOrderId = emptyMap())
                loadMyIssues(userId, orderId)
            }
        }
    }

    fun loadIssuesForReview() {
        viewModelScope.launch {
            issueRepo.getIssuesForReview().onSuccess { issues ->
                _state.value = _state.value.copy(issuesForReview = issues)
            }
        }
    }

    fun resolveIssue(issueId: String, approve: Boolean, refundAmount: Int) {
        viewModelScope.launch {
            issueRepo.resolveIssue(issueId, approve, refundAmount).onSuccess {
                loadIssuesForReview()
            }
        }
    }
}
