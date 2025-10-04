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

// -------------------------
// نماذج البيانات
// -------------------------
data class User(val id: String, val createdAt: Long, var balance: Double)
enum class OrderStatus { PENDING, IN_PROGRESS, DONE, REJECTED }
data class ServiceItem(
    val id: Int,
    val category: String,
    val display: String,   // نص الزر كما في البوت (الاسم + الكمية + السعر)
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
    val method: String,       // "asiacell", "superkey", ...
    val code: String?,        // لاسياسيل
    val submittedAt: Long,
    var status: TopupStatus,
    var approvedAmount: Double?,
    var note: String?
)

// -------------------------
// كاتالوج الخدمات (قابلة للتعديل لاحقًا)
// -------------------------
object Catalog {

    // ترتيب الأقسام لزر "الخدمات"
    val sections: LinkedHashMap<String, String> = linkedMapOf(
        "tiktok_followers" to "قسم المتابعين (تيكتوك)",
        "instagram_followers" to "قسم المتابعين (انستغرام)",
        "tiktok_likes" to "قسم الإعجابات (تيكتوك)",
        "instagram_likes" to "قسم الإعجابات (انستغرام)",
        "tiktok_views" to "قسم المشاهدات (تيكتوك)",
        "instagram_views" to "قسم المشاهدات (انستغرام)",
        "tiktok_live" to "قسم مشاهدات البث (تيكتوك)",
        "instagram_live" to "قسم مشاهدات البث (انستغرام)",
        "telegram_members_channels" to "قسم خدمات التليجرام (قنوات)",
        "telegram_members_groups" to "قسم خدمات التليجرام (كروبات)",
        "ludo" to "قسم خدمات اللودو",
        "pubg" to "قسم شحن شدات ببجي",
        "itunes" to "قسم شراء رصيد ايتونز",
        "bank_score" to "قسم رفع سكور تيكتوك",
        "balance_buy" to "قسم شراء رصيد الهاتف"
    )

    val items: List<ServiceItem> = buildList {
        // === مثال لخدمات (يمكنك توسيعها لاحقاً) ===
        // متابعين تيكتوك
        add(ServiceItem(1001,"tiktok_followers","متابعين تيكتوك (1000) - $3.5",1000,3.5))
        add(ServiceItem(1002,"tiktok_followers","متابعين تيكتوك (2000) - $7.0",2000,7.0))
        add(ServiceItem(1003,"tiktok_followers","متابعين تيكتوك (3000) - $10.5",3000,10.5))
        add(ServiceItem(1004,"tiktok_followers","متابعين تيكتوك (4000) - $14.0",4000,14.0))

        // متابعين انستغرام
        add(ServiceItem(1101,"instagram_followers","متابعين انستغرام (1000) - $3.0",1000,3.0))
        add(ServiceItem(1102,"instagram_followers","متابعين انستغرام (2000) - $6.0",2000,6.0))

        // لايكات تيكتوك
        add(ServiceItem(1201,"tiktok_likes","لايكات تيكتوك (1000) - $1.0",1000,1.0))

        // مشاهدات تيكتوك
        add(ServiceItem(1401,"tiktok_views","مشاهدات تيكتوك (10000) - $0.8",10000,0.8))

        // مشاهدات بث تيكتوك
        add(ServiceItem(1601,"tiktok_live","مشاهدات بث تيكتوك (1000) - $2.0",1000,2.0))

        // تليجرام
        add(ServiceItem(1801,"telegram_members_channels","أعضاء قنوات تيلي 1k - $3.0",1000,3.0))
        add(ServiceItem(1901,"telegram_members_groups","أعضاء كروبات تيلي 1k - $3.0",1000,3.0))

        // لودو
        add(ServiceItem(2001,"ludo","لودو 810 الماسة - $4.0",810,4.0))

        // ببجي
        add(ServiceItem(2101,"pubg","ببجي 60 شدة - $2.0",60,2.0))

        // ايتونز
        add(ServiceItem(2201,"itunes","$9.0 - شراء رصيد 5 ايتونز",5,9.0))

        // رفع سكور
        add(ServiceItem(2301,"bank_score","رفع سكور بنك (1000) - $2.0",1000,2.0))

        // شراء رصيد الشبكات
        add(ServiceItem(2401,"balance_buy","$3.5 - شراء رصيد 2 دولار أثير",2,3.5))
        add(ServiceItem(2411,"balance_buy","$3.5 - شراء رصيد 2 دولار اسيا",2,3.5))
        add(ServiceItem(2421,"balance_buy","$3.5 - شراء رصيد 2 دولار كورك",2,3.5))
    }

