package com.ourlist.app.ui.screens.more

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.backup.ExportManager
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.sync.SyncEngine
import com.ourlist.app.data.sync.SyncState
import com.ourlist.app.ui.navigation.AppRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "더보기",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Private Lists
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    MoreMenuItem(
                        icon = Icons.Filled.Person,
                        title = "개인 버킷리스트",
                        subtitle = "나만 볼 수 있는 로컬 목록",
                        onClick = { onNavigate(AppRoutes.PERSONAL) }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    MoreMenuItem(
                        icon = Icons.Filled.Lock,
                        title = "개인 비밀 버킷리스트",
                        subtitle = "메뉴 안에 숨겨진 나만의 비밀 목록 (잠금 없음)",
                        onClick = { onNavigate(AppRoutes.SECRET_PERSONAL) }
                    )
                }
            }

            // Section 2: Sync & Cloud
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    MoreMenuItem(
                        icon = Icons.Filled.Cloud,
                        title = "Google 계정 및 공유 Drive",
                        subtitle = if (sharedFolderId.isNotEmpty()) "연결됨: $sharedFolderId" else "공유 폴더 설정 필요",
                        onClick = { onNavigate(AppRoutes.DRIVE_SETUP) }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    syncEngine.sync()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "지금 동기화 실행", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            val lastSyncStr = if (lastSyncTime > 0) {
                                SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date(lastSyncTime))
                            } else "동기화 기록 없음"
                            Text(text = "마지막 동기화: $lastSyncStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            // Section 3: Data Management & Backup
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    MoreMenuItem(
                        icon = Icons.Filled.Security,
                        title = "암호화 백업 및 복원",
                        subtitle = "개인/비밀 목록 암호화 파일 및 자동 백업",
                        onClick = { onNavigate(AppRoutes.BACKUP) }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    MoreMenuItem(
                        icon = Icons.Filled.FileDownload,
                        title = "데이터 내보내기",
                        subtitle = "JSON, Markdown(.md), CSV 형식 지원",
                        onClick = { showExportDialog = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    MoreMenuItem(
                        icon = Icons.Filled.DeleteSweep,
                        title = "휴지통",
                        subtitle = "삭제 후 30일 동안 보관 및 복원 가능",
                        onClick = { onNavigate(AppRoutes.TRASH) }
                    )
                }
            }

            // Section 4: App Info
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Our List v1.0.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "커플 2명을 위한 서버 없는 안전한 버킷리스트 앱.\nGoogle Drive 공유 폴더를 통해 직접 동기화됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("버킷리스트 내보내기") },
        text = {
            Column {
                Text(
                    text = "내보낼 파일 형식을 선택해주세요. 평문 파일로 내보내집니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val items = db.bucketDao().getAllItems().map { it.toDomain() }
                            val md = ExportManager.exportToMarkdown("전체 목록", items)
                            withContext(Dispatchers.Main) {
                                onShare(md, "OurList_Export.md")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Markdown (.md)으로 내보내기")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val items = db.bucketDao().getAllItems().map { it.toDomain() }
                            val json = ExportManager.exportToJson(items)
                            withContext(Dispatchers.Main) {
                                onShare(json, "OurList_Export.json")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("JSON (.json)으로 내보내기")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val items = db.bucketDao().getAllItems().map { it.toDomain() }
                            val csv = ExportManager.exportToCsv(items)
                            withContext(Dispatchers.Main) {
                                onShare(csv, "OurList_Export.csv")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CSV (.csv)으로 내보내기")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
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
