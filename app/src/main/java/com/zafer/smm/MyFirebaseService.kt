package com.zafer.smm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

class MyFirebaseService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "MyFirebaseService"
        private const val CHANNEL_ID = "zafer_main_high"
        private val nextId = AtomicInteger((System.currentTimeMillis() % 10000).toInt())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM new token: $token")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = loadOrCreateUid(applicationContext)
                val serverUrl = "$API_BASE/api/users/fcm_token"
                val body = JSONObject().put("uid", uid).put("fcm", token).toString()
                httpPostNoAuth(serverUrl, body)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send token to server: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "Message received from: ${remoteMessage.from}")
        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "تنبيه"
        val body = data["body"] ?: remoteMessage.notification?.body ?: ""
        showNotification(applicationContext, title, body, data)
    }

    private fun showNotification(ctx: Context, title: String, body: String, data: Map<String, String>) {
        createChannelIfNeeded(ctx)
        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (k, v) -> putExtra(k, v) }
        }
        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(ctx, 0, tapIntent, piFlags)

        val nid = nextId.incrementAndGet()
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(ctx).notify(nid, builder.build())
    }

    private fun createChannelIfNeeded(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "App Alerts", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "User orders, balance updates, and general alerts"
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
    }
}
