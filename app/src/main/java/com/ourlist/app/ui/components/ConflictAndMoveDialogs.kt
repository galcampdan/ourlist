package com.ourlist.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.ConflictRecord
import com.ourlist.app.data.model.ConflictType

@Composable
fun ConflictResolutionDialog(
    conflict: ConflictRecord,
    onResolveField: (chosenValue: String?) -> Unit,
    onResolveDelete: (keepItem: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚡ 변경 내용 충돌 해결",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "'${conflict.itemTitle}' 항목에서 동시 수정이 감지되었습니다. 유지할 내용을 선택해주세요.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (conflict.conflictType == ConflictType.FIELD_CONFLICT) {
                    val fieldName = when (conflict.fieldPath) {
                        "title" -> "제목"
                        "targetDate" -> "목표 날짜"
                        "placeText" -> "장소"
                        "memo" -> "메모"
                        "estimatedCostKRW" -> "예상 비용"
                        "wishType" -> "희망 유형"
                        else -> conflict.fieldPath ?: "정보"
                    }

                    Text(
                        text = "수정 항목: $fieldName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Option A: Local
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "내 수정 내용:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = conflict.localValue ?: "(비어 있음)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onResolveField(conflict.localValue) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("내 값으로 유지하기")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option B: Remote
                    val partnerName = conflict.remoteActorName.ifEmpty { "상대방" }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "${partnerName}님의 수정 내용:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = conflict.remoteValue ?: "(비어 있음)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onResolveField(conflict.remoteValue) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${partnerName}님의 값으로 반영하기")
                            }
                        }
                    }
                } else {
                    // Delete vs Update
                    val partnerName = conflict.remoteActorName.ifEmpty { "상대방" }
                    Text(
                        text = "${partnerName}님이 항목을 삭제했고, 내가 내용을 수정했습니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onResolveDelete(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("내 수정본 유지하기 (복원)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onResolveDelete(false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${partnerName}님의 삭제 수용하기 (삭제)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에 해결")
            }
        }
    )
}

@Composable
fun MoveItemDialog(
    currentCollection: BucketCollection,
    onMoveConfirm: (BucketCollection) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCollection by remember {
        mutableStateOf(
            BucketCollection.values().firstOrNull { it != currentCollection } ?: BucketCollection.SHARED
        )
    }

    val isMovingToLocal = (currentCollection == BucketCollection.SHARED || currentCollection == BucketCollection.HEART) &&
            (selectedCollection == BucketCollection.PERSONAL || selectedCollection == BucketCollection.SECRET_LOCAL)

    val isMovingToShared = (currentCollection == BucketCollection.PERSONAL || currentCollection == BucketCollection.SECRET_LOCAL) &&
            (selectedCollection == BucketCollection.SHARED || selectedCollection == BucketCollection.HEART)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("다른 목록으로 이동")
        },
        text = {
            Column {
                Text(
                    text = "이동할 대상 목록을 선택하세요:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))

                BucketCollection.values().forEach { col ->
                    if (col != currentCollection) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedCollection == col,
                                onClick = { selectedCollection = col }
                            )
                            Text(
                                text = col.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isMovingToLocal) {
                    Text(
                        text = "⚠️ 공동 목록에서 개인 목록으로 이동하면 상대방의 화면에서 항목이 사라집니다. 단, 상대 기기에 이미 다운로드되었던 과거 사본이나 백업 파일은 원격 삭제되지 않습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isMovingToShared) {
                    Text(
                        text = "ℹ️ 개인 목록에서 공동 목록으로 이동하면 동기화 후 상대방도 이 버킷리스트를 함께 보고 수정할 수 있게 됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onMoveConfirm(selectedCollection) }) {
                Text("이동")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
