@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zafer.smm

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ---------------------------------------------------------
// نماذج البيانات
// ---------------------------------------------------------
data class User(val id: String, val createdAt: Long, var balance: Double)
enum class OrderStatus { PENDING, IN_PROGRESS, DONE, REJECTED }
data class ServiceItem(
    val id: Int,
    val categoryKey: String,
    val categoryTitle: String,
    val display: String,   // النص الظاهر (الاسم + الكمية + السعر) كما تريد بالضبط
    val quantity: Int,
    val price: Double
)
data class Order(
    val id: String,
    val userId: String,
    val serviceId: Int,
    val serviceDisplay: String,
    val quantity: Int,
    val price: Double,
    val input: String,
    var status: OrderStatus,
    val createdAt: Long
)
enum class TopupStatus { PENDING, APPROVED, REJECTED }
data class TopupRequest(
    val id: String,
    val userId: String,
    val method: String,
    val code: String?,
    val submittedAt: Long,
    var status: TopupStatus,
    var approvedAmount: Double?,
    var note: String?
)

// ---------------------------------------------------------
// كتالوج افتراضي بسيط (يُستبدل عند الاستيراد)
// ---------------------------------------------------------
object DefaultCatalog {
    // مفاتيح الأقسام الافتراضية وترجمتها (يمكن تعديل العناوين فقط لو رغبت)
    val sectionTitles: LinkedHashMap<String, String> = linkedMapOf(
        "followers_tiktok" to "قسم المتابعين (تيكتوك)",
        "followers_instagram" to "قسم المتابعين (انستغرام)",
        "likes_tiktok" to "قسم الإعجابات (تيكتوك)",
        "likes_instagram" to "قسم الإعجابات (انستغرام)",
        "views_tiktok" to "قسم المشاهدات (تيكتوك)",
        "views_instagram" to "قسم المشاهدات (انستغرام)",
        "live_tiktok" to "قسم مشاهدات البث (تيكتوك)",
        "live_instagram" to "قسم مشاهدات البث (انستغرام)",
        "pubg" to "قسم شحن شدات ببجي",
        "itunes" to "قسم شراء رصيد ايتونز",
        "telegram_channels" to "قسم خدمات التليجرام (قنوات)",
        "telegram_groups" to "قسم خدمات التليجرام (كروبات)",
        "ludo" to "قسم خدمات اللودو",
        "score_tiktok" to "قسم رفع سكور تيكتوك",
        "mobile_balance" to "قسم شراء رصيد الهاتف"
    )

    // عناصر قليلة كعَيّنة (سوف تُستبدل بالكامل عند الاستيراد)
    val items: List<ServiceItem> = listOf(
        ServiceItem(1000, "followers_tiktok", sectionTitles["followers_tiktok"]!!, "متابعين تيكتوك (1000) - $3.50", 1000, 3.50),
        ServiceItem(1001, "likes_tiktok", sectionTitles["likes_tiktok"]!!, "لايكات تيكتوك (1000) - $0.80", 1000, 0.80),
        ServiceItem(2000, "pubg", sectionTitles["pubg"]!!, "ببجي 60 UC - $1.90", 60, 1.90),
        ServiceItem(3000, "itunes", sectionTitles["itunes"]!!, "بطاقة iTunes $10 - $9.70", 10, 9.70),
        ServiceItem(4000, "mobile_balance", sectionTitles["mobile_balance"]!!, "شراء رصيد أثير (5$) - $8.50", 5, 8.50)
    )
}

