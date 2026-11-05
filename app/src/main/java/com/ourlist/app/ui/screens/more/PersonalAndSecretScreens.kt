package com.ourlist.app.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.SortOption
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
import com.ourlist.app.ui.theme.TextSecondary

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
    val sortDirection by viewModel.sortDirection.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedWishTypes by viewModel.selectedWishTypes.collectAsState()
    val haptics = rememberHaptics()

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = OurListDimens.ScreenPadding,
            top = 16.dp,
            end = OurListDimens.ScreenPadding,
            bottom = 24.dp,
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptics.tick()
                            onNavigateBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                selectedWishTypes = selectedWishTypes,
                onWishTypeToggle = { viewModel.toggleWishType(it) },
                sortOption = sortOption,
                sortDirection = sortDirection,
                onSortOptionChange = { viewModel.setSortOption(it) },
                viewMode = viewMode,
                onToggleViewMode = { viewModel.toggleViewMode() },
            )
        }

        // Hint banner for manual reorder mode
        if (sortOption == SortOption.MANUAL && items.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceRaised)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "직접 순서 모드: 항목을 꾹 눌러 위아래로 순서를 이동하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
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
            itemsIndexed(items, key = { _, itm -> itm.id }) { index, item ->
                val isBeingDragged = draggedIndex == index
                val itemModifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isBeingDragged) 10f else 1f)
                    .graphicsLayer {
                        if (isBeingDragged) {
                            translationY = dragOffsetY
                            scaleX = 1.03f
                            scaleY = 1.03f
                            shadowElevation = 16f
                        }
                    }
                    .pointerInput(index, items.size, sortOption) {
                        if (sortOption == SortOption.MANUAL) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptics.confirm()
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                    val threshold = 72.dp.toPx()
                                    if (dragOffsetY > threshold && currentIdx < items.size - 1) {
                                        viewModel.reorderItems(currentIdx, currentIdx + 1, items)
                                        draggedIndex = currentIdx + 1
                                        dragOffsetY -= threshold
                                        haptics.tick()
                                    } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                        viewModel.reorderItems(currentIdx, currentIdx - 1, items)
                                        draggedIndex = currentIdx - 1
                                        dragOffsetY += threshold
                                        haptics.tick()
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                    haptics.tick()
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                }
                            )
                        }
                    }

                if (viewMode == ViewMode.CARD) {
                    BucketItemCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        onCompleteToggle = { viewModel.completeItem(item.id) },
                        onChecklistToggle = { checkId, checked ->
                            viewModel.toggleChecklistItem(item.id, checkId, checked)
                        },
                        modifier = itemModifier,
                    )
                } else {
                    BucketItemRow(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        onCompleteToggle = { viewModel.completeItem(item.id) },
                        modifier = itemModifier,
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
    val sortDirection by viewModel.sortDirection.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedWishTypes by viewModel.selectedWishTypes.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: 진행 중, 1: 달성한 비밀 추억
    val haptics = rememberHaptics()
    val tabOptions = listOf("진행 중 (${items.size})", "비밀 추억 (${secretMemories.size})")

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = OurListDimens.ScreenPadding,
            top = 16.dp,
            end = OurListDimens.ScreenPadding,
            bottom = 24.dp,
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptics.tick()
                            onNavigateBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    selectedWishTypes = selectedWishTypes,
                    onWishTypeToggle = { viewModel.toggleWishType(it) },
                    sortOption = sortOption,
                    sortDirection = sortDirection,
                    onSortOptionChange = { viewModel.setSortOption(it) },
                    viewMode = viewMode,
                    onToggleViewMode = { viewModel.toggleViewMode() },
                    accentColor = Due,
                )
            }

            // Hint banner for manual reorder mode
            if (sortOption == SortOption.MANUAL && items.size > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceRaised)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = Due,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "직접 순서 모드: 항목을 꾹 눌러 위아래로 순서를 이동하세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
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
                itemsIndexed(items, key = { _, itm -> itm.id }) { index, item ->
                    val isBeingDragged = draggedIndex == index
                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isBeingDragged) 10f else 1f)
                        .graphicsLayer {
                            if (isBeingDragged) {
                                translationY = dragOffsetY
                                scaleX = 1.03f
                                scaleY = 1.03f
                                shadowElevation = 16f
                            }
                        }
                        .pointerInput(index, items.size, sortOption) {
                            if (sortOption == SortOption.MANUAL) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptics.confirm()
                                        draggedIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                        val threshold = 72.dp.toPx()
                                        if (dragOffsetY > threshold && currentIdx < items.size - 1) {
                                            viewModel.reorderItems(currentIdx, currentIdx + 1, items)
                                            draggedIndex = currentIdx + 1
                                            dragOffsetY -= threshold
                                            haptics.tick()
                                        } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                            viewModel.reorderItems(currentIdx, currentIdx - 1, items)
                                            draggedIndex = currentIdx - 1
                                            dragOffsetY += threshold
                                            haptics.tick()
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex = null
                                        dragOffsetY = 0f
                                        haptics.tick()
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                        }

                    if (viewMode == ViewMode.CARD) {
                        BucketItemCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) },
                            onCompleteToggle = { viewModel.completeItem(item.id) },
                            onChecklistToggle = { checkId, checked ->
                                viewModel.toggleChecklistItem(item.id, checkId, checked)
                            },
                            modifier = itemModifier,
                        )
                    } else {
                        BucketItemRow(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) },
                            onCompleteToggle = { viewModel.completeItem(item.id) },
                            modifier = itemModifier,
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
