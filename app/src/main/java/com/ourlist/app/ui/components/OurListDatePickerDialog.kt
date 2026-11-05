package com.ourlist.app.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Danger
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.Due
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.PrimarySoft
import com.ourlist.app.ui.theme.Surface
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Premium dark OLED calendar date picker styled for OurList.
 * Strictly guarantees valid YYYY-MM-DD output and eliminates arbitrary text input.
 */
@Composable
fun OurListDatePickerDialog(
    initialDate: String = "",
    title: String = "날짜 선택",
    isPastMode: Boolean = false, // true for anniversaries/dating date, false for bucket target dates
    allowClear: Boolean = true,
    onDateSelected: (String) -> Unit, // Returns strictly YYYY-MM-DD or "" if cleared
    onDismiss: () -> Unit
) {
    val haptics = rememberHaptics()
    val today = remember { LocalDate.now() }

    val parsedInitial = remember(initialDate) {
        try {
            if (initialDate.isNotBlank()) LocalDate.parse(initialDate.trim()) else today
        } catch (e: Exception) {
            today
        }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(if (initialDate.isNotBlank()) parsedInitial else null) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(parsedInitial)) }
    var showYearSelector by remember { mutableStateOf(false) }

    val dayNames = listOf("일", "월", "화", "수", "목", "금", "토")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // D-Day badge for selected date
                    if (selectedDate != null) {
                        val dDays = ChronoUnit.DAYS.between(today, selectedDate!!)
                        val badgeText = when {
                            isPastMode -> {
                                val pastDays = ChronoUnit.DAYS.between(selectedDate!!, today) + 1
                                if (pastDays > 0) "+${pastDays}일" else "D$dDays"
                            }
                            dDays == 0L -> "D-Day"
                            dDays > 0L -> "D-$dDays"
                            else -> "D+${-dDays}"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }

                // Selected date display
                Text(
                    text = selectedDate?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (EEEE)", Locale.KOREAN))
                        ?: "날짜를 터치하여 선택해주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedDate != null) PrimarySoft else TextTertiary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Quick Shortcut Chips
                val shortcuts = remember(isPastMode, today) {
                    if (isPastMode) {
                        listOf(
                            "오늘" to today,
                            "100일 전" to today.minusDays(99),
                            "200일 전" to today.minusDays(199),
                            "1년 전" to today.minusYears(1),
                            "2년 전" to today.minusYears(2),
                            "3년 전" to today.minusYears(3),
                        )
                    } else {
                        listOf(
                            "오늘" to today,
                            "내일" to today.plusDays(1),
                            "이번 주말" to today.plusDays(((6 - today.dayOfWeek.value % 7).toLong())),
                            "1달 뒤" to today.plusMonths(1),
                            "올해 말" to LocalDate.of(today.year, 12, 31),
                            "1년 뒤" to today.plusYears(1),
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(shortcuts) { (label, target) ->
                        val isSelected = selectedDate == target
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Primary else SurfaceRaised)
                                .clickable {
                                    haptics.tick()
                                    selectedDate = target
                                    currentYearMonth = YearMonth.from(target)
                                    showYearSelector = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Ink else TextSecondary
                            )
                        }
                    }
                }

                // 2. Month & Year Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            haptics.tick()
                            currentYearMonth = currentYearMonth.minusMonths(1)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "이전 달",
                            tint = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptics.tick()
                                showYearSelector = !showYearSelector
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${currentYearMonth.year}년 ${currentYearMonth.monthValue}월",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = if (showYearSelector) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.tick()
                            currentYearMonth = currentYearMonth.plusMonths(1)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "다음 달",
                            tint = TextPrimary
                        )
                    }
                }

                // 3. Quick Year / Month Selector Overlay
                AnimatedVisibility(visible = showYearSelector) {
                    val currentYear = currentYearMonth.year
                    val years = remember(currentYear) {
                        val start = (currentYear - 6).coerceAtLeast(1990)
                        (start..(start + 11)).toList()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceRaised)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "연도 빠른 선택",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontWeight = FontWeight.Bold
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.height(130.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(years) { yr ->
                                val isSelected = yr == currentYearMonth.year
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Primary else Surface)
                                        .clickable {
                                            haptics.tick()
                                            currentYearMonth = YearMonth.of(yr, currentYearMonth.monthValue)
                                            showYearSelector = false
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${yr}년",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Ink else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Days of Week Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayNames.forEachIndexed { index, name ->
                        val color = when (index) {
                            0 -> Danger
                            6 -> PrimarySoft
                            else -> TextTertiary
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 5. Calendar Days Grid
                val firstOfMonth = currentYearMonth.atDay(1)
                val daysInMonth = currentYearMonth.lengthOfMonth()
                // Sunday = 0, Monday = 1, ..., Saturday = 6
                val leadingBlanks = firstOfMonth.dayOfWeek.value % 7

                val totalCells = leadingBlanks + daysInMonth
                val totalRows = (totalCells + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (row in 0 until totalRows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - leadingBlanks + 1

                                if (dayNum in 1..daysInMonth) {
                                    val date = currentYearMonth.atDay(dayNum)
                                    val isSelected = selectedDate == date
                                    val isToday = date == today

                                    val dayColor = when {
                                        isSelected -> Ink
                                        col == 0 -> Danger
                                        col == 6 -> PrimarySoft
                                        else -> TextPrimary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Primary else Color.Transparent
                                            )
                                            .clickable {
                                                haptics.tick()
                                                selectedDate = date
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = dayColor
                                            )
                                            if (isToday && !isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(Primary)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.confirm()
                    if (selectedDate != null) {
                        onDateSelected(selectedDate!!.toString())
                    }
                    onDismiss()
                },
                enabled = selectedDate != null,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("선택 완료", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (allowClear && selectedDate != null) {
                    TextButton(
                        onClick = {
                            haptics.tick()
                            onDateSelected("")
                            onDismiss()
                        }
                    ) {
                        Text("삭제", color = Danger)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("취소", color = TextSecondary)
                }
            }
        }
    )
}
