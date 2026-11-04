package com.ourlist.app.ui.screens.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.ConflictRecord
import com.ourlist.app.data.model.SortOption
import com.ourlist.app.data.model.ViewMode
import com.ourlist.app.data.model.WishType
import com.ourlist.app.data.repository.BucketRepository
import com.ourlist.app.data.repository.ConflictRepository
import com.ourlist.app.data.sync.SyncEngine
import com.ourlist.app.data.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BucketListViewModel(
    val collection: BucketCollection,
    private val bucketRepository: BucketRepository,
    private val conflictRepository: ConflictRepository,
    private val dataStoreManager: DataStoreManager,
    private val syncEngine: SyncEngine
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncEngine.syncState
    val viewMode: StateFlow<ViewMode> = dataStoreManager.viewModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST
    )
    val sortOption: StateFlow<SortOption> = dataStoreManager.sortOptionFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), SortOption.TARGET_DATE
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedWishType = MutableStateFlow<WishType?>(null)
    val selectedWishType: StateFlow<WishType?> = _selectedWishType

    val unresolvedConflicts: StateFlow<List<ConflictRecord>> =
        conflictRepository.getUnresolvedConflicts().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val items: StateFlow<List<BucketItem>> = combine(
        bucketRepository.getTodoItems(collection),
        searchQuery,
        selectedWishType,
        sortOption,
        dataStoreManager.manualRanksFlow
    ) { rawItems, query, wishType, sort, manualRanks ->
        var filtered = rawItems

        // Filter by WishType
        if (wishType != null) {
            filtered = filtered.filter { it.wishType == wishType }
        }

        // Filter by unified search query (title, place, checklist)
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { item ->
                item.title.lowercase().contains(q) ||
                        (item.placeText?.lowercase()?.contains(q) == true) ||
                        item.checklist.any { it.text.lowercase().contains(q) }
            }
        }

        // Sort items
        when (sort) {
            SortOption.TARGET_DATE -> filtered.sortedWith(
                compareBy(nullsLast()) { it.targetDate }
            )
            SortOption.CREATED_AT -> filtered.sortedByDescending { it.createdAt }
            SortOption.WISH_TYPE -> filtered.sortedBy { it.wishType.ordinal }
            SortOption.COST -> filtered.sortedWith(
                compareBy(nullsLast()) { it.estimatedCostKRW }
            )
            SortOption.CREATED_BY -> filtered.sortedBy { it.createdByName }
            SortOption.MANUAL -> filtered.sortedBy { manualRanks[it.id] ?: Int.MAX_VALUE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedWishType(type: WishType?) {
        _selectedWishType.value = type
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch {
            dataStoreManager.setSortOption(option)
        }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val current = viewMode.value
            val next = if (current == ViewMode.LIST) ViewMode.CARD else ViewMode.LIST
            dataStoreManager.setViewMode(next)
        }
    }

    fun toggleChecklistItem(itemId: String, checkId: String, checked: Boolean) {
        viewModelScope.launch {
            bucketRepository.toggleChecklistItem(itemId, checkId, checked)
        }
    }

    fun completeItem(itemId: String) {
        viewModelScope.launch {
            bucketRepository.completeItem(itemId)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncEngine.sync()
        }
    }

    fun resolveFieldConflict(conflict: ConflictRecord, chosenValue: String?) {
        viewModelScope.launch {
            conflictRepository.resolveFieldConflict(conflict, chosenValue)
        }
    }

    fun resolveDeleteConflict(conflict: ConflictRecord, keepItem: Boolean) {
        viewModelScope.launch {
            conflictRepository.resolveDeleteVsUpdateConflict(conflict, keepItem)
        }
    }

    class Factory(
        private val collection: BucketCollection,
        private val bucketRepository: BucketRepository,
        private val conflictRepository: ConflictRepository,
        private val dataStoreManager: DataStoreManager,
        private val syncEngine: SyncEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BucketListViewModel(
                collection,
                bucketRepository,
                conflictRepository,
                dataStoreManager,
                syncEngine
            ) as T
        }
    }
}
