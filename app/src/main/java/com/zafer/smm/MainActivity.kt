@file:Suppress("UnusedImport")

package com.zafer.smm

import android.os.Bundle
import android.os.Build
import android.widget.Toast
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/* =========================
   Theme — داكن احترافي
   ========================= */
private val Bg       = Color(0xFF0F1115)
private val Surface1 = Color(0xFF151821)
private val Surface2 = Color(0xFF1E2230)
private val OnBg     = Color(0xFFE9EAEE)
private val Accent   = Color(0xFFFFD54F) // أصفر بارز
private val Mint     = Color(0xFF4CD964) // أخضر للأزرار البارزة

// عنوان باك-إند هيروكو — بدون سلاش أخير
private const val BASE_URL = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com"

/* =========================
   MainActivity
   ========================= */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { AppRoot() } }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Surface1,
            surfaceVariant = Surface2,
            primary = Accent,
            secondary = Mint,
            onBackground = OnBg,
            onSurface = OnBg,
            onPrimary = Color(0xFF111111),
            onSecondary = Color(0xFF10311F)
        ),
        typography = Typography(),
        content = content
    )
}

/* =========================
   Screens
   ========================= */
enum class Screen { HOME, SERVICES, ORDERS, WALLET, SUPPORT, OWNER }

/* =========================
   Models
   ========================= */
data class Order(
    val id: Int,
    val userId: Int,
    val category: String,
    val serviceName: String,
    val qty: Int,
    val price: Double,
    val status: String,
    val link: String,
    val ts: Long = System.currentTimeMillis()
)
data class CardSubmission(val userId: Int, val digits: String, val ts: Long = System.currentTimeMillis())

/* =========================
   ViewModel (بدون تعديل سعر/كمية)
   ========================= */
private const val OWNER_PIN = "123456"

class AppViewModel : ViewModel() {
    val currentUserId = 1

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner
    fun enableOwner() { _isOwner.value = true }
    fun disableOwner() { _isOwner.value = false }

    private val _balance = MutableStateFlow(15.75)
    val balance: StateFlow<Double> = _balance

    private val _moderators = MutableStateFlow<Set<Int>>(emptySet())
    val moderators: StateFlow<Set<Int>> = _moderators

