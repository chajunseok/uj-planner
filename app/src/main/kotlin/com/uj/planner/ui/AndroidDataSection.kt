package com.uj.planner.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.planner.data.Backup
import com.uj.planner.domain.nowLocalDateTime
import com.uj.planner.ui.icons.UjIcons
import com.uj.planner.ui.pad2
import com.uj.planner.ui.settings.PillButton
import com.uj.planner.ui.settings.SettingsViewModel
import com.uj.planner.ui.theme.PlannerColors
import kotlinx.datetime.number

/**
 * 설정 화면의 "데이터" 블록 중 안드로이드 몫.
 *
 * 파일을 고르는 방식(SAF)과 가져오기 뒤 앱을 다시 시작하는 방식이 플랫폼 전용이라 여기 있다.
 * 메시지와 저장 가드 같은 나머지 규칙은 [SettingsViewModel] 이 공용으로 갖는다.
 */
@Composable
fun ColumnScope.AndroidDataSection(backup: Backup, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    // 가져올 파일을 골랐고 아직 확인을 받지 않았다.
    var pendingImport by rememberSaveable { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.exportWith { backup.exportTo(uri) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    Text("데이터", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 28.dp, bottom = 6.dp))
    Text(
        "일정과 기록은 이 기기에만 있어요. 폰을 바꾸거나 앱을 지우기 전에 파일로 내보내 두세요.",
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = PlannerColors.Muted,
    )
    Row(Modifier.padding(top = 14.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton(UjIcons.FileUpload, "내보내기") { exportLauncher.launch("uj-planner-${exportStamp()}.db") }
        PillButton(UjIcons.FileDownload, "가져오기") { importLauncher.launch(arrayOf("*/*")) }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("지금 데이터를 파일의 내용으로 바꿀까요?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text("지금 있는 일정과 기록은 모두 사라지고 되돌릴 수 없어요. 바꾼 뒤에는 앱이 다시 시작돼요.", fontSize = 14.sp, lineHeight = 22.sp, color = PlannerColors.Body)
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingImport = null
                        viewModel.importWith(read = { backup.importFrom(uri) }, restart = { restartApp(context) })
                    },
                    modifier = Modifier.height(44.dp),
                ) { Text("가져오기", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }, modifier = Modifier.height(44.dp)) { Text("취소", style = MaterialTheme.typography.labelLarge) }
            },
        )
    }
}

/** 내보내기 파일 이름에 붙이는 YYYYMMDD. */
private fun exportStamp(): String {
    val today = nowLocalDateTime().date
    return "${today.year}${pad2(today.month.number)}${pad2(today.day)}"
}

/** 가져오기 뒤에는 DB 가 닫혀 있다. 열어 둔 화면과 ViewModel 을 모두 버리고 새로 시작한다. */
// ponytail: 시작 요청을 보낸 뒤 프로세스를 끝낸다. 드물게 다시 뜨지 않는 기기가 있으면 별도 프로세스에서 다시 띄우는 방식으로 바꾼다.
private fun restartApp(context: Context) {
    val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}

/** 앱 버전. 버전 줄 하나 때문에 설정 화면이 죽지 않게 하고, 못 읽으면 비워 둔다. */
@Composable
fun appVersion(): String {
    val context = LocalContext.current
    return remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
}
