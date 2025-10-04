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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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

/* ======================= مفاتيح التخزين ======================= */
private const val PREFS = "smm_store"
private const val KEY_USER_ID = "user_id"
private const val KEY_BALANCE = "balance"
private const val KEY_TOTAL_SPENT = "total_spent"
private const val KEY_IS_OWNER = "is_owner"
private const val KEY_IS_MOD = "is_mod"
private const val KEY_CATALOG = "catalog_json"
private const val KEY_PENDING_CARDS = "pending_cards"
private const val KEY_ORDERS = "orders"
private const val KEY_ANNOUNCE = "announcement"
private const val KEY_BLOCKED_UNTIL = "blocked_until"
private const val KEY_BLOCK_REASON = "block_reason"
private const val KEY_REF_INVITER = "ref_inviter"
private const val KEY_REF_PAID = "ref_paid"

/* ======================= أدوات التخزين ======================= */
private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

private fun loadUserId(ctx: Context): String {
    val p = prefs(ctx)
    var id = p.getString(KEY_USER_ID, null)
    if (id == null) {
        id = "USR-" + Random.nextInt(100000, 999999)
        p.edit().putString(KEY_USER_ID, id).apply()
    }
    return id
}

private fun isBlocked(ctx: Context): Pair<Boolean, String?> {
    val p = prefs(ctx)
    val until = p.getLong(KEY_BLOCKED_UNTIL, 0L)
    val reason = p.getString(KEY_BLOCK_REASON, null)
    val now = System.currentTimeMillis()
    return if (until > now) true to (reason ?: "محظور مؤقتًا") else false to null
}

private fun setBlock(ctx: Context, hours: Int, reason: String) {
    val until = System.currentTimeMillis() + hours * 3600_000L
    prefs(ctx).edit()
        .putLong(KEY_BLOCKED_UNTIL, until)
        .putString(KEY_BLOCK_REASON, reason)
        .apply()
}

private fun clearBlock(ctx: Context) {
    prefs(ctx).edit()
        .remove(KEY_BLOCKED_UNTIL)
        .remove(KEY_BLOCK_REASON)
        .apply()
}

private fun loadBalance(ctx: Context) = prefs(ctx).getFloat(KEY_BALANCE, 0f).toDouble()
private fun saveBalance(ctx: Context, v: Double) = prefs(ctx).edit().putFloat(KEY_BALANCE, v.toFloat()).apply()

private fun loadTotalSpent(ctx: Context) = prefs(ctx).getFloat(KEY_TOTAL_SPENT, 0f).toDouble()
private fun saveTotalSpent(ctx: Context, v: Double) = prefs(ctx).edit().putFloat(KEY_TOTAL_SPENT, v.toFloat()).apply()

private fun isOwner(ctx: Context) = prefs(ctx).getBoolean(KEY_IS_OWNER, false)
private fun setOwner(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_IS_OWNER, v).apply()

private fun isModerator(ctx: Context) = prefs(ctx).getBoolean(KEY_IS_MOD, false)
private fun setModerator(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_IS_MOD, v).apply()

private fun loadAnnouncement(ctx: Context) = prefs(ctx).getString(KEY_ANNOUNCE, "") ?: ""
private fun saveAnnouncement(ctx: Context, msg: String) = prefs(ctx).edit().putString(KEY_ANNOUNCE, msg).apply()

private fun loadInviter(ctx: Context) = prefs(ctx).getString(KEY_REF_INVITER, null)
private fun setInviter(ctx: Context, id: String?) = prefs(ctx).edit().putString(KEY_REF_INVITER, id).apply()
private fun isRefPaid(ctx: Context) = prefs(ctx).getBoolean(KEY_REF_PAID, false)
private fun setRefPaid(ctx: Context) = prefs(ctx).edit().putBoolean(KEY_REF_PAID, true).apply()

/* ======================= نماذج البيانات ======================= */
data class Service(val id: String, val name: String, val price: Double, val category: String = "عام")
data class Section(val key: String, val title: String, val services: MutableList<Service>)
enum class OrderStatus { PENDING, COMPLETED, REJECTED, REFUNDED, WAITING }
data class Order(
    val id: Long,
    val serviceId: String,
    val serviceName: String,
    val price: Double,
    val category: String,
    val note: String,
    val createdAt: Long,
    var status: OrderStatus = OrderStatus.PENDING
)
data class PendingCard(val id: Long, val digits: String, val createdAt: Long)

