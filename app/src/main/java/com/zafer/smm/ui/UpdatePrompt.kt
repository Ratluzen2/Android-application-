package com.zafer.smm.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private const val GITHUB_LATEST_API =
    "https://api.github.com/repos/Ratluzen2/Android-application-/releases/latest"

private const val PREFS_NAME = "update_prompt_prefs"
private const val KEY_SNOOZE_UNTIL = "update_snooze_until"

/** Try to read a *numeric* version from versionName first (e.g., 1747),
 *  falling back to versionCode when versionName doesn't contain digits. */
private fun currentNumericVersion(context: Context): Int {
    return try {
        val pm = context.packageManager
        val pInfo = pm.getPackageInfo(context.packageName, 0)
        // Extract digits from versionName like "v1711" -> 1711
        val digitsFromName = pInfo.versionName?.let {
            Regex("\\d+").findAll(it).joinToString("") { m -> m.value }
        }?.takeIf { it.isNotEmpty() }?.toIntOrNull()

        val code = if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode.toInt() else pInfo.versionCode

        when {
            digitsFromName != null && digitsFromName > code -> digitsFromName
            else -> code
        }
    } catch (e: Exception) {
        0
    }
}

private data class Latest(
    val numericTag: Int,
    val htmlUrl: String,
    val apkUrl: String?
)

private suspend fun fetchLatest(): Latest? = withContext(Dispatchers.IO) {
    try {
        val url = URL(GITHUB_LATEST_API + "?_ts=" + System.currentTimeMillis())
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "ratluzen-app")
            setRequestProperty("Cache-Control", "no-cache")
        }
        conn.inputStream.use { stream ->
            val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            val obj = JSONObject(text)
            val tag = obj.optString("tag_name") // e.g. "v1711"
            val numeric = Regex("\\d+").findAll(tag).joinToString("") { it.value }.toIntOrNull() ?: return@withContext null
            val html = obj.optString("html_url")
            val assets = obj.optJSONArray("assets") ?: JSONArray()
            var apk: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                   apk = a.optString("browser_download_url")
                   break
                }
            }
            return@withContext Latest(numeric, html, apk)
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun UpdatePromptHost(
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var show by remember { mutableStateOf(false) }
    var latest by remember { mutableStateOf<Latest?>(null) }
    var current by remember { mutableStateOf(0) }

    // One-time check on launch / composition
    LaunchedEffect(Unit) {
        val snoozeUntil = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
val now = System.currentTimeMillis()
// NOTE: We will fetch latest first, and if a NEWER version exists we ignore snooze.


        current = currentNumericVersion(ctx)
        val remote = fetchLatest()
        latest = remote
        // Decide showing prompt: if remote > current => show regardless of snooze
        if (remote != null && remote.numericTag > current) {
            show = true
        } else {
            // No newer version: respect snooze and hide
            if (now < snoozeUntil) {
                show = false
            } else {
                show = false
            }
        }

        show = when {
            remote == null -> false
            remote.numericTag > current -> true
            else -> false
        }
    }

    AnimatedVisibility(visible = show, modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = true) { /* swallow */ },
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                UpdateCard(
                    currentVersion = current,
                    latestVersion = latest?.numericTag ?: 0,
                    onLater = {
                        // Snooze for 1 hour
                        prefs.edit().putLong(
                            KEY_SNOOZE_UNTIL,
                            System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
                        ).apply()
                        show = false
                    },
                    onUpdateNow = {
                        latest?.let { l ->
                            val openUrl = l.apkUrl ?: l.htmlUrl
                            runCatching {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(openUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UpdateCard(
    currentVersion: Int,
    latestVersion: Int,
    onLater: () -> Unit,
    onUpdateNow: () -> Unit,
) {
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(cardBg)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "تحديث جديد متوفر",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "يتوفر إصدار أحدث للتطبيق. ننصحك بالتحديث الآن للحصول على الاضافات الجديدة والخدمات الجديدة والإصلاحات.",
            style = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "الإصدار الحالي: $currentVersion\nالإصدار الجديد: $latestVersion",
            style = MaterialTheme.typography.bodyMedium.copy(color = textColor.copy(alpha = 0.85f)),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        // Buttons
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onUpdateNow,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("تحديث الآن", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "لاحقًا",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onLater() }
                    .padding(vertical = 10.dp),
                color = textColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
