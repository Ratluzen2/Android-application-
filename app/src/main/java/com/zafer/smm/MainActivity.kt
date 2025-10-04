@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.zafer.smm

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
    val display: String,   // نص الزر كاملًا كما في البوت (الاسم+الكمية+السعر)
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
// كاتالوج الخدمات (مطابق للصور حرفيًا)
// -------------------------
object Catalog {

    // ترتيب الأقسام كما بالبوت
    val sections: LinkedHashMap<String, String> = linkedMapOf(
        "tiktok_followers" to "متابعين تيكتوك",
        "instagram_followers" to "متابعين انستغرام",
        "tiktok_likes" to "لايكات تيكتوك",
        "instagram_likes" to "لايكات انستغرام",
        "tiktok_views" to "مشاهدات تيكتوك",
        "instagram_views" to "مشاهدات انستغرام",
        "tiktok_live" to "مشاهدات بث تيكتوك",
        "instagram_live" to "مشاهدات بث انستغرام",
        "telegram_members_channels" to "اعضاء قنوات تيلي",
        "telegram_members_groups" to "اعضاء كروبات تيلي",
        "ludo" to "خدمات لودو",
        "pubg" to "شحن شدات ببجي",
        "itunes" to "شراء رصيد ايتونز",
        "bank_score" to "رفع سكور بنك تيكتوك",
        "balance_buy" to "شراء رصيد (أثير/اسيا/كورك)"
    )

