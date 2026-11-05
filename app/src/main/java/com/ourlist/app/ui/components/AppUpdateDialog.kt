package com.ourlist.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourlist.app.BuildConfig
import com.ourlist.app.data.update.AppUpdateInfo
import com.ourlist.app.data.update.AppUpdateManager
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Danger
import com.ourlist.app.ui.theme.Income
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.Surface
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private enum class UpdateUiState {
    IDLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    ERROR
}

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    appUpdateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    var uiState by remember { mutableStateOf(UpdateUiState.IDLE) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun startDownload() {
        haptics.confirm()
        uiState = UpdateUiState.DOWNLOADING
        downloadProgress = 0f
        errorMessage = ""

        coroutineScope.launch {
            val fileName = "OurList-${updateInfo.latestVersionName}.apk"
            val result = appUpdateManager.downloadApk(
                apkUrl = updateInfo.apkUrl,
                targetFileName = fileName,
                onProgress = { progress, downloaded, total ->
                    downloadProgress = progress
                    downloadedBytes = downloaded
                    totalBytes = total
                }
            )

            result.onSuccess { file ->
                downloadedFile = file
                uiState = UpdateUiState.READY_TO_INSTALL
                haptics.confirm()
                // Auto trigger install
                appUpdateManager.installApk(file)
            }.onFailure { error ->
                errorMessage = error.message ?: "알 수 없는 다운로드 오류"
                uiState = UpdateUiState.ERROR
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.forceUpdate && uiState != UpdateUiState.DOWNLOADING) {
                onDismiss()
            }
        },
        containerColor = Surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "새로운 버전 업데이트",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "v${updateInfo.latestVersionName} (현재: v${BuildConfig.VERSION_NAME})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = "주요 업데이트 내용",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceRaised)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                when (uiState) {
                    UpdateUiState.IDLE -> {
                        Text(
                            text = "지금 업데이트하면 항상 최신 기능과 개선된 연결 상태를 이용할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            lineHeight = 18.sp
                        )
                    }
                    UpdateUiState.DOWNLOADING -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Primary,
                                trackColor = SurfaceRaised,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "다운로드 중: ${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (totalBytes > 0) "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}" else formatBytes(downloadedBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                    UpdateUiState.READY_TO_INSTALL -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Income.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "다운로드가 완료되었습니다!\n안드로이드 패키지 설치 화면이 열리지 않으면 아래 [지금 설치하기]를 눌러주세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Income,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    UpdateUiState.ERROR -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Danger.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "다운로드 오류: $errorMessage\n외부 브라우저에서 직접 다운로드할 수도 있습니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Danger,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (uiState) {
                UpdateUiState.IDLE -> {
                    Button(
                        onClick = { startDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("지금 업데이트", fontWeight = FontWeight.Bold)
                    }
                }
                UpdateUiState.DOWNLOADING -> {
                    // Downloading, no action button needed
                }
                UpdateUiState.READY_TO_INSTALL -> {
                    Button(
                        onClick = {
                            haptics.confirm()
                            downloadedFile?.let { appUpdateManager.installApk(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Income, contentColor = Ink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("지금 설치하기", fontWeight = FontWeight.Bold)
                    }
                }
                UpdateUiState.ERROR -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (updateInfo.apkUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkUrl))
                                    context.startActivity(browserIntent)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("브라우저 열기")
                            }
                        }
                        Button(
                            onClick = { startDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("재시도", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (!updateInfo.forceUpdate && uiState != UpdateUiState.DOWNLOADING) {
                TextButton(onClick = onDismiss) {
                    Text("나중에", color = TextSecondary)
                }
            }
        }
    )
}
