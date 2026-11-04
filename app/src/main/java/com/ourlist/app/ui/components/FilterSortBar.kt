package com.ourlist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface as MaterialSurface
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
import androidx.compose.ui.unit.dp
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
    selectedWishType: WishType?,
    onWishTypeSelect: (WishType?) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Primary,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    Column(modifier = modifier.fillMaxWidth()) {
        // Search & View Mode Row
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

            // View Mode Toggle Button (Circular Raised)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SurfaceRaised)
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            haptics.tick()
                            onToggleViewMode()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (viewMode == ViewMode.CARD) Icons.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = "보기 방식 전환",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Filter Chips & Sort Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OurListDimens.ScreenPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sort Dropdown Button
            Box {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSunken)
                        .clickable(role = Role.Button) {
                            haptics.tick()
                            sortMenuExpanded = true
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = accentColor,
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = sortOption.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(SurfaceRaised),
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (option == sortOption) accentColor else TextPrimary,
                                    fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                )
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

            Spacer(modifier = Modifier.width(8.dp))

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val isAllSelected = selectedWishType == null
                FilterPillChip(
                    text = "전체",
                    selected = isAllSelected,
                    accentColor = accentColor,
                    onClick = {
                        haptics.tick()
                        onWishTypeSelect(null)
                    },
                )

                WishType.values().forEach { type ->
                    val isSelected = selectedWishType == type
                    FilterPillChip(
                        text = type.label,
                        selected = isSelected,
                        accentColor = accentColor,
                        onClick = {
                            haptics.tick()
                            onWishTypeSelect(if (isSelected) null else type)
                        },
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
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
