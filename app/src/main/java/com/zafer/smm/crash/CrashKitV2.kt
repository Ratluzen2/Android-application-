
package com.zafer.smm.crash

import android.app.Activity
import android.app.Application
import android.content.*
import android.database.Cursor
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * CrashKitV2 (Plain Views)
 * يلتقط أي كراش، يحفظ تقريراً، ويعرض شاشة السجل تلقائيًا في أول تشغيل لاحق.
 * بدون Compose لتقليل التبعيات ومنع كراش ثانوي.
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
 * ContentProvider للتثبيت المبكر جدًا.
 * initOrder العالي يضمن تشغيله قبل أي Provider آخر في نفس العملية.
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

/**
 * Activity خفيفة جدًا (بدون Compose) تُعرض في عملية منفصلة ':crash' لتتفادى تهيئة كود التطبيق الأساسي.
 */
class CrashReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // واجهة بسيطة: صف أزرار + مساحة نص قابلة للتمرير
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        fun mkButton(text: String, onClick: () -> Unit): Button =
            Button(this).apply {
                this.text = text
                setOnClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, 16, 0)
                }
            }

        val tv = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 13f
            isVerticalScrollBarEnabled = true
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = 16
            }
            addView(tv, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        val ctx = this@CrashReportActivity
        var current = CrashKitV2.loadReport(ctx) ?: "لا توجد سجلات خطأ."
        tv.text = current

        val copyBtn = mkButton("نسخ") {
            val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.setPrimaryClip(ClipData.newPlainText("Crash Report", current))
            Toast.makeText(ctx, "تم النسخ", Toast.LENGTH_SHORT).show()
        }
        val shareBtn = mkButton("مشاركة") {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, current)
            }
            startActivity(Intent.createChooser(share, "مشاركة السجل"))
        }
        val clearBtn = mkButton("مسح") {
            CrashKitV2.clearReport(ctx)
            current = "لا توجد سجلات خطأ."
            tv.text = current
        }
        val openBtn = mkButton("فتح التطبيق") {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(launch)
            }
            finish()
        }

        btnRow.addView(copyBtn)
        btnRow.addView(shareBtn)
        btnRow.addView(clearBtn)
        btnRow.addView(openBtn)

        root.addView(btnRow)
        root.addView(scroll)
        setContentView(root)
    }
}
