// File: app/src/main/java/com/zafer/smm/push/MyFirebaseService.kt
package com.zafer.smm.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zafer.smm.R
import com.zafer.smm.storage.NotificationStore

/**
 * Professional FCM handler:
 * - Shows system notifications (foreground/background)
 * - Persists notifications locally into the in-app "bell center" for owner or user
 * - Distinguishes owner vs user using data.for_owner == "true"
 * - Safe no-crash defaults (null-safe parsing)
 */
class MyFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "orders_updates_channel"
        private const val CHANNEL_NAME = "Updates & Orders"
        private const val CHANNEL_DESC = "Notifications for new orders and status updates"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("MyFirebaseService", "FCM token refreshed: $token")
        // TODO: send this token to backend via your existing API call in MainActivity/App start
        // You likely already do this. Keep as-is.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Parse title/body from both "notification" and "data"
        val nTitle = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val nBody  = message.notification?.body  ?: message.data["body"]  ?: "لديك تحديث جديد"
        val forOwner = message.data["for_owner"]?.equals("true", ignoreCase = true) == true
        val type = message.data["type"] ?: "generic"
        val orderId = message.data["order_id"] ?: ""

        // Persist into local bell center
        try {
            NotificationStore.saveNotification(
                context = applicationContext,
                forOwner = forOwner,
                title = nTitle,
                body = nBody,
                type = type,
                orderId = orderId
            )
        } catch (e: Exception) {
            Log.e("MyFirebaseService", "Failed to save notification locally", e)
        }

        // Show system notification (outside app + inside)
        try {
            ensureChannel(applicationContext)
            val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // ensure this resource exists
                .setContentTitle(nTitle)
                .setContentText(nBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            // Optional: add tap intent to open MainActivity to a specific screen
            // val pendingIntent = PendingIntent.getActivity(...)
            // builder.setContentIntent(pendingIntent)

            with(NotificationManagerCompat.from(applicationContext)) {
                notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
            }
        } catch (e: Exception) {
            Log.e("MyFirebaseService", "Failed to show system notification", e)
        }
    }
}
