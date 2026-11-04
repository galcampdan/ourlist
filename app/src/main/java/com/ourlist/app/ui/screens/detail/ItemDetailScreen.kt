package com.ourlist.app.ui.screens.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.WishType
import com.ourlist.app.ui.components.MoveItemDialog
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    viewModel: ItemDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is DetailUiState.Saved || uiState is DetailUiState.Deleted) {
            onNavigateBack()
        }
    }

    when (val state = uiState) {
        is DetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DetailUiState.Success -> {
            ItemDetailContent(
                item = state.item,
                initialReminderOn = state.isReminderOn,
                onSave = { title, wishType, date, cost, place, checklist, memo, reminderOn ->
                    val success = viewModel.saveItem(
                        title, wishType, date, cost, place, checklist, memo, reminderOn
                    )
                    if (!success) {
                        Toast.makeText(
                            context,
                            "제목은 필수이며, '구체적인 날에 하고 싶다'는 목표 날짜가 필수입니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onComplete = { viewModel.completeItem() },
                onUncomplete = { viewModel.uncompleteItem() },
                onDoAgain = { date -> viewModel.doAgain(date) },
                onDelete = { viewModel.moveToTrash() },
                onMoveCollection = { targetCol -> viewModel.moveCollection(targetCol) },
                onNavigateBack = onNavigateBack,
                modifier = modifier
            )
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ItemDetailContent(
    item: BucketItem,
    initialReminderOn: Boolean,
    onSave: (String, WishType, String?, Long?, String?, List<ChecklistItem>, String?, Boolean) -> Unit,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onDoAgain: (String?) -> Unit,
    onDelete: () -> Unit,
    onMoveCollection: (com.ourlist.app.data.model.BucketCollection) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(item.title) }
    var wishType by remember { mutableStateOf(item.wishType) }
    var targetDate by remember { mutableStateOf(item.targetDate ?: "") }
    var costText by remember { mutableStateOf(item.estimatedCostKRW?.toString() ?: "") }
    var placeText by remember { mutableStateOf(item.placeText ?: "") }
    var memo by remember { mutableStateOf(item.memo ?: "") }
    var isReminderOn by remember { mutableStateOf(initialReminderOn) }

    val checklistItems = remember { mutableStateListOf<ChecklistItem>().apply { addAll(item.checklist) } }
    var newChecklistText by remember { mutableStateOf("") }
    var showMoveDialog by remember { mutableStateOf(false) }

    if (showMoveDialog) {
        MoveItemDialog(
            currentCollection = item.collection,
            onMoveConfirm = { target ->
                showMoveDialog = false
                onMoveCollection(target)
            },
            onDismiss = { showMoveDialog = false }
        )
    }

    val isNew = item.title.isEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) "새 버킷리스트" else item.collection.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val cost = costText.toLongOrNull()
                            onSave(
                                title,
                                wishType,
                                targetDate,
                                cost,
                                placeText,
                                checklistItems.toList(),
                                memo,
                                isReminderOn
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("저장")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Input (Required)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("제목 (필수)") },
                placeholder = { Text("하고 싶은 일을 적어주세요") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Wish Type Selector
            Column {
                Text(
                    text = "희망 유형",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WishType.values().forEach { type ->
                        FilterChip(
                            selected = wishType == type,
                            onClick = { wishType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            // Target Date (Condition: mandatory if SPECIFIC_DATE)
            OutlinedTextField(
                value = targetDate,
                onValueChange = { targetDate = it },
                label = {
                    Text(
                        text = if (wishType == WishType.SPECIFIC_DATE) "목표 날짜 (필수: YYYY-MM-DD)" else "목표 날짜 (선택: YYYY-MM-DD)"
                    )
                },
                placeholder = { Text("2026-12-31") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Estimated Cost & Place
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("예상 비용 (원)") },
                    placeholder = { Text("50000") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = placeText,
                    onValueChange = { placeText = it },
                    label = { Text("장소") },
                    placeholder = { Text("제주도 서귀포") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Reminder Switch
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "목표 날짜 알림 받기", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "7일 전 오전 9시 & 당일 오전 9시 알림", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isReminderOn,
                        onCheckedChange = { isReminderOn = it }
                    )
                }
            }

            // Checklist Section
            Column {
                Text(
                    text = "준비물 체크리스트",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Checklist Add Row
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newChecklistText,
                        onValueChange = { newChecklistText = it },
                        placeholder = { Text("준비물 추가 (예: 카메라, 여권)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newChecklistText.isNotBlank()) {
                                checklistItems.add(
                                    ChecklistItem(
                                        id = UUID.randomUUID().toString(),
                                        text = newChecklistText.trim(),
                                        checked = false
                                    )
                                )
                                newChecklistText = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "추가", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                for (idx in checklistItems.indices) {
                    val checkItem = checklistItems[idx]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkItem.checked,
                            onCheckedChange = { isChecked ->
                                checklistItems[idx] = checkItem.copy(checked = isChecked)
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = checkItem.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { checklistItems.removeAt(idx) }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "삭제", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Free Memo
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("자유 메모") },
                placeholder = { Text("두 사람만의 특별한 팁이나 계획 메모") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Only for existing items)
            if (!isNew) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Complete / Uncomplete
                    if (item.status == BucketStatus.TODO) {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("완료하기 (추억에 보관)")
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onUncomplete,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Filled.Undo, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("미완료로")
                            }
                            Button(
                                onClick = { onDoAgain(null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Filled.Replay, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("다시 하기")
                            }
                        }
                    }

                    // Move to other collection
                    OutlinedButton(
                        onClick = { showMoveDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.DriveFileMove, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("다른 목록으로 이동 (${item.collection.displayName})")
                    }

                    // Delete to Trash
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("휴지통으로 삭제")
                    }
                }
            }
        }
    }
}