// ---------------------------------------------------------
// تخزين محلي (SharedPreferences + JSON) + كتالوج مخصّص
// ---------------------------------------------------------
class LocalRepo(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("smm_local", Context.MODE_PRIVATE)
    private fun getString(key: String) = prefs.getString(key, null)
    private fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()

    // --- المستخدم ---
    fun getOrCreateUser(): User {
        val raw = getString("user")
        if (raw != null) return userFromJson(JSONObject(raw))
        val u = User(UUID.randomUUID().toString(), System.currentTimeMillis(), 0.0)
        saveUser(u); return u
    }
    fun saveUser(u: User) = putString("user", userToJson(u).toString())
    fun credit(userId: String, amount: Double) : User {
        val u = getOrCreateUser()
        if (u.id == userId) { u.balance += amount; saveUser(u) }
        return u
    }
    fun debit(userId: String, amount: Double): Boolean {
        val u = getOrCreateUser()
        if (u.id != userId) return false
        if (u.balance + 1e-9 < amount) return false
        u.balance -= amount; saveUser(u); return true
    }

    // --- الطلبات ---
    fun loadOrders(): MutableList<Order> {
        val arr = JSONArray(getString("orders") ?: "[]")
        return MutableList(arr.length()) { i -> orderFromJson(arr.getJSONObject(i)) }
    }
    fun saveOrders(list: List<Order>) {
        val arr = JSONArray(); list.forEach { arr.put(orderToJson(it)) }; putString("orders", arr.toString())
    }

    // --- شحن/كروت ---
    fun loadTopups(): MutableList<TopupRequest> {
        val arr = JSONArray(getString("topups") ?: "[]")
        return MutableList(arr.length()) { i -> topupFromJson(arr.getJSONObject(i)) }
    }
    fun saveTopups(list: List<TopupRequest>) {
        val arr = JSONArray(); list.forEach { arr.put(topupToJson(it)) }; putString("topups", arr.toString())
    }

    // --- الكتالوج المخصّص ---
    fun hasCustomCatalog(): Boolean = getString("catalog_json") != null
    fun loadCatalogOrDefault(): Pair<LinkedHashMap<String, String>, List<ServiceItem>> {
        val raw = getString("catalog_json")
        if (raw.isNullOrBlank()) return DefaultCatalog.sectionTitles to DefaultCatalog.items
        return importFromJson(raw)
    }
    fun saveCustomCatalog(sectionTitles: LinkedHashMap<String,String>, items: List<ServiceItem>) {
        val obj = JSONObject()
        val titlesObj = JSONObject()
        sectionTitles.forEach { (k,v) -> titlesObj.put(k, v) }
        obj.put("titles", titlesObj)
        val arr = JSONArray()
        items.forEach { arr.put(serviceToJson(it)) }
        obj.put("items", arr)
        putString("catalog_json", obj.toString())
    }
    fun clearCustomCatalog() = putString("catalog_json", null)

    // --- JSON Helpers ---
    private fun userToJson(u: User) = JSONObject().apply {
        put("id", u.id); put("createdAt", u.createdAt); put("balance", u.balance)
    }
    private fun userFromJson(o: JSONObject) = User(
        id = o.getString("id"), createdAt = o.getLong("createdAt"), balance = o.optDouble("balance",0.0)
    )
    private fun orderToJson(o: Order) = JSONObject().apply {
        put("id",o.id); put("userId",o.userId); put("serviceId",o.serviceId)
        put("serviceDisplay",o.serviceDisplay); put("quantity",o.quantity)
        put("price",o.price); put("input",o.input)
        put("status",o.status.name); put("createdAt",o.createdAt)
    }
    private fun orderFromJson(o: JSONObject) = Order(
        id=o.getString("id"), userId=o.getString("userId"), serviceId=o.getInt("serviceId"),
        serviceDisplay=o.getString("serviceDisplay"), quantity=o.getInt("quantity"),
        price=o.getDouble("price"), input=o.optString("input",""),
        status=OrderStatus.valueOf(o.getString("status")), createdAt=o.getLong("createdAt")
    )
    private fun topupToJson(t: TopupRequest) = JSONObject().apply {
        put("id",t.id); put("userId",t.userId); put("method",t.method)
        put("code",t.code); put("submittedAt",t.submittedAt)
        put("status",t.status.name); put("approvedAmount",t.approvedAmount)
        put("note",t.note)
    }
    private fun topupFromJson(o: JSONObject) = TopupRequest(
        id=o.getString("id"), userId=o.getString("userId"), method=o.getString("method"),
        code= if (o.isNull("code")) null else o.getString("code"),
        submittedAt=o.getLong("submittedAt"),
        status=TopupStatus.valueOf(o.getString("status")),
        approvedAmount= if (o.isNull("approvedAmount")) null else o.getDouble("approvedAmount"),
        note= if (o.isNull("note")) null else o.getString("note")
    )
    private fun serviceToJson(s: ServiceItem) = JSONObject().apply {
        put("id", s.id); put("categoryKey", s.categoryKey); put("categoryTitle", s.categoryTitle)
        put("display", s.display); put("quantity", s.quantity); put("price", s.price)
    }
    private fun serviceFromJson(o: JSONObject) = ServiceItem(
        id=o.getInt("id"),
        categoryKey=o.getString("categoryKey"),
        categoryTitle=o.getString("categoryTitle"),
        display=o.getString("display"),
        quantity=o.getInt("quantity"),
        price=o.getDouble("price")
    )
    private fun importFromJson(raw: String): Pair<LinkedHashMap<String, String>, List<ServiceItem>> {
        val obj = JSONObject(raw)
        val titlesObj = obj.getJSONObject("titles")
        val titles = LinkedHashMap<String,String>()
        titlesObj.keys().forEach { k -> titles[k] = titlesObj.getString(k) }
        val arr = obj.getJSONArray("items")
        val items = MutableList(arr.length()) { i -> serviceFromJson(arr.getJSONObject(i)) }
        return titles to items
    }
}

