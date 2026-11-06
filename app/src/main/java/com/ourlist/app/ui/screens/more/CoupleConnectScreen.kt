package com.ourlist.app.ui.screens.more

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.ourlist.app.data.model.DDayCalculator
import java.time.LocalDate
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.entity.BucketItemEntity
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.repository.BucketRepository
import com.ourlist.app.data.sync.CoupleSyncClient
import com.ourlist.app.data.sync.SyncEngine
import com.ourlist.app.data.sync.SyncState
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.components.OurListDatePickerDialog
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Composable
fun CoupleConnectScreen(
    dataStoreManager: DataStoreManager,
    coupleClient: CoupleSyncClient,
    syncEngine: SyncEngine,
    bucketRepository: BucketRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    val coupleCode by dataStoreManager.coupleCodeFlow.collectAsState(initial = "")
    val coupleProtocol by dataStoreManager.coupleProtocolFlow.collectAsState(initial = 2)
    var pendingRequests by remember { mutableStateOf<Map<String, com.ourlist.app.data.sync.JoinRequest>>(emptyMap()) }
    var confirmationCode by remember { mutableStateOf("") }
    val isPartnerConnected by dataStoreManager.isPartnerConnectedFlow.collectAsState(initial = false)
    val datingStartDate by dataStoreManager.datingStartDateFlow.collectAsState(initial = "")
    val userName by dataStoreManager.userNameFlow.collectAsState(initial = "")
    val partnerName by dataStoreManager.partnerNameFlow.collectAsState(initial = "")
    val lastSyncTime by dataStoreManager.lastSyncTimeFlow.collectAsState(initial = 0L)
    val syncState by syncEngine.syncState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: 코드 생성, 1: 코드 입력
    var inputCode by remember { mutableStateOf("") }
    var inputUserName by remember(userName) { mutableStateOf(userName) }
    var inputPartnerName by remember(partnerName) { mutableStateOf(partnerName) }

    var isOperating by remember { mutableStateOf(false) }
    var generatedCodeDisplay by remember { mutableStateOf("") }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showDatingDatePickerDialog by remember { mutableStateOf(false) }

    // Active polling to detect when partner joins
    LaunchedEffect(coupleCode, coupleProtocol) {
        pendingRequests = emptyMap()
        confirmationCode = ""
        if (coupleCode.isBlank() || coupleProtocol < 2) return@LaunchedEffect
        while (true) {
            try {
                val meta = coupleClient.fetchMeta(coupleCode)
                if (meta != null) {
                    val both = meta.isBothConnected
                    if (meta.ownerUid == coupleClient.currentUid() && !both && !meta.closed) {
                        pendingRequests = coupleClient.pendingJoinRequests(coupleCode)
                    } else {
                        pendingRequests = emptyMap()
                    }
                    if (both && !dataStoreManager.isPartnerConnectedFlow.first()) syncEngine.sync()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: com.ourlist.app.data.sync.SyncHttpException) {
                if (e.status == 401 || e.status == 403) {
                    try { confirmationCode = coupleClient.ownJoinRequest(coupleCode)?.confirmationCode.orEmpty() } catch (_: java.io.IOException) { }
                }
            } catch (_: Exception) { }
            delay(2500)
        }
    }

    fun formatCode(code: String): String {
        val clean = code.filter { it.isDigit() }
        return if (clean.length == 6) {
            "${clean.substring(0, 3)} - ${clean.substring(3, 6)}"
        } else {
            clean
        }
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
            bottom = 32.dp,
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                HomeHeader(
                    title = "커플 실시간 연동",
                    subtitle = "코드와 확인번호로 안전하게 연결",
                    accentColor = Primary,
                )
            }
        }

        if (coupleCode.isNotBlank() && coupleProtocol < 2) {
            item {
                OurListCard(containerColor = Surface) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("보안 연결로 전환", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("기존 공동·하트 목록은 이 휴대폰에 보관됩니다. 두 휴대폰을 모두 업데이트한 뒤 한 명이 새 코드를 만들고 상대방의 연결 요청을 승인해주세요.", color = TextSecondary)
                        Button(onClick = { coroutineScope.launch {
                            dataStoreManager.setPartnerConnected(false)
                            dataStoreManager.setCoupleCode("")
                            dataStoreManager.setCoupleProtocol(2)
                        } }) { Text("목록을 유지하고 새 연결 시작") }
                    }
                }
            }
            return@LazyColumn
        }
        if (coupleCode.isNotBlank() && !isPartnerConnected) {
            if (confirmationCode.isNotBlank()) item {
                OurListCard(containerColor = Surface) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("상대방의 연결 승인 대기", color = TextPrimary)
                        Text("확인번호: $confirmationCode", style = MaterialTheme.typography.headlineSmall, color = Primary)
                        Text("상대방 휴대폰의 요청 화면에도 같은 번호가 표시되는지 확인한 뒤 승인해주세요. 승인 전에는 목록이 공유되지 않습니다.", color = TextSecondary)
                    }
                }
            }
            pendingRequests.forEach { (uid, request) -> item(key = uid) {
                OurListCard(containerColor = Surface) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${request.userName}님의 연결 요청", color = TextPrimary)
                        Text("확인번호: ${request.confirmationCode}", style = MaterialTheme.typography.titleLarge, color = Primary)
                        Text("상대방 휴대폰의 확인번호와 일치할 때 승인해주세요.", color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !isOperating, onClick = { coroutineScope.launch {
                                isOperating = true
                                try { coupleClient.approveJoin(coupleCode, uid); pendingRequests = emptyMap() }
                                catch (e: Exception) { Toast.makeText(context, "승인 실패: ${e.message}", Toast.LENGTH_LONG).show() }
                                finally { isOperating = false }
                            } }) { Text("번호 확인 후 승인") }
                            OutlinedButton(enabled = !isOperating, onClick = { coroutineScope.launch {
                                try { coupleClient.rejectJoin(coupleCode, uid); pendingRequests = pendingRequests - uid }
                                catch (e: Exception) { Toast.makeText(context, "거절 실패: ${e.message}", Toast.LENGTH_LONG).show() }
                            } }) { Text("거절") }
                        }
                    }
                }
            } }
        }
        if (coupleCode.isNotBlank()) {
            if (!isPartnerConnected) {
                // ==========================================
                // STATUS: WAITING FOR PARTNER
                // ==========================================
                item {
                    OurListCard(containerColor = Surface) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Review.copy(alpha = 0.15f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 2.dp,
                                    color = Review
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "상대방 연결 대기 중",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Review
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "우리 둘만의 커플 코드",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatCode(coupleCode),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        letterSpacing = 6.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (confirmationCode.isNotBlank()) "코드를 만든 상대방에게 위 확인번호를 보여주고 승인을 기다려주세요." else "상대방 스마트폰의 [코드 입력하기] 탭에\n위 6자리 숫자를 입력한 뒤 확인번호를 대조하고 연결을 승인합니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }

                            // Copy & Share Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptics.tick()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("OurList Couple Code", coupleCode))
                                        Toast.makeText(context, "커플 코드가 복사되었습니다: $coupleCode", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = TextPrimary),
                                    border = BorderStroke(1.dp, Divider),
                                ) {
                                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("코드 복사", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        haptics.tick()
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "💖 OurList 커플 연결 코드: $coupleCode\n앱의 [커플 실시간 연동]에서 이 코드를 입력한 뒤 확인번호를 확인하고 연결을 승인해주세요.")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "커플 코드 공유"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                                ) {
                                    Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("카톡 공유", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Cancel waiting button
                            OutlinedButton(
                                onClick = {
                                    haptics.tick()
                                    showDisconnectDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Divider),
                            ) {
                                Text("대기 취소하고 다른 코드 입력하기", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // STATUS: ALREADY CONNECTED
                // ==========================================

                // 2. Connection Status Card
                item {
                    OurListCard(containerColor = Surface) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Income.copy(alpha = 0.15f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Income)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "상대방과 실시간 연결됨",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Income
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "우리 둘만의 커플 코드",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatCode(coupleCode),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        letterSpacing = 4.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }

                            // Copy & Share Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        haptics.tick()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("OurList Couple Code", coupleCode))
                                        Toast.makeText(context, "커플 코드가 복사되었습니다: $coupleCode", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Divider),
                                ) {
                                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("코드 복사", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptics.tick()
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "💖 OurList 커플 연결 코드: $coupleCode\n앱에서 이 코드를 입력한 뒤 확인번호를 확인하고 연결을 승인해주세요.")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "커플 코드 공유"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Divider),
                                ) {
                                    Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("공유하기", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            // Last Sync Info
                            val lastSyncStr = if (lastSyncTime > 0) {
                                SimpleDateFormat("yyyy.MM.dd a h:mm:ss", Locale.KOREA).format(Date(lastSyncTime))
                            } else "아직 동기화 전"

                            Text(
                                text = "최근 동기화: $lastSyncStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                        }
                    }
                }

                // 3. Actions Card: Sync Now & Disconnect
                item {
                    OurListCard(containerColor = Surface) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "동기화 관리",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = "버킷리스트를 추가, 수정, 완료, 삭제할 때마다 백그라운드에서 실시간으로 자동 동기화됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = {
                                    haptics.confirm()
                                    coroutineScope.launch {
                                        val result = syncEngine.sync()
                                        when (result) {
                                            is SyncState.Success -> {
                                                Toast.makeText(context, "동기화 완료! (${result.syncedCount}개 항목 반영)", Toast.LENGTH_SHORT).show()
                                            }
                                            is SyncState.Error -> {
                                                Toast.makeText(context, "동기화 오류: ${result.message}", Toast.LENGTH_LONG).show()
                                            }
                                            else -> {}
                                        }
                                    }
                                },
                                enabled = syncState !is SyncState.Syncing,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                            ) {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Ink)
                                    Spacer(Modifier.width(8.dp))
                                    Text("동기화 중…", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("지금 즉시 동기화", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    haptics.tick()
                                    showDisconnectDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                            ) {
                                Icon(imageVector = Icons.Filled.LinkOff, contentDescription = null, tint = Danger, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("연결 해제하기", color = Danger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Nickname Settings Card
            item {
                OurListCard(containerColor = Surface) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "화면 표시 호칭 설정", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "버킷 카드 및 활동 기록에 표시되는 이름입니다", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = inputUserName,
                                onValueChange = { inputUserName = it },
                                label = { Text("내 호칭", color = TextTertiary) },
                                placeholder = { Text("나", color = TextTertiary) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceRaised,
                                    unfocusedContainerColor = SurfaceRaised,
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Divider,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                            )
                            OutlinedTextField(
                                value = inputPartnerName,
                                onValueChange = { inputPartnerName = it },
                                label = { Text("상대방 호칭", color = TextTertiary) },
                                placeholder = { Text("상대방", color = TextTertiary) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceRaised,
                                    unfocusedContainerColor = SurfaceRaised,
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Divider,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                            )
                        }

                        Button(
                            onClick = {
                                haptics.confirm()
                                coroutineScope.launch {
                                    if (inputUserName.isNotBlank()) dataStoreManager.setUserName(inputUserName.trim())
                                    if (inputPartnerName.isNotBlank()) dataStoreManager.setPartnerName(inputPartnerName.trim())
                                    Toast.makeText(context, "호칭이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = Primary),
                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
                        ) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("호칭 변경 저장", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 4.dp))

                        // Dating start date D-Day row
                        val dDayStr = DDayCalculator.formatDDayText(datingStartDate)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceRaised)
                                .clickable {
                                    haptics.tick()
                                    showDatingDatePickerDialog = true
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "💖 사귄 날짜 (D-Day)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (datingStartDate.isNotBlank()) "$datingStartDate ($dDayStr)" else "터치하여 사귄 날짜를 설정하세요",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (datingStartDate.isNotBlank()) Income else TextTertiary
                                )
                            }
                            Text(
                                text = if (datingStartDate.isNotBlank()) "변경" else "설정",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // STATUS: NOT CONNECTED (Tabs: Create / Join)
            // ==========================================

            // 2. Guide Banner
            item {
                OurListCard(containerColor = SurfaceRaised) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "복잡한 구글 콘솔 없이, 코드와 확인번호로 안전하게 연결!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                            )
                        }
                        Text(
                            text = "한 분이 [새 코드 만들기]로 6자리 코드를 발급받고, 상대방이 [코드 입력하기]에서 숫자를 입력하고 두 휴대폰의 확인번호를 대조해 승인하면 연동됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            // 3. Tab Selector
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Surface,
                    contentColor = Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Primary,
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            haptics.tick()
                            selectedTab = 0
                        },
                        text = {
                            Text(
                                text = "1. 새 코드 만들기",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Primary else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            haptics.tick()
                            selectedTab = 1
                        },
                        text = {
                            Text(
                                text = "2. 코드 입력하기",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Primary else TextSecondary
                            )
                        }
                    )
                }
            }

            // 4. Tab Content: CREATE ROOM (Tab 0)
            if (selectedTab == 0) {
                item {
                    OurListCard(containerColor = Surface) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "내 스마트폰에서 새로운 커플 코드를 생성합니다.\n현재 작성된 버킷리스트가 클라우드로 복제되어 함께 공유됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            if (generatedCodeDisplay.isEmpty()) {
                                Button(
                                    onClick = {
                                        haptics.confirm()
                                        isOperating = true
                                        coroutineScope.launch {
                                            try {
                                                var randomCode = ""
                                                val myDeviceId = dataStoreManager.getOrCreateDeviceId()
                                                val myName = userName.ifBlank { "나" }

                                                val sharedItems = bucketRepository.getAllInCollectionDirect(BucketCollection.SHARED)
                                                val heartItems = bucketRepository.getAllInCollectionDirect(BucketCollection.HEART)

                                                var created = false
                                                repeat(10) {
                                                    if (!created) {
                                                        randomCode = (100000 + java.security.SecureRandom().nextInt(900000)).toString()
                                                        try {
                                                            coupleClient.createRoom(randomCode, myDeviceId, myName, sharedItems, heartItems)
                                                            created = true
                                                        } catch (_: com.ourlist.app.data.sync.RoomCodeInUseException) { }
                                                    }
                                                }
                                                check(created) { "코드를 발급하지 못했습니다. 잠시 후 다시 시도해주세요." }

                                                dataStoreManager.setCoupleProtocol(2)
                                                dataStoreManager.setCoupleCode(randomCode)
                                                dataStoreManager.setPartnerConnected(false)
                                                dataStoreManager.setLastSyncTime(System.currentTimeMillis())
                                                generatedCodeDisplay = randomCode

                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "커플 코드 발급 완료! ($randomCode)", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "코드 생성 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            } finally {
                                                isOperating = false
                                            }
                                        }
                                    },
                                    enabled = !isOperating,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                                ) {
                                    if (isOperating) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Ink)
                                        Spacer(Modifier.width(8.dp))
                                        Text("코드 생성 및 방 개설 중…", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(imageVector = Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("💖 새 커플 코드 발급하기", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "발급된 커플 코드",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = formatCode(generatedCodeDisplay),
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            letterSpacing = 6.sp,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "상대방이 앱에서 위 6자리 숫자를 입력하면\n자동으로 연결이 완료됩니다!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Income,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            haptics.tick()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("OurList Couple Code", generatedCodeDisplay))
                                            Toast.makeText(context, "커플 코드가 복사되었습니다: $generatedCodeDisplay", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = TextPrimary),
                                        border = BorderStroke(1.dp, Divider)
                                    ) {
                                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("코드 복사", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            haptics.tick()
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "💖 OurList 커플 연결 코드: $generatedCodeDisplay\n앱의 [커플 실시간 연동]에서 이 코드를 입력한 뒤 확인번호를 확인하고 연결을 승인해주세요.")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "커플 코드 전달하기"))
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("카톡 공유", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Tab Content: JOIN ROOM (Tab 1)
            if (selectedTab == 1) {
                item {
                    OurListCard(containerColor = Surface) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "상대방에게 전달받은 6자리 숫자를 입력하세요.\n상대방이 확인번호를 대조하고 승인하면 공동·하트 목록이 동기화됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { newValue ->
                                    val digitsOnly = newValue.filter { it.isDigit() }
                                    if (digitsOnly.length <= 6) {
                                        inputCode = digitsOnly
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "0 0 0 0 0 0",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            letterSpacing = 6.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextTertiary
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    letterSpacing = 6.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Primary,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceRaised,
                                    unfocusedContainerColor = SurfaceRaised,
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Divider,
                                ),
                            )

                            Button(
                                onClick = {
                                    val clean = inputCode.filter { it.isDigit() }
                                    if (clean.length != 6) {
                                        Toast.makeText(context, "6자리 숫자를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    haptics.confirm()
                                    isOperating = true
                                    coroutineScope.launch {
                                        try {
                                            val myDeviceId = dataStoreManager.getOrCreateDeviceId()
                                            val myName = userName.ifBlank { "상대방" }

                                            val roomData = coupleClient.joinRoom(
                                                roomCode = clean,
                                                myDeviceId = myDeviceId,
                                                myName = myName
                                            )

                                            // Merge initial room state into Room DB
                                            bucketRepository.importSharedSnapshot(roomData)

                                            dataStoreManager.setCoupleProtocol(2)
                                            dataStoreManager.setCoupleCode(clean)
                                            dataStoreManager.setPartnerConnected(false)
                                            dataStoreManager.setLastSyncTime(System.currentTimeMillis())

                                            // Trigger initial full sync
                                            val result = syncEngine.sync()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, if (result is SyncState.Success) "연결 및 동기화가 완료되었습니다." else "연결되었습니다. 동기화 오류를 확인하고 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: com.ourlist.app.data.sync.ApprovalRequiredException) {
                                            dataStoreManager.setCoupleProtocol(2)
                                            dataStoreManager.setCoupleCode(clean)
                                            dataStoreManager.setPartnerConnected(false)
                                            confirmationCode = coupleClient.ownJoinRequest(clean)?.confirmationCode.orEmpty()
                                            Toast.makeText(context, "연결 요청을 보냈습니다. 상대방에게 확인번호를 보여주세요.", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        } finally {
                                            isOperating = false
                                        }
                                    }
                                },
                                enabled = !isOperating && inputCode.filter { it.isDigit() }.length == 6,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                            ) {
                                if (isOperating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Ink)
                                    Spacer(Modifier.width(8.dp))
                                    Text("연결 및 데이터 수신 중…", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("💖 지금 연결하기", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }


    }

    // Disconnect Confirmation Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = {
                Text(
                    text = if (isPartnerConnected) "커플 연동 해제" else "연결 대기 취소",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isPartnerConnected)
                        "현재 연결된 커플 코드를 해제하시겠습니까?\n\n내 폰에 저장된 기존 버킷리스트 데이터는 그대로 유지되며, 이후 작성되는 내용은 더 이상 상대방과 공유되지 않습니다."
                    else
                        "커플 코드 대기를 취소하시겠습니까?\n취소 후 다른 코드를 입력하거나 새 코드를 발급받을 수 있습니다.",
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        coroutineScope.launch {
                            try {
                                coupleClient.closeRoom(coupleCode, dataStoreManager.getOrCreateDeviceId())
                                dataStoreManager.setCoupleCode("")
                                dataStoreManager.setPartnerConnected(false)
                                generatedCodeDisplay = ""
                                showDisconnectDialog = false
                                Toast.makeText(context, "서버 연결을 해제했습니다. 로컬 목록은 보관됩니다.", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "해제 실패: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Ink)
                ) {
                    Text(if (isPartnerConnected) "해제하기" else "대기 취소", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDatingDatePickerDialog) {
        OurListDatePickerDialog(
            initialDate = datingStartDate,
            title = "사귄 날짜 설정 💕",
            isPastMode = true,
            allowClear = true,
            onDateSelected = { selectedDate ->
                showDatingDatePickerDialog = false
                coroutineScope.launch {
                    dataStoreManager.setDatingStartDate(selectedDate)
                    if (coupleCode.isNotBlank()) {
                        try { coupleClient.updateDatingStartDate(coupleCode, selectedDate) }
                        catch (e: java.io.IOException) { Toast.makeText(context, "날짜는 이 휴대폰에 저장됐습니다. 서버 반영 실패: ${e.message}", Toast.LENGTH_LONG).show() }
                    }
                    if (selectedDate.isNotBlank()) {
                        val days = DDayCalculator.calculateDays(selectedDate)
                        Toast.makeText(context, "사귄 날짜가 저장되었습니다! (함께한 지 ${days}일째 💕)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "사귄 날짜가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showDatingDatePickerDialog = false }
        )
    }
}
