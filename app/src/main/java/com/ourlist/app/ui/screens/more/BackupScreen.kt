package com.ourlist.app.ui.screens.more

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.ourlist.app.data.backup.*
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.sync.DriveFileMetadata
import com.ourlist.app.ui.components.OurListCard
import com.ourlist.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private suspend fun readBackupFile(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        val buffer = CharArray(8192)
        val text = StringBuilder()
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            require(text.length + count <= 20 * 1024 * 1024) { "백업 파일은 20MB까지 읽을 수 있습니다." }
            text.append(buffer, 0, count)
        }
        text.toString()
    } ?: error("백업 파일을 열 수 없습니다.")
}

@Composable
private fun BackupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    OurListCard(modifier=Modifier.fillMaxWidth(),containerColor=Surface) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,color=TextPrimary)
            content()
        }
    }
}

@Composable
private fun SecretInput(value:String,onChange:(String)->Unit,label:String) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},singleLine=true,
        modifier=Modifier.fillMaxWidth(),visualTransformation=if(visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Password),
        trailingIcon={TextButton(onClick={visible=!visible}) {Text(if(visible) "숨기기" else "표시")}})
}

@Composable
fun BackupScreen(backupManager:BackupManager,dataStoreManager:DataStoreManager,onNavigateBack:()->Unit,modifier:Modifier=Modifier,onConnectDrive:()->Unit={}) {
    val context=LocalContext.current
    val clipboard=LocalClipboardManager.current
    val scope=rememberCoroutineScope()
    val recoveryCode by dataStoreManager.recoveryCodeFlow.collectAsState(initial="")
    val autoBackup by dataStoreManager.autoBackupEnabledFlow.collectAsState(initial=false)
    val lastBackup by dataStoreManager.lastBackupTimeFlow.collectAsState(initial=0L)
    val email by dataStoreManager.userEmailFlow.collectAsState(initial="")
    val backupError by dataStoreManager.lastBackupErrorFlow.collectAsState(initial="")
    var password by remember { mutableStateOf("") }
    var restoreInput by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var overwrite by rememberSaveable { mutableStateOf(false) }
    var showRestore by rememberSaveable { mutableStateOf(false) }
    var confirmNewCode by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var cloudBackups by remember { mutableStateOf<List<DriveFileMetadata>?>(null) }
    val pendingFile=remember { File(context.cacheDir,"pending-manual-backup.enc") }
    val saveFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if(uri!=null) scope.launch {
            busy=true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri,"wt")?.use { output -> pendingFile.inputStream().use { it.copyTo(output) } } ?: error("파일을 저장할 수 없습니다.")
                }
                message="암호화 백업을 저장했습니다. 복구 코드도 따로 보관해주세요."
            } catch(e:Exception) { message="저장 실패: ${e.message}" } finally {busy=false}
        }
    }
    val openFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) scope.launch {
            busy=true
            try { restoreInput=readBackupFile(context,uri);showRestore=true;message="백업 파일을 불러왔습니다." }
            catch(e:Exception) {message="파일 읽기 실패: ${e.message}"} finally {busy=false}
        }
    }
    if(confirmNewCode) AlertDialog(onDismissRequest={confirmNewCode=false},title={Text("복구 코드를 재발급할까요?")},
        text={Text("새 코드는 앞으로 생성하는 백업에 적용됩니다. 이전 백업은 당시 비밀번호 또는 이전 복구 코드가 필요합니다.")},
        confirmButton={TextButton(onClick={scope.launch {dataStoreManager.setRecoveryCode(CryptoEngine.generateRecoveryCode());confirmNewCode=false}}){Text("재발급")}},
        dismissButton={TextButton(onClick={confirmNewCode=false}){Text("취소")}})
    if(cloudBackups!=null) AlertDialog(onDismissRequest={cloudBackups=null},title={Text("자동 백업 선택")},
        text={Column(Modifier.verticalScroll(rememberScrollState())) {
            if(cloudBackups!!.isEmpty()) Text("저장된 자동 백업이 없습니다.")
            cloudBackups!!.forEach { file -> TextButton(enabled=!busy,onClick={scope.launch {
                busy=true
                try {restoreInput=backupManager.readAutoBackup(file.id);cloudBackups=null;showRestore=true}
                catch(e:Exception) {message="백업 다운로드 실패: ${e.message}"} finally {busy=false}
            }}) {Text(file.name)} }
        }},confirmButton={TextButton(onClick={cloudBackups=null}){Text("닫기")}})
    if(showRestore) AlertDialog(onDismissRequest={if(!busy) showRestore=false},title={Text("암호화 백업 복원")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("개인·비밀 목록만 복원합니다. 1.0.7 이전 백업은 당시 비밀번호가 필요합니다.")
            OutlinedButton(enabled=!busy,onClick={openFile.launch(arrayOf("*/*"))}){Text("백업 파일 선택")}
            OutlinedTextField(value=restoreInput,onValueChange={restoreInput=it},label={Text("백업 내용 (파일 선택 또는 붙여넣기)")},minLines=2,maxLines=3,modifier=Modifier.fillMaxWidth())
            SecretInput(restorePassword,{restorePassword=it},"비밀번호 또는 백업 당시 복구 코드")
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked=overwrite,onCheckedChange={overwrite=it}); Text("기존 개인·비밀 목록을 교체 (기본은 병합)",modifier=Modifier.weight(1f))
            }
            if(overwrite) Text("덮어쓰기는 현재 개인·비밀 목록과 휴지통을 백업 내용으로 교체합니다.",color=Danger)
            if(message.isNotBlank()) Text(message,color=TextSecondary)
        }},confirmButton={TextButton(enabled=!busy&&restoreInput.isNotBlank()&&restorePassword.isNotEmpty(),onClick={scope.launch {
            busy=true
            try {
                val result=backupManager.restoreEncryptedBackup(restoreInput,restorePassword,if(overwrite) RestoreMode.OVERWRITE else RestoreMode.MERGE)
                result.onSuccess {message="${it}개 항목을 복원했습니다.";showRestore=false;restorePassword=""}
                    .onFailure {message="복원 실패: ${it.message ?: "비밀번호와 백업 파일을 확인해주세요."}"}
            } finally {busy=false}
        }}){Text(if(busy) "처리 중" else "복원 실행")}},dismissButton={TextButton(enabled=!busy,onClick={showRestore=false}){Text("취소")}})

    Column(modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick=onNavigateBack){Text("뒤로가기")}
            Text("백업 및 복원",style=MaterialTheme.typography.headlineSmall,color=TextPrimary)
        }
        if(busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
        if(message.isNotBlank()) Text(message,color=TextPrimary,modifier=Modifier.fillMaxWidth())
        BackupSection("내 복구 코드") {
            Text("새 백업은 비밀번호 또는 백업 당시의 복구 코드로 열 수 있습니다. 이 코드를 잃지 않도록 따로 보관해주세요.",color=TextSecondary)
            if(recoveryCode.isNotBlank()) {
                Text(recoveryCode,color=Primary)
                TextButton(onClick={clipboard.setText(AnnotatedString(recoveryCode));message="복구 코드를 복사했습니다."}){Text("복구 코드 복사")}
            }
            OutlinedButton(enabled=!busy,onClick={if(recoveryCode.isNotBlank()) confirmNewCode=true else scope.launch {dataStoreManager.setRecoveryCode(CryptoEngine.generateRecoveryCode())}}) {
                Text(if(recoveryCode.isBlank()) "복구 코드 발급" else "복구 코드 재발급")
            }
        }
        BackupSection("개인·비밀 목록 암호화 백업") {
            Text("공동·하트 목록은 포함되지 않습니다. 선택한 파일 위치에 암호화하여 저장합니다.",color=TextSecondary)
            SecretInput(password,{password=it},"백업 비밀번호")
            Button(enabled=!busy&&password.isNotBlank(),onClick={scope.launch {
                busy=true
                try {
                    val encrypted=backupManager.createEncryptedBackup(password)
                    withContext(Dispatchers.IO){pendingFile.writeText(encrypted)}
                    val time=SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(Date())
                    saveFile.launch("OurList-backup-$time.enc")
                } catch(e:Exception){message="백업 실패: ${e.message}"} finally {busy=false}
            }}){Text("암호화 백업 파일 저장")}
            OutlinedButton(enabled=!busy,onClick={openFile.launch(arrayOf("*/*"))}){Text("백업 파일에서 복원")}
            TextButton(enabled=!busy,onClick={showRestore=true}){Text("백업 내용 붙여넣기로 복원")}
        }
        BackupSection("Google Drive 자동 암호화 백업") {
            Text(if(email.isBlank()) "자동 백업을 사용하려면 Google 계정을 연결해주세요." else "연결 계정: $email",color=TextSecondary)
            OutlinedButton(enabled=!busy,onClick=onConnectDrive){Text("Google 계정 연결 / 권한 갱신")}
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked=autoBackup,enabled=!busy,onCheckedChange={checked ->
                    if(checked&&(email.isBlank()||recoveryCode.isBlank())) message="Google 계정을 연결하고 복구 코드를 먼저 발급해주세요."
                    else scope.launch {
                        dataStoreManager.setAutoBackupEnabled(checked)
                        message=if(checked) "네트워크에 연결되면 첫 백업을 시작합니다. 아래 성공 시각과 오류를 확인해주세요." else "자동 백업을 껐습니다."
                        if(checked) WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<AutoBackupWorker>()
                            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
                    }
                }); Text("하루 한 번 자동 백업",color=TextPrimary)
            }
            Text("개인·비밀 목록을 암호화하여 앱 전용 Drive 공간에 저장하고 최신 7개를 보관합니다. 복원할 때 백업 당시 복구 코드를 사용하세요.",color=TextSecondary)
            if(backupError.isNotBlank()) Text("최근 백업 오류: $backupError",color=Danger)
            if(lastBackup>0) Text("최근 성공: ${SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.KOREA).format(Date(lastBackup))}",color=Primary)
            OutlinedButton(enabled=!busy&&email.isNotBlank(),onClick={scope.launch {
                busy=true
                try {cloudBackups=backupManager.listAutoBackups()} catch(e:Exception){message="백업 조회 실패: ${e.message}"} finally {busy=false}
            }}){Text("자동 백업 찾아서 복원")}
        }
    }
}