    // قوائم الخدمات (سعر نهائي للبند الظاهر)
    val servicesTikIgViewsLikesScore = linkedMapOf(
        "لايكات تيكتوك 1k" to 1.0,
        "لايكات تيكتوك 2k" to 2.0,
        "لايكات تيكتوك 3k" to 3.0,
        "لايكات تيكتوك 4k" to 4.0,
        "متابعين تيكتوك 1k" to 3.50,
        "متابعين تيكتوك 2k" to 7.0,
        "متابعين تيكتوك 3k" to 10.50,
        "متابعين تيكتوك 4k" to 14.0,
        "مشاهدات تيكتوك 1k" to 0.10,
        "مشاهدات تيكتوك 10k" to 0.80,
        "مشاهدات تيكتوك 20k" to 1.60,
        "مشاهدات تيكتوك 30k" to 2.40,
        "مشاهدات تيكتوك 50k" to 3.20,
        "لايكات انستغرام 1k" to 1.0,
        "لايكات انستغرام 2k" to 2.0,
        "لايكات انستغرام 3k" to 3.0,
        "لايكات انستغرام 4k" to 4.0,
        "مشاهدات انستغرام 10k" to 0.80,
        "مشاهدات انستغرام 20k" to 1.60,
        "مشاهدات انستغرام 30k" to 2.40,
        "مشاهدات انستغرام 50k" to 3.20,
        "مشاهدات بث تيكتوك 1k" to 2.0,
        "مشاهدات بث تيكتوك 2k" to 4.0,
        "مشاهدات بث تيكتوك 3k" to 6.0,
        "مشاهدات بث تيكتوك 4k" to 8.0,
        "مشاهدات بث انستغرام 1k" to 2.0,
        "مشاهدات بث انستغرام 2k" to 4.0,
        "مشاهدات بث انستغرام 3k" to 6.0,
        "مشاهدات بث انستغرام 4k" to 8.0,
        "رفع سكور بثك1k" to 2.0,
        "رفع سكور بثك2k" to 4.0,
        "رفع سكور بثك3k" to 6.0,
        "رفع سكور بثك10k" to 20.0,
    )
    val servicesTelegram = linkedMapOf(
        "اعضاء قنوات تلي 1k" to 3.0,
        "اعضاء قنوات تلي 2k" to 6.0,
        "اعضاء قنوات تلي 3k" to 9.0,
        "اعضاء قنوات تلي 4k" to 12.0,
        "اعضاء قنوات تلي 5k" to 15.0,
        "اعضاء كروبات تلي 1k" to 3.0,
        "اعضاء كروبات تلي 2k" to 6.0,
        "اعضاء كروبات تلي 3k" to 9.0,
        "اعضاء كروبات تلي 4k" to 12.0,
        "اعضاء كروبات تلي 5k" to 15.0,
    )
    val servicesPubg = linkedMapOf(
        "ببجي 60 شدة" to 2.0,
        "ببجي 120 شده" to 4.0,
        "ببجي 180 شدة" to 6.0,
        "ببجي 240 شدة" to 8.0,
        "ببجي 325 شدة" to 9.0,
        "ببجي 660 شدة" to 15.0,
        "ببجي 1800 شدة" to 40.0,
    )
    val servicesItunes = linkedMapOf(
        "شراء رصيد 5 ايتونز" to 9.0,
        "شراء رصيد 10 ايتونز" to 18.0,
        "شراء رصيد 15 ايتونز" to 27.0,
        "شراء رصيد 20 ايتونز" to 36.0,
        "شراء رصيد 25 ايتونز" to 45.0,
        "شراء رصيد 30 ايتونز" to 54.0,
        "شراء رصيد 35 ايتونز" to 63.0,
        "شراء رصيد 40 ايتونز" to 72.0,
        "شراء رصيد 45 ايتونز" to 81.0,
        "شراء رصيد 50 ايتونز" to 90.0,
    )
    val servicesMobile = linkedMapOf(
        "شراء رصيد 2دولار اثير" to 3.5,
        "شراء رصيد 5دولار اثير" to 7.0,
        "شراء رصيد 10دولار اثير" to 13.0,
        "شراء رصيد 15دولار اثير" to 19.0,
        "شراء رصيد 40دولار اثير" to 52.0,
        "شراء رصيد 2دولار اسيا" to 3.5,
        "شراء رصيد 5دولار اسيا" to 7.0,
        "شراء رصيد 10دولار اسيا" to 13.0,
        "شراء رصيد 15دولار اسيا" to 19.0,
        "شراء رصيد 40دولار اسيا" to 52.0,
        "شراء رصيد 2دولار كورك" to 3.5,
        "شراء رصيد 5دولار كورك" to 7.0,
        "شراء رصيد 10دولار كورك" to 13.0,
        "شراء رصيد 15دولار كورك" to 19.0,
    )
    val servicesLudo = linkedMapOf(
        "لودو 810 الماسة" to 4.0,
        "لودو 2280 الماسة" to 8.9,
        "لودو 5080 الماسة" to 17.5,
        "لودو 12750 الماسة" to 42.7,
        "لودو 66680 ذهب" to 4.0,
        "لودو 219500 ذهب" to 8.9,
        "لودو 1443000 ذهب" to 17.5,
        "لودو 3627000 ذهب" to 42.7,
    )

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders
    private var orderAutoId = 10000

    private val cardSubmissions = mutableListOf<CardSubmission>()
    private val CARD_DUP_LIMIT = 2
    private val CARD_SPAM_COUNT = 5
    private val CARD_SPAM_WINDOW_MS = 120_000L

    fun isModerator(userId: Int) = _moderators.value.contains(userId)
    fun addModerator(userId: Int) { _moderators.value = _moderators.value + userId }
    fun removeModerator(userId: Int) { _moderators.value = _moderators.value - userId }

    fun addOrder(userId: Int, category: String, service: String, qty: Int, price: Double, link: String) {
        val o = Order(++orderAutoId, userId, category, service, qty, price, "pending", link)
        _orders.value = listOf(o) + _orders.value
    }

    fun addBalance(a: Double) { _balance.value = (_balance.value + a).coerceAtLeast(0.0) }
    fun withdrawBalance(a: Double): Boolean = if (a <= _balance.value) { _balance.value -= a; true } else false

