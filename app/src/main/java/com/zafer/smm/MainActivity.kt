@file:Suppress("UnusedImport", "UNCHECKED_CAST")

package com.zafer.smm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.Locale
import kotlin.math.max
import kotlin.math.round

/* =========================
   ثابتات أساسية
   ========================= */
private const val OWNER_PIN = "123456"
private const val SERVER_URL = "https://ratluzen-smm-backend-e12a704bf3c1.herokuapp.com"

/* =========================
   عميل HTTP خفيف
   ========================= */
object Api {
    private fun httpGet(path: String, timeout: Int = 8000): Pair<Int, String?> {
        val url = URL("$SERVER_URL$path")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeout
            readTimeout = timeout
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val body = stream?.bufferedReader(Charset.forName("UTF-8"))?.use(BufferedReader::readText)
            code to body
        } finally {
            c.disconnect()
        }
    }

    private fun httpPost(path: String, json: JSONObject, timeout: Int = 8000): Pair<Int, String?> {
        val url = URL("$SERVER_URL$path")
        val c = (url.openConnection() as HttpURLConnection).apply {
            doOutput = true
            requestMethod = "POST"
            connectTimeout = timeout
            readTimeout = timeout
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        val bytes = json.toString().toByteArray(Charset.forName("UTF-8"))
        return try {
            c.outputStream.use { it.write(bytes) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val body = stream?.bufferedReader(Charset.forName("UTF-8"))?.use(BufferedReader::readText)
            code to body
        } finally {
            c.disconnect()
        }
    }

    suspend fun health(): Boolean = runCatching {
        val candidates = listOf("/api/health", "/healthz", "/")
        for (p in candidates) {
            val (code, body) = httpGet(p)
            if (code in 200..299) return true
            if (body?.contains("OK", true) == true) return true
        }
        false
    }.getOrDefault(false)

    suspend fun upsertUser(uid: String): Boolean = runCatching {
        val payload = JSONObject().put("uid", uid)
        val (code, _) = httpPost("/api/users/upsert", payload)
        code in 200..299
    }.getOrDefault(false)

    suspend fun createOrder(uid: String, service: String, qty: Int, price: Double, link: String): Boolean =
        runCatching {
            val payload = JSONObject()
                .put("uid", uid)
                .put("service_name", service)
                .put("quantity", qty)
                .put("price", price)
                .put("link", link)
            val (code, _) = httpPost("/api/orders", payload)
            code in 200..299
        }.getOrDefault(false)
}

/* =========================
   الثيم
   ========================= */
private val Bg       = Color(0xFF0F1115)
private val Surface1 = Color(0xFF151821)
private val Surface2 = Color(0xFF1E2230)
private val OnBg     = Color(0xFFE9EAEE)
private val Accent   = Color(0xFFFFD54F)
private val Mint     = Color(0xFF4CD964)

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
   تفضيلات UID المحلية
   ========================= */
object Prefs {
    private const val PREF = "app_prefs"
    private const val KEY_UID = "uid"

    fun getUid(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_UID, null)

    fun saveUid(ctx: Context, uid: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_UID, uid).apply()
    }

    fun ensureUid(ctx: Context): String {
        val e = getUid(ctx)
        if (!e.isNullOrBlank()) return e
        val n = "U" + (100000..999999).random()
        saveUid(ctx, n)
        return n
    }
}

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

/* =========================
   الشاشات
   ========================= */
enum class Screen { HOME, SERVICES, ORDERS, WALLET, SUPPORT, OWNER }

/* =========================
   النماذج
   ========================= */
data class Order(
    val id: Int,
    val userUid: String,
    val category: String,
    val serviceName: String,
    val qty: Int,
    val price: Double,
    val status: String,
    val link: String,
    val ts: Long = System.currentTimeMillis()
)

/* =========================
   ViewModel
   ========================= */
class AppViewModel : ViewModel() {

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid

    private val _serverOk = MutableStateFlow(false)
    val serverOk: StateFlow<Boolean> = _serverOk

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner
    fun enableOwner() { _isOwner.value = true }
    fun disableOwner() { _isOwner.value = false }

    private val _balance = MutableStateFlow(15.75)
    val balance: StateFlow<Double> = _balance