// ---------------------------------------------------------
// أدوات مساعدة للكتالوج: parsing من نص
// ---------------------------------------------------------
object CatalogImporter {
    // تحويل اسم قسم عربي إلى مفتاح (slug)
    private fun slugify(ar: String): String {
        val base = ar.trim()
            .replace("[^\\p{L}\\p{Nd}\\s]".toRegex(), "")
            .replace("\\s+".toRegex(), "_")
        return base.lowercase()
    }

    /**
     * صيغة السطر الواحد (بدقّة):
     * القسم | اسم الخدمة كما تريد أن يظهر | الكمية (رقم فقط) | السعر (بالدولار)
     *
     * مثال:
     * قسم المتابعين (تيكتوك) | متابعين تيكتوك (1000) - $3.50 | 1000 | 3.50
     *
     * ملاحظات:
     * - "display" هو النص الظاهر في الزر، اكتبه كما تحب (سوف نعرضه كما هو).
     * - الكمية والسعر يجب أن يكونا أرقامًا قابلة للتحويل.
     * - يمكن استخدام فاصلة عشرية بنقطة فقط.
     */
    fun parse(text: String): Pair<LinkedHashMap<String,String>, List<ServiceItem>> {
        val titles = LinkedHashMap<String,String>()
        val items = mutableListOf<ServiceItem>()
        var autoId = 10000
        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach
            // تجاهل التعليقات
            if (line.startsWith("#")) return@forEach

            val parts = line.split("|").map { it.trim() }
            if (parts.size < 4) {
                // سطر غير صالح — نتجاهله
                return@forEach
            }
            val sectionTitle = parts[0]
            val display = parts[1]
            val qty = parts[2].replace("k","000", ignoreCase = true).trim().toIntOrNull()
            val price = parts[3].trim().toDoubleOrNull()

            if (qty == null || price == null) return@forEach

            val key = slugify(sectionTitle)
            if (!titles.containsKey(key)) titles[key] = sectionTitle

            items += ServiceItem(
                id = autoId++,
                categoryKey = key,
                categoryTitle = sectionTitle,
                display = display,
                quantity = qty,
                price = price
            )
        }
        return titles to items
    }
}

// ---------------------------------------------------------
// تنقّل الشاشات
// ---------------------------------------------------------
sealed class Screen {
    object HOME: Screen()
    object SERVICES: Screen()
    data class SERVICE_LIST(val catKey: String): Screen()
    data class ORDER_CREATE(val item: ServiceItem): Screen()
    object BALANCE: Screen()
    object TOPUP_METHODS: Screen()
    object TOPUP_ASIACELL: Screen()
    data class TOPUP_SUPPORT(val method: String): Screen()
    object MY_ORDERS: Screen()
    object REFERRAL: Screen()
    object LEADERBOARD: Screen()
    object ADMIN_LOGIN: Screen()
    object ADMIN_PANEL: Screen()
    object ADMIN_IMPORT: Screen()
}

// ---------------------------------------------------------
// النشاط الرئيسي
// ---------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppRoot() } }
    }
}