    fun submitCard(userId: Int, digits: String): Pair<Boolean, String> {
        val now = System.currentTimeMillis()
        val dup = cardSubmissions.count { it.userId == userId && it.digits == digits }
        if (dup > CARD_DUP_LIMIT) return false to "مرفوض: تكرار لنفس رقم الكارت"
        val recent = cardSubmissions.count { it.userId == userId && it.ts >= now - CARD_SPAM_WINDOW_MS }
        if (recent > CARD_SPAM_COUNT) return false to "مرفوض: محاولات كثيرة خلال وقت قصير"
        cardSubmissions += CardSubmission(userId, digits, now)
        return true to "تم إرسال الكارت للمراجعة"
    }
}

/* =========================
   Helpers — UID + تخزين محلي + API
   ========================= */
private const val PREFS = "ratluzen_prefs"
private const val KEY_UID = "user_uid"
private const val KEY_LOGGED_IN = "logged_in"

private fun getOrCreateUserUid(context: Context): String {
    val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val existing = sp.getString(KEY_UID, null)
    if (existing != null) return existing
    val newId = "U" + (100000..999999).random()
    sp.edit().putString(KEY_UID, newId).apply()
    return newId
}
private fun setUid(context: Context, newUid: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_UID, newUid).apply()
}
private fun readUid(context: Context): String =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_UID, "") ?: ""

private fun isLoggedIn(context: Context): Boolean {
    val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return sp.getBoolean(KEY_LOGGED_IN, false)
}
private fun setLoggedIn(context: Context, v: Boolean) {
    val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    sp.edit().putBoolean(KEY_LOGGED_IN, v).apply()
}

@Composable
private fun rememberUserUid(): String {
    val ctx = LocalContext.current
    return remember { getOrCreateUserUid(ctx) }
}

private fun extractQtyFromName(name: String): Int {
    val k = Regex("(\\d+)\\s*k", RegexOption.IGNORE_CASE).find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (k != null) return k * 1000
    return Regex("(\\d+)").findAll(name).lastOrNull()?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
}

/* ========= شبكة عامة ========= */
private fun httpGet(urlStr: String, connectMs: Int = 12000, readMs: Int = 12000): Pair<Int, String> {
    val url = URL(urlStr)
    val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = connectMs
        readTimeout = readMs
        requestMethod = "GET"
    }
    val code = conn.responseCode
    val body = try {
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    } catch (_: Exception) { "" }
    conn.disconnect()
    return code to body
}
private fun httpJson(
    urlStr: String,
    method: String,
    body: String,
    connectMs: Int = 12000,
    readMs: Int = 12000
): Pair<Int, String> {
    val url = URL(urlStr)
    val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = connectMs
        readTimeout = readMs
        requestMethod = method
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    }
    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    val code = conn.responseCode
    val resp = try {
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    } catch (_: Exception) { "" }
    conn.disconnect()
    return code to resp
}

/* ========= /health ========= */
private suspend fun pingHealth(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val (c1, b1) = httpGet("$BASE_URL/health", 6000, 6000)
        if (c1 == 200 && b1.contains("\"ok\"")) return@withContext true to b1
        httpGet("$BASE_URL/", 6000, 6000) // إيقاظ هيروكو
        delay(1500)
        val (c2, b2) = httpGet("$BASE_URL/health", 15000, 15000)
        (c2 == 200 && b2.contains("\"ok\"")) to b2
    } catch (e: Exception) {
        false to (e.message ?: "Network error")
    }
}

/* ========= التحقق من وجود UID ========= */
private suspend fun checkUidExists(uid: String): Boolean = withContext(Dispatchers.IO) {
    val candidates = listOf(
        "$BASE_URL/auth/exists?uid=$uid",
        "$BASE_URL/users/exists?uid=$uid",
        "$BASE_URL/user/exists?uid=$uid",
        "$BASE_URL/api/users/exists?uid=$uid",
        "$BASE_URL/users/$uid",
        "$BASE_URL/api/users/$uid"
    )
    try {
        for (u in candidates) {
            val (code, body) = httpGet(u)
            if (code == 200) {
                val norm = body.lowercase()
                if (norm.contains("\"exists\":true") || norm.contains(":true") || norm.trim() == "true") return@withContext true
                if (norm.contains(uid.lowercase()) && !norm.contains("not found") && !norm.contains("false")) return@withContext true
            } else if (code == 404) {
                return@withContext false
            }
        }
        false
    } catch (_: Exception) { false }
}

