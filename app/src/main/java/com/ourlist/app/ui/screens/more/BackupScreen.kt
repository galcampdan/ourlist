package com.ourlist.app.ui.screens.more

import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.backup.BackupManager
import com.ourlist.app.data.backup.CryptoEngine
import com.ourlist.app.data.backup.RestoreMode
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.components.OurListSwitch
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Divider
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    backupManager: BackupManager,
    dataStoreManager: DataStoreManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    val recoveryCode by dataStoreManager.recoveryCodeFlow.collectAsState(initial = "")
    val autoBackupEnabled by dataStoreManager.autoBackupEnabledFlow.collectAsState(initial = false)
    val lastBackupTime by dataStoreManager.lastBackupTimeFlow.collectAsState(initial = 0L)

    var backupPassword by remember { mutableStateOf("") }
    var restoreInput by remember { mutableStateOf("") }
    var restorePasswordOrCode by remember { mutableStateOf("") }
    var restoreMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = SurfaceRaised,
            shape = RoundedCornerShape(OurListDimens.CardCorner),
            title = {
                Text(
                    text = "암호화 백업 복원",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "백업 데이터(Base64)와 백업 시 설정했던 비밀번호(또는 복구 코드)를 입력하세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    OutlinedTextField(
                        value = restoreInput,
                        onValueChange = { restoreInput = it },
                        placeholder = { Text("암호화 백업 데이터 붙여넣기", color = TextTertiary) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                    OutlinedTextField(
                        value = restorePasswordOrCode,
                        onValueChange = { restorePasswordOrCode = it },
                        placeholder = { Text("비밀번호 또는 복구 코드", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "복원 방식:", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = restoreMode == RestoreMode.MERGE,
                            onClick = { restoreMode = RestoreMode.MERGE },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Text("병합 (최신 데이터 유지)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = restoreMode == RestoreMode.OVERWRITE,
                            onClick = { restoreMode = RestoreMode.OVERWRITE },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Text("덮어쓰기 (기존 개인 데이터 교체)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreInput.isBlank() || restorePasswordOrCode.isBlank()) {
                            Toast.makeText(context, "데이터와 비밀번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        haptics.confirm()
                        coroutineScope.launch {
                            val res = backupManager.restoreEncryptedBackup(
                                restoreInput.trim(),
                                restorePasswordOrCode.trim(),
                                restoreMode
                            )
                            if (res.isSuccess) {
                                Toast.makeText(context, "${res.getOrNull()}개 항목이 성공적으로 복원되었습니다.", Toast.LENGTH_SHORT).show()
                                showRestoreDialog = false
                            } else {
                                Toast.makeText(context, "복원 실패: 비밀번호나 백업 데이터를 확인하세요.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("복원 실행", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding(),
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
                    title = "백업 및 복원",
                    subtitle = "AES-256-GCM 보안 암호화",
                    accentColor = Income,
                )
            }
        }

        // 2. Recovery Code Card
        item {
            OurListCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Surface,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Filled.Key, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "내 고유 복구 코드", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "백업 비밀번호를 잊어버렸을 때 이 복구 코드로 데이터를 복구할 수 있습니다. 안전한 곳에 따로 기록해두세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceRaised)
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = recoveryCode.ifEmpty { "발급 버튼을 눌러주세요" },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (recoveryCode.isEmpty()) TextTertiary else Primary,
                            )
                            if (recoveryCode.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        haptics.tick()
                                        clipboardManager.setText(AnnotatedString(recoveryCode))
                                        Toast.makeText(context, "복구 코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text("복사", color = Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            haptics.confirm()
                            coroutineScope.launch {
                                val newCode = CryptoEngine.generateRecoveryCode()
                                dataStoreManager.setRecoveryCode(newCode)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Divider),
                    ) {
                        Text(
                            if (recoveryCode.isEmpty()) "새 복구 코드 발급" else "복구 코드 재발급",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        // 3. Manual Encrypted Backup Card
        item {
            OurListCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Surface,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Income.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = Income, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "수동 암호화 백업", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "개인 및 비밀 버킷리스트를 AES-256-GCM으로 암호화하여 파일로 저장합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        placeholder = { Text("백업 암호화 비밀번호 입력", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceRaised,
                            unfocusedContainerColor = SurfaceRaised,
                            focusedBorderColor = Income,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (backupPassword.isBlank()) {
                                Toast.makeText(context, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            haptics.confirm()
                            coroutineScope.launch {
                                val encrypted = backupManager.createEncryptedBackup(backupPassword)
                                shareText(context, encrypted, "OurList_Encrypted_Backup.enc")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Income, contentColor = Ink),
                    ) {
                        Text("암호화 백업 생성 및 내보내기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Restore Card
        item {
            OurListCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Surface,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Review.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Filled.Restore, contentDescription = null, tint = Review, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "암호화 백업 복원", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "저장해둔 암호화 백업 데이터와 비밀번호를 입력하여 개인 및 비밀 목록을 복원합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            haptics.tick()
                            showRestoreDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Divider),
                    ) {
                        Text("백업 데이터 복원하기", color = TextPrimary)
                    }
                }
            }
        }

        // 5. Google Drive Auto Backup Card
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Google Drive 자동 백업", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        OurListSwitch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch {
                                    dataStoreManager.setAutoBackupEnabled(isChecked)
                                }
                            },
                            activeColor = Primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "변경 사항이 있을 때 하루 한 번 내 개인 Google Drive에 자동 암호화 업로드하며, 최근 7개 버전을 보관합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    if (lastBackupTime > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val backupStr = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(lastBackupTime))
                        Text(
                            text = "최근 자동 백업: $backupStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
