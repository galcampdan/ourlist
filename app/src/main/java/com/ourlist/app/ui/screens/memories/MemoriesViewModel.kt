package com.ourlist.app.ui.screens.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.repository.BucketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoriesViewModel(
    private val bucketRepository: BucketRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow<BucketCollection?>(null) // null = 전체
    val selectedTab: StateFlow<BucketCollection?> = _selectedTab

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val memories: StateFlow<List<BucketItem>> = combine(
        bucketRepository.getMemories(),
        selectedTab,
        searchQuery
    ) { allMemories, tab, query ->
        var filtered = allMemories
        if (tab != null) {
            filtered = filtered.filter { it.collection == tab }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { item ->
                item.title.lowercase().contains(q) ||
                        (item.placeText?.lowercase()?.contains(q) == true) ||
                        item.checklist.any { it.text.lowercase().contains(q) }
            }
        }
        filtered.sortedByDescending { it.completedAt ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tab: BucketCollection?) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun doAgain(originalItemId: String, newTargetDate: String?) {
        viewModelScope.launch {
            bucketRepository.doAgain(originalItemId, newTargetDate)
        }
    }

    fun revertToTodo(itemId: String) {
        viewModelScope.launch {
            bucketRepository.uncompleteItem(itemId)
        }
    }

    class Factory(private val bucketRepository: BucketRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MemoriesViewModel(bucketRepository) as T
        }
    }
}
