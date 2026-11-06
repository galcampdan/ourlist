package com.ourlist.app.ui.screens.more

import android.app.Activity
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.sync.DriveSyncClient
import com.ourlist.app.ui.components.HomeHeader
import com.ourlist.app.ui.components.OurListCard
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Extracts Google Drive Folder ID from raw ID or full Google Drive URL.
 * Supports:
 * - https://drive.google.com/drive/folders/{id}
 * - https://drive.google.com/drive/u/0/folders/{id}
 * - https://drive.google.com/open?id={id}
 * - raw folder ID (e.g. 1jTbU5HI34WskGcDoITJlGq4P81sDjbh_)
 */
fun extractGoogleDriveFolderId(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""
    val folderRegex = Regex("""/folders/([a-zA-Z0-9_-]+)""")
    val match = folderRegex.find(trimmed)
    if (match != null) {
        return match.groupValues[1]
    }
    val idParamRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")
    val idMatch = idParamRegex.find(trimmed)
    if (idMatch != null) {
        return idMatch.groupValues[1]
    }
    return if (!trimmed.contains("/")) trimmed else trimmed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveSetupScreen(
    dataStoreManager: DataStoreManager,
    driveClient: DriveSyncClient,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    val currentFolderId by dataStoreManager.sharedFolderIdFlow.collectAsState(initial = "")
    val userEmail by dataStoreManager.userEmailFlow.collectAsState(initial = "")
    val userName by dataStoreManager.userNameFlow.collectAsState(initial = "")
    val partnerName by dataStoreManager.partnerNameFlow.collectAsState(initial = "")
    val googleAuthToken by dataStoreManager.googleAuthTokenFlow.collectAsState(initial = "")

    var inputFolderText by remember(currentFolderId) { mutableStateOf(currentFolderId) }
    val extractedFolderId = remember(inputFolderText) { extractGoogleDriveFolderId(inputFolderText) }

    var inputUserName by remember(userName) { mutableStateOf(userName) }
    var inputPartnerName by remember(partnerName) { mutableStateOf(partnerName) }

    var inputAuthToken by remember(googleAuthToken) { mutableStateOf(googleAuthToken) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showManualToken by remember { mutableStateOf(false) }

    // Google Sign-In Client configuration
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/drive"),
                Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val token = GoogleAuthUtil.getToken(
                                context,
                                account.account ?: return@launch,
                                DriveSyncClient.OAUTH_SCOPE
                            )
                            dataStoreManager.setGoogleAuthToken(token)
                            dataStoreManager.setUserInfo(
                                sub = account.id ?: "",
                                email = account.email ?: "",
                                name = account.displayName ?: "나"
                            )
                            withContext(Dispatchers.Main) {
                                haptics.confirm()
                                Toast.makeText(context, "${account.email} 로그인 및 Drive 권한 연결 완료!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "토큰 발급 실패: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google 로그인 실패: ${e.statusCode} ${e.message}", Toast.LENGTH_LONG).show()
            }
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
            bottom = 24.dp,
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
                    title = "Google Drive 연동",
                    subtitle = "서버 없는 2인 실시간 동기화",
                    accentColor = Primary,
                )
            }
        }

        // 1.5 Architecture Guide Box
        item {
            OurListCard(containerColor = SurfaceRaised) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "💡 두 사람이 연결되는 원리",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )
                    Text(
                        text = "OurList는 중앙 서버 없이 Google Drive의 동일한 '공유 폴더'를 매개체로 두 기기가 1:1로 직접 연결됩니다.\n" +
                                "1. 한 분이 Google Drive에서 폴더를 생성하고 상대방 구글 계정을 '편집자'로 초대합니다.\n" +
                                "2. 두 분 모두 앱에서 동일한 '공유 폴더 주소'를 입력하고 본인 계정으로 로그인하면 자동으로 동기화됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                    )
                }
            }
        }

        // 2. Google Account Authentication Card
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
                            Icon(imageVector = Icons.Filled.AccountCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Google 계정 로그인", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    if (userEmail.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceRaised)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Income, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = userEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Google Drive 접근 권한 연결됨", style = MaterialTheme.typography.labelSmall, color = Income)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isSigningIn = true
                                haptics.tick()
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Divider),
                        ) {
                            Text("다른 Google 계정으로 전환", color = TextSecondary)
                        }
                    } else {
                        Text(
                            text = "앱에서 Google Drive 공유 폴더를 읽고 쓰기 위해 본인의 Google 계정으로 로그인해주세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )

                        Button(
                            onClick = {
                                isSigningIn = true
                                haptics.confirm()
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Ink)
                                Spacer(Modifier.width(8.dp))
                                Text("로그인 연결 중…", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Google 계정으로 로그인", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Shared Folder Setup Card (Accepts URL or Folder ID)
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Due.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Filled.FolderShared, contentDescription = null, tint = Due, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "공유 폴더 주소 (링크)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Text(
                        text = "Google Drive에서 두 사람이 공유한 폴더의 전체 웹 주소 링크(URL) 또는 폴더 ID를 그대로 붙여넣으세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )

                    OutlinedTextField(
                        value = inputFolderText,
                        onValueChange = { inputFolderText = it },
                        placeholder = { Text("예: https://drive.google.com/drive/folders/1A2b3C... 또는 폴더 ID", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceRaised,
                            unfocusedContainerColor = SurfaceRaised,
                            focusedBorderColor = Due,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )

                    if (extractedFolderId.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceRaised)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Income, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "인식된 폴더 ID: $extractedFolderId",
                                style = MaterialTheme.typography.labelSmall,
                                color = Income,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Test Connection Button
                    OutlinedButton(
                        onClick = {
                            if (extractedFolderId.isBlank()) {
                                Toast.makeText(context, "공유 폴더 링크 또는 ID를 먼저 입력해주세요.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            isTestingConnection = true
                            haptics.tick()
                            coroutineScope.launch {
                                if (inputAuthToken.isNotBlank()) {
                                    dataStoreManager.setGoogleAuthToken(inputAuthToken.trim())
                                }
                                try {
                                    val files = driveClient.listFilesInFolder(extractedFolderId)
                                    Toast.makeText(context, "연결 성공! 공유 폴더 내 ${files.size}개 파일 확인됨.", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isTestingConnection = false
                                }
                            }
                        },
                        enabled = !isTestingConnection,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Divider),
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Google Drive 연결 확인 중…", color = TextSecondary)
                        } else {
                            Text("Google Drive 연결 상태 테스트", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // 3.5 Optional Nickname Card
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
                            Text(text = "표시용 호칭 설정 (선택)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "버킷 카드 및 활동 기록에 표시될 이름입니다 (미입력 시 기본값 사용)", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
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
                }
            }
        }

        // 4. Sync Condition Card ("변동이 있을 때 자동 동기화")
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Income.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Filled.Sync, contentDescription = null, tint = Income, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "동기화 조건: 변동 시 자동 동기화", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(
                        text = "버킷리스트의 추가, 수정, 완료, 순서 변경, 삭제 등 데이터 변동이 발생할 때마다 앱이 백그라운드에서 Google Drive 공유 폴더로 즉시 변경 사항을 전송하고 동기화합니다.\n\n또한 앱 시작 시 및 상단 동기화 버튼을 눌러 언제든 즉시 동기화할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        // 5. Advanced / Manual Token Section (Toggleable)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (showManualToken) "▼ 수동 인증 토큰 설정 접기" else "▶ 고급: OAuth Access Token 직접 입력 (테스트용)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier
                        .clickable { showManualToken = !showManualToken }
                        .padding(vertical = 4.dp),
                )
                if (showManualToken) {
                    Spacer(Modifier.height(8.dp))
                    OurListCard(containerColor = Surface) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputAuthToken,
                                onValueChange = { inputAuthToken = it },
                                placeholder = { Text("Bearer OAuth Access Token", color = TextTertiary) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(10.dp),
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
                    }
                }
            }
        }

        // 6. Save Settings Button
        item {
            Button(
                onClick = {
                    haptics.confirm()
                    val targetId = extractedFolderId.ifBlank { inputFolderText.trim() }
                    coroutineScope.launch {
                        dataStoreManager.setSharedFolderId(targetId)
                        if (inputUserName.isNotBlank()) {
                            dataStoreManager.setUserName(inputUserName.trim())
                        }
                        if (inputPartnerName.isNotBlank()) {
                            dataStoreManager.setPartnerName(inputPartnerName.trim())
                        }
                        if (inputAuthToken.isNotBlank()) {
                            dataStoreManager.setGoogleAuthToken(inputAuthToken.trim())
                        }
                        Toast.makeText(context, "Google Drive 연동 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("설정 저장 및 연동 완료", fontWeight = FontWeight.Bold)
            }
        }
    }
}
