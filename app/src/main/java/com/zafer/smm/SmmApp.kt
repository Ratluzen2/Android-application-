package com.zafer.smm

import android.app.Application
import com.zafer.smm.crash.CrashCatcher

class SmmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ثبّت ماسك الكراش لعرض الستاك تريس داخل التطبيق
        CrashCatcher.install(this)
    }
}
