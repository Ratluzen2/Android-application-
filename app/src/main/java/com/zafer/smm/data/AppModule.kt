package com.zafer.smm.data

import android.content.Context
import android.provider.Settings
import androidx.room.Room
import com.zafer.smm.data.local.AppDatabase

object AppModule {
    lateinit var db: AppDatabase
        private set

    fun init(context: Context) {
        if (!::db.isInitialized) {
            db = Room.databaseBuilder(context, AppDatabase::class.java, "smm.db").build()
        }
    }

    fun deviceId(context: Context): String {
        // لا يتطلب صلاحية
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
    }
}