    fun byCategory(cat: String) = items.filter { it.category == cat }
}

// -------------------------
// التخزين المحلي (SharedPreferences + JSON)
// -------------------------
class LocalRepo(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("smm_local", Context.MODE_PRIVATE)
    private fun getString(key: String) = prefs.getString(key, null)
    private fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()

    fun getOrCreateUser(): User {
        val raw = getString("user")
        if (raw != null) return userFromJson(JSONObject(raw))
        val u = User(UUID.randomUUID().toString(), System.currentTimeMillis(), 0.0)
        saveUser(u); return u
    }
    fun saveUser(u: User) = putString("user", userToJson(u).toString())

    fun loadOrders(): MutableList<Order> {
        val arr = JSONArray(getString("orders") ?: "[]")
        return MutableList(arr.length()) { i -> orderFromJson(arr.getJSONObject(i)) }
    }
    fun saveOrders(list: List<Order>) {
        val arr = JSONArray(); list.forEach { arr.put(orderToJson(it)) }; putString("orders", arr.toString())
    }

    fun loadTopups(): MutableList<TopupRequest> {
        val arr = JSONArray(getString("topups") ?: "[]")
        return MutableList(arr.length()) { i -> topupFromJson(arr.getJSONObject(i)) }
    }
    fun saveTopups(list: List<TopupRequest>) {
        val arr = JSONArray(); list.forEach { arr.put(topupToJson(it)) }; putString("topups", arr.toString())
    }

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

    // JSON helpers
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
}

// -------------------------
// التنقّل
// -------------------------
sealed class Screen {
    object HOME: Screen()
    object SERVICES: Screen()                          // شاشة عرض أقسام الخدمات
    data class SERVICE_LIST(val cat: String): Screen() // خدمات قسم محدد
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
}

// -------------------------
// النشاط
// -------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppRoot() } }
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val repo = remember { LocalRepo(ctx) }
    val user by remember { mutableStateOf(repo.getOrCreateUser()) }
    var screen by remember { mutableStateOf<Screen>(Screen.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "خدمات راتلوزن",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        SmallAdminButton { screen = Screen.ADMIN_LOGIN } // زر المالك أعلى اليمين
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
                    onBack = { screen = Screen.HOME },
                    onOpenCategory = { key -> screen = Screen.SERVICE_LIST(key) }
                )

                is Screen.SERVICE_LIST -> ServiceListScreen(
                    cat = s.cat,
                    onBack = { screen = Screen.SERVICES },
                    onPick = { item -> screen = Screen.ORDER_CREATE(item) }
                )

                is Screen.ORDER_CREATE -> OrderCreateScreen(
                    repo = repo, userId = user.id, item = s.item,
                    onDone = {
                        if (it) Toast.makeText(ctx,"تم إرسال الطلب وخصم الرصيد",Toast.LENGTH_SHORT).show()
                        screen = Screen.MY_ORDERS
                    },
                    onBack = { screen = Screen.SERVICE_LIST(s.item.category) }
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
                    onBack = { screen = Screen.HOME }
                )
            }
        }
    }
}

