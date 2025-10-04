@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zafer.smm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

/* ============================================================
   تخزين محلي (SharedPreferences + JSON)
   ============================================================ */
private const val PREFS = "smm_store"
private const val KEY_USER_ID = "user_id"
private const val KEY_BALANCE = "balance"
private const val KEY_IS_OWNER = "is_owner"
private const val KEY_CATALOG = "catalog_json"
private const val KEY_PENDING_CARDS = "pending_cards"
private const val KEY_ORDERS = "orders"

private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

private fun loadUserId(ctx: Context): String {
    val p = prefs(ctx)
    var id = p.getString(KEY_USER_ID, null)
    if (id == null) {
        id = "USR-" + Random.nextInt(100000, 999999).toString()
        p.edit().putString(KEY_USER_ID, id).apply()
    }
    return id!!
}

private fun loadBalance(ctx: Context) = prefs(ctx).getFloat(KEY_BALANCE, 0f).toDouble()
private fun saveBalance(ctx: Context, v: Double) = prefs(ctx).edit().putFloat(KEY_BALANCE, v.toFloat()).apply()

private fun isOwner(ctx: Context) = prefs(ctx).getBoolean(KEY_IS_OWNER, false)
private fun setOwner(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_IS_OWNER, v).apply()

/* ============================================================
   نماذج
   ============================================================ */
data class Service(val id: String, val name: String, val price: Double)
data class Section(val key: String, val title: String, val services: MutableList<Service>)
data class PendingCard(val id: Long, val digits: String, val createdAt: Long)
data class Order(val id: Long, val serviceName: String, val price: Double, val note: String, val createdAt: Long)

/* ============================================================
   كتالوج الخدمات (وفق صور البوت) — مع قابلية تعديل السعر/الكمية
   ============================================================ */
