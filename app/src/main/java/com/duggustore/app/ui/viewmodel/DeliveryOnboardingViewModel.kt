package com.duggustore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duggustore.app.data.model.DeliveryPartner
import com.duggustore.app.data.model.DeliveryPartnerDocument
import com.duggustore.app.data.repository.DeliveryOnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DeliveryOnboardingState(
    val hasLoaded: Boolean = false,
    val partner: DeliveryPartner? = null,
    val documents: List<DeliveryPartnerDocument> = emptyList(),
    val isSaving: Boolean = false,
    val isSubmitting: Boolean = false,
    val uploadingDocType: String? = null,
    val error: String? = null,
    val allPartners: List<DeliveryPartner> = emptyList(),
    val isLoadingAll: Boolean = false
)

class DeliveryOnboardingViewModel : ViewModel() {
    private val repository = DeliveryOnboardingRepository()

    private val _state = MutableStateFlow(DeliveryOnboardingState())
    val state: StateFlow<DeliveryOnboardingState> = _state

    fun load(userId: String) {
        viewModelScope.launch {
            val partnerResult = repository.getMyPartner(userId)
            val partner = partnerResult.getOrNull()
            val docs = if (partner != null) repository.getDocuments(userId).getOrElse { emptyList() } else emptyList()
            _state.value = _state.value.copy(hasLoaded = true, partner = partner, documents = docs)
        }
    }

    fun save(partner: DeliveryPartner, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            val result = repository.saveApplication(partner)
            result.onSuccess {
                load(partner.id)
                onDone()
            }
            result.onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(isSaving = false)
        }
    }

    fun uploadDocument(partnerId: String, docType: String, bytes: ByteArray, contentType: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(uploadingDocType = docType, error = null)
            val result = repository.uploadDocument(partnerId, docType, bytes, contentType)
            result.onFailure { _state.value = _state.value.copy(error = it.message) }
            result.onSuccess {
                repository.getDocuments(partnerId).onSuccess { docs ->
                    _state.value = _state.value.copy(documents = docs)
                }
            }
            _state.value = _state.value.copy(uploadingDocType = null)
        }
    }

    fun submit(partnerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, error = null)
            val result = repository.submitForReview(partnerId)
            result.onSuccess { load(partnerId) }
            result.onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(isSubmitting = false)
        }
    }

    fun loadAllForReview() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingAll = true)
            repository.getAllPartners().onSuccess { list ->
                _state.value = _state.value.copy(allPartners = list)
            }
            _state.value = _state.value.copy(isLoadingAll = false)
        }
    }

    fun review(partnerId: String, approve: Boolean, rejectionReason: String = "") {
        viewModelScope.launch {
            repository.reviewPartner(partnerId, approve, rejectionReason).onSuccess {
                loadAllForReview()
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