/* ======================= الكتالوج الافتراضي ======================= */
private fun defaultCatalog(): MutableList<Section> = mutableListOf(
    Section("followers", "قسم المتابعين", mutableListOf(
        Service("tk_f_1k",  "متابعين تيكتوك (1000)", 3.5, "tiktok"),
        Service("tk_f_2k",  "متابعين تيكتوك (2000)", 7.0, "tiktok"),
        Service("ig_f_1k",  "متابعين انستغرام (1000)", 3.0, "instagram"),
        Service("ig_f_2k",  "متابعين انستغرام (2000)", 6.0, "instagram"),
    )),
    Section("likes", "قسم الإعجابات", mutableListOf(
        Service("tk_l_1k",  "لايكات تيكتوك (1000)", 1.0, "tiktok"),
        Service("ig_l_1k",  "لايكات انستغرام (1000)", 1.0, "instagram"),
    )),
    Section("views", "قسم المشاهدات", mutableListOf(
        Service("tk_v_10k",  "مشاهدات تيكتوك (10000)", 0.8, "tiktok"),
        Service("ig_v_10k",  "مشاهدات انستغرام (10000)", 0.8, "instagram"),
    )),
    Section("live_views", "قسم مشاهدات البث المباشر", mutableListOf(
        Service("tk_lv_1k",  "مشاهدات بث تيكتوك (1000)", 2.0, "tiktok"),
        Service("ig_lv_1k",  "مشاهدات بث انستغرام (1000)", 2.0, "instagram"),
    )),
    Section("score", "رفع سكور تيكتوك", mutableListOf(
        Service("sc_1k", "رفع سكور بنك (1000)", 2.0, "tiktok"),
    )),
    Section("pubg", "قسم شحن شدات ببجي", mutableListOf(
        Service("uc_60",   "ببجي 60 شدة", 2.0, "pubg"),
        Service("uc_660",  "ببجي 660 شدة", 15.0, "pubg"),
        Service("uc_1800", "ببجي 1800 شدة", 40.0, "pubg"),
    )),
    Section("itunes", "قسم شراء رصيد ايتونز", mutableListOf(
        Service("it_10", "شراء رصيد 10 ايتونز", 18.0, "itunes"),
        Service("it_50", "شراء رصيد 50 ايتونز", 90.0, "itunes"),
    )),
    Section("mobile_recharge", "قسم شراء رصيد الهاتف", mutableListOf(
        Service("ath_2",  "شراء رصيد2 دولار أثير", 3.5, "mobile"),
        Service("asy_2",  "شراء رصيد2 دولار آسيا", 3.5, "mobile"),
        Service("krk_2",  "شراء رصيد2 دولار كورك", 3.5, "mobile"),
    )),
    Section("telegram", "قسم خدمات التليجرام", mutableListOf(
        Service("tg_ch_1k", "أعضاء قنوات تيلي 1k", 3.0, "telegram"),
        Service("tg_gp_1k", "أعضاء كروبات تيلي 1k", 3.0, "telegram"),
    )),
    Section("ludo", "قسم خدمات اللودو", mutableListOf(
        Service("ld_dm_810", "لودو 810 الأماسة", 4.0, "ludo")
    )),
)

/* ======================= تحميل/حفظ الكتالوج ======================= */
private fun loadCatalog(ctx: Context): MutableList<Section> {
    val js = prefs(ctx).getString(KEY_CATALOG, null) ?: return defaultCatalog()
    return runCatching {
        val arr = JSONArray(js)
        val out = mutableListOf<Section>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val key = o.getString("key")
            val title = o.getString("title")
            val services = mutableListOf<Service>()
            val sArr = o.getJSONArray("services")
            for (j in 0 until sArr.length()) {
                val it = sArr.getJSONObject(j)
                services += Service(
                    id = it.getString("id"),
                    name = it.getString("name"),
                    price = it.getDouble("price"),
                    category = it.optString("category", "عام")
                )
            }
            out += Section(key, title, services)
        }
        out
    }.getOrElse { defaultCatalog() }
}

private fun saveCatalog(ctx: Context, catalog: List<Section>) {
    val arr = JSONArray()
    catalog.forEach { sec ->
        val o = JSONObject()
        o.put("key", sec.key)
        o.put("title", sec.title)
        val sArr = JSONArray()
        sec.services.forEach { sv ->
            val so = JSONObject()
            so.put("id", sv.id)
            so.put("name", sv.name)
            so.put("price", sv.price)
            so.put("category", sv.category)
            sArr.put(so)
        }
        o.put("services", sArr)
        arr.put(o)
    }
    prefs(LocalContext.current).edit().putString(KEY_CATALOG, arr.toString()).apply()
}

/* ======================= الطلبات ======================= */
private fun loadOrders(ctx: Context): MutableList<Order> {
    val js = prefs(ctx).getString(KEY_ORDERS, "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(js)
        val list = mutableListOf<Order>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += Order(
                id = o.getLong("id"),
                serviceId = o.getString("serviceId"),
                serviceName = o.getString("serviceName"),
                price = o.getDouble("price"),
                category = o.optString("category", "عام"),
                note = o.optString("note", ""),
                createdAt = o.getLong("createdAt"),
                status = OrderStatus.valueOf(o.getString("status"))
            )
        }
        list.sortedByDescending { it.createdAt }.toMutableList()
    }.getOrElse { mutableListOf() }
}

