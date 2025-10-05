package com.zafer.smm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/* ======= ألوان بسيطة ======= */
private val Bg = Color(0xFF0F1115)
private val Surface1 = Color(0xFF151821)
private val Surface2 = Color(0xFF1E2230)
private val OnBg = Color(0xFFE9EAEE)
private val Accent = Color(0xFFFFD54F)
private val Mint = Color(0xFF4CD964)

/* ======= شاشات بدون Navigation Compose ======= */
private enum class Screen { HOME, SERVICES, ORDERS, WALLET, SUPPORT }

/* ======= ثابت عنوان السيرفر ======= */
private const val BASE_URL = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com"

/* ======= نشاط رئيسي ======= */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Surface(color = Bg) {
                AppRoot()
            }
        }
    }
}

/* ======= دوال شبكة خفيفة (بدون مكتبات إضافية) ======= */
private suspend fun httpGet(url: String): Pair<Int, String> = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 12_000
    conn.readTimeout = 12_000
    conn.inputStream.buffered().use { ins ->
        val text = ins.reader().readText()
        Pair(conn.responseCode, text)
    }
}

private suspend fun httpPostJson(url: String, jsonBody: String): Pair<Int, String> = withContext(Dispatchers.IO) {
    val bytes = jsonBody.toByteArray(Charsets.UTF_8)
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 12_000
    conn.readTimeout = 12_000
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
    conn.setRequestProperty("Content-Length", bytes.size.toString())
    conn.outputStream.use { it.write(bytes) }
    val status = conn.responseCode
    val stream = if (status in 200..299) conn.inputStream else conn.errorStream
    val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
    Pair(status, text)
}

/* ======= UID: إنشاء/حفظ محليًا + إرسال للسيرفر ======= */
private fun ensureUid(context: Context): String {
    val prefs = context.getSharedPreferences("smm_prefs", Context.MODE_PRIVATE)
    var uid = prefs.getString("uid", null)
    if (uid.isNullOrBlank()) {
        uid = "U" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
        prefs.edit().putString("uid", uid).apply()
    }
    return uid
}

private suspend fun upsertUidRemote(uid: String) {
    val body = JSONObject().put("uid", uid).toString()
    httpPostJson("$BASE_URL/api/users/upsert", body)
}

/* ======= جذر الواجهة (بدون TopAppBar) ======= */
@Composable
private fun AppRoot() {
    var current by rememberSaveable { mutableStateOf(Screen.HOME) }

    // حالة السيرفر
    var serverOk by remember { mutableStateOf<Boolean?>(null) }
    var serverMsg by remember { mutableStateOf<String?>(null) }

    // UID
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var uid by remember { mutableStateOf(ensureUid(ctx)) }

    // عند الإقلاع: أرسل UID وسوّي فحص صحة
    LaunchedEffect(Unit) {
        runCatching { upsertUidRemote(uid) }
        val (code, text) = runCatching { httpGet("$BASE_URL/health") }.getOrElse { 0 to it.message.orEmpty() }
        serverOk = (code in 200..299 && text.contains("\"status\":\"ok\""))
        serverMsg = if (serverOk == true) "متصل" else "غير متصل"
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            BottomNav(current) { current = it }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Bg)
        ) {
            when (current) {
                Screen.HOME -> HomeScreen(
                    uid = uid,
                    serverOk = serverOk,
                    serverMsg = serverMsg ?: ""
                )
                Screen.SERVICES -> ServicesScreenSimple()
                Screen.ORDERS -> OrdersScreenSimple()
                Screen.WALLET -> WalletScreenSimple()
                Screen.SUPPORT -> SupportScreenSimple()
            }
        }
    }
}

