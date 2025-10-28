
package com.zafer.smm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.zafer.smm.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight in‑app update prompt driven by GitHub Releases.
 *
 * How it works:
 *  - Reads the latest release "tag_name" from:
 *      https://api.github.com/repos/{OWNER}/{REPO}/releases/latest
 *    (e.g., v1710) and extracts the digits as a versionCode.
 *  - Compares it to BuildConfig.VERSION_CODE.
 *  - If the remote is greater, shows a modal dialog with a button that opens
 *    the direct APK download URL:
 *      https://github.com/{OWNER}/{REPO}/releases/latest/download/app-release.apk
 *
 * No changes are required to your backend. To publish an update, just create a
 * GitHub Release with a tag like v1710 and attach app-release.apk.
 *
 * To enable the prompt, call UpdatePromptHost() once anywhere inside your
 * setContent {} tree (for example at the top of your Home screen or Scaffold).
 */
object UpdatePromptConfig {
    // TODO: change these to your repository if different
    const val OWNER = "Ratluzen2"
    const val REPO  = "Android-application-"

    val latestApkUrl: String
        get() = "https://github.com/$OWNER/$REPO/releases/latest/download/app-release.apk"

    val latestReleaseApiUrl: String
        get() = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    // SharedPreferences name/key to avoid re-prompting for the same version
    const val PREFS = "update_prompt_prefs"
    const val KEY_LAST_SHOWN_REMOTE_VC = "last_shown_remote_vc"
}

@Composable
fun UpdatePromptHost(
    // Optional: force check even if we've already shown this remote version in the past
    forceCheck: Boolean = false,
    // Optional: custom title / message if you don't like the defaults
    title: String = "تحديث جديد متوفر",
    messagePrefix: String = "يتوفر إصدار أحدث للتطبيق. ننصحك بالتحديث الآن للحصول على التحسينات والإصلاحات."
) {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var remoteVc by remember { mutableIntStateOf(0) }
    var downloadUrl by remember { mutableStateOf(UpdatePromptConfig.latestApkUrl) }

    LaunchedEffect(forceCheck) {
        val prefs = context.getSharedPreferences(UpdatePromptConfig.PREFS, Context.MODE_PRIVATE)
        val lastShown = prefs.getInt(UpdatePromptConfig.KEY_LAST_SHOWN_REMOTE_VC, 0)
        val latest = fetchLatestVersionCode()
        if (latest > BuildConfig.VERSION_CODE) {
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
                        // Prefer opening in the browser (no extra permissions required)
                        openInBrowser(context, downloadUrl)
                        rememberDontShowAgain(context, remoteVc)
                        show = false
                    }
                ) {
                    Text("تحديث الآن")
                }
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
                            append(BuildConfig.VERSION_CODE)
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
    } catch (_: Throwable) {
        // no-op
    }
}

private suspend fun fetchLatestVersionCode(): Int = withContext(Dispatchers.IO) {
    // Query GitHub Releases latest
    val apiUrl = UpdatePromptConfig.latestReleaseApiUrl
    try {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            readTimeout = 8000
            connectTimeout = 8000
            // GitHub API requires a User-Agent
            setRequestProperty("User-Agent", "SmmApp-UpdateChecker")
            // Fewer headers to keep it simple
        }
        conn.connect()
        val code = conn.responseCode
        if (code in 200..299) {
            conn.inputStream.bufferedReader().use(BufferedReader::readText).let { body ->
                val tag = JSONObject(body).optString("tag_name", "")
                // Expecting tags like v1709 -> extract digits
                val digits = Regex("(\\d+)").find(tag)?.groupValues?.getOrNull(1)
                return@withContext digits?.toIntOrNull() ?: 0
            }
        }
    } catch (_: Throwable) {
        // ignore and fall through
    }
    0
}
