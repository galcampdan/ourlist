package com.ourlist.app.ui.screens.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ourlist.app.data.model.ConflictRecord
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
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedScreen(
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
    val unresolvedConflicts by viewModel.unresolvedConflicts.collectAsState()

    val haptics = rememberHaptics()
    var activeConflictToResolve by remember { mutableStateOf<ConflictRecord?>(null) }

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
                title = "우리의 버킷리스트",
                accentColor = Primary,
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
                },
            )
        }

        // 2. Conflict Notice Banner
        if (unresolvedConflicts.isNotEmpty()) {
            item {
                OurListCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
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

        // 3. Couple Overview Card
        item {
            CoupleOverviewCard(
                completedCount = completedCount,
                totalCount = totalCount,
                syncState = syncState,
                title = "함께 완성해가는 꿈",
                accentColor = Primary,
            )
        }

        // 4. Filter & Search Controls
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
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }

        // 5. Items List
        if (items.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (searchQuery.isNotEmpty() || selectedWishType != null) {
                        "조건에 맞는 버킷리스트가 없어요"
                    } else {
                        "아직 등록된 공동 버킷리스트가 없어요"
                    },
                    description = if (searchQuery.isNotEmpty() || selectedWishType != null) {
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