private fun defaultCatalog(): MutableList<Section> = mutableListOf(
    Section("followers", "قسم المتابعين", mutableListOf(
        Service("tk_f_1k",  "متابعين تيكتوك (1000)", 3.5),
        Service("tk_f_2k",  "متابعين تيكتوك (2000)", 7.0),
        Service("tk_f_3k",  "متابعين تيكتوك (3000)", 10.5),
        Service("tk_f_4k",  "متابعين تيكتوك (4000)", 14.0),

        Service("ig_f_1k",  "متابعين انستغرام (1000)", 3.0),
        Service("ig_f_2k",  "متابعين انستغرام (2000)", 6.0),
        Service("ig_f_3k",  "متابعين انستغرام (3000)", 9.0),
        Service("ig_f_4k",  "متابعين انستغرام (4000)", 12.0),
    )),
    Section("likes", "قسم الإعجابات", mutableListOf(
        Service("tk_l_1k",  "لايكات تيكتوك (1000)", 1.0),
        Service("tk_l_2k",  "لايكات تيكتوك (2000)", 2.0),
        Service("tk_l_3k",  "لايكات تيكتوك (3000)", 3.0),
        Service("tk_l_4k",  "لايكات تيكتوك (4000)", 4.0),

        Service("ig_l_1k",  "لايكات انستغرام (1000)", 1.0),
        Service("ig_l_2k",  "لايكات انستغرام (2000)", 2.0),
        Service("ig_l_3k",  "لايكات انستغرام (3000)", 3.0),
        Service("ig_l_4k",  "لايكات انستغرام (4000)", 4.0),
    )),
    Section("views", "قسم المشاهدات", mutableListOf(
        Service("tk_v_1k",   "مشاهدات تيكتوك (1000)", 0.1),
        Service("tk_v_10k",  "مشاهدات تيكتوك (10000)", 0.8),
        Service("tk_v_20k",  "مشاهدات تيكتوك (20000)", 1.6),
        Service("tk_v_30k",  "مشاهدات تيكتوك (30000)", 2.4),
        Service("tk_v_50k",  "مشاهدات تيكتوك (50000)", 3.2),

        Service("ig_v_10k",  "مشاهدات انستغرام (10000)", 0.8),
        Service("ig_v_20k",  "مشاهدات انستغرام (20000)", 1.6),
        Service("ig_v_30k",  "مشاهدات انستغرام (30000)", 2.4),
        Service("ig_v_50k",  "مشاهدات انستغرام (50000)", 3.2),
    )),
    Section("live_views", "قسم مشاهدات البث المباشر", mutableListOf(
        Service("tk_lv_1k",  "مشاهدات بث تيكتوك (1000)", 2.0),
        Service("tk_lv_2k",  "مشاهدات بث تيكتوك (2000)", 4.0),
        Service("tk_lv_3k",  "مشاهدات بث تيكتوك (3000)", 6.0),
        Service("tk_lv_4k",  "مشاهدات بث تيكتوك (4000)", 8.0),

        Service("ig_lv_1k",  "مشاهدات بث انستغرام (1000)", 2.0),
        Service("ig_lv_2k",  "مشاهدات بث انستغرام (2000)", 4.0),
        Service("ig_lv_3k",  "مشاهدات بث انستغرام (3000)", 6.0),
        Service("ig_lv_4k",  "مشاهدات بث انستغرام (4000)", 8.0),
    )),
    Section("score", "رفع سكور تيكتوك", mutableListOf(
        Service("sc_1k", "رفع سكور بنك (1000)", 2.0),
        Service("sc_2k", "رفع سكور بنك (2000)", 4.0),
        Service("sc_3k", "رفع سكور بنك (3000)", 6.0),
        Service("sc_10k","رفع سكور بنك (10000)", 20.0),
    )),
    Section("pubg", "قسم شحن شدات ببجي", mutableListOf(
        Service("uc_60",   "ببجي 60 شدة", 2.0),
        Service("uc_120",  "ببجي 120 شدة", 4.0),
        Service("uc_180",  "ببجي 180 شدة", 6.0),
        Service("uc_240",  "ببجي 240 شدة", 8.0),
        Service("uc_325",  "ببجي 325 شدة", 9.0),
        Service("uc_660",  "ببجي 660 شدة", 15.0),
        Service("uc_1800", "ببجي 1800 شدة", 40.0),
    )),
    Section("itunes", "قسم شراء رصيد ايتونز", mutableListOf(
        Service("it_5",  "شراء رصيد 5 ايتونز", 9.0),
        Service("it_10", "شراء رصيد 10 ايتونز", 18.0),
        Service("it_15", "شراء رصيد 15 ايتونز", 27.0),
        Service("it_20", "شراء رصيد 20 ايتونز", 36.0),
        Service("it_25", "شراء رصيد 25 ايتونز", 45.0),
        Service("it_30", "شراء رصيد 30 ايتونز", 54.0),
        Service("it_35", "شراء رصيد 35 ايتونز", 63.0),
        Service("it_40", "شراء رصيد 40 ايتونز", 72.0),
        Service("it_45", "شراء رصيد 45 ايتونز", 81.0),
        Service("it_50", "شراء رصيد 50 ايتونز", 90.0),
    )),
    Section("mobile_recharge", "قسم شراء رصيد الهاتف", mutableListOf(
        Service("ath_2",  "شراء رصيد2 دولار أثير", 3.5),
        Service("ath_5",  "شراء رصيد5 دولار أثير", 7.0),
        Service("ath_10", "شراء رصيد10 دولار أثير", 13.0),
        Service("ath_15", "شراء رصيد15 دولار أثير", 19.0),
        Service("ath_40", "شراء رصيد40 دولار أثير", 52.0),

        Service("asy_2",  "شراء رصيد2 دولار آسيا", 3.5),
        Service("asy_5",  "شراء رصيد5 دولار آسيا", 7.0),
        Service("asy_10", "شراء رصيد10 دولار آسيا", 13.0),
        Service("asy_15", "شراء رصيد15 دولار آسيا", 19.0),
        Service("asy_40", "شراء رصيد40 دولار آسيا", 52.0),

        Service("krk_2",  "شراء رصيد2 دولار كورك", 3.5),
        Service("krk_5",  "شراء رصيد5 دولار كورك", 7.0),
        Service("krk_10", "شراء رصيد10 دولار كورك", 13.0),
        Service("krk_15", "شراء رصيد15 دولار كورك", 19.0),
    )),
    Section("telegram", "قسم خدمات التليجرام", mutableListOf(
        Service("tg_ch_1k", "أعضاء قنوات تيلي 1k", 3.0),
        Service("tg_ch_2k", "أعضاء قنوات تيلي 2k", 6.0),
        Service("tg_ch_3k", "أعضاء قنوات تيلي 3k", 9.0),
        Service("tg_ch_4k", "أعضاء قنوات تيلي 4k", 12.0),
        Service("tg_ch_5k", "أعضاء قنوات تيلي 5k", 15.0),

        Service("tg_gp_1k", "أعضاء كروبات تيلي 1k", 3.0),
        Service("tg_gp_2k", "أعضاء كروبات تيلي 2k", 6.0),
        Service("tg_gp_3k", "أعضاء كروبات تيلي 3k", 9.0),
        Service("tg_gp_4k", "أعضاء كروبات تيلي 4k", 12.0),
        Service("tg_gp_5k", "أعضاء كروبات تيلي 5k", 15.0),
    )),
    Section("ludo", "قسم خدمات اللودو", mutableListOf(
        Service("ld_dm_810",   "لودو 810 الأماسة", 4.0),
        Service("ld_dm_2280",  "لودو 2280 الأماسة", 8.9),
        Service("ld_dm_5080",  "لودو 5080 الأماسة", 17.5),
        Service("ld_dm_12750", "لودو 12750 الأماسة", 42.7),

        Service("ld_gold_66680",   "لودو ذهب 66680", 4.0),
        Service("ld_gold_219500",  "لودو ذهب 219500", 8.9),
        Service("ld_gold_1443000", "لودو ذهب 1443000", 17.5),
        Service("ld_gold_3627000", "لودو ذهب 3627000", 42.7),
    )),
)

