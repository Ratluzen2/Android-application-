package com.zafer.smm.util

import android.content.Context
import android.provider.Settings

object DeviceIdProvider {
    fun getAndroidId(ctx: Context): String {
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"
    }
}