    val items: List<ServiceItem> = buildList {

        // متابعين تيكتوك
        add(ServiceItem(1001,"tiktok_followers","متابعين تيكتوك (1000) - $3.5",1000,3.5))
        add(ServiceItem(1002,"tiktok_followers","متابعين تيكتوك (2000) - $7.0",2000,7.0))
        add(ServiceItem(1003,"tiktok_followers","متابعين تيكتوك (3000) - $10.5",3000,10.5))
        add(ServiceItem(1004,"tiktok_followers","متابعين تيكتوك (4000) - $14.0",4000,14.0))

        // متابعين انستغرام
        add(ServiceItem(1101,"instagram_followers","متابعين انستغرام (1000) - $3.0",1000,3.0))
        add(ServiceItem(1102,"instagram_followers","متابعين انستغرام (2000) - $6.0",2000,6.0))
        add(ServiceItem(1103,"instagram_followers","متابعين انستغرام (3000) - $9.0",3000,9.0))
        add(ServiceItem(1104,"instagram_followers","متابعين انستغرام (4000) - $12.0",4000,12.0))

        // لايكات تيكتوك
        add(ServiceItem(1201,"tiktok_likes","لايكات تيكتوك (1000) - $1.0",1000,1.0))
        add(ServiceItem(1202,"tiktok_likes","لايكات تيكتوك (2000) - $2.0",2000,2.0))
        add(ServiceItem(1203,"tiktok_likes","لايكات تيكتوك (3000) - $3.0",3000,3.0))
        add(ServiceItem(1204,"tiktok_likes","لايكات تيكتوك (4000) - $4.0",4000,4.0))

        // لايكات انستغرام
        add(ServiceItem(1301,"instagram_likes","لايكات انستغرام (1000) - $1.0",1000,1.0))
        add(ServiceItem(1302,"instagram_likes","لايكات انستغرام (2000) - $2.0",2000,2.0))
        add(ServiceItem(1303,"instagram_likes","لايكات انستغرام (3000) - $3.0",3000,3.0))
        add(ServiceItem(1304,"instagram_likes","لايكات انستغرام (4000) - $4.0",4000,4.0))

        // مشاهدات تيكتوك
        add(ServiceItem(1401,"tiktok_views","مشاهدات تيكتوك (1000) - $0.1",1000,0.1))
        add(ServiceItem(1402,"tiktok_views","مشاهدات تيكتوك (10000) - $0.8",10000,0.8))
        add(ServiceItem(1403,"tiktok_views","مشاهدات تيكتوك (20000) - $1.6",20000,1.6))
        add(ServiceItem(1404,"tiktok_views","مشاهدات تيكتوك (30000) - $2.4",30000,2.4))
        add(ServiceItem(1405,"tiktok_views","مشاهدات تيكتوك (50000) - $3.2",50000,3.2))

        // مشاهدات انستغرام
        add(ServiceItem(1501,"instagram_views","مشاهدات انستغرام (10000) - $0.8",10000,0.8))
        add(ServiceItem(1502,"instagram_views","مشاهدات انستغرام (20000) - $1.6",20000,1.6))
        add(ServiceItem(1503,"instagram_views","مشاهدات انستغرام (30000) - $2.4",30000,2.4))
        add(ServiceItem(1504,"instagram_views","مشاهدات انستغرام (50000) - $3.2",50000,3.2))

        // مشاهدات بث تيكتوك
        add(ServiceItem(1601,"tiktok_live","مشاهدات بث تيكتوك (1000) - $2.0",1000,2.0))
        add(ServiceItem(1602,"tiktok_live","مشاهدات بث تيكتوك (2000) - $4.0",2000,4.0))
        add(ServiceItem(1603,"tiktok_live","مشاهدات بث تيكتوك (3000) - $6.0",3000,6.0))
        add(ServiceItem(1604,"tiktok_live","مشاهدات بث تيكتوك (4000) - $8.0",4000,8.0))

        // مشاهدات بث انستغرام
        add(ServiceItem(1701,"instagram_live","مشاهدات بث انستغرام (1000) - $2.0",1000,2.0))
        add(ServiceItem(1702,"instagram_live","مشاهدات بث انستغرام (2000) - $4.0",2000,4.0))
        add(ServiceItem(1703,"instagram_live","مشاهدات بث انستغرام (3000) - $6.0",3000,6.0))
        add(ServiceItem(1704,"instagram_live","مشاهدات بث انستغرام (4000) - $8.0",4000,8.0))

        // اعضاء قنوات تيلي
        add(ServiceItem(1801,"telegram_members_channels","اعضاء قنوات تيلي 1k - 3.0$",1000,3.0))
        add(ServiceItem(1802,"telegram_members_channels","اعضاء قنوات تيلي 2k - 6.0$",2000,6.0))
        add(ServiceItem(1803,"telegram_members_channels","اعضاء قنوات تيلي 3k - 9.0$",3000,9.0))
        add(ServiceItem(1804,"telegram_members_channels","اعضاء قنوات تيلي 4k - 12.0$",4000,12.0))
        add(ServiceItem(1805,"telegram_members_channels","اعضاء قنوات تيلي 5k - 15.0$",5000,15.0))

        // اعضاء كروبات تيلي
        add(ServiceItem(1901,"telegram_members_groups","اعضاء كروبات تيلي 1k - 3.0$",1000,3.0))
        add(ServiceItem(1902,"telegram_members_groups","اعضاء كروبات تيلي 2k - 6.0$",2000,6.0))
        add(ServiceItem(1903,"telegram_members_groups","اعضاء كروبات تيلي 3k - 9.0$",3000,9.0))
        add(ServiceItem(1904,"telegram_members_groups","اعضاء كروبات تيلي 4k - 12.0$",4000,12.0))
        add(ServiceItem(1905,"telegram_members_groups","اعضاء كروبات تيلي 5k - 15.0$",5000,15.0))

        // لودو ألماس وذهب
        add(ServiceItem(2001,"ludo","لودو 810 الماسة - $4.0",810,4.0))
        add(ServiceItem(2002,"ludo","لودو 2280 الماسة - $8.9",2280,8.9))
        add(ServiceItem(2003,"ludo","لودو 5080 الماسة - $17.5",5080,17.5))
        add(ServiceItem(2004,"ludo","لودو 12750 الماسة - $42.7",12750,42.7))
        add(ServiceItem(2005,"ludo","لودو ذهب 66680 - $4.0",66680,4.0))
        add(ServiceItem(2006,"ludo","لودو ذهب 219500 - $8.9",219500,8.9))
        add(ServiceItem(2007,"ludo","لودو ذهب 1443000 - $17.5",1443000,17.5))
        add(ServiceItem(2008,"ludo","لودو ذهب 3627000 - $42.7",3627000,42.7))

        // ببجي
        add(ServiceItem(2101,"pubg","ببجي 60 شدة - $2.0",60,2.0))
        add(ServiceItem(2102,"pubg","ببجي 120 شدة - $4.0",120,4.0))
        add(ServiceItem(2103,"pubg","ببجي 180 شدة - $6.0",180,6.0))
        add(ServiceItem(2104,"pubg","ببجي 240 شدة - $8.0",240,8.0))
        add(ServiceItem(2105,"pubg","ببجي 325 شدة - $9.0",325,9.0))
        add(ServiceItem(2106,"pubg","ببجي 660 شدة - $15.0",660,15.0))
        add(ServiceItem(2107,"pubg","ببجي 1800 شدة - $40.0",1800,40.0))

        // آيتونز (السعر أولاً كما بالصورة)
        add(ServiceItem(2201,"itunes","$9.0 - شراء رصيد 5 ايتونز",5,9.0))
        add(ServiceItem(2202,"itunes","$18.0 - شراء رصيد 10 ايتونز",10,18.0))
        add(ServiceItem(2203,"itunes","$27.0 - شراء رصيد 15 ايتونز",15,27.0))
        add(ServiceItem(2204,"itunes","$36.0 - شراء رصيد 20 ايتونز",20,36.0))
        add(ServiceItem(2205,"itunes","$45.0 - شراء رصيد 25 ايتونز",25,45.0))
        add(ServiceItem(2206,"itunes","$54.0 - شراء رصيد 30 ايتونز",30,54.0))
        add(ServiceItem(2207,"itunes","$63.0 - شراء رصيد 35 ايتونز",35,63.0))
        add(ServiceItem(2208,"itunes","$72.0 - شراء رصيد 40 ايتونز",40,72.0))
        add(ServiceItem(2209,"itunes","$81.0 - شراء رصيد 45 ايتونز",45,81.0))
        add(ServiceItem(2210,"itunes","$90.0 - شراء رصيد 50 ايتونز",50,90.0))

        // رفع سكور بنك تيكتوك
        add(ServiceItem(2301,"bank_score","رفع سكور بنك (1000) - $2.0",1000,2.0))
        add(ServiceItem(2302,"bank_score","رفع سكور بنك (2000) - $4.0",2000,4.0))
        add(ServiceItem(2303,"bank_score","رفع سكور بنك (3000) - $6.0",3000,6.0))
        add(ServiceItem(2304,"bank_score","رفع سكور بنك (10000) - $20.0",10000,20.0))

        // شراء رصيد الشبكات (أثير/اسيا/كورك) — السعر أولاً كما بالصورة
        add(ServiceItem(2401,"balance_buy","$3.5 - شراء رصيد 2 دولار أثير",2,3.5))
        add(ServiceItem(2402,"balance_buy","$7.0 - شراء رصيد 5 دولار أثير",5,7.0))
        add(ServiceItem(2403,"balance_buy","$13.0 - شراء رصيد 10 دولار أثير",10,13.0))
        add(ServiceItem(2404,"balance_buy","$19.0 - شراء رصيد 15 دولار أثير",15,19.0))
        add(ServiceItem(2405,"balance_buy","$52.0 - شراء رصيد 40 دولار أثير",40,52.0))

        add(ServiceItem(2411,"balance_buy","$3.5 - شراء رصيد 2 دولار اسيا",2,3.5))
        add(ServiceItem(2412,"balance_buy","$7.0 - شراء رصيد 5 دولار اسيا",5,7.0))
        add(ServiceItem(2413,"balance_buy","$13.0 - شراء رصيد 10 دولار اسيا",10,13.0))
        add(ServiceItem(2414,"balance_buy","$19.0 - شراء رصيد 15 دولار اسيا",15,19.0))
        add(ServiceItem(2415,"balance_buy","$52.0 - شراء رصيد 40 دولار اسيا",40,52.0))

        add(ServiceItem(2421,"balance_buy","$3.5 - شراء رصيد 2 دولار كورك",2,3.5))
        add(ServiceItem(2422,"balance_buy","$7.0 - شراء رصيد 5 دولار كورك",5,7.0))
        add(ServiceItem(2423,"balance_buy","$13.0 - شراء رصيد 10 دولار كورك",10,13.0))
        add(ServiceItem(2424,"balance_buy","$19.0 - شراء رصيد 15 دولار كورك",15,19.0))
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
    data class SERVICE_LIST(val cat: String): Screen()
    data class ORDER_CREATE(val item: ServiceItem): Screen()
    object BALANCE: Screen()
    object TOPUP_METHODS: Screen()
    object TOPUP_ASIACELL: Screen()
    data class TOPUP_SUPPORT(val method: String): Screen()
    object MY_ORDERS: Screen()
    object ADMIN_LOGIN: Screen()
    object ADMIN_PANEL: Screen()
}

// -------------------------
// النشاط
// -------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) { AppRoot() }
        }
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
                    Text(
                        "خدمات راتلوزن",
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {}, onLongClick = { screen = Screen.ADMIN_LOGIN }),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (val s = screen) {
                Screen.HOME -> HomeScreen(
                    user = user,
                    onSection = { screen = Screen.SERVICE_LIST(it) },
                    onBalance = { screen = Screen.BALANCE },
                    onOrders = { screen = Screen.MY_ORDERS }
                )
                is Screen.SERVICE_LIST -> ServiceListScreen(
                    cat = s.cat,
                    onBack = { screen = Screen.HOME },
                    onPick = { screen = Screen.ORDER_CREATE(it) }
                )
                is Screen.ORDER_CREATE -> OrderCreateScreen(
                    repo = repo, userId = user.id, item = s.item,
                    onDone = { ok ->
                        if (ok) Toast.makeText(LocalContext.current,"تم إرسال الطلب وخصم الرصيد",Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(LocalContext.current,"تم استلام طلبك، سوف يتم شحن حسابك قريبًا.",Toast.LENGTH_LONG).show()
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
                Screen.ADMIN_LOGIN -> AdminLoginScreen(
                    onCancel = { screen = Screen.HOME },
                    onOk = { pass ->
                        if (pass == "ratluzen") screen = Screen.ADMIN_PANEL
                        else Toast.makeText(LocalContext.current,"كلمة مرور خاطئة",Toast.LENGTH_SHORT).show()
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
// الواجهات
// -------------------------
@Composable
fun HomeScreen(
    user: User,
    onSection: (String) -> Unit,
    onBalance: () -> Unit,
    onOrders: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(scroll)
    ) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))) {
            Column(Modifier.padding(14.dp)) {
                Text("رصيدك الحالي:", fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Text("$${"%.2f".format(user.balance)}", fontSize = 22.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GreenButton("رصيدي / شحن") { onBalance() }
                    GreenButton("طلباتي") { onOrders() }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("الأقسام", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Catalog.sections.forEach { (key, title) ->
            GreenItem(title) { onSection(key) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

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

@Composable
fun BalanceScreen(
    repo: LocalRepo,
    userId: String,
    onBack: () -> Unit,
    onTopup: () -> Unit
) {
    val user by remember { mutableStateOf(repo.getOrCreateUser()) }
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
                clip.setText(androidx.compose.ui.text.AnnotatedString(phone))
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
            onSubmitted()
        }
    }
}

@Composable
fun MyOrdersScreen(repo: LocalRepo, userId: String, onBack: () -> Unit) {
    val orders = remember { mutableStateListOf<Order>().apply {
        addAll(repo.loadOrders().filter { it.userId==userId }.sortedByDescending { it.createdAt })
    } }
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
    val topups = remember { mutableStateListOf<TopupRequest>().apply { addAll(repo.loadTopups().sortedByDescending { it.submittedAt }) } }
    val orders = remember { mutableStateListOf<Order>().apply { addAll(repo.loadOrders().sortedByDescending { it.createdAt }) } }
    fun refreshAll() {
        topups.clear(); topups.addAll(repo.loadTopups().sortedByDescending { it.submittedAt })
        orders.clear(); orders.addAll(repo.loadOrders().sortedByDescending { it.createdAt })
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(onBack)
        Text("لوحة تحكم المالك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Text("الكروت/الشحنات المعلقة", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (topups.none { it.status == TopupStatus.PENDING }) {
            Text("لا توجد كروت معلقة حالياً.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topups.filter { it.status==TopupStatus.PENDING }) { t ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("الطريقة: ${when(t.method){
                                "asiacell"->"اسياسيل"; "superkey"->"سوبركي"; "usdt"->"USDT"; else->t.method
                            }}", fontWeight = FontWeight.Bold)
                            Text("المستخدم: ${t.userId.take(8)}…")
                            t.code?.let { Text("الكارت: $it") }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                var show by remember { mutableStateOf(false) }
                                var amount by remember { mutableStateOf("") }
                                if (show) {
                                    AlertDialog(
                                        onDismissRequest = { show=false },
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
                                                        show=false; refreshAll()
                                                        Toast.makeText(ctx,"تم إضافة $$a للمستخدم",Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }) { Text("اعتماد") }
                                        },
                                        dismissButton = { TextButton(onClick = { show=false }) { Text("إلغاء") } },
                                        title = { Text("اعتماد الكارت") },
                                        text = {
                                            Column {
                                                Text("ضع مبلغ الشحن (بالدولار):")
                                                OutlinedTextField(
                                                    value = amount, onValueChange = { amount = it.filter { ch-> ch.isDigit() || ch=='.' } },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                            }
                                        }
                                    )
                                }
                                GreenMini("اعتماد + رصيد") { show = true }
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
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("الطلبات", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (orders.isEmpty()) Text("لا توجد طلبات.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders) { o ->
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
                                    Toast.makeText(LocalContext.current,"تم الرفض واسترجاع $${cur.price}",Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------
// عناصر واجهة مشتركة
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