private fun loadCatalog(ctx: Context): MutableList<Section> {
    val js = prefs(ctx).getString(KEY_CATALOG, null) ?: return defaultCatalog()
    return try {
        val arr = JSONArray(js)
        val out = mutableListOf<Section>()
        for (i in 0 until arr.length()) {
            val so = arr.getJSONObject(i)
            val sArr = so.getJSONArray("services")
            val list = mutableListOf<Service>()
            for (j in 0 until sArr.length()) {
                val it = sArr.getJSONObject(j)
                list += Service(it.getString("id"), it.getString("name"), it.getDouble("price"))
            }
            out += Section(so.getString("key"), so.getString("title"), list)
        }
        out
    } catch (_: Throwable) { defaultCatalog() }
}

private fun saveCatalog(ctx: Context, catalog: List<Section>) {
    val arr = JSONArray()
    catalog.forEach { sec ->
        val o = JSONObject()
        o.put("key", sec.key)
        o.put("title", sec.title)
        val services = JSONArray()
        sec.services.forEach { sv ->
            val so = JSONObject()
            so.put("id", sv.id); so.put("name", sv.name); so.put("price", sv.price)
            services.put(so)
        }
        o.put("services", services)
        arr.put(o)
    }
    prefs(ctx).edit().putString(KEY_CATALOG, arr.toString()).apply()
}

/* ============================================================
   الكروت المعلّقة + الطلبات
   ============================================================ */
private fun loadPendingCards(ctx: Context): MutableList<PendingCard> {
    val js = prefs(ctx).getString(KEY_PENDING_CARDS, "[]") ?: "[]"
    return try {
        val arr = JSONArray(js)
        val list = mutableListOf<PendingCard>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += PendingCard(o.getLong("id"), o.getString("digits"), o.getLong("createdAt"))
        }
        list.sortedByDescending { it.createdAt }.toMutableList()
    } catch (_: Throwable) { mutableListOf() }
}

private fun savePendingCards(ctx: Context, list: List<PendingCard>) {
    val arr = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id); o.put("digits", it.digits); o.put("createdAt", it.createdAt)
        arr.put(o)
    }
    prefs(ctx).edit().putString(KEY_PENDING_CARDS, arr.toString()).apply()
}

private fun addPendingCard(ctx: Context, digits: String) {
    val list = loadPendingCards(ctx)
    list += PendingCard(System.currentTimeMillis(), digits, System.currentTimeMillis())
    savePendingCards(ctx, list)
}

