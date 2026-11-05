package com.ourlist.app.ui.screens.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.ConflictRecord
import com.ourlist.app.data.model.SortDirection
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DreamGaugeStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val percentage: Int = 0
)

data class SortConfig(
    val option: SortOption,
    val direction: SortDirection
)

class BucketListViewModel(
    val collection: BucketCollection,
    private val bucketRepository: BucketRepository,
    private val conflictRepository: ConflictRepository,
    private val dataStoreManager: DataStoreManager,
    private val syncEngine: SyncEngine
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncEngine.syncState
    val datingStartDate: StateFlow<String> = dataStoreManager.datingStartDateFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val myDeviceId: StateFlow<String> = dataStoreManager.deviceIdFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val myUserSub: StateFlow<String> = dataStoreManager.userSubFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val partnerName: StateFlow<String> = dataStoreManager.partnerNameFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "상대방"
    )
    val viewMode: StateFlow<ViewMode> = dataStoreManager.viewModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST
    )
    val sortOption: StateFlow<SortOption> = dataStoreManager.sortOptionFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), SortOption.TARGET_DATE
    )

    private val _sortDirection = MutableStateFlow(SortDirection.ASC)
    val sortDirection: StateFlow<SortDirection> = _sortDirection

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // UI-06: 3 options exposed simultaneously, multi-choice, initially all selected, all can be deselected
    private val _selectedWishTypes = MutableStateFlow<Set<WishType>>(WishType.entries.toSet())
    val selectedWishTypes: StateFlow<Set<WishType>> = _selectedWishTypes

    // Backward compatibility helper
    val selectedWishType: StateFlow<WishType?> = MutableStateFlow(null)

    val unresolvedConflicts: StateFlow<List<ConflictRecord>> =
        conflictRepository.getUnresolvedConflicts().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    // UI-05: Dream gauge based on all active items in collection, independent of filters
    val dreamGaugeStats: StateFlow<DreamGaugeStats> = bucketRepository
        .getAllActiveInCollection(collection)
        .map { list ->
            val total = list.size
            val completed = list.count { it.isDone }
            val percent = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0
            DreamGaugeStats(totalCount = total, completedCount = completed, percentage = percent)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DreamGaugeStats())

    private val sortConfig = combine(sortOption, sortDirection) { opt, dir ->
        SortConfig(opt, dir)
    }

    val items: StateFlow<List<BucketItem>> = combine(
        bucketRepository.getTodoItems(collection),
        searchQuery,
        selectedWishTypes,
        sortConfig,
        dataStoreManager.manualRanksFlow
    ) { rawItems, query, wishTypes, config, manualRanks ->
        var filtered = rawItems.filter { it.wishType in wishTypes }

        // Filter by unified search query (title, place, checklist)
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { item ->
                item.title.lowercase().contains(q) ||
                        (item.placeText?.lowercase()?.contains(q) == true) ||
                        item.checklist.any { it.text.lowercase().contains(q) }
            }
        }

        val sort = config.option
        val dir = config.direction

        // Sort items with Ascending / Descending toggles
        when (sort) {
            SortOption.TARGET_DATE -> {
                if (dir == SortDirection.ASC) {
                    filtered.sortedWith(compareBy<BucketItem> { it.targetDate == null }.thenBy { it.targetDate })
                } else {
                    filtered.sortedWith(compareBy<BucketItem> { it.targetDate == null }.thenByDescending { it.targetDate })
                }
            }
            SortOption.COST -> {
                if (dir == SortDirection.ASC) {
                    filtered.sortedWith(compareBy<BucketItem> { it.estimatedCostKRW == null }.thenBy { it.estimatedCostKRW })
                } else {
                    filtered.sortedWith(compareBy<BucketItem> { it.estimatedCostKRW == null }.thenByDescending { it.estimatedCostKRW })
                }
            }
            SortOption.CREATED_BY -> {
                if (dir == SortDirection.ASC) {
                    filtered.sortedBy { it.createdByName }
                } else {
                    filtered.sortedByDescending { it.createdByName }
                }
            }
            SortOption.CREATED_AT -> filtered.sortedByDescending { it.createdAt }
            SortOption.WISH_TYPE -> filtered.sortedBy { it.wishType.ordinal }
            SortOption.MANUAL -> filtered.sortedBy { manualRanks[it.id] ?: Int.MAX_VALUE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleWishType(type: WishType) {
        val current = _selectedWishTypes.value
        _selectedWishTypes.value = if (type in current) {
            current - type
        } else {
            current + type
        }
    }

    fun setSelectedWishType(type: WishType?) {
        if (type == null) {
            _selectedWishTypes.value = WishType.entries.toSet()
        } else {
            toggleWishType(type)
        }
    }

    fun setSortOption(option: SortOption) {
        if (option in listOf(SortOption.COST, SortOption.TARGET_DATE, SortOption.CREATED_BY)) {
            if (sortOption.value == option) {
                _sortDirection.value = if (_sortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
            } else {
                _sortDirection.value = SortDirection.ASC
                viewModelScope.launch { dataStoreManager.setSortOption(option) }
            }
        } else {
            viewModelScope.launch { dataStoreManager.setSortOption(option) }
        }
    }

    fun reorderItems(fromIndex: Int, toIndex: Int, currentList: List<BucketItem>) {
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) return
        val mutable = currentList.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        val newRanks = mutable.mapIndexed { idx, itm -> itm.id to idx }.toMap()
        viewModelScope.launch {
            dataStoreManager.setSortOption(SortOption.MANUAL)
            dataStoreManager.setManualRanks(newRanks)
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
