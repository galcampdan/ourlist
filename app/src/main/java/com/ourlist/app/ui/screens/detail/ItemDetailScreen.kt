package com.ourlist.app.ui.screens.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.WishType
import com.ourlist.app.ui.components.MoveItemDialog
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.components.OurListSwitch
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Danger
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.Due
import com.ourlist.app.ui.theme.Income
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.Surface
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.SurfaceSunken
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.TextTertiary
import com.ourlist.app.ui.theme.WishDateColor
import com.ourlist.app.ui.theme.WishMustColor
import com.ourlist.app.ui.theme.WishSomedayColor
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    viewModel: ItemDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = rememberHaptics()

    LaunchedEffect(uiState) {
        if (uiState is DetailUiState.Saved || uiState is DetailUiState.Deleted) {
            onNavigateBack()
        }
    }

    when (val state = uiState) {
        is DetailUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Ink),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
        is DetailUiState.Success -> {
            ItemDetailContent(
                item = state.item,
                initialReminderOn = state.isReminderOn,
                onSave = { title, wishType, date, cost, place, checklist, memo, reminderOn ->
                    val isNewItem = state.item.title.isEmpty()
                    val success = viewModel.saveItem(
                        title, wishType, date, cost, place, checklist, memo, reminderOn
                    )
                    if (!success) {
                        Toast.makeText(
                            context,
                            "제목은 필수이며, '구체적인 날에 하고 싶다'는 목표 날짜가 필수입니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if (isNewItem) {
                            haptics.confirm() // UI-12: 1-time subtle vibration on valid new item save
                        }
                    }
                    success
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemDetailContent(
    item: BucketItem,
    initialReminderOn: Boolean,
    onSave: (String, WishType, String?, Long?, String?, List<ChecklistItem>, String?, Boolean) -> Boolean,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onDoAgain: (String?) -> Unit,
    onDelete: () -> Unit,
    onMoveCollection: (BucketCollection) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNew = item.title.isEmpty()
    val haptics = rememberHaptics()
    val coroutineScope = rememberCoroutineScope()

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
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // BringIntoView requesters for UI-04 keyboard collision handling
    val checklistBringIntoViewRequester = remember { BringIntoViewRequester() }
    val memoBringIntoViewRequester = remember { BringIntoViewRequester() }

    // Dirty state tracking (UI-01)
    val isDirty = remember(
        title, wishType, targetDate, costText, placeText, memo, isReminderOn, checklistItems.size, checklistItems.toList()
    ) {
        if (isNew) {
            title.isNotBlank() || targetDate.isNotBlank() || costText.isNotBlank() ||
                    placeText.isNotBlank() || memo.isNotBlank() || checklistItems.isNotEmpty()
        } else {
            title != item.title ||
                    wishType != item.wishType ||
                    targetDate != (item.targetDate ?: "") ||
                    costText != (item.estimatedCostKRW?.toString() ?: "") ||
                    placeText != (item.placeText ?: "") ||
                    memo != (item.memo ?: "") ||
                    isReminderOn != initialReminderOn ||
                    checklistItems.toList() != item.checklist
        }
    }

    // System Back Handler (UI-01)
    BackHandler(enabled = isDirty) {
        showUnsavedDialog = true
    }

    // Unsaved Changes Confirmation Dialog (UI-01)
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            containerColor = SurfaceRaised,
            shape = RoundedCornerShape(OurListDimens.CardCorner),
            title = {
                Text(
                    text = "변경사항이 저장되지 않았습니다",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            },
            text = {
                Text(
                    text = "작성 중인 내용이 있습니다. 저장하지 않고 나가시겠습니까?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = costText.toLongOrNull()
                        val success = onSave(
                            title,
                            wishType,
                            targetDate.ifBlank { null },
                            cost,
                            placeText.ifBlank { null },
                            checklistItems.toList(),
                            memo.ifBlank { null },
                            isReminderOn,
                        )
                        if (success) {
                            showUnsavedDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                ) {
                    Text("저장", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showUnsavedDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("저장하지 않고 나가기", color = Danger)
                    }
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("계속 작성", color = TextSecondary)
                    }
                }
            }
        )
    }

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

    val wishTypeOptions = listOf(WishType.MUST_DO, WishType.SOMEDAY, WishType.SPECIFIC_DATE)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = OurListDimens.ScreenPadding,
            top = 16.dp,
            end = OurListDimens.ScreenPadding,
            bottom = 36.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Header with back and save button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(OurListDimens.MinTouchTarget)
                            .clip(CircleShape)
                            .background(SurfaceRaised)
                            .clickable {
                                haptics.tick()
                                if (isDirty) {
                                    showUnsavedDialog = true
                                } else {
                                    onNavigateBack()
                                }
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
                    Text(
                        text = if (isNew) "새 버킷리스트" else item.collection.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                }

                Button(
                    onClick = {
                        val cost = costText.toLongOrNull()
                        onSave(
                            title,
                            wishType,
                            targetDate.ifBlank { null },
                            cost,
                            placeText.ifBlank { null },
                            checklistItems.toList(),
                            memo.ifBlank { null },
                            isReminderOn,
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                ) {
                    Text("저장", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Title Section
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "제목",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("하고 싶은 일을 적어주세요 (필수)", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceRaised,
                            unfocusedContainerColor = SurfaceRaised,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                }
            }
        }

        // 3. Wish Type Single-line Chips (UI-02: Prevent awkward word wrapping)
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "희망 유형",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        wishTypeOptions.forEach { type ->
                            val isSelected = wishType == type
                            val typeColor = when (type) {
                                WishType.MUST_DO -> WishMustColor
                                WishType.SOMEDAY -> WishSomedayColor
                                WishType.SPECIFIC_DATE -> WishDateColor
                            }
                            MaterialSurface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptics.tick()
                                        wishType = type
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) typeColor.copy(alpha = 0.18f) else SurfaceSunken,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) typeColor else Divider,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = type.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) typeColor else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Target Date & Reminder
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (wishType == WishType.SPECIFIC_DATE) "목표 날짜 (필수)" else "목표 날짜 (선택)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (wishType == WishType.SPECIFIC_DATE) Due else Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        placeholder = { Text("예: 2026-12-31 (YYYY-MM-DD)", color = TextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TextSecondary,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceRaised,
                            unfocusedContainerColor = SurfaceRaised,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )

                    Spacer(Modifier.height(14.dp))

                    // Reminder Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceRaised)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "목표 날짜 알림 받기",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "7일 전 오전 9시 & 당일 오전 9시",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                        OurListSwitch(
                            checked = isReminderOn,
                            onCheckedChange = { isReminderOn = it },
                            activeColor = Primary,
                        )
                    }
                }
            }
        }

        // 5. Responsive Place & Estimated Cost (UI-03: Stacks vertically if narrow <360dp)
        item {
            OurListCard(containerColor = Surface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "장소 및 예상 비용",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 360.dp
                        if (isNarrow) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = placeText,
                                    onValueChange = { placeText = it },
                                    label = { Text("장소", maxLines = 1, softWrap = false) },
                                    placeholder = { Text("장소 (예: 제주도)", color = TextTertiary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Primary,
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceRaised,
                                        unfocusedContainerColor = SurfaceRaised,
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Divider,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                    ),
                                )

                                OutlinedTextField(
                                    value = costText,
                                    onValueChange = { costText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("예상 비용", maxLines = 1, softWrap = false) },
                                    placeholder = { Text("예상 비용 (원)", color = TextTertiary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Payments,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Due,
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceRaised,
                                        unfocusedContainerColor = SurfaceRaised,
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Divider,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                    ),
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedTextField(
                                    value = placeText,
                                    onValueChange = { placeText = it },
                                    label = { Text("장소", maxLines = 1, softWrap = false) },
                                    placeholder = { Text("장소 (예: 제주도)", color = TextTertiary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Primary,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceRaised,
                                        unfocusedContainerColor = SurfaceRaised,
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Divider,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                    ),
                                )

                                OutlinedTextField(
                                    value = costText,
                                    onValueChange = { costText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("예상 비용", maxLines = 1, softWrap = false) },
                                    placeholder = { Text("예상 비용 (원)", color = TextTertiary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Payments,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Due,
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceRaised,
                                        unfocusedContainerColor = SurfaceRaised,
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Divider,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Checklist (UI-04: BringIntoViewRequester on focus)
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "준비물 체크리스트",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newChecklistText,
                            onValueChange = { newChecklistText = it },
                            placeholder = { Text("준비물 추가 (예: 여권, 카메라)", color = TextTertiary) },
                            modifier = Modifier
                                .weight(1f)
                                .bringIntoViewRequester(checklistBringIntoViewRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        coroutineScope.launch { checklistBringIntoViewRequester.bringIntoView() }
                                    }
                                },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceRaised,
                                unfocusedContainerColor = SurfaceRaised,
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Divider,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceRaised)
                                .clickable {
                                    if (newChecklistText.isNotBlank()) {
                                        haptics.tick()
                                        checklistItems.add(
                                            ChecklistItem(
                                                id = UUID.randomUUID().toString(),
                                                text = newChecklistText.trim(),
                                                checked = false,
                                            )
                                        )
                                        newChecklistText = ""
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "추가",
                                tint = Primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    if (checklistItems.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        for (idx in checklistItems.indices) {
                            val checkItem = checklistItems[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checkItem.checked,
                                    onCheckedChange = { isChecked ->
                                        haptics.tick()
                                        checklistItems[idx] = checkItem.copy(checked = isChecked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Primary,
                                        uncheckedColor = TextTertiary,
                                    ),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = checkItem.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (checkItem.checked) TextTertiary else TextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        haptics.tick()
                                        checklistItems.removeAt(idx)
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "삭제",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Free Memo (UI-04: BringIntoViewRequester on focus)
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "자유 메모",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = memo,
                        onValueChange = { memo = it },
                        placeholder = { Text("두 사람만의 특별한 팁이나 계획 메모", color = TextTertiary) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(memoBringIntoViewRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    coroutineScope.launch { memoBringIntoViewRequester.bringIntoView() }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceRaised,
                            unfocusedContainerColor = SurfaceRaised,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                }
            }
        }

        // 8. Actions (Move, Complete, Delete)
        if (!isNew) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Complete / Undo
                    if (item.status == BucketStatus.TODO) {
                        Button(
                            onClick = {
                                haptics.confirm()
                                onComplete()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Income, contentColor = Ink),
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("완료하기 (추억에 보관)", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tick()
                                    onUncomplete()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Divider),
                            ) {
                                Icon(imageVector = Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                                Spacer(Modifier.width(4.dp))
                                Text("미완료로", color = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    haptics.confirm()
                                    onDoAgain(null)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                            ) {
                                Icon(imageVector = Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("다시 하기", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Move to other collection
                    OutlinedButton(
                        onClick = {
                            haptics.tick()
                            showMoveDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Divider),
                    ) {
                        Icon(imageVector = Icons.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
                        Spacer(Modifier.width(8.dp))
                        Text("다른 보관함으로 이동 (${item.collection.displayName})", color = TextPrimary)
                    }

                    // Delete to Trash
                    TextButton(
                        onClick = {
                            haptics.reject()
                            onDelete()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Danger)
                        Spacer(Modifier.width(6.dp))
                        Text("휴지통으로 삭제", color = Danger, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