/* ========= تسجيل UID جديد تلقائيًا إذا لم يكن موجودًا ========= */
private suspend fun registerUidIfMissing(uid: String): Boolean = withContext(Dispatchers.IO) {
    val payload = """{"uid":"$uid","device_model":"${Build.MODEL}","platform":"android"}"""
    val candidates = listOf(
        Triple("$BASE_URL/auth/register", "POST", """{"uid":"$uid"}"""),
        Triple("$BASE_URL/users", "POST", payload),
        Triple("$BASE_URL/api/users", "POST", payload),
        Triple("$BASE_URL/users/$uid", "PUT", payload),
        Triple("$BASE_URL/api/users/$uid", "PUT", payload)
    )
    try {
        for ((u, m, b) in candidates) {
            val (code, body) = httpJson(u, m, b)
            if (code in 200..201) return@withContext true
            if (code == 409) return@withContext true
            if (code == 200 && body.lowercase().contains("exists")) return@withContext true
        }
        false
    } catch (_: Exception) { false }
}

/* ========= ضمان وجود المستخدم (exists أو register) ========= */
private suspend fun ensureUser(uid: String): Boolean {
    val (ok, _) = pingHealth()
    if (!ok) return false
    return if (checkUidExists(uid)) true else registerUidIfMissing(uid)
}

/* =========================
   Root + PIN + تدفق التسجيل التلقائي
   ========================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val ctx = LocalContext.current
    var current by rememberSaveable { mutableStateOf(Screen.HOME) }
    val isOwner by vm.isOwner.collectAsState()
    val drawer = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // حالة تسجيل محلي + UID
    var loggedIn by remember { mutableStateOf(isLoggedIn(ctx)) }
    val localUid = rememberUserUid()

    // تسجيل تلقائي عند أول تشغيل
    LaunchedEffect(Unit) {
        if (!loggedIn) {
            val ok = ensureUser(localUid)
            if (ok) {
                setLoggedIn(ctx, true)
                loggedIn = true
                Toast.makeText(ctx, "تم إنشاء/تفعيل الحساب تلقائيًا ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, "تعذر إنشاء الحساب الآن. حاول لاحقًا.", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showPin by remember { mutableStateOf(false) }
    var taps by remember { mutableStateOf(0) }
    var lastTap by remember { mutableStateOf(0L) }

    // حوار تسجيل الدخول اليدوي لاستعادة حساب من جهاز آخر
    var showLoginDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            DrawerContent(current, isOwner) {
                current = it
                scope.launch { drawer.close() }
            }
        }
    ) {
        Scaffold(
            containerColor = Bg,
            topBar = {
                TopBar(
                    balanceFlow = vm.balance,
                    onMenu = { scope.launch { drawer.open() } },
                    onLogoTapped = {
                        val now = System.currentTimeMillis()
                        if (now - lastTap > 2000) taps = 0
                        taps++; lastTap = now
                        if (taps >= 5) { taps = 0; showPin = true }
                    },
                    onSearch = { current = Screen.SERVICES },
                    onCloudCheck = {
                        Toast.makeText(ctx, "جاري فحص السيرفر…", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            val (ok, _) = pingHealth()
                            Toast.makeText(
                                ctx,
                                if (ok) "السيرفر متصل ✅" else "تعذر الاتصال بالسيرفر ❌",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            },
            bottomBar = { BottomNav(current, isOwner) { current = it } },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    containerColor = Mint,
                    contentColor = Color.Black,
                    onClick = { current = Screen.SUPPORT },
                    text = { Text("راسل الدعم", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.ChatBubble, contentDescription = null) }
                )
            },
            floatingActionButtonPosition = FabPosition.Center,
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (current) {
                    Screen.HOME     -> HomeScreen(vm,
                        open = { current = it },
                        onOpenLogin = { showLoginDialog = true }
                    )
                    Screen.SERVICES -> ServicesScreen(vm)
                    Screen.ORDERS   -> OrdersScreen(vm)
                    Screen.WALLET   -> WalletScreen(vm)
                    Screen.SUPPORT  -> SupportScreen()
                    Screen.OWNER    -> OwnerDashboard(vm)
                }
            }
        }
    }

    if (showPin) {
        OwnerPinDialog(
            isOwner = isOwner,
            onDismiss = { showPin = false },
            onEnable = { if (it == OWNER_PIN) vm.enableOwner() },
            onDisable = { vm.disableOwner() }
        )
    }

    if (showLoginDialog) {
        LoginDialog(
            onClose = { showLoginDialog = false },
            onSwitchUid = { inputUid ->
                scope.launch {
                    if (inputUid.isBlank()) return@launch
                    val ok = checkUidExists(inputUid)
                    if (ok) {
                        setUid(ctx, inputUid)
                        setLoggedIn(ctx, true)
                        Toast.makeText(ctx, "تم تسجيل الدخول واستعادة الحساب ✅", Toast.LENGTH_SHORT).show()
                        showLoginDialog = false
                    } else {
                        Toast.makeText(ctx, "UID غير موجود في قاعدة البيانات ❌", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onCreateNew = {
                scope.launch {
                    // إنشاء UID جديد عشوائي وتسجيله فورًا
                    val newUid = "U" + (100000..999999).random()
                    val created = registerUidIfMissing(newUid)
                    if (created) {
                        setUid(ctx, newUid)
                        setLoggedIn(ctx, true)
                        Toast.makeText(ctx, "تم إنشاء حساب جديد وتسجيل الدخول ✅ ($newUid)", Toast.LENGTH_LONG).show()
                        showLoginDialog = false
                    } else {
                        Toast.makeText(ctx, "تعذر إنشاء حساب جديد الآن ❌", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

/* =========================
   شاشة/حوار تسجيل الدخول البسيط (إدخال UID)
   ========================= */
