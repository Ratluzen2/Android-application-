package com.zafer.smm.crash

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Looper
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashCatcher {

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stack = sw.toString()

            val deviceInfo = buildString {
                appendLine("Brand: ${Build.BRAND}")
                appendLine("Device: ${Build.DEVICE}")
                appendLine("Model: ${Build.MODEL}")
                appendLine("SDK: ${Build.VERSION.SDK_INT}")
                appendLine("Thread: ${thread.name}")
            }

            val fullReport = "$deviceInfo\n\n$stack"

            val i = Intent(app, CrashReportActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("stacktrace", fullReport)
            }

            android.os.Handler(Looper.getMainLooper()).post {
                try {
                    app.startActivity(i)
                } catch (_: Throwable) {
                    previous?.uncaughtException(thread, throwable)
                } finally {
                    try { previous?.uncaughtException(thread, throwable) } catch (_: Throwable) {}
                    android.os.Process.killProcess(android.os.Process.myPid())
                    exitProcess(10)
                }
            }
        }
    }
}