    // الخدمات
    val servicesTikIgViewsLikesScore = linkedMapOf(
        "متابعين تيكتوك 1k" to 3.50,
        "متابعين تيكتوك 2k" to 7.0,
        "متابعين تيكتوك 3k" to 10.50,
        "متابعين تيكتوك 4k" to 14.0,
        "مشاهدات تيكتوك 1k" to 0.10,
        "مشاهدات تيكتوك 10k" to 0.80,
        "مشاهدات تيكتوك 20k" to 1.60,
        "مشاهدات تيكتوك 30k" to 2.40,
        "مشاهدات تيكتوك 50k" to 3.20,
        "متابعين انستغرام 1k" to 3.0,
        "متابعين انستغرام 2k" to 6.0,
        "متابعين انستغرام 3k" to 9.0,
        "متابعين انستغرام 4k" to 12.0,
        "لايكات تيكتوك 1k" to 1.0,
        "لايكات تيكتوك 2k" to 2.0,
        "لايكات تيكتوك 3k" to 3.0,
        "لايكات تيكتوك 4k" to 4.0,
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
        "شراء رصيد 40دولار كورك" to 52.0,
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

    fun onAppStart(ctx: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val ensured = Prefs.ensureUid(ctx)
            _uid.value = ensured
            _serverOk.value = Api.health()
            if (_serverOk.value) Api.upsertUser(ensured)
        }
    }

    fun loginWithUid(ctx: Context, uid: String) {
        val clean = uid.trim().uppercase(Locale.ROOT)
        if (clean.isNotBlank()) {
            Prefs.saveUid(ctx, clean)
            _uid.value = clean
            viewModelScope.launch(Dispatchers.IO) {
                _serverOk.value = Api.health()
                if (_serverOk.value) Api.upsertUser(clean)
            }
        }
    }

    fun effectiveBasePrice(default: Double): Double = default

    fun addOrder(ctx: Context, category: String, service: String, qty: Int, price: Double, link: String) {
        val uidNow = _uid.value ?: Prefs.ensureUid(ctx)
        val o = Order(++orderAutoId, uidNow, category, service, qty, round2(price), "pending", link)
        _orders.value = listOf(o) + _orders.value
        viewModelScope.launch(Dispatchers.IO) {
            if (Api.health()) Api.createOrder(uidNow, service, qty, price, link)
        }
    }

    fun addBalance(a: Double) { _balance.value = (_balance.value + a).coerceAtLeast(0.0) }
    fun withdrawBalance(a: Double): Boolean =
        if (a <= _balance.value) { _balance.value -= a; true } else false

    private fun round2(v: Double) = kotlin.math.round(v * 100.0) / 100.0
}

/* =========================
   Helpers
   ========================= */
private fun extractQtyFromName(name: String): Int {
    val k = Regex("(\\d+)\\s*k", RegexOption.IGNORE_CASE).find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (k != null) return k * 1000
    return Regex("(\\d+)").findAll(name).lastOrNull()?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1000
}
private fun stepFor(serviceName: String) = if (serviceName.contains("شدة") || serviceName.contains("ببجي")) 1 else 1000
private fun priceFor(serviceName: String, qty: Int, basePrice: Double): Double {
    val div = when {
        serviceName.contains("10k", true) -> 10000.0
        serviceName.contains("شدة") || serviceName.contains("ببجي") -> 1.0
        else -> 1000.0
    }
    val raw = basePrice * (qty.toDouble() / div)
    return (round(raw * 100.0) / 100.0)
}

/* =========================
   الجذر (بدون شريط علوي)
   ========================= */
@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.onAppStart(ctx) }

    var current by rememberSaveable { mutableStateOf(Screen.HOME) }
    val isOwner by vm.isOwner.collectAsState()

    Scaffold(
        containerColor = Bg,
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
                Screen.HOME     -> HomeScreen(vm) { current = it }
                Screen.SERVICES -> ServicesScreen(vm)
                Screen.ORDERS   -> OrdersScreen(vm)
                Screen.WALLET   -> WalletScreen(vm)
                Screen.SUPPORT  -> SupportScreen()
                Screen.OWNER    -> OwnerDashboard(vm)
            }
        }
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
   HOME & SERVICES — شارات عليا + قائمة الخدمات
   ========================= */
data class BuyInfo(val category: String, val service: String, val qty: Int, val price: Double)

@Composable
fun HomeScreen(vm: AppViewModel, open: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        TopChipsRow(vm, onWallet = { open(Screen.WALLET) })
        Spacer(Modifier.height(8.dp))
        ServicesBody(vm)
    }
}

@Composable
fun ServicesScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        TopChipsRow(vm, onWallet = { /* فتح المحفظة من مكان آخر إن أردت */ })
        Spacer(Modifier.height(8.dp))
        ServicesBody(vm)
    }
}

@Composable
private fun TopChipsRow(vm: AppViewModel, onWallet: () -> Unit) {
    val balance by vm.balance.collectAsState()
    val ok by vm.serverOk.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onWallet,
            leadingIcon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
            label = { Text("رصيدي: ${"%.2f".format(balance)} $") }
        )
        AssistChip(
            onClick = {},
            leadingIcon = {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (ok) Color(0xFF2ECC71) else Color(0xFFE74C3C))
                )
            },
            label = { Text(if (ok) "الخادم: متصل" else "الخادم: غير متصل") }
        )
    }
}