// ---------------------------------------------------------
// الجذر
// ---------------------------------------------------------
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val repo = remember { LocalRepo(ctx) }
    val user by remember { mutableStateOf(repo.getOrCreateUser()) }
    var screen by remember { mutableStateOf<Screen>(Screen.HOME) }
    var sectionTitles by remember { mutableStateOf(LinkedHashMap(DefaultCatalog.sectionTitles)) }
    var allItems by remember { mutableStateOf(DefaultCatalog.items) }

    // تحميل الكتالوج (مخصّص أو افتراضي)
    LaunchedEffect(Unit) {
        val (t, items) = repo.loadCatalogOrDefault()
        sectionTitles = LinkedHashMap(t)
        allItems = items
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "خدمات راتلوزن",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        SmallAdminButton { screen = Screen.ADMIN_LOGIN }
                    }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (val s = screen) {
                Screen.HOME -> HomeScreen(
                    user = user,
                    onOpenServices = { screen = Screen.SERVICES },
                    onOrders = { screen = Screen.MY_ORDERS },
                    onBalance = { screen = Screen.BALANCE },
                    onReferral = { screen = Screen.REFERRAL },
                    onLeaderboard = { screen = Screen.LEADERBOARD },
                )

                Screen.SERVICES -> ServicesCategoriesScreen(
                    sectionTitles = sectionTitles,
                    onBack = { screen = Screen.HOME },
                    onOpenCategory = { key -> screen = Screen.SERVICE_LIST(key) }
                )

                is Screen.SERVICE_LIST -> ServiceListScreen(
                    sectionKey = s.catKey,
                    sectionTitles = sectionTitles,
                    allItems = allItems,
                    onBack = { screen = Screen.SERVICES },
                    onPick = { item -> screen = Screen.ORDER_CREATE(item) }
                )

                is Screen.ORDER_CREATE -> OrderCreateScreen(
                    repo = repo, userId = user.id, item = s.item,
                    onDone = {
                        if (it) Toast.makeText(ctx,"تم إرسال الطلب وخصم الرصيد",Toast.LENGTH_SHORT).show()
                        screen = Screen.MY_ORDERS
                    },
                    onBack = { screen = Screen.SERVICE_LIST(s.item.categoryKey) }
                )

                Screen.BALANCE -> BalanceScreen(
                    repo = repo, userId = user.id,
                    onBack = { screen = Screen.HOME },
                    onTopup = { screen = Screen.TOPUP_METHODS }
                )

                Screen.TOPUP_METHODS -> TopupMethodsScreen(
                    onBack = { screen = Screen.BALANCE },
                    onAsiacell = { screen = Screen.TOPUP_ASIACELL },
                    onSupport = { method -> screen = Screen.TOPUP_SUPPORT(method) }
                )

                Screen.TOPUP_ASIACELL -> AsiacellCardScreen(
                    repo = repo, userId = user.id,
                    onBack = { screen = Screen.TOPUP_METHODS },
                    onSubmitted = {
                        Toast.makeText(ctx,"تم استلام طلبك، سوف يتم شحن حسابك قريبًا.",Toast.LENGTH_LONG).show()
                        screen = Screen.BALANCE
                    }
                )

                is Screen.TOPUP_SUPPORT -> SupportTopupScreen(
                    method = s.method,
                    onBack = { screen = Screen.TOPUP_METHODS }
                )

                Screen.MY_ORDERS -> MyOrdersScreen(
                    repo = repo, userId = user.id,
                    onBack = { screen = Screen.HOME }
                )

                Screen.REFERRAL -> ReferralScreen(onBack = { screen = Screen.HOME })
                Screen.LEADERBOARD -> LeaderboardScreen(onBack = { screen = Screen.HOME })

                Screen.ADMIN_LOGIN -> AdminLoginScreen(
                    onCancel = { screen = Screen.HOME },
                    onOk = { pass ->
                        if (pass.trim() == "2000") screen = Screen.ADMIN_PANEL
                        else Toast.makeText(ctx,"كلمة مرور خاطئة",Toast.LENGTH_SHORT).show()
                    }
                )

                Screen.ADMIN_PANEL -> AdminPanelScreen(
                    repo = repo,
                    onBack = { screen = Screen.HOME },
                    onOpenImport = { screen = Screen.ADMIN_IMPORT },
                    onCatalogChanged = {
                        val (t, items) = repo.loadCatalogOrDefault()
                        sectionTitles = LinkedHashMap(t)
                        allItems = items
                    }
                )

                Screen.ADMIN_IMPORT -> ImportServicesScreen(
                    repo = repo,
                    onBack = { screen = Screen.ADMIN_PANEL },
                    onSaved = {
                        // إعادة تحميل الكتالوج
                        val (t, items) = repo.loadCatalogOrDefault()
                        sectionTitles = LinkedHashMap(t)
                        allItems = items
                        screen = Screen.ADMIN_PANEL
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------
// الواجهة الرئيسية (يحافظ على الشكل/الألوان الحالية)
// ---------------------------------------------------------
@Composable
fun HomeScreen(
    user: User,
    onOpenServices: () -> Unit,
    onOrders: () -> Unit,
    onBalance: () -> Unit,
    onReferral: () -> Unit,
    onLeaderboard: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("رصيدك الحالي:", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("$${"%.2f".format(user.balance)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("القائمة الرئيسية", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { GreenItem("الخدمات") { onOpenServices() } }
                Box(modifier = Modifier.weight(1f)) { GreenItem("طلباتي") { onOrders() } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { GreenItem("رصيدي") { onBalance() } }
                Box(modifier = Modifier.weight(1f)) { GreenItem("الإحالة") { onReferral() } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { GreenItem("المتصدرين 🎉") { onLeaderboard() } }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------
// أقسام الخدمات
// ---------------------------------------------------------
@Composable
fun ServicesCategoriesScreen(
    sectionTitles: LinkedHashMap<String, String>,
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("الخدمات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sectionTitles.entries.toList()) { (key, title) ->
                GreenItem(title) { onOpenCategory(key) }
            }
        }
    }
}

// ---------------------------------------------------------
// خدمات قسم محدد
// ---------------------------------------------------------
@Composable
fun ServiceListScreen(
    sectionKey: String,
    sectionTitles: LinkedHashMap<String, String>,
    allItems: List<ServiceItem>,
    onBack: () -> Unit,
    onPick: (ServiceItem) -> Unit
) {
    val title = sectionTitles[sectionKey] ?: "خدمات"
    val itemsInSection = remember(sectionKey, allItems) { allItems.filter { it.categoryKey == sectionKey } }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (itemsInSection.isEmpty()) {
            Text("لا توجد خدمات في هذا القسم (أضفها من لوحة المالك > استيراد الخدمات).", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(itemsInSection) { item ->
                    GreenItem(item.display) { onPick(item) }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// إنشاء الطلب
// ---------------------------------------------------------
@Composable
fun OrderCreateScreen(
    repo: LocalRepo,
    userId: String,
    item: ServiceItem,
    onDone: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("تأكيد الطلب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(item.display, fontWeight = FontWeight.Bold)
        Text("السعر: $${item.price} | الكمية: ${item.quantity}")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = input, onValueChange = { input = it },
            label = { Text("أدخل رابط/يوزر أو بيانات الطلب") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        GreenButton("اطلب الآن") {
            val ok = repo.debit(userId, item.price)
            if (!ok) {
                msg = "رصيدك غير كافٍ لإتمام العملية."
            } else {
                val orders = repo.loadOrders()
                orders += Order(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    serviceId = item.id,
                    serviceDisplay = item.display,
                    quantity = item.quantity,
                    price = item.price,
                    input = input.trim(),
                    status = OrderStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )
                repo.saveOrders(orders)
                onDone(true)
            }
        }
        msg?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color.Red) }
    }
}

// ---------------------------------------------------------
// رصيدي وطرق الشحن
// ---------------------------------------------------------
@Composable
fun BalanceScreen(
    repo: LocalRepo,
    userId: String,
    onBack: () -> Unit,
    onTopup: () -> Unit
) {
    val userState = remember { mutableStateOf(repo.getOrCreateUser()) }
    val user = userState.value
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("رصيدي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("رصيدك الحالي: $${"%.2f".format(user.balance)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        GreenItem("الشحن / زيادة الرصيد") { onTopup() }
    }
}

@Composable
fun TopupMethodsScreen(
    onBack: () -> Unit,
    onAsiacell: () -> Unit,
    onSupport: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("اختر طريقة الشحن", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        GreenItem("شحن عبر اسياسيل") { onAsiacell() }
        GreenItem("شحن عبر سوبركي") { onSupport("شحن عبر سوبركي") }
        GreenItem("شحن عبر نقاط سنتات") { onSupport("شحن عبر نقاط سنتات") }
        GreenItem("شحن عبر زين كاش") { onSupport("شحن عبر زين كاش") }
        GreenItem("شحن عبر هلا بي") { onSupport("شحن عبر هلا بي") }
        GreenItem("شحن عبر USDT") { onSupport("شحن عبر USDT") }
        Spacer(Modifier.height(10.dp))
        Text("عند اختيار أي طريقة غير اسياسيل سيتم توجيهك للدعم على واتساب.", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun SupportTopupScreen(method: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val phone = "+9647763410970"
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text(method, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("لإكمال طلبك تواصل مع الدعم الفني عبر الواتساب:\n$phone")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GreenButton("نسخ الرقم") {
                clip.setText(AnnotatedString(phone))
                Toast.makeText(ctx,"تم نسخ الرقم",Toast.LENGTH_SHORT).show()
            }
            GreenButton("فتح واتساب") {
                val url = "https://wa.me/${phone.replace("+","")}"
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }
}

@Composable
fun AsiacellCardScreen(
    repo: LocalRepo,
    userId: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    val ctx = LocalContext.current
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("شحن عبر اسياسيل", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("أرسل رقم الكارت المكون من 14 أو 16 رقم.")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(20) },
            label = { Text("رقم الكارت") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Spacer(Modifier.height(6.dp)); Text(it, color = Color.Red) }
        Spacer(Modifier.height(12.dp))
        GreenButton("إرسال") {
            val valid = (code.length == 14 || code.length == 16)
            if (!valid) { error = "الرجاء إدخال رقم صحيح من 14 أو 16 رقم"; return@GreenButton }
            val list = repo.loadTopups()
            list += TopupRequest(
                id = UUID.randomUUID().toString(),
                userId = userId,
                method = "asiacell",
                code = code,
                submittedAt = System.currentTimeMillis(),
                status = TopupStatus.PENDING,
                approvedAmount = null,
                note = null
            )
            repo.saveTopups(list)
            Toast.makeText(ctx,"تم استلام طلبك، سوف يتم شحن حسابك قريبًا.",Toast.LENGTH_LONG).show()
            onSubmitted()
        }
    }
}

// ---------------------------------------------------------
// طلباتي
// ---------------------------------------------------------
@Composable
fun MyOrdersScreen(repo: LocalRepo, userId: String, onBack: () -> Unit) {
    val orders = remember { mutableStateListOf<Order>() }
    LaunchedEffect(Unit) {
        orders.clear()
        orders.addAll(repo.loadOrders().filter { it.userId==userId }.sortedByDescending { it.createdAt })
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("طلباتي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (orders.isEmpty()) Text("لا توجد طلبات بعد.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders) { o ->
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text(o.serviceDisplay, fontWeight = FontWeight.Bold)
                        Text("السعر: $${o.price} | الحالة: ${when(o.status){
                            OrderStatus.PENDING -> "قيد المراجعة"
                            OrderStatus.IN_PROGRESS -> "قيد التنفيذ"
                            OrderStatus.DONE -> "مكتمل"
                            OrderStatus.REJECTED -> "مرفوض"
                        }}")
                        if (o.input.isNotBlank()) Text("البيانات: ${o.input}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// الإحالة (عرض مبسّط)
@Composable
fun ReferralScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val invite = remember { "RAT-${UUID.randomUUID().toString().take(8)}" }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("نظام الإحالة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("كود الدعوة الخاص بك:")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(invite, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            GreenMini("نسخ") {
                clip.setText(AnnotatedString(invite))
                Toast.makeText(ctx,"تم نسخ الكود",Toast.LENGTH_SHORT).show()
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("عند أول تمويل للمحالة تُضاف عمولة ثابتة لحسابك (مثال: $0.10).")
    }
}

// ---------------------------------------------------------
// المتصدرين (Placeholder)
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("المتصدرين 🎉", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("سيتم عرض أعلى المستخدمين إنفاقًا هنا (عند ربط قاعدة بيانات).")
    }
}

// ---------------------------------------------------------
// دخول المالك + لوحة التحكم + استيراد الخدمات
// ---------------------------------------------------------
@Composable
fun AdminLoginScreen(onCancel: () -> Unit, onOk: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onCancel)
        Text("تسجيل دخول المالك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("كلمة المرور") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        GreenButton("دخول") { onOk(pass) }
    }
}

@Composable
fun AdminPanelScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onOpenImport: () -> Unit,
    onCatalogChanged: () -> Unit
) {
    val ctx = LocalContext.current
    val topups = remember { mutableStateListOf<TopupRequest>() }
    val orders = remember { mutableStateListOf<Order>() }

    fun refreshAll() {
        topups.clear(); topups.addAll(repo.loadTopups().sortedByDescending { it.submittedAt })
        orders.clear(); orders.addAll(repo.loadOrders().sortedByDescending { it.createdAt })
    }
    LaunchedEffect(Unit) { refreshAll() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("لوحة تحكم المالك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Text("إجراءات سريعة", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        AdminGrid(
            listOf(
                "استيراد/تعديل الخدمات (نص)",
                "حذف الخدمات المخصّصة",
                "الطلبات المعلقه (الخدمات)",
                "الكارتات المعلقه",
                "طلبات شدات ببجي",
                "طلبات شحن الايتونز",
                "طلبات الارصدة المعلقه",
                "طلبات لودو المعلقه",
                "خصم الرصيد",
                "اضافه رصيد",
                "فحص حالة طلب API",
                "فحص رصيد API",
                "رصيد المستخدمين",
                "عدد المستخدمين",
                "ادارة المشرفين",
                "الغاء حظر المستخدم",
                "حظر المستخدم",
                "اعلان التطبيق",
                "اكواد خدمات API",
                "نظام الاحالة",
                "شرح الخصومات",
                "المتصدرين"
            )
        ) { title ->
            when (title) {
                "استيراد/تعديل الخدمات (نص)" -> onOpenImport()
                "حذف الخدمات المخصّصة" -> {
                    repo.clearCustomCatalog()
                    onCatalogChanged()
                    Toast.makeText(ctx,"تم حذف الخدمات المخصصة والرجوع للافتراضي", Toast.LENGTH_LONG).show()
                }
                "اضافه رصيد" -> quickBalanceDialog(ctx, repo, add = true) { refreshAll() }
                "خصم الرصيد" -> quickBalanceDialog(ctx, repo, add = false) { refreshAll() }
                else -> Toast.makeText(ctx, "$title (قريبًا)", Toast.LENGTH_SHORT).show()
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("الكروت/الشحنات المعلقة", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val pendingTopups = topups.filter { it.status == TopupStatus.PENDING }
        if (pendingTopups.isEmpty()) Text("لا توجد كروت معلقة حالياً.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pendingTopups) { t ->
                var showApprove by remember { mutableStateOf(false) }
                var amount by remember { mutableStateOf("") }

                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "الطريقة: ${when(t.method){
                                "asiacell"->"اسياسيل"; "superkey"->"سوبركي"; "usdt"->"USDT"; else->t.method
                            }}",
                            fontWeight = FontWeight.Bold
                        )
                        Text("المستخدم: ${t.userId.take(8)}…")
                        t.code?.let { Text("الكارت: $it") }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GreenMini("اعتماد + رصيد") { showApprove = true }
                            GreenMini("رفض") {
                                val list = repo.loadTopups().toMutableList()
                                val idx = list.indexOfFirst { it.id==t.id }
                                if (idx>=0) {
                                    val tt = list[idx]; tt.status = TopupStatus.REJECTED
                                    list[idx] = tt; repo.saveTopups(list); refreshAll()
                                    Toast.makeText(ctx,"تم رفض الطلب",Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                if (showApprove) {
                    AlertDialog(
                        onDismissRequest = { showApprove=false },
                        confirmButton = {
                            TextButton(onClick = {
                                val a = amount.toDoubleOrNull()
                                if (a==null || a<=0) {
                                    Toast.makeText(ctx,"أدخل مبلغ صحيح",Toast.LENGTH_SHORT).show()
                                } else {
                                    val list = repo.loadTopups().toMutableList()
                                    val idx = list.indexOfFirst { it.id==t.id }
                                    if (idx>=0) {
                                        val tt = list[idx]
                                        tt.status = TopupStatus.APPROVED
                                        tt.approvedAmount = a
                                        list[idx] = tt
                                        repo.saveTopups(list)
                                        repo.credit(t.userId, a)
                                        showApprove=false; refreshAll()
                                        Toast.makeText(ctx,"تم إضافة $$a للمستخدم",Toast.LENGTH_LONG).show()
                                    }
                                }
                            }) { Text("اعتماد") }
                        },
                        dismissButton = { TextButton(onClick = { showApprove=false }) { Text("إلغاء") } },
                        title = { Text("اعتماد الكارت") },
                        text = {
                            Column {
                                Text("ضع مبلغ الشحن (بالدولار):")
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = { amount = it.filter { ch-> ch.isDigit() || ch=='.' } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// شاشة استيراد الخدمات كنص
@Composable
fun ImportServicesScreen(
    repo: LocalRepo,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<ServiceItem>>(emptyList()) }
    var titles by remember { mutableStateOf(LinkedHashMap<String,String>()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("استيراد/تعديل الخدمات (لصق نص)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "الصيغة: القسم | النص الظاهر (display) | الكمية | السعر\n" +
            "مثال: قسم المتابعين (تيكتوك) | متابعين تيكتوك (1000) - \$3.50 | 1000 | 3.50",
            fontSize = 12.sp, color = Color.Gray
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { txt ->
                text = txt
                try {
                    val (t, items) = CatalogImporter.parse(text)
                    titles = LinkedHashMap(t)
                    preview = items
                    error = if (items.isEmpty()) "لا توجد أسطر صالحة" else null
                } catch (e: Exception) {
                    error = "خطأ في التحليل: ${e.message}"
                    preview = emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text("ألصق كل الخدمات هنا… سطر لكل خدمة") }
        )
        Spacer(Modifier.height(8.dp))
        error?.let { Text(it, color = Color.Red) }

        if (preview.isNotEmpty()) {
            Text("معاينة: ${preview.size} خدمة", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.height(140.dp)) {
                LazyColumn {
                    items(preview.take(6)) { s -> Text("• ${s.categoryTitle} | ${s.display}") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GreenButton("حفظ واستخدام") {
                if (preview.isEmpty()) {
                    Toast.makeText(ctx,"لا توجد بيانات صالحة",Toast.LENGTH_SHORT).show()
                } else {
                    repo.saveCustomCatalog(titles, preview)
                    Toast.makeText(ctx,"تم حفظ الخدمات. سيتم استخدامها الآن.",Toast.LENGTH_LONG).show()
                    onSaved()
                }
            }
        }
    }
}

// ---------------------------------------------------------
// حوار سريع لإضافة/خصم رصيد
// ---------------------------------------------------------
@Composable
private fun quickBalanceDialog(
    ctx: Context,
    repo: LocalRepo,
    add: Boolean,
    onDone: () -> Unit
) {
    var open by remember { mutableStateOf(true) }
    var uid by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    if (!open) return

    AlertDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull()
                if (uid.isBlank() || a==null || a<=0) {
                    Toast.makeText(ctx,"أدخل معرّف مستخدم ومبلغ صحيح",Toast.LENGTH_SHORT).show()
                } else {
                    if (add) repo.credit(uid, a) else {
                        val ok = repo.debit(uid, a)
                        if (!ok) { Toast.makeText(ctx,"رصيد غير كافٍ أو مستخدم غير مطابق",Toast.LENGTH_SHORT).show() }
                    }
                    open = false; onDone()
                    Toast.makeText(ctx, if (add) "تمت إضافة $$a" else "تم الخصم $$a", Toast.LENGTH_SHORT).show()
                }
            }) { Text(if (add) "تنفيذ الإضافة" else "تنفيذ الخصم") }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text("إلغاء") } },
        title = { Text(if (add) "إضافة رصيد" else "خصم رصيد") },
        text = {
            Column {
                Text("أدخل معرّف المستخدم (User ID) والمبلغ بالدولار:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("User ID") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch=='.' } },
                    label = { Text("Amount $") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    )
}

// ---------------------------------------------------------
// عناصر واجهة مشتركة (يحافظ على الألوان والشكل)
// ---------------------------------------------------------
@Composable fun BackButton(onBack: () -> Unit) { TextButton(onClick = onBack) { Text("رجوع") } }

@Composable
fun GreenButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF43A047), Color(0xFF2E7D32))))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
fun GreenMini(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF66BB6A), Color(0xFF43A047))))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun GreenItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF66BB6A), Color(0xFF43A047))))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SmallAdminButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF2E7D32))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) { Text("دخول المالك", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun AdminGrid(titles: List<String>, onClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val rows = titles.chunked(2)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { t -> Box(Modifier.weight(1f)) { GreenItem(t) { onClick(t) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