private fun saveOrders(ctx: Context, list: List<Order>) {
    val arr = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("serviceId", it.serviceId)
        o.put("serviceName", it.serviceName)
        o.put("price", it.price)
        o.put("category", it.category)
        o.put("note", it.note)
        o.put("createdAt", it.createdAt)
        o.put("status", it.status.name)
        arr.put(o)
    }
    prefs(ctx).edit().putString(KEY_ORDERS, arr.toString()).apply()
}

/* ======================= الكروت ======================= */
private fun loadPendingCards(ctx: Context): MutableList<PendingCard> {
    val js = prefs(ctx).getString(KEY_PENDING_CARDS, "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(js)
        val list = mutableListOf<PendingCard>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += PendingCard(o.getLong("id"), o.getString("digits"), o.getLong("createdAt"))
        }
        list.sortedByDescending { it.createdAt }.toMutableList()
    }.getOrElse { mutableListOf() }
}

private fun savePendingCards(ctx: Context, list: List<PendingCard>) {
    val arr = JSONArray()
    list.forEach {
        val o = JSONObject()
        o.put("id", it.id)
        o.put("digits", it.digits)
        o.put("createdAt", it.createdAt)
        arr.put(o)
    }
    prefs(ctx).edit().putString(KEY_PENDING_CARDS, arr.toString()).apply()
}

/* ======================= التنقل ======================= */
sealed class Screen {
    object HOME : Screen()
    object SERVICES : Screen()
    data class SECTION(val secKey: String) : Screen()
    object ORDERS : Screen()
    object BALANCE : Screen()
    object REFERRAL : Screen()
    object LEADERBOARD : Screen()
    object OWNER_LOGIN : Screen()
    object OWNER_DASH : Screen()
    object OWNER_EDIT : Screen()
    object OWNER_PENDING_CARDS : Screen()
    object OWNER_PENDING_ORDERS : Screen()
    data class OWNER_PENDING_CATEGORY(val category: String) : Screen()  // pubg/itunes/mobile/ludo/telegram/...
    object OWNER_BALANCE : Screen()
    object OWNER_API_TOOLS : Screen()
    object OWNER_USERS : Screen()
    object OWNER_MODS : Screen()
    object OWNER_BLOCK : Screen()
    object OWNER_UNBLOCK : Screen()
    object OWNER_BROADCAST : Screen()
    object OWNER_API_CODES : Screen()
    object OWNER_REFERRALS : Screen()
    object OWNER_DISCOUNTS_INFO : Screen()
    object OWNER_LEADERBOARD : Screen()
}

/* ======================= النشاط الرئيسي ======================= */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

