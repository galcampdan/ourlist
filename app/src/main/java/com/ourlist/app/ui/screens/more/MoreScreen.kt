package com.ourlist.app.ui.screens.more

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.backup.ExportManager
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.sync.SyncEngine
import com.ourlist.app.data.sync.SyncState
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.components.SectionHeader
import com.ourlist.app.ui.navigation.AppRoutes
import com.ourlist.app.ui.rememberHaptics
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    dataStoreManager: DataStoreManager,
    syncEngine: SyncEngine,
    db: OurListDatabase,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    val syncState by syncEngine.syncState.collectAsState()
    val lastSyncTime by dataStoreManager.lastSyncTimeFlow.collectAsState(initial = 0L)
    val userEmail by dataStoreManager.userEmailFlow.collectAsState(initial = "")
    val sharedFolderId by dataStoreManager.sharedFolderIdFlow.collectAsState(initial = "")

    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        ExportScopeDialog(
            db = db,
            onDismiss = { showExportDialog = false },
            onShare = { text, title ->
                shareText(context, text, title)
                showExportDialog = false
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Header
        item {
            HomeHeader(
                title = "더보기",
                subtitle = "개인 보관함 및 시스템 설정",
                accentColor = Primary,
            )
        }

        // 2. Google Drive Sync Card
        item {
            OurListCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Surface,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cloud,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Drive 동기화",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                val emailStr = userEmail.ifEmpty { "계정 연결 필요" }
                                Text(
                                    text = emailStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }

                        Button(
                            onClick = {
                                haptics.tick()
                                coroutineScope.launch { syncEngine.sync() }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceRaised,
                                contentColor = Primary,
                            ),
                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            if (syncState is SyncState.Syncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Primary,
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Primary,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "지금 동기화",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val lastSyncStr = if (lastSyncTime > 0) {
                        SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.KOREA).format(Date(lastSyncTime))
                    } else "동기화 기록 없음"

                    Text(
                        text = "마지막 동기화: $lastSyncStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }
        }

        // 3. Section: 내 보관함
        item {
            Column {
                SectionHeader(title = "내 보관함")
                Spacer(Modifier.height(8.dp))
                OurListCard(containerColor = Surface) {
                    Column {
                        MoreMenuItem(
                            icon = Icons.Filled.Person,
                            iconTint = Primary,
                            title = "개인 버킷리스트",
                            subtitle = "나만 볼 수 있는 로컬 목록",
                            onClick = {
                                haptics.tick()
                                onNavigate(AppRoutes.PERSONAL)
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Divider,
                        )
                        MoreMenuItem(
                            icon = Icons.Filled.Lock,
                            iconTint = Due,
                            title = "개인 비밀 버킷리스트",
                            subtitle = "메뉴 안에 숨겨진 나만의 비밀 목록 (잠금 없음)",
                            onClick = {
                                haptics.tick()
                                onNavigate(AppRoutes.SECRET_PERSONAL)
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Divider,
                        )
                        MoreMenuItem(
                            icon = Icons.Filled.DeleteSweep,
                            iconTint = TextSecondary,
                            title = "휴지통",
                            subtitle = "삭제 후 30일 동안 보관 및 복원 가능",
                            onClick = {
                                haptics.tick()
                                onNavigate(AppRoutes.TRASH)
                            },
                        )
                    }
                }
            }
        }

        // 4. Section: 데이터 관리 및 백업
        item {
            Column {
                SectionHeader(title = "데이터 및 백업")
                Spacer(Modifier.height(8.dp))
                OurListCard(containerColor = Surface) {
                    Column {
                        MoreMenuItem(
                            icon = Icons.Filled.Cloud,
                            iconTint = Review,
                            title = "Google 계정 및 공유 Drive",
                            subtitle = if (sharedFolderId.isNotEmpty()) "연결됨: $sharedFolderId" else "공유 폴더 설정 필요",
                            onClick = {
                                haptics.tick()
                                onNavigate(AppRoutes.DRIVE_SETUP)
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Divider,
                        )
                        MoreMenuItem(
                            icon = Icons.Filled.Security,
                            iconTint = Income,
                            title = "암호화 백업 및 복원",
                            subtitle = "개인/비밀 목록 암호화 파일 및 자동 백업",
                            onClick = {
                                haptics.tick()
                                onNavigate(AppRoutes.BACKUP)
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Divider,
                        )
                        MoreMenuItem(
                            icon = Icons.Filled.FileDownload,
                            iconTint = Primary,
                            title = "데이터 내보내기",
                            subtitle = "JSON, Markdown(.md), CSV 형식 지원",
                            onClick = {
                                haptics.tick()
                                showExportDialog = true
                            },
                        )
                    }
                }
            }
        }

        // 5. Section: 앱 정보 & 보안 안내
        item {
            Column {
                SectionHeader(title = "앱 정보")
                Spacer(Modifier.height(8.dp))
                OurListCard(containerColor = Surface) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Our List v1.0.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "커플 2명을 위한 서버 없는 안전한 버킷리스트 앱.\nGoogle Drive 공유 폴더를 통해 데이터가 직접 동기화되며, 개인 데이터는 AES-256-GCM으로 안전하게 보호됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun ExportScopeDialog(
    db: OurListDatabase,
    onDismiss: () -> Unit,
    onShare: (text: String, title: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceRaised,
        shape = RoundedCornerShape(OurListDimens.CardCorner),
        title = {
            Text(
                text = "버킷리스트 내보내기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "내보낼 파일 형식을 선택해주세요. 평문 파일로 내보내집니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = {
                        haptics.confirm()
                        coroutineScope.launch {
                            val items = db.bucketDao().getAllItems().map { it.toDomain() }
                            val md = ExportManager.exportToMarkdown("전체 목록", items)
                            withContext(Dispatchers.Main) {
                                onShare(md, "OurList_Export.md")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Divider),
                ) {
                    Text("Markdown (.md)으로 내보내기", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = {
                        haptics.confirm()
                        coroutineScope.launch {
                            val items = db.bucketDao().getAllItems().map { it.toDomain() }
                            val json = ExportManager.exportToJson(items)
                            withContext(Dispatchers.Main) {
                                onShare(json, "OurList_Export.json")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Divider),
                ) {
                    Text("JSON (.json)으로 내보내기", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = {
                        haptics.confirm()
                        coroutineScope.launch {
                            val items = db.bucketDao().getAllItems().map { it.toDomain() }
                            val csv = ExportManager.exportToCsv(items)
                            withContext(Dispatchers.Main) {
                                onShare(csv, "OurList_Export.csv")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Divider),
                ) {
                    Text("CSV (.csv)으로 내보내기", color = TextPrimary)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = TextSecondary)
            }
        }
    )
}

fun shareText(context: Context, text: String, title: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, title)
    context.startActivity(shareIntent)
}
