
package com.zafer.smm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * In‑app update prompt (external download) backed by GitHub Releases.
 * - Reads latest tag from: https://api.github.com/repos/{OWNER}/{REPO}/releases/latest
 * - Compares its numeric part to the *installed* versionCode (resolved at runtime).
 * - If newer, shows a dialog with "Update now" that opens the browser to download APK:
 *   https://github.com/{OWNER}/{REPO}/releases/latest/download/app-release.apk
 *
 * Drop-in: call UpdatePromptHost() once inside your setContent { } tree.
 */
object UpdatePromptConfig {
    const val OWNER = "Ratluzen2"              // TODO: change if different
    const val REPO  = "Android-application-"   // TODO: change if different

    val latestApkUrl: String
        get() = "https://github.com/$OWNER/$REPO/releases/latest/download/app-release.apk"

    val latestReleaseApiUrl: String
        get() = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    // SharedPreferences key to avoid re-prompting for same remote VC
    const val PREFS = "update_prompt_prefs"
    const val KEY_LAST_SHOWN_REMOTE_VC = "last_shown_remote_vc"
}

@Composable
fun UpdatePromptHost(
    forceCheck: Boolean = false,
    title: String = "تحديث جديد متوفر",
    messagePrefix: String = "يتوفر إصدار أحدث للتطبيق. ننصحك بالتحديث الآن للحصول على التحسينات والإصلاحات."
) {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var remoteVc by remember { mutableIntStateOf(0) }
    var downloadUrl by remember { mutableStateOf(UpdatePromptConfig.latestApkUrl) }
    val currentVc = remember { resolveInstalledVersionCode(context) }

    LaunchedEffect(forceCheck) {
        val prefs = context.getSharedPreferences(UpdatePromptConfig.PREFS, Context.MODE_PRIVATE)
        val lastShown = prefs.getInt(UpdatePromptConfig.KEY_LAST_SHOWN_REMOTE_VC, 0)
        val latest = fetchLatestVersionCode()
        if (latest > currentVc) {
            remoteVc = latest
            downloadUrl = UpdatePromptConfig.latestApkUrl
            if (forceCheck || latest != lastShown) {
                show = true
            }
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false; rememberDontShowAgain(context, remoteVc) },
            confirmButton = {
                Button(
                    onClick = {
                        openInBrowser(context, downloadUrl)
                        rememberDontShowAgain(context, remoteVc)
                        show = false
                    }
                ) { Text("تحديث الآن") }
            },
            dismissButton = {
                TextButton(onClick = { show = false; rememberDontShowAgain(context, remoteVc) }) {
                    Text("لاحقًا")
                }
            },
            title = { Text(title, textAlign = TextAlign.Start) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = buildString {
                            append(messagePrefix)
                            append("\n\n")
                            append("الإصدار الحالي: ")
                            append(currentVc)
                            append("\n")
                            append("الإصدار الجديد: ")
                            append(remoteVc)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        )
    }
}

private fun resolveInstalledVersionCode(context: Context): Int {
    return try {
        val pm = context.packageManager
        val pkg = context.packageName
        val info = pm.getPackageInfo(pkg, 0)
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else info.versionCode
    } catch (_: Throwable) {
        0
    }
}

private fun rememberDontShowAgain(context: Context, remoteVc: Int) {
    context.getSharedPreferences(UpdatePromptConfig.PREFS, Context.MODE_PRIVATE)
        .edit()
        .putInt(UpdatePromptConfig.KEY_LAST_SHOWN_REMOTE_VC, remoteVc)
        .apply()
}

private fun openInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Throwable) { /* no-op */ }
}

private suspend fun fetchLatestVersionCode(): Int = withContext(Dispatchers.IO) {
    val apiUrl = UpdatePromptConfig.latestReleaseApiUrl
    try {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            readTimeout = 8000
            connectTimeout = 8000
            setRequestProperty("User-Agent", "SmmApp-UpdateChecker")
        }
        conn.connect()
        val code = conn.responseCode
        if (code in 200..299) {
            conn.inputStream.bufferedReader().use(BufferedReader::readText).let { body ->
                val tag = JSONObject(body).optString("tag_name", "")
                val digits = Regex("(\\d+)").find(tag)?.groupValues?.getOrNull(1)
                return@withContext digits?.toIntOrNull() ?: 0
            }
        }
    } catch (_: Throwable) { /* ignore */ }
    0
}
