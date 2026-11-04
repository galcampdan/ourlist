package com.ourlist.app.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.ViewMode
import com.ourlist.app.ui.components.BucketItemCard
import com.ourlist.app.ui.components.BucketItemRow
import com.ourlist.app.ui.components.EmptyStateCard
import com.ourlist.app.ui.components.FilterSortBar
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.InlineAddButton
import com.ourlist.app.ui.components.SegmentedControl
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.screens.memories.MemoryItemCard
import com.ourlist.app.ui.screens.shared.BucketListViewModel
import com.ourlist.app.ui.theme.Due
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.Review
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(
    viewModel: BucketListViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedWishType by viewModel.selectedWishType.collectAsState()
    val haptics = rememberHaptics()

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(OurListDimens.MinTouchTarget)
                        .clip(CircleShape)
                        .background(SurfaceRaised)
                        .clickable {
                            haptics.tick()
                            onNavigateBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                HomeHeader(
                    title = "개인 버킷리스트",
                    subtitle = "나만 볼 수 있는 로컬 보관함",
                    accentColor = Primary,
                )
            }
        }

        // 2. Filter & Sort Bar
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
            )
        }

        // 3. Items List
        if (items.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "등록된 개인 버킷리스트가 없어요",
                    description = "공유되지 않고 오직 이 기기에만 저장되는 나만의 꿈을 적어보세요.",
                    action = {
                        InlineAddButton(
                            text = "개인 버킷리스트 추가하기",
                            onClick = onNavigateToAdd,
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
                    text = "개인 버킷리스트 추가",
                    onClick = onNavigateToAdd,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretPersonalScreen(
    viewModel: BucketListViewModel,
    secretMemories: List<BucketItem>,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateBack: () -> Unit,
    onDoAgain: (String) -> Unit,
    onRevert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedWishType by viewModel.selectedWishType.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: 진행 중, 1: 달성한 비밀 추억
    val haptics = rememberHaptics()
    val tabOptions = listOf("진행 중 (${items.size})", "비밀 추억 (${secretMemories.size})")

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(OurListDimens.MinTouchTarget)
                        .clip(CircleShape)
                        .background(SurfaceRaised)
                        .clickable {
                            haptics.tick()
                            onNavigateBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                HomeHeader(
                    title = "개인 비밀 보관함",
                    subtitle = "공유 Drive에 올라가지 않는 비밀 목록",
                    accentColor = Due,
                )
            }
        }

        // 2. Tab Segmented Control
        item {
            SegmentedControl(
                options = listOf(0, 1),
                selected = selectedTab,
                label = { tabOptions[it] },
                onSelect = { selectedTab = it },
            )
        }

        // 3. Tab Content
        if (selectedTab == 0) {
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
                    accentColor = Due,
                )
            }

            if (items.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "비밀 버킷리스트가 없어요",
                        description = "Google Drive에 절대 동기화되지 않는 나만의 소중한 비밀 목표를 적어보세요.",
                        action = {
                            InlineAddButton(
                                text = "비밀 버킷리스트 추가하기",
                                onClick = onNavigateToAdd,
                                accentColor = Due,
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
                        text = "비밀 버킷리스트 추가",
                        onClick = onNavigateToAdd,
                        accentColor = Due,
                    )
                }
            }
        } else {
            // Secret Memories
            if (secretMemories.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "달성한 비밀 추억이 없어요",
                        description = "비밀 버킷리스트를 완료하면 이곳에 안전하게 보관됩니다.",
                    )
                }
            } else {
                items(secretMemories, key = { it.id }) { item ->
                    MemoryItemCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        onDoAgain = { onDoAgain(item.id) },
                        onRevert = { onRevert(item.id) },
                    )
                }
            }
        }
    }
}