@Composable
private fun LoginDialog(
    onClose: () -> Unit,
    onSwitchUid: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    var uid by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("تسجيل الدخول / استعادة حساب") },
        text = {
            Column {
                Text("أدخل UID الخاص بك لاستعادة حسابك من أي جهاز.", color = OnBg.copy(alpha = 0.8f))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it.trim() },
                    label = { Text("UID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = onCreateNew,
                    label = { Text("أريد حسابًا جديدًا") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSwitchUid(uid) }) { Text("تسجيل الدخول") } },
        dismissButton = { TextButton(onClick = onClose) { Text("إلغاء") } }
    )
}

/* =========================
   Top Bar — اسم التطبيق
   ========================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    balanceFlow: StateFlow<Double>,
    onMenu: () -> Unit,
    onLogoTapped: () -> Unit,
    onSearch: () -> Unit,
    onCloudCheck: () -> Unit
) {
    val balance by balanceFlow.collectAsState()
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Bg,
            titleContentColor = OnBg
        ),
        navigationIcon = {
            Icon(
                Icons.Filled.Menu,
                contentDescription = "القائمة",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(CircleShape)
                    .clickable { onMenu() }
                    .padding(8.dp)
            )
        },
        title = {
            Text(
                text = "خدمات راتلوزن",
                color = OnBg,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clickable { onLogoTapped() }
            )
        },
        actions = {
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("د.ع ${"%.2f".format(balance)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Accent)
                }
            }
            Icon(
                Icons.Filled.CloudQueue, contentDescription = "السيرفر",
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(CircleShape)
                    .clickable { onCloudCheck() }
                    .padding(8.dp)
            )
            Icon(
                Icons.Filled.Search, contentDescription = "بحث",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .clickable { onSearch() }
                    .padding(8.dp)
            )
        }
    )
}

/* =========================
   Drawer
   ========================= */
