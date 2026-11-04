package com.ourlist.app.ui.screens.memories

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.ui.components.EmptyStateCard
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.components.SegmentedControl
import com.ourlist.app.ui.components.StatusPill
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.HeartRed
import com.ourlist.app.ui.theme.Income
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.Review
import com.ourlist.app.ui.theme.Surface
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    viewModel: MemoriesViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val haptics = rememberHaptics()
    var itemToDoAgain by remember { mutableStateOf<BucketItem?>(null) }
    var newTargetDateInput by remember { mutableStateOf("") }

    if (itemToDoAgain != null) {
        AlertDialog(
            onDismissRequest = { itemToDoAgain = null },
            containerColor = SurfaceRaised,
            shape = RoundedCornerShape(OurListDimens.CardCorner),
            title = {
                Text(
                    text = "버킷리스트 다시 하기",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            },
            text = {
                Column {
                    Text(
                        text = "'${itemToDoAgain!!.title}'을(를) 새로운 버킷리스트로 복사합니다. 기존의 달성한 추억은 그대로 보존됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newTargetDateInput,
                        onValueChange = { newTargetDateInput = it },
                        label = { Text("새 목표 날짜 (YYYY-MM-DD, 선택)") },
                        placeholder = { Text("예: 2026-12-31") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        viewModel.doAgain(itemToDoAgain!!.id, newTargetDateInput.ifBlank { null })
                        itemToDoAgain = null
                        newTargetDateInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("다시 시작하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDoAgain = null }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

    val tabOptions = listOf<BucketCollection?>(null, BucketCollection.SHARED, BucketCollection.HEART, BucketCollection.PERSONAL)

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
                title = "달성한 추억",
                subtitle = "함께 이룬 모든 순간들의 기록",
                accentColor = Income,
            )
        }

        // 2. Segmented Scope Control
        item {
            SegmentedControl(
                options = tabOptions,
                selected = selectedTab,
                label = { option ->
                    when (option) {
                        null -> "전체"
                        BucketCollection.SHARED -> "공동"
                        BucketCollection.HEART -> "하트"
                        BucketCollection.PERSONAL -> "개인"
                        else -> option.displayName
                    }
                },
                onSelect = { viewModel.setSelectedTab(it) },
            )
        }

        // 3. Search Box
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "추억 검색 (제목, 장소, 준비물)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = TextSecondary,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "지우기",
                                modifier = Modifier.size(16.dp),
                                tint = TextSecondary,
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceRaised,
                    unfocusedContainerColor = SurfaceRaised,
                    focusedBorderColor = Income,
                    unfocusedBorderColor = Divider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )
        }

        // 4. Memory Items
        if (memories.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "아직 달성한 추억이 없어요",
                    description = "버킷리스트를 완료하면 두 사람의 소중한 달성 기록이 이곳에 차곡차곡 쌓입니다.",
                )
            }
        } else {
            items(memories, key = { it.id }) { item ->
                MemoryItemCard(
                    item = item,
                    onClick = { onNavigateToDetail(item.id) },
                    onDoAgain = { itemToDoAgain = item },
                    onRevert = {
                        haptics.tick()
                        viewModel.revertToTodo(item.id)
                    },
                )
            }
        }
    }
}

@Composable
fun MemoryItemCard(
    item: BucketItem,
    onClick: () -> Unit,
    onDoAgain: () -> Unit,
    onRevert: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completedDateStr = item.completedAt?.let {
        SimpleDateFormat("yyyy.MM.dd 달성", Locale.KOREA).format(Date(it))
    } ?: "완료"

    val collectionColor = when (item.collection) {
        BucketCollection.HEART -> HeartRed
        BucketCollection.SHARED -> Primary
        else -> Review
    }

    OurListCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = Surface,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Collection Badge + Achievement Date Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = item.collection.displayName,
                    color = collectionColor,
                )

                StatusPill(
                    text = "🎉 $completedDateStr",
                    color = Income,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            // Place
            if (!item.placeText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Income,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.placeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            // Memo
            if (!item.memo.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceRaised)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = item.memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions: 다시 하기, 미완료로 되돌리기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRevert) {
                    Icon(
                        imageVector = Icons.Filled.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = TextTertiary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("미완료로", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onDoAgain,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("다시 하기", style = MaterialTheme.typography.labelMedium, color = Primary)
                }
            }
        }
    }
}
