package com.ourlist.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.WishType
import com.ourlist.app.data.repository.BucketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val item: BucketItem, val isReminderOn: Boolean) : DetailUiState()
    object Saved : DetailUiState()
    object Deleted : DetailUiState()
}

class ItemDetailViewModel(
    private val itemId: String?,
    private val initialCollection: BucketCollection,
    private val bucketRepository: BucketRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            if (itemId.isNullOrEmpty() || itemId == "new") {
                val newItem = BucketItem(
                    id = UUID.randomUUID().toString(),
                    collection = initialCollection,
                    title = ""
                )
                _uiState.value = DetailUiState.Success(newItem, isReminderOn = true)
            } else {
                val item = bucketRepository.getItemById(itemId)
                if (item != null) {
                    val reminderOn = dataStoreManager.isReminderEnabled(item.id)
                    _uiState.value = DetailUiState.Success(item, reminderOn)
                } else {
                    val fallback = BucketItem(
                        id = UUID.randomUUID().toString(),
                        collection = initialCollection,
                        title = ""
                    )
                    _uiState.value = DetailUiState.Success(fallback, isReminderOn = true)
                }
            }
        }
    }

    private var isSaving = false

    fun saveItem(
        title: String,
        wishType: WishType,
        targetDate: String?,
        estimatedCostKRW: Long?,
        placeText: String?,
        checklist: List<ChecklistItem>,
        memo: String?,
        isReminderOn: Boolean
    ): Boolean {
        if (isSaving) return false
        // Validation: Title required, SPECIFIC_DATE requires targetDate
        if (title.isBlank()) return false
        if (wishType == WishType.SPECIFIC_DATE && targetDate.isNullOrBlank()) return false

        val currentSuccess = _uiState.value as? DetailUiState.Success ?: return false
        isSaving = true
        val original = currentSuccess.item

        val isNew = itemId.isNullOrEmpty() || itemId == "new"
        val updated = original.copy(
            title = title.trim(),
            wishType = wishType,
            targetDate = targetDate?.ifBlank { null },
            estimatedCostKRW = estimatedCostKRW,
            placeText = placeText?.ifBlank { null },
            checklist = checklist,
            memo = memo?.ifBlank { null }
        )

        viewModelScope.launch {
            dataStoreManager.setItemReminderEnabled(updated.id, isReminderOn)
            if (isNew) {
                bucketRepository.createItem(updated)
            } else {
                bucketRepository.updateItem(updated)
            }
            _uiState.value = DetailUiState.Saved
        }
        return true
    }

    fun completeItem() {
        val current = (_uiState.value as? DetailUiState.Success)?.item ?: return
        viewModelScope.launch {
            bucketRepository.completeItem(current.id)
            loadItem()
        }
    }

    fun uncompleteItem() {
        val current = (_uiState.value as? DetailUiState.Success)?.item ?: return
        viewModelScope.launch {
            bucketRepository.uncompleteItem(current.id)
            loadItem()
        }
    }

    fun doAgain(newTargetDate: String?) {
        val current = (_uiState.value as? DetailUiState.Success)?.item ?: return
        viewModelScope.launch {
            bucketRepository.doAgain(current.id, newTargetDate)
            _uiState.value = DetailUiState.Saved
        }
    }

    fun moveToTrash() {
        val current = (_uiState.value as? DetailUiState.Success)?.item ?: return
        viewModelScope.launch {
            bucketRepository.moveToTrash(current.id)
            _uiState.value = DetailUiState.Deleted
        }
    }

    fun moveCollection(targetCollection: BucketCollection) {
        val current = (_uiState.value as? DetailUiState.Success)?.item ?: return
        viewModelScope.launch {
            bucketRepository.moveCollection(current.id, targetCollection)
            loadItem()
        }
    }

    class Factory(
        private val itemId: String?,
        private val initialCollection: BucketCollection,
        private val bucketRepository: BucketRepository,
        private val dataStoreManager: DataStoreManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ItemDetailViewModel(
                itemId,
                initialCollection,
                bucketRepository,
                dataStoreManager
            ) as T
        }
    }
}
