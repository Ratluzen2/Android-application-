package com.zafer.smm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.zafer.smm.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

// === إعدادات المستودع على GitHub (عدّلها إذا اختلف الاسم) ===
private const val OWNER = "Ratluzen2"
private const val REPO  = "Android-application-"

// === مفاتيح التخزين المؤقت لميزة "لاحقًا" (ساعة واحدة) ===
private const val PREFS_NAME = "update_prompt_prefs"
private const val KEY_SNOOZE_UNTIL = "update_snooze_until"

// رابط التحميل المباشر لآخر إصدار (اسم الملف يجب أن يكون app-release.apk داخل الـRelease)
private val latestApkUrl: String
    get() = "https://github.com/$OWNER/$REPO/releases/latest/download/app-release.apk"

// عنوان API لإحضار آخر إصدار
private val latestReleaseApiUrl: String
    get() = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

/** يفحص إن كان هناك تحديث جديد (باستخدام GitHub Releases) ويراعي الإرجاء لمدة ساعة عند اختيار "لاحقًا". */
private suspend fun checkForUpdateOrNull(ctx: Context): Int? = withContext(Dispatchers.IO) {
    try {
        // احترم التأجيل لمدة ساعة
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snoozeUntil = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        if (System.currentTimeMillis() < snoozeUntil) return@withContext null

        val url = URL(latestReleaseApiUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "ZaferApp-UpdateChecker")
        }
        conn.connect()
        if (conn.responseCode !in 200..299) {
            Log.w("UpdatePrompt", "GitHub API HTTP ${conn.responseCode}")
            return@withContext null
        }
        val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val tagName = JSONObject(body).optString("tag_name").trim() // مثل v1711
        val remoteVc = tagName.filter { it.isDigit() }.toIntOrNull() ?: return@withContext null

        return@withContext if (remoteVc > BuildConfig.VERSION_CODE) remoteVc else null
    } catch (t: Throwable) {
        Log.e("UpdatePrompt", "checkForUpdateOrNull failed", t)
        null
    }
}

/** ينفّذ فتح رابط التحديث في المتصفح. */
private fun openUpdateInBrowser(ctx: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestApkUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    } catch (_: Throwable) {
        // ignore
    }
}

/** يفعّل تأجيل ظهور النافذة لمدة ساعة واحدة. */
private fun snoozeOneHour(ctx: Context) {
    val until = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putLong(KEY_SNOOZE_UNTIL, until)
        .apply()
}

/** الواجهة المنبثقة ذاتها (Dialog) — تُعرض فقط عند توفر تحديث أحدث. */
@Composable
fun UpdatePrompt() {
    val ctx = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var remoteVc by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val newer = checkForUpdateOrNull(ctx)
        if (newer != null) {
            remoteVc = newer
            show = true
        }
    }

    if (!show) return

    // ألوان جذّابة للأزرار والنصوص داخل الـDialog
    val primaryButtonColor = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF7C4DFF), // بنفسجي أنيق
        contentColor   = Color.White
    )
    val laterButtonColor = ButtonDefaults.textButtonColors(
        contentColor = Color(0xFF5C6BC0) // أزرق بنفسجي لطيف
    )

    AlertDialog(
        onDismissRequest = {
            // إغلاق بالنقر خارج الحوار = اعتبرها "لاحقًا" أيضاً مع تأجيل ساعة
            snoozeOneHour(ctx)
            show = false
        },
        title = {
            Text(
                "تحديث جديد متوفر",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "يتوفر إصدار أحدث للتطبيق. ننصحك بالتحديث الآن للحصول على الاضافات الجديدة والخدمات الجديدة والإصلاحات.",
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "الإصدار الحالي: ${BuildConfig.VERSION_CODE}\nالإصدار الجديد: $remoteVc",
                    fontSize = 14.sp,
                    color = Color(0xFF424242)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openUpdateInBrowser(ctx)
                    show = false
                },
                shape = RoundedCornerShape(12.dp),
                colors = primaryButtonColor
            ) {
                Text("تحديث الآن", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // لاحقًا = تأجيل لساعة
                    snoozeOneHour(ctx)
                    show = false
                },
                colors = laterButtonColor
            ) {
                Text("لاحقًا", fontSize = 15.sp)
            }
        }
    )
}

/** غلاف بسيط ليسهّل الاستدعاء من MainActivity بدون تغيير بقية الكود. */
@Composable
fun UpdatePromptHost() {
    UpdatePrompt()
}
