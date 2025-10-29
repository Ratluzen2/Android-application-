package com.zafer.smm.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private const val TAG = "UpdatePrompt"

// ✅ مضبوط على مستودعك
private const val OWNER = "Ratluzen2"
private const val REPO  = "Android-application-"

private const val PREFS = "update_prompt_prefs"
private const val KEY_SNOOZE_UNTIL = "snooze_until"

@Composable
fun UpdatePromptHost() {
    val ctx = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var tagText by remember { mutableStateOf<String?>(null) }
    var apkUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val current = currentNumericVersion(ctx)
            val latest = fetchLatestRelease(OWNER, REPO)

            Log.d(TAG, "current=$current, remote=${latest.numericTag}, tag=${latest.tag}")

            // ✅ إذا الإصدار الأحدث أكبر من الحالي → أظهر النافذة
            show = latest.numericTag > current
            tagText = latest.tag
            apkUrl = latest.apkUrl
        } catch (t: Throwable) {
            Log.e(TAG, "update check failed: ${t.message}")
        }
    }

    if (show) {
        val tag = tagText ?: "جديد"
        UpdateDialog(
            tagName = tag,
            onUpdate = {
                val url = apkUrl ?: "https://github.com/$OWNER/$REPO/releases/latest"
                openUrl(LocalContext.current, url)
            },
            onLater = {
                // سكون ساعة (سوف يُتجاهَل تلقائيًا عند وجود إصدار أحدث)
                val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val until = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
                prefs.edit().putLong(KEY_SNOOZE_UNTIL, until).apply()
                show = false
            }
        )
    }
}

@Composable
private fun UpdateDialog(
    tagName: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("تحديث متاح $tagName") },
        text  = { Text("يوجد إصدار أحدث من التطبيق. يُنصح بالتحديث للحصول على آخر الميزات والإصلاحات.") },
        confirmButton = { Button(onClick = onUpdate) { Text("تحديث الآن") } },
        dismissButton = { Button(onClick = onLater)  { Text("لاحقًا") } }
    )
}

private data class ReleaseInfo(
    val tag: String,
    val numericTag: Int,
    val apkUrl: String?
)

private suspend fun fetchLatestRelease(owner: String, repo: String): ReleaseInfo =
    withContext(Dispatchers.IO) {
        val api = "https://api.github.com/repos/$owner/$repo/releases/latest?_ts=${System.currentTimeMillis()}"
        val conn = URL(api).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "update-checker")
        conn.setRequestProperty("Cache-Control", "no-cache")

        conn.inputStream.use { input ->
            val body = input.bufferedReader().readText()
            val json = JSONObject(body)

            val tag = json.optString("tag_name", "")
            val num = Regex("""\d+""").find(tag)?.value?.toIntOrNull() ?: 0

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val url = a.optString("browser_download_url", null)
                    if (url != null && url.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = url
                        break
                    }
                }
            }

            ReleaseInfo(tag = if (tag.isBlank()) "v$num" else tag, numericTag = num, apkUrl = apkUrl)
        }
    }

private fun currentNumericVersion(ctx: Context): Int {
    val pInfo = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: Throwable) {
        null
    }

    val name = pInfo?.versionName.orEmpty()
    // نفضّل قراءة vN من الـversionName أولاً (مثال: "v5" → 5)
    val fromName = Regex("""\bv(\d+)\b""").find(name)?.groupValues?.get(1)?.toIntOrNull()
    if (fromName != null) return fromName

    // fallback: versionCode
    val code = if (android.os.Build.VERSION.SDK_INT >= 28) {
        pInfo?.longVersionCode?.toInt() ?: 0
    } else {
        @Suppress("DEPRECATION")
        pInfo?.versionCode ?: 0
    }
    return code
}

private fun openUrl(ctx: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }.onFailure {
        Log.e(TAG, "Failed to open url: $url, ${it.message}")
    }
}
