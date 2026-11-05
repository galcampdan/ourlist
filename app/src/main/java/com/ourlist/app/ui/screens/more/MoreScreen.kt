package com.ourlist.app.ui.screens.more

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import com.ourlist.app.data.backup.SafBackupManager
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import com.ourlist.app.BuildConfig
import com.ourlist.app.data.model.DDayCalculator
import com.ourlist.app.data.sync.CoupleSyncClient
import com.ourlist.app.data.update.AppUpdateInfo
import com.ourlist.app.data.update.AppUpdateManager
import com.ourlist.app.ui.components.AppUpdateDialog
import java.time.LocalDate
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
import androidx.compose.ui.unit.sp
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
    safBackupManager: SafBackupManager,
    coupleClient: CoupleSyncClient,
    appUpdateManager: AppUpdateManager,
    db: OurListDatabase,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    val syncState by syncEngine.syncState.collectAsState()
    val lastSyncTime by dataStoreManager.lastSyncTimeFlow.collectAsState(initial = 0L)
    val coupleCode by dataStoreManager.coupleCodeFlow.collectAsState(initial = "")
    val isPartnerConnected by dataStoreManager.isPartnerConnectedFlow.collectAsState(initial = false)
    val datingStartDate by dataStoreManager.datingStartDateFlow.collectAsState(initial = "")
    val userEmail by dataStoreManager.userEmailFlow.collectAsState(initial = "")
    val sharedFolderId by dataStoreManager.sharedFolderIdFlow.collectAsState(initial = "")

    val safFolderUri by dataStoreManager.safBackupTreeUriFlow.collectAsState(initial = "")
    val safBackupEnabled by dataStoreManager.safBackupEnabledFlow.collectAsState(initial = true)
    val lastSafBackupTime by dataStoreManager.lastSafBackupTimeFlow.collectAsState(initial = 0L)
    var showSafFolderDialog by remember { mutableStateOf(false) }
    var isBackingUpSaf by remember { mutableStateOf(false) }

    var showDatingDateManageDialog by remember { mutableStateOf(false) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    fun showNativeDatePicker() {
        val parts = datingStartDate.split("-")
        val initYear = parts.getOrNull(0)?.toIntOrNull() ?: LocalDate.now().year
        val initMonth = (parts.getOrNull(1)?.toIntOrNull() ?: LocalDate.now().monthValue) - 1
        val initDay = parts.getOrNull(2)?.toIntOrNull() ?: LocalDate.now().dayOfMonth

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                coroutineScope.launch {
                    dataStoreManager.setDatingStartDate(formatted)
                    if (coupleCode.isNotBlank()) {
                        coupleClient.updateDatingStartDate(coupleCode, formatted)
                    }
                    val days = DDayCalculator.calculateDays(formatted)
                    Toast.makeText(context, "사귄 날짜가 저장되었습니다! (함께한 지 ${days}일째 💕)", Toast.LENGTH_SHORT).show()
                }
            },
            initYear,
            initMonth,
            initDay
        )
        datePickerDialog.show()
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // ignore
            }
            coroutineScope.launch {
                dataStoreManager.setSafBackupTreeUri(uri.toString())
                val res = safBackupManager.backupNow(uri.toString())
                if (res.isSuccess) {
                    Toast.makeText(context, "Google Drive 백업 폴더가 연결되었고, 즉시 백업되었습니다! (${res.getOrNull()}개 항목)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "폴더 연결됨 (백업 오류: ${res.exceptionOrNull()?.message})", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
            bottom = 100.dp,
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

        // 2. Realtime Sync Card
        item {
            OurListCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.tick()
                        onNavigate(AppRoutes.COUPLE_CONNECT)
                    },
                containerColor = Surface,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val isConnected = coupleCode.isNotEmpty() && isPartnerConnected
                        val isWaiting = coupleCode.isNotEmpty() && !isPartnerConnected

                        val statusColor = when {
                            isConnected -> Income
                            isWaiting -> Review
                            else -> Primary
                        }
                        val statusIcon = when {
                            isConnected -> Icons.Filled.Favorite
                            else -> Icons.Filled.Sync
                        }
                        val statusTitle = when {
                            isConnected -> "커플 실시간 연동 (연결됨)"
                            isWaiting -> "커플 실시간 연동 (대기 중)"
                            else -> "커플 실시간 연동 (미연결)"
                        }
                        val subText = when {
                            isConnected -> {
                                val clean = coupleCode.filter { it.isDigit() }
                                if (clean.length == 6) "🟢 연결됨: ${clean.substring(0, 3)} - ${clean.substring(3, 6)}" else "🟢 연결됨: $coupleCode"
                            }
                            isWaiting -> {
                                val clean = coupleCode.filter { it.isDigit() }
                                if (clean.length == 6) "🟡 상대방 연결 대기 중 (${clean.substring(0, 3)} - ${clean.substring(3, 6)})" else "🟡 상대방 연결 대기 중"
                            }
                            else -> "6자리 코드로 1초 연결하기 >"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = statusTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor,
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
                            icon = Icons.Filled.Favorite,
                            iconTint = when {
                                coupleCode.isNotEmpty() && isPartnerConnected -> Income
                                coupleCode.isNotEmpty() -> Review
                                else -> Primary
                            },
                            title = "커플 실시간 연동 (6자리 코드)",
                            subtitle = when {
                                coupleCode.isEmpty() -> "6자리 코드로 1초 간편 연결"
                                !isPartnerConnected -> "대기 중: ${coupleCode.chunked(3).joinToString(" - ")} (상대방 미연결)"
                                else -> "연결됨: ${coupleCode.chunked(3).joinToString(" - ")}"
                            },
                            onClick = {
                                haptics.tick()
                                onNavigate(AppRoutes.COUPLE_CONNECT)
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Divider,
                        )
                        MoreMenuItem(
                            icon = Icons.Filled.CalendarMonth,
                            iconTint = Primary,
                            title = "사귄 날짜 설정 (D-Day)",
                            subtitle = if (datingStartDate.isNotBlank()) {
                                val dDayStr = DDayCalculator.formatDDayText(datingStartDate) ?: ""
                                "$datingStartDate ($dDayStr)"
                            } else {
                                "처음 만난 날을 등록하고 D-Day를 확인하세요"
                            },
                            onClick = {
                                haptics.tick()
                                if (datingStartDate.isNotBlank()) {
                                    showDatingDateManageDialog = true
                                } else {
                                    showNativeDatePicker()
                                }
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Divider,
                        )
                        MoreMenuItem(
                            icon = Icons.Filled.FolderShared,
                            iconTint = Income,
                            title = "Google Drive 폴더 자동 백업",
                            subtitle = if (safFolderUri.isNotBlank()) "연결됨: ${safBackupManager.getFriendlyFolderName(safFolderUri)}" else "OurList 폴더 지정 시 자동 백업",
                            onClick = {
                                haptics.tick()
                                if (safFolderUri.isBlank()) {
                                    safFolderLauncher.launch(null)
                                } else {
                                    showSafFolderDialog = true
                                }
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
                                text = "Our List v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "커플 2명을 위한 실시간 동기화 버킷리스트 앱.\nFirebase 실시간 통신망과 Google Drive 자동 백업으로 데이터를 가장 안전하게 보호합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                haptics.tick()
                                isCheckingUpdate = true
                                coroutineScope.launch {
                                    val res = appUpdateManager.checkForUpdate(BuildConfig.VERSION_CODE)
                                    isCheckingUpdate = false
                                    res.onSuccess { info ->
                                        if (info != null) {
                                            availableUpdateInfo = info
                                            showUpdateDialog = true
                                        } else {
                                            Toast.makeText(context, "현재 최신 버전(v${BuildConfig.VERSION_NAME})을 사용 중입니다! 🎉", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure {
                                        Toast.makeText(context, "업데이트 확인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = Primary),
                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary)
                                Spacer(Modifier.width(8.dp))
                                Text("최신 버전 확인 중…", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("🔄 최신 버전 확인", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatingDateManageDialog) {
        val days = DDayCalculator.calculateDays(datingStartDate)
        AlertDialog(
            onDismissRequest = { showDatingDateManageDialog = false },
            title = {
                Text("사귄 날짜 관리", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "현재 설정된 날짜: $datingStartDate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "우리 함께한 지 ${days ?: 0}일째 💕",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDatingDateManageDialog = false
                        showNativeDatePicker()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink)
                ) {
                    Text("날짜 변경", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                dataStoreManager.setDatingStartDate("")
                                if (coupleCode.isNotBlank()) {
                                    coupleClient.updateDatingStartDate(coupleCode, "")
                                }
                                showDatingDateManageDialog = false
                                Toast.makeText(context, "사귄 날짜가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("삭제", color = Danger)
                    }
                    TextButton(onClick = { showDatingDateManageDialog = false }) {
                        Text("닫기", color = TextSecondary)
                    }
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showUpdateDialog && availableUpdateInfo != null) {
        AppUpdateDialog(
            updateInfo = availableUpdateInfo!!,
            appUpdateManager = appUpdateManager,
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (showSafFolderDialog) {
        val lastSafStr = if (lastSafBackupTime > 0) {
            SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.KOREA).format(Date(lastSafBackupTime))
        } else "백업 기록 없음"

        AlertDialog(
            onDismissRequest = { showSafFolderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.FolderShared, contentDescription = null, tint = Income, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Google Drive 백업 관리", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "현재 연결된 폴더:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Text(
                        text = safBackupManager.getFriendlyFolderName(safFolderUri),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Income
                    )
                    Text(
                        text = "최근 백업: $lastSafStr\n\nFirebase로 실시간 동기화될 때마다, 위 폴더의 bucketlist.json과 heart.json도 자동으로 최신 상태로 저장됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        isBackingUpSaf = true
                        coroutineScope.launch {
                            val res = safBackupManager.backupNow()
                            isBackingUpSaf = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Google Drive 백업 완료! (${res.getOrNull()}개 항목)", Toast.LENGTH_SHORT).show()
                                showSafFolderDialog = false
                            } else {
                                Toast.makeText(context, "백업 실패: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isBackingUpSaf,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink)
                ) {
                    if (isBackingUpSaf) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Ink)
                        Spacer(Modifier.width(6.dp))
                        Text("백업 중…")
                    } else {
                        Text("지금 즉시 백업", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showSafFolderDialog = false
                        safFolderLauncher.launch(null)
                    }) {
                        Text("폴더 변경", color = TextSecondary)
                    }
                    TextButton(onClick = {
                        coroutineScope.launch {
                            dataStoreManager.setSafBackupTreeUri("")
                            showSafFolderDialog = false
                            Toast.makeText(context, "백업 폴더 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("연결 해제", color = Danger)
                    }
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp)
        )
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
