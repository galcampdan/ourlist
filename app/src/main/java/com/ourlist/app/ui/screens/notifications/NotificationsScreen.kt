package com.ourlist.app.ui.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.InAppNotification
import com.ourlist.app.ui.components.EmptyStateCard
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
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val haptics = rememberHaptics()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = OurListDimens.ScreenPadding,
            top = 16.dp,
            end = OurListDimens.ScreenPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Header with back button
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptics.tick()
                                onNavigateBack()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "상대방 소식",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            text = "실시간 공동 작업 알림",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }

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
            }
        }

        // 2. Notifications List
        if (notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "아직 새로운 알림이 없어요",
                    description = "상대방이 공동 버킷리스트를 추가, 수정, 완료하면 이곳에 실시간으로 기록됩니다.",
                )
            }
        } else {
            items(notifications, key = { it.id }) { notification ->
                NotificationItemRow(
                    notification = notification,
                    onClick = {
                        viewModel.markAsRead(notification.id)
                        if (notification.itemId.isNotBlank()) {
                            onNavigateToDetail(notification.itemId)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: InAppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val isUnread = !notification.isRead

    val (icon, iconColor, kindText) = when (notification.actionType) {
        "ADD" -> Triple(Icons.AutoMirrored.Filled.PlaylistAdd, Primary, "새 항목")
        "UPDATE" -> Triple(Icons.Filled.Edit, Due, "수정")
        "COMPLETE" -> Triple(Icons.Filled.Celebration, Income, "완료 🎉")
        "DELETE" -> Triple(Icons.Filled.Delete, Danger, "삭제")
        else -> Triple(Icons.Filled.Edit, Review, "알림")
    }

    val timeStr = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(notification.timestamp))

    OurListCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (isUnread) SurfaceRaised else Surface,
        border = BorderStroke(1.dp, if (isUnread) iconColor.copy(alpha = 0.4f) else Divider),
        onClick = {
            haptics.tick()
            onClick()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        text = "$kindText · ${notification.collection.displayName}",
                        color = iconColor,
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = notification.itemTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary,
                )

                if (notification.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
