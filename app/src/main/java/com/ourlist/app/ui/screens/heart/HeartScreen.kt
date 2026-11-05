package com.ourlist.app.ui.screens.heart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ourlist.app.data.model.SortOption
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
import com.ourlist.app.ui.theme.TextSecondary

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
    val sortDirection by viewModel.sortDirection.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedWishTypes by viewModel.selectedWishTypes.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val dreamGaugeStats by viewModel.dreamGaugeStats.collectAsState()
    val myDeviceId by viewModel.myDeviceId.collectAsState()
    val myUserSub by viewModel.myUserSub.collectAsState()
    val partnerName by viewModel.partnerName.collectAsState()

    val haptics = rememberHaptics()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Ink),
        contentPadding = PaddingValues(
            start = OurListDimens.ScreenPadding,
            top = 16.dp,
            end = OurListDimens.ScreenPadding,
            bottom = 100.dp,
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
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

        // 2. Romantic Gradient Overview Card (Independent Dream Gauge, UI-05)
        item {
            CoupleOverviewCard(
                completedCount = dreamGaugeStats.completedCount,
                totalCount = dreamGaugeStats.totalCount,
                syncState = syncState,
                gradientColors = listOf(HeartGradientStart, HeartGradientEnd),
                title = "둘만의 설레는 순간들",
                accentColor = HeartRed,
            )
        }

        // 3. Filter & Sort Bar (Multi-choice wish type chips, UI-06)
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
                accentColor = HeartRed,
            )
        }

        // 4. Hint banner for manual reorder mode
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
                        tint = HeartRed,
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

        // 5. Items List
        if (items.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (searchQuery.isNotEmpty() || selectedWishTypes.size < 3) {
                        "조건에 맞는 하트 버킷리스트가 없어요"
                    } else {
                        "아직 등록된 하트 버킷리스트가 없어요"
                    },
                    description = if (searchQuery.isNotEmpty() || selectedWishTypes.size < 3) {
                        "다른 검색어나 필터를 선택해보세요."
                    } else {
                        "둘만의 소중한 로맨틱 버킷리스트를 지금 바로 등록해보세요!"
                    },
                    action = {
                        InlineAddButton(
                            text = "첫 하트 버킷리스트 추가하기",
                            onClick = onNavigateToAdd,
                            accentColor = HeartRed,
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
                        myDeviceId = myDeviceId,
                        myUserSub = myUserSub,
                        partnerName = partnerName,
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

            // Inline Add Button at footer
            item {
                Spacer(Modifier.height(4.dp))
                InlineAddButton(
                    text = "새 하트 버킷리스트 추가",
                    onClick = onNavigateToAdd,
                    accentColor = HeartRed,
                )
            }
        }
    }
}
