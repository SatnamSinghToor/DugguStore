package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.Seller
import com.duggustore.app.data.model.SellerDocument
import com.duggustore.app.data.repository.SellerOnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SellerOnboardingState(
    /** Null and not [hasLoaded] means "haven't checked yet" — the caller should wait rather than assume no application exists. */
    val hasLoaded: Boolean = false,
    val seller: Seller? = null,
    val documents: List<SellerDocument> = emptyList(),
    val isSaving: Boolean = false,
    val isSubmitting: Boolean = false,
    val uploadingDocType: String? = null,
    val error: String? = null,
    /** Set once so the review queue can be refreshed by whoever's watching it. */
    val allSellers: List<Seller> = emptyList(),
    val isLoadingAll: Boolean = false,
    /** An admin's view of an applicant's documents, keyed by seller id — separate from [documents] (the signed-in seller's own), since an admin browsing the queue must not clobber that. */
    val reviewDocuments: Map<String, List<SellerDocument>> = emptyMap(),
    /** Signed, directly-loadable URLs for review documents, keyed by document id. */
    val reviewDocumentUrls: Map<String, String> = emptyMap(),
    val loadingReviewDocsFor: String? = null
)

class SellerOnboardingViewModel : ViewModel() {
    private val repository = SellerOnboardingRepository()

    private val _state = MutableStateFlow(SellerOnboardingState())
    val state: StateFlow<SellerOnboardingState> = _state

    fun load(userId: String) {
        viewModelScope.launch {
            val sellerResult = repository.getMySeller(userId)
            val seller = sellerResult.getOrNull()
            val docs = if (seller != null) repository.getDocuments(userId).getOrElse { emptyList() } else emptyList()
            _state.value = _state.value.copy(hasLoaded = true, seller = seller, documents = docs)
        }
    }

    fun save(seller: Seller, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            val result = repository.saveApplication(seller)
            result.onSuccess {
                load(seller.id)
                onDone()
            }
            result.onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(isSaving = false)
        }
    }

    fun uploadDocument(sellerId: String, docType: String, bytes: ByteArray, contentType: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(uploadingDocType = docType, error = null)
            val result = repository.uploadDocument(sellerId, docType, bytes, contentType)
            result.onFailure { _state.value = _state.value.copy(error = it.message) }
            result.onSuccess {
                repository.getDocuments(sellerId).onSuccess { docs ->
                    _state.value = _state.value.copy(documents = docs)
                }
            }
            _state.value = _state.value.copy(uploadingDocType = null)
        }
    }

    fun submit(sellerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, error = null)
            val result = repository.submitForReview(sellerId)
            result.onSuccess { load(sellerId) }
            result.onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(isSubmitting = false)
        }
    }

    fun loadAllForReview() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingAll = true)
            repository.getAllSellers().onSuccess { list ->
                _state.value = _state.value.copy(allSellers = list)
            }
            _state.value = _state.value.copy(isLoadingAll = false)
        }
    }

    fun review(sellerId: String, approve: Boolean, rejectionReason: String = "") {
        viewModelScope.launch {
            repository.reviewSeller(sellerId, approve, rejectionReason).onSuccess {
                loadAllForReview()
            }
        }
    }

    /** Fetches one applicant's documents plus a signed URL for each, for the admin queue to display. */
    fun loadReviewDocuments(sellerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingReviewDocsFor = sellerId)
            val docs = repository.getDocuments(sellerId).getOrElse { emptyList() }
            val urls = docs.mapNotNull { doc ->
                repository.documentUrl(doc.fileUrl).getOrNull()?.let { doc.id to it }
            }.toMap()
            _state.value = _state.value.copy(
                reviewDocuments = _state.value.reviewDocuments + (sellerId to docs),
                reviewDocumentUrls = _state.value.reviewDocumentUrls + urls,
                loadingReviewDocsFor = null
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