@Composable
private fun DrawerContent(current: Screen, isOwner: Boolean, onSelect: (Screen) -> Unit) {
    Column(Modifier.padding(14.dp)) {
        Text("القوائم", color = OnBg, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(6.dp))
        NavigationDrawerItem(label = { Text("الواجهة") }, selected = current == Screen.HOME, onClick = { onSelect(Screen.HOME) }, icon = { Icon(Icons.Filled.Home, null) })
        NavigationDrawerItem(label = { Text("الخدمات") }, selected = current == Screen.SERVICES, onClick = { onSelect(Screen.SERVICES) }, icon = { Icon(Icons.Filled.List, null) })
        NavigationDrawerItem(label = { Text("الطلبات") }, selected = current == Screen.ORDERS, onClick = { onSelect(Screen.ORDERS) }, icon = { Icon(Icons.Filled.ShoppingCart, null) })
        NavigationDrawerItem(label = { Text("المحفظة") }, selected = current == Screen.WALLET, onClick = { onSelect(Screen.WALLET) }, icon = { Icon(Icons.Filled.AccountBalanceWallet, null) })
        NavigationDrawerItem(label = { Text("الدعم") }, selected = current == Screen.SUPPORT, onClick = { onSelect(Screen.SUPPORT) }, icon = { Icon(Icons.Filled.Chat, null) })
        if (isOwner) NavigationDrawerItem(label = { Text("لوحة المالك") }, selected = current == Screen.OWNER, onClick = { onSelect(Screen.OWNER) }, icon = { Icon(Icons.Filled.Settings, null) })
    }
}

/* =========================
   Bottom Nav
   ========================= */
@Composable
private fun BottomNav(current: Screen, isOwner: Boolean, onSelect: (Screen) -> Unit) {
    NavigationBar(containerColor = Surface1, tonalElevation = 3.dp) {
        val items = listOf(
            Triple(Screen.OWNER, Icons.Filled.Settings, "الإعدادات"),
            Triple(Screen.SERVICES, Icons.Filled.List, "الخدمات"),
            Triple(Screen.ORDERS, Icons.Filled.ShoppingCart, "الطلبات"),
            Triple(Screen.SUPPORT, Icons.Filled.ChatBubble, "الدعم"),
            Triple(Screen.HOME, Icons.Filled.Home, "الرئيسية"),
        )
        items.forEach { (scr, icon, label) ->
            val enabled = if (scr == Screen.OWNER) isOwner else true
            NavigationBarItem(
                selected = current == scr,
                onClick = { if (enabled) onSelect(scr) else onSelect(Screen.SUPPORT) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

/* =========================
   HOME — UID + زر تسجيل الدخول مرتب
   ========================= */
@Composable
fun HomeScreen(vm: AppViewModel, open: (Screen) -> Unit, onOpenLogin: () -> Unit) {
    val userUid = rememberUserUid()
    val banners = listOf(
        Color(0xFF141821) to Color(0xFF252A39),
        Color(0xFF19202A) to Color(0xFF2C3446),
        Color(0xFF1A1F2A) to Color(0xFF2A3042)
    )
    var bannerIndex by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(Bg)) {

        // شارة UID قابلة للنسخ + زر تسجيل الدخول/التبديل
        UserIdRow(userUid, onOpenLogin)

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(banners.size) { i ->
                BannerCard(
                    modifier = Modifier
                        .width(320.dp)
                        .height(160.dp)
                        .clickable { bannerIndex = i },
                    start = banners[i].first,
                    end = banners[i].second
                )
            }
        }
        Dots(count = banners.size, active = bannerIndex)

        Spacer(Modifier.height(8.dp))
        Row(Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الخدمات", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            AssistChip(onClick = { open(Screen.SERVICES) }, label = { Text("عرض الكل") })
        }
        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            CategoryIcon("الكل", Icons.Filled.Apps, selected = true) { open(Screen.SERVICES) }
            CategoryIcon("سوشيال", Icons.Filled.Group, selected = false) { open(Screen.SERVICES) }     // ← تمت إضافة selected
            CategoryIcon("تليجرام", Icons.Filled.Send, selected = false) { open(Screen.SERVICES) }      // ← تمت إضافة selected
            CategoryIcon("الألعاب", Icons.Filled.SportsEsports, selected = false) { open(Screen.SERVICES) } // ← تمت إضافة selected
            CategoryIcon("شحن/رصيد", Icons.Filled.CreditCard, selected = false) { open(Screen.SERVICES) }   // ← تمت إضافة selected
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ProductCard(
                        title = "تيكتوك / انستغرام",
                        tag = "مميز",
                        colors = listOf(Color(0xFF2D2A49), Color(0xFF5F5DB8))
                    ) { open(Screen.SERVICES) }
                    ProductCard(
                        title = "تليجرام",
                        tag = "مطلوب",
                        colors = listOf(Color(0xFF1F2A3E), Color(0xFF4C77B1))
                    ) { open(Screen.SERVICES) }
                    ProductCard(
                        title = "شحن ببجي",
                        tag = "شائع",
                        colors = listOf(Color(0xFF0E3B2E), Color(0xFF15A979))
                    ) { open(Screen.SERVICES) }
                }
            }
        }
    }
}

