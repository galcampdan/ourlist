package com.ourlist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourlist.app.data.model.SortDirection
import com.ourlist.app.data.model.SortOption
import com.ourlist.app.data.model.ViewMode
import com.ourlist.app.data.model.WishType
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.OurListMotion
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.SurfaceSelected
import com.ourlist.app.ui.theme.SurfaceSunken
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.TextTertiary

@Composable
fun FilterSortBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedWishTypes: Set<WishType>,
    onWishTypeToggle: (WishType) -> Unit,
    sortOption: SortOption,
    sortDirection: SortDirection = SortDirection.ASC,
    onSortOptionChange: (SortOption) -> Unit,
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Primary,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    val sortLabel = when (sortOption) {
        SortOption.COST -> if (sortDirection == SortDirection.ASC) "금액 ↑" else "금액 ↓"
        SortOption.TARGET_DATE -> if (sortDirection == SortDirection.ASC) "날짜 ↑" else "날짜 ↓"
        SortOption.CREATED_BY -> if (sortDirection == SortDirection.ASC) "등록자 ↑" else "등록자 ↓"
        SortOption.CREATED_AT -> "등록일순"
        SortOption.MANUAL -> "직접 순서"
        SortOption.WISH_TYPE -> "희망 유형별"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Search & View Mode Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OurListDimens.ScreenPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                placeholder = {
                    Text(
                        "제목, 장소, 준비물 검색",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "검색",
                        modifier = Modifier.size(18.dp),
                        tint = TextSecondary,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
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
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Divider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )

            Spacer(modifier = Modifier.width(10.dp))

            // View Mode Toggle Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SurfaceRaised)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            haptics.tick()
                            onToggleViewMode()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (viewMode == ViewMode.CARD) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = "보기 방식 전환",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 2. Sort Dropdown + 3 Wish Type Chips Row (All on one screen without scrolling)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OurListDimens.ScreenPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Sort Dropdown Button
            Box {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSunken)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button
                        ) {
                            haptics.tick()
                            sortMenuExpanded = true
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = accentColor,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier
                        .background(SurfaceRaised)
                        .border(BorderStroke(1.dp, Divider), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = SurfaceRaised,
                    shadowElevation = 8.dp,
                ) {
                    val supportedOptions = listOf(
                        SortOption.TARGET_DATE to "날짜",
                        SortOption.COST to "금액",
                        SortOption.CREATED_BY to "등록자",
                        SortOption.CREATED_AT to "등록일순",
                        SortOption.MANUAL to "직접 순서",
                    )

                    supportedOptions.forEach { (option, baseName) ->
                        val isSelected = option == sortOption
                        val directionArrow = if (isSelected && option in listOf(SortOption.COST, SortOption.TARGET_DATE, SortOption.CREATED_BY)) {
                            if (sortDirection == SortDirection.ASC) " ↑" else " ↓"
                        } else ""

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$baseName$directionArrow",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) accentColor else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                haptics.tick()
                                onSortOptionChange(option)
                                sortMenuExpanded = false
                            },
                        )
                    }
                }
            }

            // 3 Wish Type Filter Chips: "꼭 하고 싶다", "언젠간", "특정 날짜" (Equally weighted, fits in 1 screen without scrolling)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val wishTypesWithLabels = listOf(
                    WishType.MUST_DO to "꼭 하고 싶다",
                    WishType.SOMEDAY to "언젠간",
                    WishType.SPECIFIC_DATE to "특정 날짜",
                )

                wishTypesWithLabels.forEach { (type, label) ->
                    val isSelected = type in selectedWishTypes
                    FilterPillChip(
                        text = label,
                        selected = isSelected,
                        accentColor = accentColor,
                        onClick = {
                            haptics.tick()
                            onWishTypeToggle(type)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPillChip(
    text: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) SurfaceSelected else SurfaceSunken,
        animationSpec = OurListMotion.snappy(),
        label = "filterPillBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) accentColor else TextSecondary,
        animationSpec = OurListMotion.snappy(),
        label = "filterPillText",
    )

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onClick = onClick
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
    }
}
