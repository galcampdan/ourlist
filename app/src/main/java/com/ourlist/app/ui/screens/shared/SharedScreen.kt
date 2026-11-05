package com.ourlist.app.ui.screens.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ourlist.app.data.model.ConflictRecord
import com.ourlist.app.data.model.DDayCalculator
import com.ourlist.app.data.model.SortOption
import com.ourlist.app.data.model.ViewMode
import com.ourlist.app.data.sync.SyncState
import com.ourlist.app.ui.components.BucketItemCard
import com.ourlist.app.ui.components.BucketItemRow
import com.ourlist.app.ui.components.ConflictResolutionDialog
import com.ourlist.app.ui.components.CoupleOverviewCard
import com.ourlist.app.ui.components.EmptyStateCard
import com.ourlist.app.ui.components.FilterSortBar
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.InlineAddButton
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Danger
import com.ourlist.app.ui.theme.HeartRed
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedScreen(
    viewModel: BucketListViewModel,
    unreadNotificationCount: Int,
    onNavigateToNotifications: () -> Unit,
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
    val unresolvedConflicts by viewModel.unresolvedConflicts.collectAsState()
    val dreamGaugeStats by viewModel.dreamGaugeStats.collectAsState()
    val datingStartDate by viewModel.datingStartDate.collectAsState()

    val haptics = rememberHaptics()
    var activeConflictToResolve by remember { mutableStateOf<ConflictRecord?>(null) }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
    }
    val dDayText = remember(datingStartDate) {
        DDayCalculator.formatDDayText(datingStartDate)
    }
    val headerSubtitle = if (dDayText != null) "$todayFormatted · $dDayText" else todayFormatted

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    if (activeConflictToResolve != null) {
        ConflictResolutionDialog(
            conflict = activeConflictToResolve!!,
            onResolveField = { chosenValue ->
                viewModel.resolveFieldConflict(activeConflictToResolve!!, chosenValue)
                activeConflictToResolve = null
            },
            onResolveDelete = { keepItem ->
                viewModel.resolveDeleteConflict(activeConflictToResolve!!, keepItem)
                activeConflictToResolve = null
            },
            onDismiss = { activeConflictToResolve = null }
        )
    }

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
        // 1. Header with Sync & Notification bell icon
        item {
            HomeHeader(
                title = "우리의 버킷리스트",
                subtitle = headerSubtitle,
                accentColor = Primary,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Sync Button
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
                                    color = Primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "동기화",
                                    tint = Primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        // Notification bell icon with unread count badge (immune to clipping)
                        Box(
                            modifier = Modifier.size(OurListDimens.MinTouchTarget),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(SurfaceRaised)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        haptics.tick()
                                        onNavigateToNotifications()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "알림",
                                    tint = if (unreadNotificationCount > 0) Primary else TextSecondary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            if (unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .background(HeartRed, shape = CircleShape)
                                        .border(2.dp, Ink, shape = CircleShape)
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (unreadNotificationCount > 99) "99+" else "$unreadNotificationCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }

        // 2. Conflict Notice Banner
        if (unresolvedConflicts.isNotEmpty()) {
            item {
                OurListCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptics.tick()
                            activeConflictToResolve = unresolvedConflicts.first()
                        },
                    containerColor = SurfaceRaised,
                    border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Danger,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "동시 수정 충돌 ${unresolvedConflicts.size}건 발견",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Danger,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "탭하여 유지할 내용을 선택하세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        // 3. Couple Overview Card (Dream Gauge: Independent of search/filter, UI-05)
        item {
            CoupleOverviewCard(
                completedCount = dreamGaugeStats.completedCount,
                totalCount = dreamGaugeStats.totalCount,
                syncState = syncState,
                title = "함께 완성해가는 꿈",
                accentColor = Primary,
            )
        }

        // 4. Filter & Search Controls (Multi-choice wish type chips, UI-06)
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
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }

        // 5. Hint banner for manual reorder mode
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

        // 6. Items List
        if (items.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (searchQuery.isNotEmpty() || selectedWishTypes.size < 3) {
                        "조건에 맞는 버킷리스트가 없어요"
                    } else {
                        "아직 등록된 공동 버킷리스트가 없어요"
                    },
                    description = if (searchQuery.isNotEmpty() || selectedWishTypes.size < 3) {
                        "다른 검색어나 필터를 선택해보세요."
                    } else {
                        "두 사람의 소중한 꿈과 버킷리스트를 지금 바로 등록해보세요!"
                    },
                    action = {
                        InlineAddButton(
                            text = "첫 버킷리스트 추가하기",
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

            // Inline Add Button at footer
            item {
                Spacer(Modifier.height(4.dp))
                InlineAddButton(
                    text = "새 버킷리스트 추가",
                    onClick = onNavigateToAdd,
                )
            }
        }
    }
}
