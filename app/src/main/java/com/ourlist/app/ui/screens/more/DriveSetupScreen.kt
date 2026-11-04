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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
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
import kotlinx.coroutines.launch

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
    val partnerEmail by dataStoreManager.partnerEmailFlow.collectAsState(initial = "")
    val partnerName by dataStoreManager.partnerNameFlow.collectAsState(initial = "")
    val googleAuthToken by dataStoreManager.googleAuthTokenFlow.collectAsState(initial = "")

    var inputFolderId by remember(currentFolderId) { mutableStateOf(currentFolderId) }
    var inputUserEmail by remember(userEmail) { mutableStateOf(userEmail) }
    var inputUserName by remember(userName) { mutableStateOf(userName) }
    var inputPartnerEmail by remember(partnerEmail) { mutableStateOf(partnerEmail) }
    var inputPartnerName by remember(partnerName) { mutableStateOf(partnerName) }
    var inputAuthToken by remember(googleAuthToken) { mutableStateOf(googleAuthToken) }
    var isTestingConnection by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink),
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
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                HomeHeader(
                    title = "Google Drive 연동",
                    subtitle = "서버 없는 2인 P2P 동기화",
                    accentColor = Primary,
                )
            }
        }

        // 2. Guide Intro Card
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Filled.FolderShared, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "커플 2인 동기화 안내", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "별도의 운영 서버 없이 두 사람의 Google Drive를 직접 연결합니다.\n\n" +
                                "1단계: 한 사람이 Google Drive에 'OurList' 폴더를 생성합니다.\n" +
                                "2단계: 폴더 공유에서 상대방 Google 계정에 '편집자(writer)' 권한을 부여합니다.\n" +
                                "3단계: 두 사람 모두 해당 폴더 ID를 아래에 입력하고 저장합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        // 3. My Profile Section
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        Text(text = "내 계정 정보", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    OutlinedTextField(
                        value = inputUserName,
                        onValueChange = { inputUserName = it },
                        placeholder = { Text("내 닉네임 (표시 이름)", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
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
                        value = inputUserEmail,
                        onValueChange = { inputUserEmail = it },
                        placeholder = { Text("내 Google 이메일", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
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

        // 4. Partner Profile Section
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Review.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Filled.Group, contentDescription = null, tint = Review, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "상대방 정보", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    OutlinedTextField(
                        value = inputPartnerName,
                        onValueChange = { inputPartnerName = it },
                        placeholder = { Text("상대방 닉네임", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceRaised,
                            unfocusedContainerColor = SurfaceRaised,
                            focusedBorderColor = Review,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                    OutlinedTextField(
                        value = inputPartnerEmail,
                        onValueChange = { inputPartnerEmail = it },
                        placeholder = { Text("상대방 Google 이메일", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceRaised,
                            unfocusedContainerColor = SurfaceRaised,
                            focusedBorderColor = Review,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                }
            }
        }

        // 5. Folder ID & Token Section
        item {
            OurListCard(containerColor = Surface) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Due.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Filled.Cloud, contentDescription = null, tint = Due, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "공유 폴더 및 인증 설정", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    OutlinedTextField(
                        value = inputFolderId,
                        onValueChange = { inputFolderId = it },
                        placeholder = { Text("Google Drive 공유 폴더 ID (예: 1A2b3C_4D...)", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                    Text(
                        text = "Google Drive 웹에서 해당 폴더를 열었을 때 주소창 url의 /folders/ 뒤에 오는 문자열입니다.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = inputAuthToken,
                        onValueChange = { inputAuthToken = it },
                        placeholder = { Text("Google OAuth Access Token (선택/테스트용)", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
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

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            if (inputFolderId.isBlank()) {
                                Toast.makeText(context, "폴더 ID를 먼저 입력해주세요.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            isTestingConnection = true
                            haptics.tick()
                            coroutineScope.launch {
                                dataStoreManager.setGoogleAuthToken(inputAuthToken.trim())
                                try {
                                    val files = driveClient.listFilesInFolder(inputFolderId.trim())
                                    Toast.makeText(context, "Google Drive 연결 성공! 폴더 내 ${files.size}개 파일 확인됨.", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Google Drive 연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
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
                            Text("연결 확인 중…", color = TextSecondary)
                        } else {
                            Text("Google Drive API 연결 테스트", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // 6. Save Button
        item {
            Button(
                onClick = {
                    haptics.confirm()
                    coroutineScope.launch {
                        dataStoreManager.setUserInfo(
                            sub = inputUserEmail,
                            email = inputUserEmail.trim(),
                            name = inputUserName.trim().ifEmpty { "나" }
                        )
                        dataStoreManager.setPartnerInfo(
                            sub = inputPartnerEmail,
                            email = inputPartnerEmail.trim(),
                            name = inputPartnerName.trim().ifEmpty { "상대방" }
                        )
                        dataStoreManager.setSharedFolderId(inputFolderId.trim())
                        dataStoreManager.setGoogleAuthToken(inputAuthToken.trim())
                        Toast.makeText(context, "공유 및 인증 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("설정 저장 및 연결", fontWeight = FontWeight.Bold)
            }
        }
    }
}