private fun loadOrders(ctx: Context): MutableList<Order> {
    val js = prefs(ctx).getString(KEY_ORDERS, "[]") ?: "[]"
    return try {
        val arr = JSONArray(js)
        val list = mutableListOf<Order>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += Order(o.getLong("id"), o.getString("serviceName"), o.getDouble("price"), o.getString("note"), o.getLong("createdAt"))
        }
        list.sortedByDescending { it.createdAt }.toMutableList()
    } catch (_: Throwable) { mutableListOf() }
}

private fun addOrder(ctx: Context, serviceName: String, price: Double, note: String = "") {
    val list = loadOrders(ctx)
    list += Order(System.currentTimeMillis(), serviceName, price, note, System.currentTimeMillis())
    val arr = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id); o.put("serviceName", it.serviceName); o.put("price", it.price); o.put("note", it.note); o.put("createdAt", it.createdAt)
        arr.put(o)
    }
    prefs(ctx).edit().putString(KEY_ORDERS, arr.toString()).apply()
}

/* ============================================================
   الشاشات والتنقل
   ============================================================ */
enum class Screen {
    HOME, SERVICES, SECTION, ORDERS, BALANCE, OWNER, OWNER_EDIT_PRICES, OWNER_PENDING_CARDS, OWNER_BALANCE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(Screen.HOME) }
    var selectedSection by remember { mutableStateOf<Section?>(null) }

    var catalog by remember { mutableStateOf(loadCatalog(ctx)) }
    var balance by remember { mutableStateOf(loadBalance(ctx)) }
    val userId = remember { loadUserId(ctx) }
    var owner by remember { mutableStateOf(isOwner(ctx)) }

    fun resave() {
        saveCatalog(ctx, catalog)
        saveBalance(ctx, balance)
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F2FA)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("خدمات راتلوزن", fontWeight = FontWeight.Bold)
                        Text("معرّفك: $userId", fontSize = 12.sp)
                    }
                },
                actions = {
                    if (owner) {
                        TextButton(onClick = { screen = Screen.OWNER }) { Text("لوحة المالك") }
                    }
                }
            )

            when (screen) {
                Screen.HOME -> Home(
                    onServices = { screen = Screen.SERVICES },
                    onOrders = { screen = Screen.ORDERS },
                    onBalance = { screen = Screen.BALANCE }
                )
                Screen.SERVICES -> ServicesList(
                    catalog = catalog,
                    onOpen = { sec -> selectedSection = sec; screen = Screen.SECTION },
                    onBack = { screen = Screen.HOME }
                )
                Screen.SECTION -> SectionScreen(
                    section = selectedSection!!,
                    onOrder = { svc ->
                        // مثل البوت: بمجرد طلب الخدمة تُسجّل في طلباتي
                        addOrder(ctx, svc.name, svc.price)
                        Toast.makeText(ctx, "تم إضافة الطلب إلى طلباتك.", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { screen = Screen.SERVICES }
                )
                Screen.ORDERS -> OrdersScreen(
                    orders = loadOrders(ctx),
                    onBack = { screen = Screen.HOME }
                )
                Screen.BALANCE -> BalanceScreen(
                    balance = balance,
                    onAsiacell = {
                        scope.launch { showAsiacellDialog(ctx) { ok, digits ->
                            if (ok) {
                                addPendingCard(ctx, digits!!)
                                Toast.makeText(ctx, "تم استلام طلبك وسوف يتم الشحن قريبًا.", Toast.LENGTH_LONG).show()
                            }
                        }}
                    },
                    onSupport = { title ->
                        scope.launch { showSupportDialog(ctx, title) }
                    },
                    onBack = { screen = Screen.HOME }
                )
                Screen.OWNER -> OwnerDashboard(
                    onEditPrices = { screen = Screen.OWNER_EDIT_PRICES },
                    onPendingCards = { screen = Screen.OWNER_PENDING_CARDS },
                    onSendBalance = { screen = Screen.OWNER_BALANCE },
                    onExitOwner = { owner = false; setOwner(ctx, false) },
                    onBack = { screen = Screen.HOME }
                )
                Screen.OWNER_EDIT_PRICES -> OwnerEditPrices(
                    catalog = catalog,
                    onSave = { catalog = it; resave(); screen = Screen.OWNER },
                    onBack = { screen = Screen.OWNER }
                )
                Screen.OWNER_PENDING_CARDS -> OwnerPendingCards(
                    onApprove = { card, amount ->
                        val newBal = max(0.0, loadBalance(ctx) + amount)
                        saveBalance(ctx, newBal)
                        balance = newBal
                        val list = loadPendingCards(ctx).filterNot { it.id == card.id }
                        savePendingCards(ctx, list)
                        Toast.makeText(ctx, "تم قبول الكارت وإضافة الرصيد.", Toast.LENGTH_SHORT).show()
                    },
                    onReject = { card ->
                        val list = loadPendingCards(ctx).filterNot { it.id == card.id }
                        savePendingCards(ctx, list)
                        Toast.makeText(ctx, "تم رفض الكارت.", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { screen = Screen.OWNER }
                )
                Screen.OWNER_BALANCE -> OwnerBalanceOps(
                    onAdd = { amount ->
                        val newBal = max(0.0, loadBalance(ctx) + amount)
                        saveBalance(ctx, newBal); balance = newBal
                        Toast.makeText(ctx, "تمت إضافة $$amount", Toast.LENGTH_SHORT).show()
                    },
                    onDeduct = { amount ->
                        val newBal = max(0.0, loadBalance(ctx) - amount)
                        saveBalance(ctx, newBal); balance = newBal
                        Toast.makeText(ctx, "تم خصم $$amount", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { screen = Screen.OWNER }
                )
            }
        }
    }
}

/* ========================= HOME ========================== */
@Composable
fun Home(onServices: () -> Unit, onOrders: () -> Unit, onBalance: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BigButton("الخدمات", onServices)
        BigButton("طلباتي", onOrders)
        BigButton("رصيدي", onBalance)
    }
}

/* ======================= SERVICES ======================== */
@Composable
fun ServicesList(catalog: List<Section>, onOpen: (Section) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الأقسام", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(catalog) { sec ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(sec) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(sec.title, fontWeight = FontWeight.Bold)
                        Text("عدد الخدمات: ${sec.services.size}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun SectionScreen(section: Section, onOrder: (Service) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(section.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(section.services) { svc ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(svc.name, fontWeight = FontWeight.Bold)
                            Text("$${"%.2f".format(svc.price)}", color = Color(0xFF2E7D32))
                        }
                        Button(onClick = { onOrder(svc) }) { Text("طلب الخدمة") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

/* ======================= ORDERS ========================== */
@Composable
fun OrdersScreen(orders: List<Order>, onBack: () -> Unit) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("طلباتي", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (orders.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("لا توجد طلبات بعد.") }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orders) { o ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(o.serviceName, fontWeight = FontWeight.Bold)
                            Text("السعر: $${"%.2f".format(o.price)}")
                            if (o.note.isNotBlank()) Text("ملاحظات: ${o.note}")
                            Text("الوقت: ${fmt.format(Date(o.createdAt))}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

/* ======================= BALANCE ========================= */
@Composable
fun BalanceScreen(
    balance: Double,
    onAsiacell: () -> Unit,
    onSupport: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF))) {
            Text("رصيدك الحالي: $${"%.2f".format(balance)}", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        BigButton("شحن عبر اسياسيل", onAsiacell)
        BigButton("شحن عبر سوبركي") { onSupport("شحن عبر سوبركي") }
        BigButton("شحن عبر زين كاش") { onSupport("شحن عبر زين كاش") }
        BigButton("شحن عبر USDT")   { onSupport("شحن عبر USDT") }
        BigButton("شحن عبر نقاط سنتات") { onSupport("شحن عبر نقاط سنتات") }
        BigButton("شحن عبر هلا بي") { onSupport("شحن عبر هلا بي") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

private suspend fun showSupportDialog(ctx: Context, title: String) {
    val number = "+9647763410970"
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("whatsapp", number))
    Toast.makeText(ctx, "تم نسخ رقم الدعم: $number", Toast.LENGTH_LONG).show()
    // اختياري: فتح الواتساب مباشرة
    try {
        val uri = Uri.parse("https://wa.me/9647763410970")
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Throwable) {}
}

private suspend fun showAsiacellDialog(ctx: Context, onResult: (Boolean, String?) -> Unit) {
    val activity = ctx as? ComponentActivity ?: run { onResult(false, null); return }
    activity.setContent {
        var digits by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        Surface(Modifier.fillMaxSize().background(Color(0xAA000000))) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card {
                    Column(Modifier.padding(16.dp).widthIn(min = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أدخل رقم الكارت المكوّن من 14 أو 16 رقمًا", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = digits,
                            onValueChange = {
                                digits = it.filter { ch -> ch.isDigit() }
                                error = null
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = error != null,
                            supportingText = { if (error != null) Text(error!!, color = Color.Red) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val ok = (digits.length == 14 || digits.length == 16)
                                if (!ok) { error = "الرقم يجب أن يكون 14 أو 16 رقمًا."; return@Button }
                                onResult(true, digits)
                                (ctx as ComponentActivity).setContent { AppRoot() }
                            }) { Text("إرسال") }
                            OutlinedButton(onClick = {
                                onResult(false, null)
                                (ctx as ComponentActivity).setContent { AppRoot() }
                            }) { Text("إلغاء") }
                        }
                    }
                }
            }
        }
    }
}

/* ======================= OWNER =========================== */
@Composable
fun OwnerDashboard(
    onEditPrices: () -> Unit,
    onPendingCards: () -> Unit,
    onSendBalance: () -> Unit,
    onExitOwner: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("لوحة تحكم المالك", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        BigButton("تعديل الأسعار والكميات", onEditPrices)
        BigButton("الكروت المعلّقة", onPendingCards)
        BigButton("إرسال/خصم رصيد", onSendBalance)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
        TextButton(onClick = onExitOwner, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("إيقاف وضع المالك") }
    }
}

@Composable
fun OwnerEditPrices(catalog: List<Section>, onSave: (MutableList<Section>) -> Unit, onBack: () -> Unit) {
    var local by remember { mutableStateOf(catalog.map { sec -> Section(sec.key, sec.title, sec.services.map { it.copy() }.toMutableList()) }.toMutableList()) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("تعديل الأسعار والكميات", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(local) { sec ->
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text(sec.title, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        sec.services.forEachIndexed { idx, svc ->
                            var priceText by remember(svc.id) { mutableStateOf(svc.price.toString()) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(svc.name, modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = priceText,
                                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c=='.' } },
                                    label = { Text("السعر") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(120.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Button(onClick = {
                                    val v = priceText.toDoubleOrNull() ?: return@Button
                                    local.find { it.key == sec.key }!!.services[idx] = svc.copy(price = v)
                                    Toast.makeText(LocalContext.current, "تم تحديث السعر.", Toast.LENGTH_SHORT).show()
                                }) { Text("تحديث") }
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("رجوع") }
            Button(onClick = { onSave(local) }, modifier = Modifier.weight(1f)) { Text("حفظ") }
        }
    }
}

@Composable
fun OwnerPendingCards(
    onApprove: (PendingCard, Double) -> Unit,
    onReject: (PendingCard) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(loadPendingCards(ctx)) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الكروت المعلقة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("لا توجد كروت معلقة") }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { card ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("الرقم: ${card.digits}", fontWeight = FontWeight.Bold)
                            Text("التاريخ: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(card.createdAt))}", fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            var amountText by remember { mutableStateOf("") }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c=='.' } },
                                    label = { Text("المبلغ (USD)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    val v = amountText.toDoubleOrNull() ?: return@Button
                                    onApprove(card, v)
                                    items = loadPendingCards(ctx)
                                }) { Text("قبول") }
                                OutlinedButton(onClick = {
                                    onReject(card)
                                    items = loadPendingCards(ctx)
                                }) { Text("رفض") }
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerBalanceOps(onAdd: (Double) -> Unit, onDeduct: (Double) -> Unit, onBack: () -> Unit) {
    var value by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("إرسال/خصم رصيد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedTextField(
            value = value,
            onValueChange = { value = it.filter { c -> c.isDigit() || c=='.' } },
            label = { Text("المبلغ (USD)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { value.toDoubleOrNull()?.let(onAdd) }, modifier = Modifier.weight(1f)) { Text("إرسال") }
            OutlinedButton(onClick = { value.toDoubleOrNull()?.let(onDeduct) }, modifier = Modifier.weight(1f)) { Text("خصم") }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

/* ======================= عناصر UI ======================== */
@Composable
fun BigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(6.dp))
    }
}
