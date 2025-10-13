package com.zafer.smm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import android.content.Context
import android.provider.Settings

/** FCM Service — مستقل ولا يعتمد على دوال private داخل MainActivity.kt */
class MyFirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            val uid = loadOrCreateUidLocal(applicationContext)
            Thread {
                try {
                    sendFcmTokenToBackend(uid, token)
                } catch (_: Throwable) { /* ignore */ }
            }.start()
        } catch (_: Throwable) { /* ignore */ }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "إشعار"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "لديك تحديث جديد"

        AppNotifier.ensureChannel(applicationContext)
        AppNotifier.notifyNow(applicationContext, title, body)
    }

    // ===== Helpers (local copy) =====

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private fun loadOrCreateUidLocal(ctx: Context): String {
        val sp = prefs(ctx)
        val existing = sp.getString("uid", null)
        if (existing != null) return existing

        val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        val base = if (androidId.isNotBlank()) androidId.uppercase(Locale.getDefault()) else "R" + (100000..999999).random()
        val fresh = "U" + base.take(16)
        sp.edit().putString("uid", fresh).apply()
        return fresh
    }

    private fun sendFcmTokenToBackend(uid: String, token: String) {
        val url = URL("$API_BASE/api/users/fcm_token")
        val con = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = JSONObject().put("uid", uid).put("fcm", token).toString()
        OutputStreamWriter(con.outputStream, Charsets.UTF_8).use { it.write(body) }
        val code = con.responseCode
        con.inputStream?.close()
        con.errorStream?.close()
        if (code !in 200..299) throw RuntimeException("FCM token post failed: HTTP $code")
    }

    companion object {
        // نفس العنوان الموجود في MainActivity
        private const val API_BASE = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com"
    }
}
