package com.zafer.smm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zafer.smm.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private const val OWNER = "Ratluzen2"
private const val REPO = "Android-application-"

// SharedPreferences keys for snooze
private const val PREFS_NAME = "update_prompt_prefs"
private const val KEY_SNOOZE_UNTIL = "update_snooze_until"

data class UpdateInfo(
    val latestVc: Int,
    val downloadUrl: String
)

/**
 * Returns null if no update OR update should be snoozed.
 * Otherwise returns UpdateInfo with latest version and direct download URL.
 */
private suspend fun checkUpdateOrNull(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        // Respect "Later" snooze (1 hour)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snoozeUntil = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        if (System.currentTimeMillis() < snoozeUntil) return@withContext null

        val api = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
        val conn = (URL(api).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            requestMethod = "GET"
            addRequestProperty("Accept", "application/vnd.github+json")
            addRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299) {
            Log.w("UpdatePrompt", "GitHub API HTTP $code")
            return@withContext null
        }
        val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val json = JSONObject(body)
        val tag = json.optString("tag_name").trim()
        val latestVc = tag.filter { it.isDigit() }.toIntOrNull() ?: return@withContext null

        if (BuildConfig.VERSION_CODE >= latestVc) return@withContext null

        // We rely on the conventional file name in Releases
        val directUrl =
            "https://github.com/$OWNER/$REPO/releases/latest/download/app-release.apk"

        UpdateInfo(latestVc = latestVc, downloadUrl = directUrl)
    } catch (t: Throwable) {
        Log.e("UpdatePrompt", "checkUpdate failed", t)
        null
    }
}

@Composable
fun UpdatePrompt(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    // Load/update info once per composition
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        updateInfo = checkUpdateOrNull(ctx)
    }

    // If no update (or snoozed), render nothing
    val info = updateInfo ?: return

    // Nice gradient background (attractive colors)
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2E2B3F), // deep indigo
            Color(0xFF6D5BD0), // soft purple
            Color(0xFF9C7BFF)  // bright accent
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "تحديث جديد متوفر",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "يتوفر إصدار أحدث للتطبيق. ننصحك بالتحديث الآن للحصول على الاضافات الجديدة والخدمات الجديدة والإصلاحات.",
                    fontSize = 15.sp,
                    color = Color(0xFFEDEBFF),
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "الإصدار الحالي: ${BuildConfig.VERSION_CODE}\nالإصدار الجديد: ${info.latestVc}",
                    fontSize = 14.sp,
                    color = Color(0xFFF2F1FF)
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { openUpdate(ctx, info.downloadUrl) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE1D6FF),
                            contentColor = Color(0xFF2E2350)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("تحديث الآن", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.width(12.dp))

                    TextButton(
                        onClick = { snoozeOneHour(ctx); updateInfo = null },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            "لاحقًا",
                            color = Color(0xFFFDFBFF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun openUpdate(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (t: Throwable) {
        Log.e("UpdatePrompt", "openUpdate failed", t)
    }
}

private fun snoozeOneHour(context: Context) {
    val until = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putLong(KEY_SNOOZE_UNTIL, until)
        .apply()
}