@Composable
private fun UserIdRow(uid: String, onOpenLogin: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2)
                .clickable {
                    clipboard.setText(AnnotatedString(uid))
                    Toast.makeText(ctx, "تم نسخ المعرّف: $uid", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Badge, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text("معرّفك: $uid (انقر للنسخ)", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.width(8.dp))
        ElevatedButton(
            onClick = onOpenLogin,
            shape = RoundedCornerShape(12.dp)
        ) { Text("تسجيل الدخول") }
    }
}

@Composable
private fun BannerCard(modifier: Modifier = Modifier, start: Color, end: Color) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(start, end)))
    ) { }
}

@Composable
private fun Dots(count: Int, active: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            val w = if (i == active) 10.dp else 6.dp
            Box(
                Modifier
                    .padding(3.dp)
                    .size(w)
                    .clip(CircleShape)
                    .background(if (i == active) Accent else Surface2)
            )
        }
    }
}

@Composable
private fun CategoryIcon(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 76.dp)) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (selected) Surface2.copy(alpha = 0.9f) else Surface2)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = title, tint = if (selected) Accent else OnBg) }
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProductCard(title: String, tag: String, colors: List<Color>, onClick: () -> Unit) {
    Column(
        Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .clickable { onClick() }
    ) {
        Box(
            Modifier
                .height(180.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(title, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF203A22))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Mint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tag, fontSize = 12.sp, color = Mint)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* =========================
   SERVICES — أقسام + أزرار خدمات
   ========================= */
data class BuyInfo(val service: String, val qty: Int, val price: Double)

@Composable
fun ServicesScreen(vm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    var buy by remember { mutableStateOf<BuyInfo?>(null) }

    val blocks = listOf(
        "لايكات/مشاهدات/متابعين" to vm.servicesTikIgViewsLikesScore,
        "تليجرام" to vm.servicesTelegram,
        "ببجي" to vm.servicesPubg,
        "ايتونز" to vm.servicesItunes,
        "شحن الموبايل" to vm.servicesMobile,
        "لودو" to vm.servicesLudo
    )
    var selected by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEachIndexed { i, (title, _) ->
                FilterChip(
                    selected = selected == i,
                    onClick = { selected = i },
                    label = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("ابحث داخل القسم المحدد") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        val data = blocks[selected].second
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data.toList(), key = { it.first }) { (svc, price) ->
                if (query.isNotBlank() && !svc.contains(query, true)) return@items
                ServiceButton(
                    label = labelForButton(svc, price),
                    onClick = {
                        val qty = extractQtyFromName(svc)
                        buy = BuyInfo(svc, qty, price)
                    }
                )
            }
        }
    }

    buy?.let { info ->
        var open by remember { mutableStateOf(true) }
        var link by remember { mutableStateOf("") }
        if (open) {
            AlertDialog(
                onDismissRequest = { open = false; buy = null },
                title = { Text("شراء: ${info.service}") },
                text = {
                    Column {
                        Text("الكمية: ${info.qty} — السعر: ${"%.2f".format(info.price)} $")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("الرابط/المعرف") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.addOrder(vm.currentUserId, "smm", info.service, info.qty, info.price, link)
                        open = false; buy = null
                    }) { Text("تأكيد") }
                },
                dismissButton = { TextButton(onClick = { open = false; buy = null }) { Text("إلغاء") } }
            )
        }
    }
}

@Composable
private fun ServiceButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = OnBg)
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun labelForButton(svc: String, price: Double): String {
    val qtyRaw = Regex("(\\d+\\s*k|\\d+)").find(svc)?.value ?: ""
    val qty = qtyRaw.replace("k", "000", ignoreCase = true).trim()
    val name = svc.replace(qtyRaw, "").trim()
    return if (qty.isNotBlank()) "(${qty}) - $${"%.1f".format(price)}  $name" else "$name - $${"%.1f".format(price)}"
}