// -------------------------
// واجهة المستخدم الرئيسية (ترتيب الأزرار القديم)
// -------------------------
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
        // بطاقة الرصيد أعلى الصفحة
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "رصيدك الحالي:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("$${"%.2f".format(user.balance)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // شبكة الأزرار الرئيسية (نفس الترتيب القديم)
        Text("القائمة الرئيسية", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // الصف 1
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { GreenItem("الخدمات") { onOpenServices() } }
                Box(modifier = Modifier.weight(1f)) { GreenItem("طلباتي") { onOrders() } }
            }
            // الصف 2
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { GreenItem("رصيدي") { onBalance() } }
                Box(modifier = Modifier.weight(1f)) { GreenItem("الإحالة") { onReferral() } }
            }
            // الصف 3
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { GreenItem("المتصدرين 🎉") { onLeaderboard() } }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// -------------------------
// شاشة أقسام "الخدمات" (ترتيب ورجوع كما كان سابقًا)
// -------------------------
@Composable
fun ServicesCategoriesScreen(
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("الخدمات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Catalog.sections.entries.toList()) { (key, title) ->
                GreenItem(title) { onOpenCategory(key) }
            }
        }
    }
}

// -------------------------
// قائمة خدمات القسم المحدد
// -------------------------
@Composable
fun ServiceListScreen(
    cat: String,
    onBack: () -> Unit,
    onPick: (ServiceItem) -> Unit
) {
    val title = Catalog.sections[cat] ?: "خدمات"
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Catalog.byCategory(cat)) { item ->
                GreenItem(item.display) { onPick(item) }
            }
        }
    }
}

// -------------------------
// إنشاء الطلب
// -------------------------
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

// -------------------------
// رصيدي + طرق الشحن
// -------------------------
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

// -------------------------
// طلباتي
// -------------------------
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

// -------------------------
// الإحالة (عرض بسيط)
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

// -------------------------
// المتصدرين (محلي مبسّط)
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("المتصدرين 🎉", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("سيتم عرض أعلى المستخدمين إنفاقًا هنا (عند ربط قاعدة بيانات).")
    }
}

// -------------------------
// دخول المالك + لوحة التحكم (ترتيب الأزرار كما طلبت)
// -------------------------
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
fun AdminPanelScreen(repo: LocalRepo, onBack: () -> Unit) {
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

        // شبكة الأزرار (نفس الأسماء التي طلبتها)
        Text("إجراءات سريعة", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        AdminGrid(
            listOf(
                "تعديل الاسعار والكميات",
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
                "الكارتات المعلقه" -> Unit // القسم التفصيلي أدناه
                "الطلبات المعلقه (الخدمات)" -> Unit // سنعرض الطلبات أدناه
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

        Spacer(Modifier.height(18.dp))
        Text("الطلبات (قيد المراجعة/التنفيذ)", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val reviewOrders = orders.filter { it.status==OrderStatus.PENDING || it.status==OrderStatus.IN_PROGRESS }
        if (reviewOrders.isEmpty()) Text("لا توجد طلبات.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reviewOrders) { o ->
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text(o.serviceDisplay, fontWeight = FontWeight.Bold)
                        Text("السعر: $${o.price} | الحالة: ${o.status}")
                        if (o.input.isNotBlank()) Text("بيانات: ${o.input}", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GreenMini("بدء تنفيذ") {
                                val list = repo.loadOrders().toMutableList()
                                val idx = list.indexOfFirst { it.id==o.id }
                                if (idx>=0) { list[idx] = list[idx].copy(status = OrderStatus.IN_PROGRESS); repo.saveOrders(list); refreshAll() }
                            }
                            GreenMini("اكتمال") {
                                val list = repo.loadOrders().toMutableList()
                                val idx = list.indexOfFirst { it.id==o.id }
                                if (idx>=0) { list[idx] = list[idx].copy(status = OrderStatus.DONE); repo.saveOrders(list); refreshAll() }
                            }
                            GreenMini("رفض + استرجاع") {
                                val list = repo.loadOrders().toMutableList()
                                val idx = list.indexOfFirst { it.id==o.id }
                                if (idx>=0) {
                                    val cur = list[idx]; list[idx] = cur.copy(status = OrderStatus.REJECTED)
                                    repo.saveOrders(list); repo.credit(cur.userId, cur.price); refreshAll()
                                    Toast.makeText(ctx,"تم الرفض واسترجاع $${cur.price}",Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// حوار سريع لإضافة/خصم رصيد
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

// -------------------------
// عناصر واجهة مشتركة (نفس الألوان/الأشكال الحالية)
// -------------------------
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
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