@Composable
private fun ServicesBody(vm: AppViewModel) {
    val ctx = LocalContext.current

    val categories = listOf(
        "سوشيال (تيكتوك/انستغرام/مشاهدات/سكور)" to vm.servicesTikIgViewsLikesScore,
        "تليجرام" to vm.servicesTelegram,
        "ببجي" to vm.servicesPubg,
        "iTunes" to vm.servicesItunes,
        "شحن رصيد" to vm.servicesMobile,
        "Ludo" to vm.servicesLudo
    )

    var selectedCat by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    val qtyMap = remember { mutableStateMapOf<String, Int>() }
    var buy by remember { mutableStateOf<BuyInfo?>(null) }

    // Tabs بسيطة بدون weight
    ScrollableTabRow(selectedTabIndex = selectedCat, edgePadding = 0.dp, containerColor = Surface1) {
        categories.forEachIndexed { i, (name, _) ->
            Tab(
                selected = selectedCat == i,
                onClick = { selectedCat = i },
                text = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = query, onValueChange = { query = it },
        label = { Text("ابحث عن خدمة") },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    val (catTitle, data) = categories[selectedCat]
    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text(catTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Accent) }
        items(data.toList(), key = { it.first }) { (svc, base) ->
            if (query.isNotBlank() && !svc.contains(query, true)) return@items
            val step = stepFor(svc)
            val defaultQty = extractQtyFromName(svc)
            val qty = qtyMap[svc] ?: defaultQty
            val basePrice = vm.effectiveBasePrice(base)
            val price = priceFor(svc, qty, basePrice)

            ServiceRow(
                service = svc,
                qty = qty,
                basePrice = basePrice,
                price = price,
                onDec = { qtyMap[svc] = max(step, qty - step) },
                onInc = { qtyMap[svc] = qty + step },
                onBuy = { buy = BuyInfo(catTitle, svc, qty, price) }
            )
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
                        vm.addOrder(ctx, info.category, info.service, info.qty, info.price, link)
                        open = false; buy = null
                    }) { Text("تأكيد") }
                },
                dismissButton = { TextButton(onClick = { open = false; buy = null }) { Text("إلغاء") } }
            )
        }
    }
}

@Composable
private fun ServiceRow(
    service: String,
    qty: Int,
    basePrice: Double,
    price: Double,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onBuy: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .padding(12.dp)
    ) {
        Text(service, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text("السعر الأساسي: ${"%.2f".format(basePrice)} $", fontSize = 12.sp)
        Text("سعر الطلب الحالي: ${"%.2f".format(price)} $", fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDec) { Text("-${stepFor(service)}") }
            Text("$qty", fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onInc) { Text("+${stepFor(service)}") }
            // بدون weight لتفادي مشاكل البناء
            Spacer(Modifier.width(8.dp))
            ElevatedButton(onClick = onBuy) { Text("شراء") }
        }
    }
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
        Text("إرسال كارت آسياسيل:")
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = digits, onValueChange = { digits = it }, label = { Text("رقم الكارت") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        ElevatedButton(onClick = {
            val success = digits.isNotBlank()
            msg = if (success) "تم إرسال الكارت للمراجعة" else "يرجى إدخال رقم صالح"
            if (success) digits = ""
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
   Owner Dashboard
   ========================= */
@Composable
fun OwnerDashboard(vm: AppViewModel) {
    val uid by vm.uid.collectAsState()
    val ok by vm.serverOk.collectAsState()
    val ctx = LocalContext.current

    var showLogin by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("الإعدادات", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("حالة الخادم", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (ok) Color(0xFF2ECC71) else Color(0xFFE74C3C))
                        )
                        Text(if (ok) "متصل" else "غير متصل")
                    }
                    TextButton(onClick = { vm.onAppStart(ctx) }) { Text("إعادة فحص") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("المستخدم", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("UID: ${uid ?: "-"}", fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("uid", uid ?: ""))
                    }) { Icon(Icons.Filled.ContentCopy, contentDescription = null) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showLogin = true }) { Text("تسجيل الدخول عبر UID") }
            }
        }
    }

    if (showLogin) {
        var t by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLogin = false },
            title = { Text("تسجيل الدخول عبر UID") },
            text = { OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("UID") }) },
            confirmButton = { TextButton(onClick = { if (t.isNotBlank()) { vm.loginWithUid(ctx, t); showLogin = false } }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { showLogin = false }) { Text("إلغاء") } }
        )
    }
}

/* =========================
   PIN Dialog (غير مستخدم حاليًا)
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