/* ======= Bottom Nav ======= */
@Composable
private fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    NavigationBar(containerColor = Surface1) {
        NavigationBarItem(
            selected = current == Screen.SERVICES,
            onClick = { onSelect(Screen.SERVICES) },
            icon = { androidx.compose.material3.Icon(Icons.Filled.List, contentDescription = "الخدمات") },
            label = { Text("الخدمات", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        NavigationBarItem(
            selected = current == Screen.ORDERS,
            onClick = { onSelect(Screen.ORDERS) },
            icon = { androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "الطلبات") },
            label = { Text("الطلبات", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        NavigationBarItem(
            selected = current == Screen.WALLET,
            onClick = { onSelect(Screen.WALLET) },
            icon = { androidx.compose.material3.Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "رصيدي") },
            label = { Text("رصيدي", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        NavigationBarItem(
            selected = current == Screen.SUPPORT,
            onClick = { onSelect(Screen.SUPPORT) },
            icon = { androidx.compose.material3.Icon(Icons.Filled.Chat, contentDescription = "الدعم") },
            label = { Text("الدعم", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        NavigationBarItem(
            selected = current == Screen.HOME,
            onClick = { onSelect(Screen.HOME) },
            icon = { androidx.compose.material3.Icon(Icons.Filled.Home, contentDescription = "الرئيسية") },
            label = { Text("الرئيسية", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
    }
}

/* ======= HOME: حالة السيرفر + UID ======= */
@Composable
private fun HomeScreen(uid: String, serverOk: Boolean?, serverMsg: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Surface2)) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dot = if (serverOk == true) Color(0xFF25D366) else if (serverOk == false) Color(0xFFFF3B30) else Color.Gray
                Box(
                    Modifier.width(12.dp).height(12.dp).clip(CircleShape).background(dot)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "حالة السيرفر: ${if (serverOk == null) "يتحقق..." else serverMsg}",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Surface2)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("معرّفك (UID)", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(uid, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    Text(
                        "نسخ",
                        color = Accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface1)
                            .clickable {
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("uid", uid))
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "يمكنك تسجيل الدخول على جهاز آخر عبر هذا UID وسيتم استرجاع رصيدك وطلباتك المخزّنة على الخادم.",
                    fontSize = 12.sp
                )
            }
        }
    }
}

/* ======= SERVICES: أقسام + خدمات بدون تعديل سعر/كمية ======= */
@Composable
private fun ServicesScreenSimple() {
    val categories = listOf("سوشيال", "تليجرام", "ببجي", "شحن/رصيد", "لودو")
    var selected by remember { mutableStateOf(categories.first()) }

    val data: Map<String, List<Pair<String, Double>>> = mapOf(
        "سوشيال" to listOf(
            "متابعين تيكتوك 1k" to 3.50,
            "مشاهدات تيكتوك 10k" to 0.80,
            "لايكات انستغرام 1k" to 1.0
        ),
        "تليجرام" to listOf(
            "اعضاء قنوات تلي 1k" to 3.0,
            "اعضاء كروبات تلي 1k" to 3.0
        ),
        "ببجي" to listOf(
            "ببجي 60 شدة" to 2.0,
            "ببجي 325 شدة" to 9.0
        ),
        "شحن/رصيد" to listOf(
            "شراء رصيد 5 ايتونز" to 9.0,
            "شراء رصيد 10 ايتونز" to 18.0
        ),
        "لودو" to listOf(
            "لودو 810 الماسة" to 3.0,
            "لودو 2280 الماسة" to 7.0
        )
    )

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val selectedColor = if (cat == selected) Accent else OnBg
                Text(
                    cat,
                    color = selectedColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .clickable { selected = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val items = data[selected].orEmpty()

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { (name, basePrice) ->
                ServiceCard(name = name, price = basePrice)
            }
        }
    }
}

@Composable
private fun ServiceCard(name: String, price: Double) {
    var showDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface2)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(String.format("%.2f $", price), color = Accent)
            }
            ElevatedButton(onClick = { showDialog = true }) {
                Text("شراء")
            }
        }
    }

    if (showDialog) {
        var link by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("شراء الخدمة") },
            text = {
                Column {
                    Text("الخدمة: $name")
                    Spacer(Modifier.height(8.dp))
                    TextField(value = link, onValueChange = { link = it }, label = { Text("الرابط/المعرف") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // مبدئياً فقط إغلاق — ربط /api/orders ممكن إضافته لاحقاً
                    showDialog = false
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("إلغاء") } }
        )
    }
}

/* ======= باقي الشاشات مبسطة (تجريبية) ======= */
@Composable
private fun OrdersScreenSimple() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("طلباتك", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("لا توجد طلبات حالياً.")
    }
}

@Composable
private fun WalletScreenSimple() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("رصيدي", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("سيتم ربط الرصيد بخادمك لاحقاً.")
    }
}

@Composable
private fun SupportScreenSimple() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الدعم", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("تيليجرام: @your_channel\nالبريد: support@example.com")
    }
}
