package com.ourlist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.WishType
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.Due
import com.ourlist.app.ui.theme.Income
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.OurListMotion
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.Surface
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.SurfaceSelected
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun BucketItemCard(
    item: BucketItem,
    onClick: () -> Unit,
    onCompleteToggle: () -> Unit,
    onChecklistToggle: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    myDeviceId: String = "",
    myUserSub: String = "",
    partnerName: String = "상대방",
) {
    val haptics = rememberHaptics()
    val isDone = item.isDone

    OurListCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (isDone) Surface.copy(alpha = 0.6f) else Surface,
        border = BorderStroke(1.dp, if (isDone) Divider.copy(alpha = 0.5f) else Divider),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: WishType Tag + D-Day Tag + Circular Complete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    WishTypeBadge(wishType = item.wishType)
                    if (!item.targetDate.isNullOrEmpty() && !isDone) {
                        DDayBadge(targetDate = item.targetDate)
                    }
                }

                CompleteCheckCircle(
                    isDone = isDone,
                    onToggle = {
                        haptics.confirm()
                        onCompleteToggle()
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDone) TextTertiary else TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
            )

            // Metadata: Place & Cost
            val hasPlace = !item.placeText.isNullOrEmpty()
            val hasCost = item.estimatedCostKRW != null && item.estimatedCostKRW > 0
            if (hasPlace || hasCost) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (hasPlace) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Primary,
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = item.placeText!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (hasCost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Due,
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "%,d원".format(item.estimatedCostKRW),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            // Checklist Preview
            if (item.checklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val checkedCount = item.checklist.count { it.checked }
                val totalCount = item.checklist.size
                val progress = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceRaised)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "준비물",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "$checkedCount / $totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Primary,
                        trackColor = SurfaceSelected,
                    )

                    // First 2 items preview
                    Spacer(Modifier.height(6.dp))
                    for (check in item.checklist.take(2)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { onChecklistToggle(check.id, !check.checked) },
                        ) {
                            Checkbox(
                                checked = check.checked,
                                onCheckedChange = { onChecklistToggle(check.id, it) },
                                modifier = Modifier.size(20.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Primary,
                                    uncheckedColor = TextTertiary,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = check.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (check.checked) TextTertiary else TextPrimary,
                                textDecoration = if (check.checked) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Footer: Creator & Target Date
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val creatorDisplayName = remember(item.createdBy, item.createdByName, myDeviceId, myUserSub, partnerName) {
                    item.getDisplayCreatorName(myDeviceId, myUserSub, partnerName)
                }
                Text(
                    text = "등록: $creatorDisplayName",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )

                if (!item.targetDate.isNullOrEmpty()) {
                    Text(
                        text = item.targetDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
fun BucketItemRow(
    item: BucketItem,
    onClick: () -> Unit,
    onCompleteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val isDone = item.isDone

    OurListCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (isDone) Surface.copy(alpha = 0.6f) else Surface,
        border = BorderStroke(1.dp, if (isDone) Divider.copy(alpha = 0.5f) else Divider),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompleteCheckCircle(
                isDone = isDone,
                onToggle = {
                    haptics.confirm()
                    onCompleteToggle()
                },
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    WishTypeBadge(wishType = item.wishType, isCompact = true)
                    if (!item.targetDate.isNullOrEmpty() && !isDone) {
                        DDayBadge(targetDate = item.targetDate, isCompact = true)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) TextTertiary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                )
            }

            if (item.checklist.isNotEmpty()) {
                val done = item.checklist.count { it.checked }
                StatusPill(
                    text = "$done/${item.checklist.size}",
                    color = Primary,
                )
            }
        }
    }
}

@Composable
fun CompleteCheckCircle(
    isDone: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isDone) Income else Color.Transparent,
        animationSpec = OurListMotion.snappy(),
        label = "completeCheckBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDone) Income else Divider,
        animationSpec = OurListMotion.snappy(),
        label = "completeCheckBorder",
    )

    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                role = Role.Checkbox,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isDone) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(
                        BorderStroke(2.dp, borderColor),
                        CircleShape,
                    ),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "완료됨",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun DDayBadge(
    targetDate: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val days = try {
        val target = LocalDate.parse(targetDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        ChronoUnit.DAYS.between(today, target)
    } catch (e: Exception) {
        null
    }

    if (days != null) {
        val dDayText = when {
            days == 0L -> "D-Day"
            days > 0 -> "D-$days"
            else -> "D+${-days}"
        }
        val isUrgent = days in 0..7
        StatusPill(
            text = dDayText,
            color = if (isUrgent) Primary else Due,
            modifier = modifier,
        )
    }
}