/* ======================= الجذر ======================= */
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val userId = remember { loadUserId(ctx) }

    var screen by remember { mutableStateOf<Screen>(Screen.HOME) }
    var catalog by remember { mutableStateOf(loadCatalog(ctx)) }
    var balance by remember { mutableStateOf(loadBalance(ctx)) }
    var totalSpent by remember { mutableStateOf(loadTotalSpent(ctx)) }
    var isOwnerState by remember { mutableStateOf(isOwner(ctx)) }
    var isMod by remember { mutableStateOf(isModerator(ctx)) }
    var announcement by remember { mutableStateOf(loadAnnouncement(ctx)) }
    val (blocked, blockReason) = remember { isBlocked(ctx) }

    fun persist() {
        saveCatalog(ctx, catalog)
        saveBalance(ctx, balance)
        saveTotalSpent(ctx, totalSpent)
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F2FA)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = "خدمات راتلوزن",
                subtitle = "ID: $userId",
                showOwnerButton = isOwnerState,
                onOwnerClick = { screen = Screen.OWNER_DASH }
            )

            if (announcement.isNotBlank()) {
                Banner(announcement)
            }

            if (blocked) {
                BlockedView(reason = blockReason ?: "محظور مؤقتًا") {
                    // فقط عرض السبب، لا أزرار عمل حتى انتهاء الحظر
                }
            } else {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        onServices = { screen = Screen.SERVICES },
                        onOrders = { screen = Screen.ORDERS },
                        onBalance = { screen = Screen.BALANCE },
                        onReferral = { screen = Screen.REFERRAL },
                        onLeaderboard = { screen = Screen.LEADERBOARD },
                        onOwnerLogin = { screen = Screen.OWNER_LOGIN }
                    )
                    Screen.SERVICES -> ServicesScreen(
                        catalog = catalog,
                        isModerator = isMod,
                        onOpenSection = { secKey -> screen = Screen.SECTION(secKey) },
                        onBack = { screen = Screen.HOME }
                    )
                    is Screen.SECTION -> {
                        val sec = catalog.find { it.key == screen.secKey }!!
                        SectionScreen(
                            section = sec,
                            isModerator = isMod,
                            onOrder = { svc ->
                                val price = if (isMod) svc.price * 0.9 else svc.price
                                val orders = loadOrders(ctx)
                                orders += Order(
                                    id = System.currentTimeMillis(),
                                    serviceId = svc.id,
                                    serviceName = svc.name,
                                    price = price,
                                    category = svc.category,
                                    note = "",
                                    createdAt = System.currentTimeMillis(),
                                    status = OrderStatus.PENDING
                                )
                                saveOrders(ctx, orders)
                                totalSpent += price
                                saveTotalSpent(ctx, totalSpent)
                                Toast.makeText(ctx, "تمت إضافة الطلب (قيد المراجعة).", Toast.LENGTH_SHORT).show()
                            },
                            onBack = { screen = Screen.SERVICES }
                        )
                    }
                    Screen.ORDERS -> OrdersListScreen(
                        orders = loadOrders(ctx),
                        onBack = { screen = Screen.HOME }
                    )
                    Screen.BALANCE -> BalanceScreen(
                        balance = balance,
                        onAsiacell = {
                            // عرض Dialog إدخال الكارت
                            screen = BalanceAsiacellDialog(
                                screen, onSubmit = { digits ->
                                    val ok = digits.length == 14 || digits.length == 16
                                    if (!ok) {
                                        Toast.makeText(ctx, "الرقم يجب أن يكون 14 أو 16 رقمًا.", Toast.LENGTH_LONG).show()
                                    } else {
                                        val list = loadPendingCards(ctx)
                                        // حماية بسيطة ضد السبام: 5 خلال 2 دقيقة => حظر 2 ساعة
                                        val cutoff = System.currentTimeMillis() - 120_000
                                        val recent = list.count { it.createdAt >= cutoff }
                                        if (recent >= 5) {
                                            setBlock(ctx, hours = 2, reason = "محاولات كثيرة خلال وقت قصير.")
                                            Toast.makeText(ctx, "تم حظرك مؤقتًا لسوء الاستخدام.", Toast.LENGTH_LONG).show()
                                        } else {
                                            val updated = list + PendingCard(System.currentTimeMillis(), digits, System.currentTimeMillis())
                                            savePendingCards(ctx, updated)
                                            Toast.makeText(ctx, "تم استلام طلبك وسوف يتم الشحن قريبًا.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onCancel = { /* لا شيء */ }
                            )
                        },
                        onSupport = { title ->
                            val number = "+9647763410970"
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("whatsapp", number))
                            Toast.makeText(ctx, "تم نسخ رقم الدعم: $number", Toast.LENGTH_LONG).show()
                            try {
                                val uri = Uri.parse("https://wa.me/9647763410970")
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: Throwable) {}
                        },
                        onBack = { screen = Screen.HOME }
                    )
                    Screen.REFERRAL -> ReferralScreen(
                        myId = userId,
                        inviter = loadInviter(ctx),
                        isPaid = isRefPaid(ctx),
                        onSetInviter = { code ->
                            setInviter(ctx, code)
                            Toast.makeText(ctx, "تم تعيين الداعي.", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { screen = Screen.HOME }
                    )
                    Screen.LEADERBOARD -> LeaderboardScreen(
                        myId = userId,
                        myTotal = loadTotalSpent(ctx),
                        onBack = { screen = Screen.HOME }
                    )
                    Screen.OWNER_LOGIN -> OwnerLoginScreen(
                        onSubmit = { pass ->
                            if (pass == "2000") {
                                setOwner(ctx, true)
                                isOwnerState = true
                                Toast.makeText(ctx, "تم الدخول كمالك.", Toast.LENGTH_SHORT).show()
                                screen = Screen.OWNER_DASH
                            } else {
                                Toast.makeText(ctx, "كلمة المرور غير صحيحة.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBack = { screen = Screen.HOME }
                    )
                    Screen.OWNER_DASH -> OwnerDashboardScreen(
                        onEditPrices = { screen = Screen.OWNER_EDIT },
                        onPendingCards = { screen = Screen.OWNER_PENDING_CARDS },
                        onPendingOrders = { screen = Screen.OWNER_PENDING_ORDERS },
                        onOpenCategory = { cat -> screen = Screen.OWNER_PENDING_CATEGORY(cat) },
                        onBalanceOps = { screen = Screen.OWNER_BALANCE },
                        onApiTools = { screen = Screen.OWNER_API_TOOLS },
                        onUsers = { screen = Screen.OWNER_USERS },
                        onMods = { screen = Screen.OWNER_MODS },
                        onBlock = { screen = Screen.OWNER_BLOCK },
                        onUnblock = { screen = Screen.OWNER_UNBLOCK },
                        onBroadcast = { screen = Screen.OWNER_BROADCAST },
                        onApiCodes = { screen = Screen.OWNER_API_CODES },
                        onReferrals = { screen = Screen.OWNER_REFERRALS },
                        onDiscountsInfo = { screen = Screen.OWNER_DISCOUNTS_INFO },
                        onLeaderboard = { screen = Screen.OWNER_LEADERBOARD },
                        onExitOwner = { setOwner(ctx, false); isOwnerState = false; screen = Screen.HOME },
                        onBack = { screen = Screen.HOME }
                    )
                    Screen.OWNER_EDIT -> OwnerEditPricesScreen(
                        catalog = catalog,
                        onSave = { updated ->
                            catalog = updated
                            persist()
                            Toast.makeText(ctx, "تم حفظ التعديلات.", Toast.LENGTH_SHORT).show()
                            screen = Screen.OWNER_DASH
                        },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_PENDING_CARDS -> OwnerPendingCardsScreen(
                        onApprove = { card, amount ->
                            val newBal = max(0.0, loadBalance(ctx) + amount)
                            saveBalance(ctx, newBal)
                            balance = newBal
                            // أول تمويل => عمولة للداعي (مرة واحدة)
                            if (!isRefPaid(ctx)) {
                                // عمولة ثابتة 0.10$
                                saveBalance(ctx, newBal + 0.0) // الرصيد للمستخدم الحالي فقط؛ عادة ترسل للداعي
                                setRefPaid(ctx)
                            }
                            val list = loadPendingCards(ctx).filterNot { it.id == card.id }
                            savePendingCards(ctx, list)
                            Toast.makeText(ctx, "تم قبول الكارت وإضافة الرصيد.", Toast.LENGTH_SHORT).show()
                        },
                        onReject = { card ->
                            val list = loadPendingCards(ctx).filterNot { it.id == card.id }
                            savePendingCards(ctx, list)
                            Toast.makeText(ctx, "تم رفض الكارت.", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_PENDING_ORDERS -> OwnerPendingOrdersScreen(
                        filter = null,
                        onComplete = { id ->
                            val list = loadOrders(ctx)
                            list.find { it.id == id }?.let { it.status = OrderStatus.COMPLETED }
                            saveOrders(ctx, list)
                        },
                        onReject = { id ->
                            val list = loadOrders(ctx)
                            list.find { it.id == id }?.let { it.status = OrderStatus.REJECTED }
                            saveOrders(ctx, list)
                        },
                        onRefund = { id ->
                            val list = loadOrders(ctx)
                            list.find { it.id == id }?.let {
                                it.status = OrderStatus.REFUNDED
                                saveOrders(ctx, list)
                                val nb = max(0.0, loadBalance(ctx) + it.price)
                                saveBalance(ctx, nb)
                                balance = nb
                            }
                        },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    is Screen.OWNER_PENDING_CATEGORY -> OwnerPendingOrdersScreen(
                        filter = screen.category,
                        onComplete = { id ->
                            val list = loadOrders(ctx)
                            list.find { it.id == id }?.let { it.status = OrderStatus.COMPLETED }
                            saveOrders(ctx, list)
                        },
                        onReject = { id ->
                            val list = loadOrders(ctx)
                            list.find { it.id == id }?.let { it.status = OrderStatus.REJECTED }
                            saveOrders(ctx, list)
                        },
                        onRefund = { id ->
                            val list = loadOrders(ctx)
                            list.find { it.id == id }?.let {
                                it.status = OrderStatus.REFUNDED
                                saveOrders(ctx, list)
                                val nb = max(0.0, loadBalance(ctx) + it.price)
                                saveBalance(ctx, nb); balance = nb
                            }
                        },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_BALANCE -> OwnerBalanceOpsScreen(
                        onAdd = { amount ->
                            val nb = max(0.0, loadBalance(ctx) + amount)
                            saveBalance(ctx, nb); balance = nb
                            Toast.makeText(ctx, "تمت إضافة $$amount", Toast.LENGTH_SHORT).show()
                        },
                        onDeduct = { amount ->
                            val nb = max(0.0, loadBalance(ctx) - amount)
                            saveBalance(ctx, nb); balance = nb
                            Toast.makeText(ctx, "تم خصم $$amount", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_API_TOOLS -> OwnerApiToolsScreen(
                        onCheckOrder = { id ->
                            val st = loadOrders(ctx).find { it.id == id }?.status ?: OrderStatus.WAITING
                            st.name
                        },
                        onCheckProviderBalance = { "الرصيد (وهمي): \$1000.00" },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_USERS -> OwnerUsersScreen(
                        usersCount = 1, // محليًا: مستخدم واحد
                        usersBalance = loadBalance(ctx),
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_MODS -> OwnerModsScreen(
                        isMod = isMod,
                        onSet = { v -> setModerator(ctx, v); isMod = v },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_BLOCK -> OwnerBlockScreen(
                        onBlock = { hours, reason -> setBlock(ctx, hours, reason); Toast.makeText(ctx, "تم الحظر.", Toast.LENGTH_SHORT).show() },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_UNBLOCK -> OwnerUnblockScreen(
                        onUnblock = { clearBlock(ctx); Toast.makeText(ctx, "تم إلغاء الحظر.", Toast.LENGTH_SHORT).show() },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_BROADCAST -> OwnerBroadcastScreen(
                        current = announcement,
                        onSave = { msg -> saveAnnouncement(ctx, msg); announcement = msg; Toast.makeText(ctx, "تم حفظ الإعلان.", Toast.LENGTH_SHORT).show() },
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_API_CODES -> OwnerApiCodesScreen(
                        catalog = catalog,
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_REFERRALS -> OwnerReferralsScreen(
                        inviter = loadInviter(ctx),
                        firstFundingPaid = isRefPaid(ctx),
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_DISCOUNTS_INFO -> DiscountsInfoScreen(
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                    Screen.OWNER_LEADERBOARD -> AdminLeaderboardScreen(
                        myId = userId,
                        total = loadTotalSpent(ctx),
                        onBack = { screen = Screen.OWNER_DASH }
                    )
                }
            }
        }
    }
}

/* ======================= عناصر عامة ======================= */
@Composable
fun TopBar(title: String, subtitle: String, showOwnerButton: Boolean, onOwnerClick: () -> Unit) {
    TopAppBar(
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp)
            }
        },
        actions = {
            if (showOwnerButton) {
                TextButton(onClick = onOwnerClick) { Text("لوحة المالك") }
            }
        }
    )
}

@Composable
fun Banner(text: String) {
    if (text.isBlank()) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
    ) {
        Text(text, Modifier.padding(12.dp), color = Color(0xFF805900))
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, color: Color = Color(0xFF2E7D32)) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(6.dp))
    }
}

@Composable
fun BlockedView(reason: String, content: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("تم حظرك مؤقتًا", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red)
        Spacer(Modifier.height(6.dp))
        Text(reason, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/* ======================= HOME ======================= */
@Composable
fun HomeScreen(
    onServices: () -> Unit,
    onOrders: () -> Unit,
    onBalance: () -> Unit,
    onReferral: () -> Unit,
    onLeaderboard: () -> Unit,
    onOwnerLogin: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        BigButton("الخدمات", onServices)
        BigButton("طلباتي", onOrders)
        BigButton("رصيدي", onBalance)
        BigButton("الإحالة", onReferral)
        BigButton("المتصدرين 🎉", onLeaderboard)
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOwnerLogin) { Text("دخول المالك") }
        }
    }
}

/* ======================= SERVICES ======================= */
@Composable
fun ServicesScreen(catalog: List<Section>, isModerator: Boolean, onOpenSection: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الأقسام", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(catalog) { sec ->
                Card(Modifier.fillMaxWidth().clickable { onOpenSection(sec.key) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(sec.title, fontWeight = FontWeight.Bold)
                        val count = sec.services.size
                        val lbl = if (isModerator) "خصم 10% للمشرف" else ""
                        Text("عدد الخدمات: $count  $lbl", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun SectionScreen(section: Section, isModerator: Boolean, onOrder: (Service) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(section.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(section.services) { svc ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(svc.name, fontWeight = FontWeight.Bold)
                            val price = if (isModerator) svc.price * 0.9 else svc.price
                            Text("$${"%.2f".format(price)}", color = Color(0xFF2E7D32))
                        }
                        Button(onClick = { onOrder(svc) }) { Text("طلب الخدمة") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

/* ======================= ORDERS ======================= */
@Composable
fun OrdersListScreen(orders: List<Order>, onBack: () -> Unit) {
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
                            Text("الفئة: ${o.category}")
                            Text("الحالة: ${o.status}")
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

/* ======================= BALANCE ======================= */
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
        BigButton("شحن عبر نقاط سنتات") { onSupport("شحن عبر نقاط سنتات") }
        BigButton("شحن عبر زين كاش") { onSupport("شحن عبر زين كاش") }
        BigButton("شحن عبر هلا بي") { onSupport("شحن عبر هلا بي") }
        BigButton("شحن عبر USDT")   { onSupport("شحن عبر USDT") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

/* يظهر Dialog إدخال كارت آسيا سيل ثم يعود للشاشة نفسها */
@Composable
fun BalanceAsiacellDialog(
    returnTo: Screen,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
): Screen {
    var digits by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { onCancel() },
        title = { Text("أدخل رقم الكارت (14/16 رقم)") },
        text = {
            Column {
                OutlinedTextField(
                    value = digits,
                    onValueChange = {
                        digits = it.filter { ch -> ch.isDigit() }
                        error = null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = { if (error != null) Text(error!!, color = Color.Red) }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val ok = (digits.length == 14 || digits.length == 16)
                if (!ok) error = "الرقم يجب أن يكون 14 أو 16 رقمًا."
                else onSubmit(digits)
            }) { Text("إرسال") }
        },
        dismissButton = { OutlinedButton(onClick = { onCancel() }) { Text("إلغاء") } }
    )
    return returnTo
}

/* ======================= REFERRAL ======================= */
@Composable
fun ReferralScreen(myId: String, inviter: String?, isPaid: Boolean, onSetInviter: (String) -> Unit, onBack: () -> Unit) {
    val clip = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("نظام الإحالة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("رابط دعوتك", fontWeight = FontWeight.Bold)
                val link = "ratluzen://invite/$myId"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(link, modifier = Modifier.weight(1f))
                    TextButton(onClick = { clip.setText(AnnotatedString(link)) }) { Text("نسخ") }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("هل لديك كود داعٍ؟")
                var txt by remember { mutableStateOf(inviter ?: "") }
                OutlinedTextField(value = txt, onValueChange = { txt = it }, label = { Text("كود الداعي") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSetInviter(txt) }) { Text("حفظ") }
                    if (isPaid) Text("تم دفع العمولة لأول تمويل", color = Color(0xFF2E7D32))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

/* ======================= LEADERBOARD ======================= */
@Composable
fun LeaderboardScreen(myId: String, myTotal: Double, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("المتصدرون", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("1) $myId — \$${"%.2f".format(myTotal)}", fontWeight = FontWeight.Bold)
                Text("سيتم عرض مزيد من المستخدمين عند ربط قاعدة بيانات خارجية.", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

/* ======================= OWNER SCREENS ======================= */
@Composable
fun OwnerLoginScreen(onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var pass by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("دخول المالك", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("كلمة المرور") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        BigButton("دخول") { onSubmit(pass) }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerDashboardScreen(
    onEditPrices: () -> Unit,
    onPendingCards: () -> Unit,
    onPendingOrders: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onBalanceOps: () -> Unit,
    onApiTools: () -> Unit,
    onUsers: () -> Unit,
    onMods: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onBroadcast: () -> Unit,
    onApiCodes: () -> Unit,
    onReferrals: () -> Unit,
    onDiscountsInfo: () -> Unit,
    onLeaderboard: () -> Unit,
    onExitOwner: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("لوحة تحكم المالك", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        FlowButtons(
            listOf(
                "تعديل الأسعار والكميات" to onEditPrices,
                "الطلبات المعلّقة (الخدمات)" to onPendingOrders,
                "الكارتات المعلّقة" to onPendingCards,
                "طلبات شدات ببجي" to { onOpenCategory("pubg") },
                "طلبات شحن الايتونز" to { onOpenCategory("itunes") },
                "طلبات الأرصدة المعلّقة" to { onOpenCategory("mobile") },
                "طلبات لودو المعلّقة" to { onOpenCategory("ludo") },
                "طلبات التليجرام" to { onOpenCategory("telegram") },
                "إضافة/خصم رصيد" to onBalanceOps,
                "فحص حالة طلب API" to onApiTools,
                "فحص رصيد API" to onApiTools,
                "رصيد المستخدمين" to onUsers,
                "عدد المستخدمين" to onUsers,
                "إدارة المشرفين" to onMods,
                "حظر المستخدم" to onBlock,
                "إلغاء حظر المستخدم" to onUnblock,
                "إعلان التطبيق" to onBroadcast,
                "أكواد خدمات API" to onApiCodes,
                "نظام الإحالة" to onReferrals,
                "شرح الخصومات" to onDiscountsInfo,
                "المتصدرين" to onLeaderboard,
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("رجوع") }
            TextButton(onClick = onExitOwner, modifier = Modifier.align(Alignment.CenterVertically)) { Text("إيقاف وضع المالك") }
        }
    }
}

@Composable
fun FlowButtons(items: List<Pair<String, () -> Unit>>) {
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { (t, a) -> BigButton(t, a) }
    }
}

@Composable
fun OwnerEditPricesScreen(catalog: List<Section>, onSave: (MutableList<Section>) -> Unit, onBack: () -> Unit) {
    var local by remember {
        mutableStateOf(catalog.map { sec ->
            Section(sec.key, sec.title, sec.services.map { it.copy() }.toMutableList())
        }.toMutableList())
    }
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
fun OwnerPendingCardsScreen(onApprove: (PendingCard, Double) -> Unit, onReject: (PendingCard) -> Unit, onBack: () -> Unit) {
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
fun OwnerPendingOrdersScreen(
    filter: String?,
    onComplete: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onRefund: (Long) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var orders by remember {
        mutableStateOf(loadOrders(ctx).filter { it.status == OrderStatus.PENDING && (filter == null || it.category == filter) })
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الطلبات المعلقة" + if (filter != null) " ($filter)" else "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (orders.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("لا توجد طلبات معلّقة") }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orders) { o ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(o.serviceName, fontWeight = FontWeight.Bold)
                            Text("السعر: $${"%.2f".format(o.price)}")
                            Text("الفئة: ${o.category}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    onComplete(o.id)
                                    orders = loadOrders(ctx).filter { it.status == OrderStatus.PENDING && (filter == null || it.category == filter) }
                                }) { Text("إكمال") }
                                OutlinedButton(onClick = {
                                    onReject(o.id)
                                    orders = loadOrders(ctx).filter { it.status == OrderStatus.PENDING && (filter == null || it.category == filter) }
                                }) { Text("رفض") }
                                OutlinedButton(onClick = {
                                    onRefund(o.id)
                                    orders = loadOrders(ctx).filter { it.status == OrderStatus.PENDING && (filter == null || it.category == filter) }
                                }) { Text("استرجاع") }
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
fun OwnerBalanceOpsScreen(onAdd: (Double) -> Unit, onDeduct: (Double) -> Unit, onBack: () -> Unit) {
    var value by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("إضافة/خصم رصيد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedTextField(
            value = value, onValueChange = { value = it.filter { c -> c.isDigit() || c=='.' } },
            label = { Text("المبلغ (USD)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { value.toDoubleOrNull()?.let(onAdd) }, modifier = Modifier.weight(1f)) { Text("إضافة") }
            OutlinedButton(onClick = { value.toDoubleOrNull()?.let(onDeduct) }, modifier = Modifier.weight(1f)) { Text("خصم") }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerApiToolsScreen(onCheckOrder: (Long) -> String, onCheckProviderBalance: () -> String, onBack: () -> Unit) {
    var orderId by remember { mutableStateOf("") }
    var orderStatus by remember { mutableStateOf<String?>(null) }
    val providerBalance = remember { onCheckProviderBalance() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("أدوات API", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("فحص رصيد المزود: $providerBalance")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = orderId, onValueChange = { orderId = it.filter { c -> c.isDigit() } },
                label = { Text("رقم الطلب") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { orderStatus = orderId.toLongOrNull()?.let(onCheckOrder) ?: "غير صالح" }) { Text("فحص") }
        }
        if (orderStatus != null) Text("الحالة: $orderStatus")
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerUsersScreen(usersCount: Int, usersBalance: Double, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("المستخدمون", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("عدد المستخدمين: $usersCount")
        Text("رصيد المستخدم الحالي: $${"%.2f".format(usersBalance)}")
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerModsScreen(isMod: Boolean, onSet: (Boolean) -> Unit, onBack: () -> Unit) {
    var state by remember { mutableStateOf(isMod) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("إدارة المشرفين", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("تفعيل خصم المشرف (10%) للمستخدم الحالي")
            Spacer(Modifier.width(8.dp))
            Switch(checked = state, onCheckedChange = { state = it; onSet(it) })
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerBlockScreen(onBlock: (Int, String) -> Unit, onBack: () -> Unit) {
    var hours by remember { mutableStateOf("2") }
    var reason by remember { mutableStateOf("سلوك مريب/محاولات كثيرة") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("حظر المستخدم", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedTextField(value = hours, onValueChange = { hours = it.filter { c -> c.isDigit() } }, label = { Text("عدد الساعات") }, singleLine = true)
        OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("السبب") }, singleLine = false, modifier = Modifier.fillMaxWidth())
        BigButton("تنفيذ الحظر") { onBlock(hours.toIntOrNull() ?: 2, reason) }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerUnblockScreen(onUnblock: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("إلغاء الحظر", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        BigButton("إلغاء الحظر الآن", onUnblock)
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerBroadcastScreen(current: String, onSave: (String) -> Unit, onBack: () -> Unit) {
    var msg by remember { mutableStateOf(current) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("إعلان التطبيق", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text("نص الإعلان") }, modifier = Modifier.fillMaxWidth().height(160.dp))
        Spacer(Modifier.height(8.dp))
        BigButton("حفظ", onClick = { onSave(msg) })
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerApiCodesScreen(catalog: List<Section>, onBack: () -> Unit) {
    val clip = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("أكواد خدمات API", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        LazyColumn(Modifier.weight(1f)) {
            catalog.forEach { sec ->
                item {
                    Text(sec.title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
                items(sec.services) { sv ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${sv.name} — (${sv.id})", modifier = Modifier.weight(1f))
                        TextButton(onClick = { clip.setText(AnnotatedString(sv.id)) }) { Text("نسخ") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun OwnerReferralsScreen(inviter: String?, firstFundingPaid: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("نظام الإحالة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("الداعي الحالي للمستخدم: ${inviter ?: "لا يوجد"}")
        Text("عمولة أول تمويل مدفوعة؟ ${if (firstFundingPaid) "نعم" else "لا"}")
        Text("ملاحظة: النظام محلي. عند ربط الخادم ستظهر قوائم أكبر وأفضل الداعين.")
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun DiscountsInfoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("شرح الخصومات", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("المشرفون يحصلون على خصم تلقائي 10% على الأسعار المعروضة.")
        Text("يمكنك تفعيل/تعطيل دور المشرف من لوحة المالك > إدارة المشرفين.")
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun AdminLeaderboardScreen(myId: String, total: Double, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("المتصدرون (للمالك)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("1) $myId — \$${"%.2f".format(total)}", fontWeight = FontWeight.Bold)
                Text("سيتم حساب الترتيب لجميع المستخدمين عند ربط قاعدة البيانات.", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}