/* =========================
   Orders
   ========================= */
@Composable
fun OrdersScreen(vm: AppViewModel) {
    val orders by vm.orders.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("طلباتي", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (orders.isEmpty()) Text("لا توجد طلبات بعد.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(orders, key = { it.id }) { o ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2)
                        .padding(12.dp)
                ) {
                    Text("#${o.id} • ${o.serviceName}", fontWeight = FontWeight.SemiBold)
                    Text("${o.category} • ${o.qty} • ${"%.2f".format(o.price)}$")
                    Text("الحالة: ${o.status}")
                }
            }
        }
    }
}

/* =========================
   Wallet
   ========================= */
@Composable
fun WalletScreen(vm: AppViewModel) {
    val balance by vm.balance.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showWd by remember { mutableStateOf(false) }
    var digits by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("المحفظة", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface2)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الرصيد الحالي")
                Text(String.format("%.2f $", balance), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ElevatedButton(onClick = { showAdd = true }) { Text("إيداع") }
                    OutlinedButton(onClick = { showWd = true }) { Text("سحب") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("إرسال كارت آسياسيل (حماية ضد التكرار/السبام):")
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = digits, onValueChange = { digits = it }, label = { Text("رقم الكارت") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        ElevatedButton(onClick = {
            val (ok, m) = vm.submitCard(vm.currentUserId, digits.trim())
            msg = m; if (ok) digits = ""
        }) { Text("إرسال الكارت") }
        msg?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
    }

    if (showAdd) {
        var t by remember { mutableStateOf("5.00") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("إضافة رصيد") },
            text = { OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("القيمة بالدولار") }) },
            confirmButton = { TextButton(onClick = { t.toDoubleOrNull()?.let { vm.addBalance(it); showAdd = false } }) { Text("تأكيد") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("إلغاء") } }
        )
    }
    if (showWd) {
        var t by remember { mutableStateOf("2.00") }
        var e by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showWd = false },
            title = { Text("سحب رصيد") },
            text = {
                Column {
                    OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("القيمة بالدولار") })
                    e?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    t.toDoubleOrNull()?.let { v ->
                        if (!vm.withdrawBalance(v)) e = "الرصيد غير كافٍ" else showWd = false
                    }
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showWd = false }) { Text("إلغاء") } }
        )
    }
}

/* =========================
   Support
   ========================= */
@Composable
fun SupportScreen() {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("الدعم", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("تواصل معنا:\n- تيليجرام: @your_channel\n- البريد: support@example.com")
    }
}

/* =========================
   Owner Dashboard — إدارة المشرفين فقط
   ========================= */
@Composable
fun OwnerDashboard(vm: AppViewModel) {
    val moderators by vm.moderators.collectAsState()
    var modId by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("لوحة تحكم المالك", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("إدارة المشرفين (خصم 10%)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = modId, onValueChange = { modId = it }, label = { Text("User ID رقمي") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = { modId.toIntOrNull()?.let { vm.addModerator(it) } }) { Text("إضافة مشرف") }
                    OutlinedButton(onClick = { modId.toIntOrNull()?.let { vm.removeModerator(it) } }) { Text("حذف مشرف") }
                }
                Spacer(Modifier.height(8.dp))
                Text("قائمة المشرفين:")
                if (moderators.isEmpty()) Text("- لا يوجد") else moderators.sorted().forEach { Text("- $it") }
            }
        }
    }
}

/* =========================
   PIN Dialog
   ========================= */
@Composable
private fun OwnerPinDialog(isOwner: Boolean, onDismiss: () -> Unit, onEnable: (String) -> Unit, onDisable: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isOwner) "وضع المالك (مفعّل)" else "تفعيل وضع المالك") },
        text = {
            if (!isOwner) OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("أدخل PIN") })
            else Text("يمكنك تعطيل وضع المالك من هنا.")
        },
        confirmButton = {
            if (!isOwner) TextButton(onClick = { if (pin.isNotBlank()) { onEnable(pin); onDismiss() } }) { Text("تفعيل") }
            else TextButton(onClick = { onDisable(); onDismiss() }) { Text("تعطيل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}
