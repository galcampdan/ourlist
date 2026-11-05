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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ourlist.app.ui.theme.Danger
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.HeartRed
import com.ourlist.app.ui.theme.Income
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.Review
import com.ourlist.app.ui.theme.Surface
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.SurfaceSunken
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
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

    var itemToEditDate by remember { mutableStateOf<BucketItem?>(null) }
    var editDateInput by remember { mutableStateOf("") }
    var editDateError by remember { mutableStateOf<String?>(null) }

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // Do Again Dialog
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

    // Edit Achievement Date Dialog (UI-10)
    if (itemToEditDate != null) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val todayStr = dateFormat.format(Date())
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dateFormat.format(cal.time)

        AlertDialog(
            onDismissRequest = {
                itemToEditDate = null
                editDateError = null
            },
            containerColor = SurfaceRaised,
            shape = RoundedCornerShape(OurListDimens.CardCorner),
            title = {
                Text(
                    text = "달성일자 수정",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            },
            text = {
                Column {
                    Text(
                        text = "'${itemToEditDate!!.title}'의 달성 날짜를 수정합니다. 목록 정렬 및 동기화에 즉시 반영됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = editDateInput,
                        onValueChange = {
                            editDateInput = it
                            editDateError = null
                        },
                        label = { Text("달성 날짜 (YYYY-MM-DD)") },
                        placeholder = { Text("예: 2026-11-05") },
                        isError = editDateError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            focusedBorderColor = Income,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                    if (editDateError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = editDateError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = Danger,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                editDateInput = todayStr
                                editDateError = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Divider),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("오늘", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                editDateInput = yesterdayStr
                                editDateError = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Divider),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("어제", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val strictFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
                                isLenient = false
                            }
                            val parsed = strictFormat.parse(editDateInput.trim())
                            if (parsed != null) {
                                // Set to noon (12:00:00) to be robust against daylight saving / UTC boundary
                                val calSet = Calendar.getInstance().apply {
                                    time = parsed
                                    set(Calendar.HOUR_OF_DAY, 12)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                haptics.confirm()
                                viewModel.updateCompletedAt(itemToEditDate!!.id, calSet.timeInMillis)
                                itemToEditDate = null
                                editDateError = null
                            } else {
                                editDateError = "올바른 날짜 형식(YYYY-MM-DD)을 입력해주세요."
                            }
                        } catch (e: Exception) {
                            editDateError = "올바른 날짜 형식(YYYY-MM-DD)을 입력해주세요."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Income, contentColor = Ink),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("변경 완료", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        itemToEditDate = null
                        editDateError = null
                    }
                ) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

    val tabOptions = listOf<BucketCollection?>(null, BucketCollection.SHARED, BucketCollection.HEART, BucketCollection.PERSONAL)

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
                    onEditCompletedDate = {
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                        editDateInput = item.completedAt?.let { fmt.format(Date(it)) } ?: fmt.format(Date())
                        editDateError = null
                        itemToEditDate = item
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
    onEditCompletedDate: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
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
            // Header Row: Collection Badge + Achievement Date Pill (Clickable for UI-10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = item.collection.displayName,
                    color = collectionColor,
                )

                Box(
                    modifier = Modifier.clickable {
                        haptics.tick()
                        onEditCompletedDate()
                    }
                ) {
                    StatusPill(
                        text = "🎉 $completedDateStr ✎",
                        color = Income,
                    )
                }
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
                        modifier = Modifier.size(15.dp),
                        tint = TextTertiary,
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
                Text(
                    text = item.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    maxLines = 2,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Revert to TODO / Do Again
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRevert,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Divider),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "미완료로 복구",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }

                Button(
                    onClick = onDoAgain,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Income.copy(alpha = 0.15f),
                        contentColor = Income,
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Income,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "다시 하기",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Income,
                    )
                }
            }
        }
    }
}
