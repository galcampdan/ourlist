package com.ourlist.app.ui.screens.notifications

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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.InAppNotification
import com.ourlist.app.ui.components.EmptyStateCard
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.components.StatusPill
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Danger
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.Due
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

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val haptics = rememberHaptics()

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Header
        item {
            HomeHeader(
                title = "상대방 소식",
                subtitle = "실시간 공동 작업 알림",
                accentColor = Primary,
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = {
                                haptics.confirm()
                                viewModel.markAllAsRead()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Primary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "모두 읽음",
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                },
            )
        }

        // 2. Notifications List
        if (notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "아직 상대방의 새로운 소식이 없어요",
                    description = "상대방이 버킷리스트를 추가하거나 완료, 수정하면 여기에 실시간으로 기록됩니다.",
                )
            }
        } else {
            items(notifications, key = { it.id }) { item ->
                NotificationItemCard(
                    item = item,
                    onClick = {
                        haptics.tick()
                        viewModel.markAsRead(item.id)
                        onNavigateToDetail(item.itemId)
                    },
                    onDelete = {
                        haptics.tick()
                        viewModel.deleteNotification(item.id)
                    },
                )
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    item: InAppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (item.actionType) {
        "CREATE" -> Icons.Filled.PlaylistAdd to Primary
        "COMPLETE" -> Icons.Filled.Celebration to Income
        "DELETE" -> Icons.Filled.Delete to Danger
        else -> Icons.Filled.Edit to Due
    }

    val timeStr = formatRelativeTime(item.timestamp)

    OurListCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (!item.isRead) SurfaceRaised else Surface,
        border = BorderStroke(
            1.dp,
            if (!item.isRead) Primary.copy(alpha = 0.4f) else Divider,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Action Icon Pill
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        text = item.collection.displayName,
                        color = tint,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }

            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Primary),
                )
            }
        }
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        hours < 24 -> "${hours}시간 전"
        days < 7 -> "${days}일 전"
        else -> SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(timestamp))
    }
}
