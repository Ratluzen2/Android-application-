package com.zafer.smm.data

import android.content.Context
import android.provider.Settings
import java.util.UUID

object Prefs {
    private const val P = "smm_prefs"
    private const val K_DEVICE = "device_id"

    fun deviceId(ctx: Context): String {
        val sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
        var id = sp.getString(K_DEVICE, null)
        if (id.isNullOrBlank()) {
            id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
                ?: UUID.randomUUID().toString()
            sp.edit().putString(K_DEVICE, id).apply()
        }
        return id
    }
}
