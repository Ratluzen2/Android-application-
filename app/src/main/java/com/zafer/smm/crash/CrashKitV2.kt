package com.zafer.smm.crash

import android.app.Activity
import android.app.Application
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * CrashKitV2 — يلتقط أي كراش، يحفظ تقريرًا، ويعرض شاشة السجل تلقائيًا في أول تشغيل لاحق.
 * هذه النسخة تُثبّت نفسها مبكرًا جدًا عبر ContentProvider حتى لو الكراش يحدث قبل MainActivity.
 */
object CrashKitV2 {
    private const val FILE_NAME = "crash_latest.txt"
    private const val PREF = "crashkit_v2"
    private const val KEY_PENDING = "pending"

    @Volatile private var installed = false

    fun init(app: Application) {
        if (installed) return
        installed = true

        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val report = buildReport(app, t, e)
                File(app.filesDir, FILE_NAME).writeText(report)
                app.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_PENDING, true).apply()
            } catch (_: Throwable) {
                // ignore
            } finally {
                // مرر للمعالج السابق (قد يطبع للّوغ) ثم أنهِ العملية
                previous?.uncaughtException(t, e)
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }

    /** يُظهر شاشة السجل فور تشغيل التطبيق إذا كان هناك تقرير مُعلّق. */
    fun showIfPending(app: Application) {
        val prefs = app.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(KEY_PENDING, false)
        val file = File(app.filesDir, FILE_NAME)
        if (pending && file.exists()) {
            // شغّل على الخيط الرئيسي لضمان سلامة بدء النشاط
            Handler(Looper.getMainLooper()).post {
                val i = Intent(app, CrashReportActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                app.startActivity(i)
                prefs.edit().putBoolean(KEY_PENDING, false).apply()
            }
        }
    }

    private fun buildReport(ctx: Context, thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val trace = sw.toString()

        val pm = ctx.packageManager
        val pi = pm.getPackageInfo(ctx.packageName, 0)

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return buildString {
            appendLine("Time: ${df.format(Date())}")
            appendLine("App: ${pi.packageName} v${pi.versionName} (${pi.longVersionCode})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, SDK ${Build.VERSION.SDK_INT}")
            appendLine("Thread: ${thread.name} (${thread.id})")
            appendLine("---------- STACKTRACE ----------")
            append(trace)
        }
    }

    fun loadReport(ctx: Context): String? =
        runCatching { File(ctx.filesDir, FILE_NAME).readText() }.getOrNull()

    fun clearReport(ctx: Context) {
        runCatching { File(ctx.filesDir, FILE_NAME).delete() }
            .onSuccess {
                ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_PENDING, false).apply()
            }
    }
}

/**
 * مزوّد محتوى بسيط للتثبيت المبكر بدون أي اعتماديات إضافية.
 * يُستدعى قبل إنشاء الأنشطة؛ ينصّب CrashKitV2 ويعرض شاشة السجل إن وجدت.
 */
class CrashInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? Application ?: return true
        CrashKitV2.init(app)
        CrashKitV2.showIfPending(app)
        return true
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrashReportScreen(ctx: Context) {
    var text by remember { mutableStateOf(CrashKitV2.loadReport(ctx) ?: "لا توجد سجلات خطأ.") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("سجل الأعطال") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val clip = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    clip.setPrimaryClip(ClipData.newPlainText("Crash Report", text))
                }) { Text("نسخ") }

                Button(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    ctx.startActivity(Intent.createChooser(share, "مشاركة السجل").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("مشاركة") }

                OutlinedButton(onClick = {
                    CrashKitV2.clearReport(ctx)
                    text = "لا توجد سجلات خطأ."
                }) { Text("مسح") }

                Spacer(Modifier.weight(1f))

                Button(onClick = {
                    val pm = ctx.packageManager
                    val launch = pm.getLaunchIntentForPackage(ctx.packageName)
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        ctx.startActivity(launch)
                    }
                    if (ctx is Activity) (ctx as Activity).finish()
                }) { Text("فتح التطبيق") }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

/** Activity تعرض شاشة السجل. */
class CrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CrashReportScreen(this@CrashReportActivity)
            }
        }
    }
}
