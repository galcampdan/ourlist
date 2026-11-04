package com.ourlist.app.ui.screens.more

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.repository.BucketRepository
import com.ourlist.app.ui.components.EmptyStateCard
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.components.StatusPill
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Danger
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.Income
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.Surface
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun TrashScreen(
    bucketRepository: BucketRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trashItems by bucketRepository.getTrashItems().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    var itemToDeletePermanently by remember { mutableStateOf<BucketItem?>(null) }

    if (itemToDeletePermanently != null) {
        AlertDialog(
            onDismissRequest = { itemToDeletePermanently = null },
            containerColor = SurfaceRaised,
            shape = RoundedCornerShape(OurListDimens.CardCorner),
            title = {
                Text(
                    text = "영구 삭제",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Danger,
                )
            },
            text = {
                Text(
                    text = "'${itemToDeletePermanently!!.title}'을(를) 영구히 삭제하시겠습니까? 삭제 후에는 복원할 수 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.reject()
                        coroutineScope.launch {
                            bucketRepository.deletePermanently(itemToDeletePermanently!!.id)
                            itemToDeletePermanently = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Ink),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("영구 삭제", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeletePermanently = null }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(OurListDimens.MinTouchTarget)
                        .clip(CircleShape)
                        .background(SurfaceRaised)
                        .clickable {
                            haptics.tick()
                            onNavigateBack()
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
                HomeHeader(
                    title = "휴지통",
                    subtitle = "삭제 후 30일 동안 보관 및 복원 가능",
                    accentColor = Danger,
                )
            }
        }

        // 2. Trash Items
        if (trashItems.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "휴지통이 비어 있어요",
                    description = "삭제된 항목은 이곳에 30일간 안전하게 보관된 뒤 영구 삭제됩니다.",
                )
            }
        } else {
            items(trashItems, key = { it.id }) { item ->
                val now = System.currentTimeMillis()
                val daysElapsed = ((now - (item.deletedAt ?: now)) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
                val daysRemaining = (30 - daysElapsed).coerceAtLeast(0)

                OurListCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Surface,
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusPill(
                                text = item.collection.displayName,
                                color = Primary,
                            )
                            StatusPill(
                                text = "${daysRemaining}일 후 영구 삭제",
                                color = Danger,
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = {
                                haptics.tick()
                                itemToDeletePermanently = item
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Danger,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("영구 삭제", color = Danger, style = MaterialTheme.typography.labelSmall)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = {
                                    haptics.confirm()
                                    coroutineScope.launch {
                                        bucketRepository.restoreFromTrash(item.id)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Income.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Income,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("복원", color = Income, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
