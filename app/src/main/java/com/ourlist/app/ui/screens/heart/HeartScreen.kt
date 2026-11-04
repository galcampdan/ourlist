package com.ourlist.app.ui.screens.heart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.ViewMode
import com.ourlist.app.data.sync.SyncState
import com.ourlist.app.ui.components.BucketItemCard
import com.ourlist.app.ui.components.BucketItemRow
import com.ourlist.app.ui.components.CoupleOverviewCard
import com.ourlist.app.ui.components.EmptyStateCard
import com.ourlist.app.ui.components.FilterSortBar
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.InlineAddButton
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.screens.shared.BucketListViewModel
import com.ourlist.app.ui.theme.HeartGradientEnd
import com.ourlist.app.ui.theme.HeartGradientStart
import com.ourlist.app.ui.theme.HeartRed
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.SurfaceRaised

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartScreen(
    viewModel: BucketListViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedWishType by viewModel.selectedWishType.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    val haptics = rememberHaptics()
    val completedCount = items.count { it.isDone }
    val totalCount = items.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink),
        contentPadding = PaddingValues(
            start = OurListDimens.ScreenPadding,
            top = 16.dp,
            end = OurListDimens.ScreenPadding,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Header
        item {
            HomeHeader(
                title = "❤️ 하트 보관함",
                subtitle = "둘만의 특별하고 설레는 기록",
                accentColor = HeartRed,
                actions = {
                    Box(
                        modifier = Modifier
                            .size(OurListDimens.MinTouchTarget)
                            .clip(CircleShape)
                            .background(SurfaceRaised)
                            .clickable {
                                haptics.tick()
                                viewModel.triggerSync()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = HeartRed,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "동기화",
                                tint = HeartRed,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                },
            )
        }

        // 2. Romantic Gradient Overview Card
        item {
            CoupleOverviewCard(
                completedCount = completedCount,
                totalCount = totalCount,
                syncState = syncState,
                gradientColors = listOf(HeartGradientStart, HeartGradientEnd),
                title = "둘만의 설레는 순간들",
                accentColor = HeartRed,
            )
        }

        // 3. Filter & Sort Bar
        item {
            FilterSortBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                selectedWishType = selectedWishType,
                onWishTypeSelect = { viewModel.setSelectedWishType(it) },
                sortOption = sortOption,
                onSortOptionChange = { viewModel.setSortOption(it) },
                viewMode = viewMode,
                onToggleViewMode = { viewModel.toggleViewMode() },
                accentColor = HeartRed,
            )
        }

        // 4. Items List
        if (items.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (searchQuery.isNotEmpty() || selectedWishType != null) {
                        "조건에 맞는 하트 버킷리스트가 없어요"
                    } else {
                        "아직 등록된 하트 버킷리스트가 없어요"
                    },
                    description = if (searchQuery.isNotEmpty() || selectedWishType != null) {
                        "다른 검색어나 필터를 선택해보세요."
                    } else {
                        "오직 둘만의 비밀스럽고 설레는 계획을 지금 적어보세요!"
                    },
                    action = {
                        InlineAddButton(
                            text = "하트 버킷리스트 추가하기",
                            onClick = onNavigateToAdd,
                            accentColor = HeartRed,
                        )
                    },
                )
            }
        } else {
            items(items, key = { it.id }) { item ->
                if (viewMode == ViewMode.CARD) {
                    BucketItemCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        onCompleteToggle = { viewModel.completeItem(item.id) },
                        onChecklistToggle = { checkId, checked ->
                            viewModel.toggleChecklistItem(item.id, checkId, checked)
                        },
                    )
                } else {
                    BucketItemRow(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        onCompleteToggle = { viewModel.completeItem(item.id) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                InlineAddButton(
                    text = "하트 버킷리스트 추가",
                    onClick = onNavigateToAdd,
                    accentColor = HeartRed,
                )
            }
        }
    }
}
