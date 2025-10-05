package your.package.name

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

private const val PREFS = "app_prefs"
private const val KEY_UID = "uid"

// عدّل على مزاجك
private const val BASE_URL = "https://your-backend.example.com" // غيّر إلى سيرفرك
private val JSON = "application/json; charset=utf-8".toMediaType()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MinimalApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalApp() {
    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var serverStatus by remember { mutableStateOf<String?>(null) } // نعرضها بنص صغير
    var uid by remember { mutableStateOf<String?>(null) }

    // تسجيل/قراءة UID مرّة واحدة عند الإقلاع
    LaunchedEffect(Unit) {
        uid = ensureUid(ctx)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* نفس الشاشة */ },
                    icon = { Text("🏠", fontSize = 16.sp) },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* نفس الشاشة */ },
                    icon = { Text("🖥️", fontSize = 16.sp) },
                    label = { Text("السيرفر") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // عنوان بسيط وصغير (مو كبير)
            Text(
                text = "فحص السيرفر",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(12.dp))

            // سطر حالة السيرفر بخط صغير جدًا
            Text(
                text = serverStatus?.let { "الحالة: $it" } ?: "الحالة: غير معروف",
                style = MaterialTheme.typography.bodySmall // ← صغير
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    // فحص /health
                    val ok = checkServerHealth()
                    serverStatus = if (ok) "متصل" else "غير متصل"
                    // Snackbar بنص صغير افتراضي
                    val msg = if (ok) "✅ السيرفر متصل الآن" else "❌ السيرفر غير متصل حاليًا"
                    LaunchedEffect(msg) {
                        snackbarHostState.showSnackbar(message = msg)
                    }
                }
            ) {
                Text("تحقق من السيرفر")
            }

            Spacer(Modifier.height(24.dp))

            // للمرجعية: نعرض UID بنص صغير فقط (اختياري؛ احذفه إن ما تريده)
            uid?.let {
                Text("UID: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** يعيد UID من التخزين المحلي أو يسجّله من السيرفر ثم يخزّنه محليًا. */
private fun ensureUid(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val existing = prefs.getString(KEY_UID, null)
    if (existing != null) return existing

    // مافي UID محلي → نحاول التسجيل في السيرفر
    val created = try {
        registerUserOnServer()
    } catch (_: Exception) {
        null
    }

    val finalUid = created ?: UUID.randomUUID().toString() // fallback محلي
    prefs.edit().putString(KEY_UID, finalUid).apply()
    return finalUid
}

/** ينادي POST /users/register ويعيد uid من الاستجابة. */
private fun registerUserOnServer(): String? {
    val client = OkHttpClient()
    val payload = JSONObject().apply {
        put("platform", "android")
        put("app_version", "1.0.0")
    }.toString().toRequestBody(JSON)

    val req = Request.Builder()
        .url("$BASE_URL/users/register")
        .post(payload)
        .build()

    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return null
        val body = resp.body?.string() ?: return null
        val json = JSONObject(body)
        return json.optString("uid", null)
    }
}

/** يفحص GET /health ويرجع true لو ok && db. */
private fun checkServerHealth(): Boolean {
    val client = OkHttpClient()
    val req = Request.Builder()
        .url("$BASE_URL/health")
        .get()
        .build()

    return try {
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val body = resp.body?.string() ?: return false
            val json = JSONObject(body)
            json.optBoolean("ok", false) && json.optBoolean("db", false)
        }
    } catch (e: Exception) {
        false
    }
}
