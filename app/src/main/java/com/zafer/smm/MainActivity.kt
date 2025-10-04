package com.zafer.smm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ------------------------------
// نماذج البيانات
// ------------------------------
data class Service(val id: Int, val name: String, val price: Double)
data class Section(val key: String, val title: String, val services: List<Service>)

// نموذج كارت مُعلق
data class PendingCard(val id: String, val userId: String, val cardNumber: String, val createdAt: Long)

// كتالوج الخدمات (مطابق للصور)
object Catalog {
    val sections: List<Section> = listOf(
        Section(
            key = "followers", title = "قسم المتابعين",
            services = listOf(
                Service(1, "متابعين تيكتوك (1000)", 3.5),
                Service(2, "متابعين تيكتوك (2000)", 7.0),
                Service(3, "متابعين تيكتوك (3000)", 10.5),
                Service(4, "متابعين تيكتوك (4000)", 14.0),
                Service(5, "متابعين انستغرام (1000)", 3.0),
                Service(6, "متابعين انستغرام (2000)", 6.0),
                Service(7, "متابعين انستغرام (3000)", 9.0),
                Service(8, "متابعين انستغرام (4000)", 12.0),
            )
        ),
        Section(
            key = "likes", title = "قسم الإعجابات",
            services = listOf(
                Service(1, "لايكات تيكتوك (1000)", 1.0),
                Service(2, "لايكات تيكتوك (2000)", 2.0),
                Service(3, "لايكات تيكتوك (3000)", 3.0),
                Service(4, "لايكات تيكتوك (4000)", 4.0),
                Service(5, "لايكات انستغرام (1000)", 1.0),
                Service(6, "لايكات انستغرام (2000)", 2.0),
                Service(7, "لايكات انستغرام (3000)", 3.0),
                Service(8, "لايكات انستغرام (4000)", 4.0),
            )
        ),
        Section(
            key = "views", title = "قسم المشاهدات",
            services = listOf(
                Service(1, "مشاهدات تيكتوك (1000)", 0.1),
                Service(2, "مشاهدات تيكتوك (10000)", 0.8),
                Service(3, "مشاهدات تيكتوك (20000)", 1.6),
                Service(4, "مشاهدات تيكتوك (30000)", 2.4),
                Service(5, "مشاهدات تيكتوك (50000)", 3.2),
                Service(6, "مشاهدات انستغرام (10000)", 0.8),
                Service(7, "مشاهدات انستغرام (20000)", 1.6),
                Service(8, "مشاهدات انستغرام (30000)", 2.4),
                Service(9, "مشاهدات انستغرام (50000)", 3.2),
            )
        ),
        Section(
            key = "live", title = "قسم مشاهدات البث المباشر",
            services = listOf(
                Service(1, "مشاهدات بث تيكتوك (1000)", 2.0),
                Service(2, "مشاهدات بث تيكتوك (2000)", 4.0),
                Service(3, "مشاهدات بث تيكتوك (3000)", 6.0),
                Service(4, "مشاهدات بث تيكتوك (4000)", 8.0),
                Service(5, "مشاهدات بث انستغرام (1000)", 2.0),
                Service(6, "مشاهدات بث انستغرام (2000)", 4.0),
                Service(7, "مشاهدات بث انستغرام (3000)", 6.0),
                Service(8, "مشاهدات بث انستغرام (4000)", 8.0),
            )
        ),
        Section(
            key = "pubg", title = "قسم شحن شدات ببجي",
            services = listOf(
                Service(1, "ببجي 60 شدة", 2.0),
                Service(2, "ببجي 120 شدة", 4.0),
                Service(3, "ببجي 180 شدة", 6.0),
                Service(4, "ببجي 240 شدة", 8.0),
                Service(5, "ببجي 325 شدة", 9.0),
                Service(6, "ببجي 660 شدة", 15.0),
                Service(7, "ببجي 1800 شدة", 40.0),
            )
        ),
        Section(
            key = "score", title = "قسم رفع سكور تيكتوك",
            services = listOf(
                Service(1, "رفع سكور بنك (1000)", 2.0),
                Service(2, "رفع سكور بنك (2000)", 4.0),
                Service(3, "رفع سكور بنك (3000)", 6.0),
                Service(4, "رفع سكور بنك (10000)", 20.0),
            )
        ),
        Section(
            key = "itunes", title = "قسم شراء رصيد ايتونز",
            services = listOf(
                Service(1, "شراء رصيد 5 ايتونز", 9.0),
                Service(2, "شراء رصيد 10 ايتونز", 18.0),
                Service(3, "شراء رصيد 15 ايتونز", 27.0),
                Service(4, "شراء رصيد 20 ايتونز", 36.0),
                Service(5, "شراء رصيد 25 ايتونز", 45.0),
                Service(6, "شراء رصيد 30 ايتونز", 54.0),
                Service(7, "شراء رصيد 35 ايتونز", 63.0),
                Service(8, "شراء رصيد 40 ايتونز", 72.0),
                Service(9, "شراء رصيد 45 ايتونز", 81.0),
                Service(10, "شراء رصيد 50 ايتونز", 90.0),
            )
        ),
        Section(
            key = "telegram", title = "قسم خدمات التليجرام",
            services = listOf(
                Service(1, "أعضاء قنوات تيلي (1k)", 3.0),
                Service(2, "أعضاء قنوات تيلي (2k)", 6.0),
                Service(3, "أعضاء قنوات تيلي (3k)", 9.0),
                Service(4, "أعضاء قنوات تيلي (4k)", 12.0),
                Service(5, "أعضاء قنوات تيلي (5k)", 15.0),
                Service(6, "أعضاء كروبات تيلي (1k)", 3.0),
                Service(7, "أعضاء كروبات تيلي (2k)", 6.0),
                Service(8, "أعضاء كروبات تيلي (3k)", 9.0),
                Service(9, "أعضاء كروبات تيلي (4k)", 12.0),
                Service(10, "أعضاء كروبات تيلي (5k)", 15.0),
            )
        ),
        Section(
            key = "ludo", title = "قسم خدمات اللودو",
            services = listOf(
                Service(1, "لودو 810 الماسة", 4.0),
                Service(2, "لودو 2280 الماسة", 8.9),
                Service(3, "لودو 5080 الماسة", 17.5),
                Service(4, "لودو 12750 الماسة", 42.7),
                Service(5, "لودو 66680 ذهب", 4.0),
                Service(6, "لودو 219500 ذهب", 8.9),
                Service(7, "لودو 1443000 ذهب", 17.5),
                Service(8, "لودو 3627000 ذهب", 42.7),
            )
        ),
        Section(
            key = "mobile_recharge", title = "قسم شراء رصيد الهاتف",
            services = listOf(
                Service(1, "شراء رصيد 2 دولار أثير", 3.5),
                Service(2, "شراء رصيد 5 دولار أثير", 7.0),
                Service(3, "شراء رصيد 10 دولار أثير", 13.0),
                Service(4, "شراء رصيد 15 دولار أثير", 19.0),
                Service(5, "شراء رصيد 40 دولار أثير", 52.0),
                Service(6, "شراء رصيد 2 دولار اسيا", 3.5),
                Service(7, "شراء رصيد 5 دولار اسيا", 7.0),
                Service(8, "شراء رصيد 10 دولار اسيا", 13.0),
                Service(9, "شراء رصيد 15 دولار اسيا", 19.0),
                Service(10, "شراء رصيد 40 دولار اسيا", 52.0),
                Service(11, "شراء رصيد 2 دولار كورك", 3.5),
                Service(12, "شراء رصيد 5 دولار كورك", 7.0),
                Service(13, "شراء رصيد 10 دولار كورك", 13.0),
                Service(14, "شراء رصيد 15 دولار كورك", 19.0),
            )
        ),
    )
}

// ------------------------------
// تخزين محلي وأدوات
// ------------------------------
private fun Context.prefs(): SharedPreferences =
    getSharedPreferences("smm_prefs", Context.MODE_PRIVATE)

private const val KEY_ADMIN_LOGGED = "admin_logged"
private const val KEY_USER_ID = "user_id"
private fun balanceKey(userId: String) = "balance_$userId"

private fun SharedPreferences.getUserBalance(userId: String): Double {
    val key = balanceKey(userId)
    return java.lang.Double.longBitsToDouble(getLong(key, 0.0.toBits()))
}

private fun SharedPreferences.setUserBalance(userId: String, value: Double) {
    edit().putLong(balanceKey(userId), java.lang.Double.doubleToRawLongBits(value)).apply()
}

private const val KEY_PENDING_CARDS = "pending_cards" // صيغة تخزين: id^userId^card^ts|...

private fun SharedPreferences.loadPendingCards(): MutableList<PendingCard> {
    val raw = getString(KEY_PENDING_CARDS, "") ?: ""
    if (raw.isEmpty()) return mutableListOf()
    return raw.split("|").filter { it.isNotBlank() }.mapNotNull { row ->
        val p = row.split("^")
        if (p.size == 4) PendingCard(p[0], p[1], p[2], p[3].toLongOrNull() ?: 0L) else null
    }.toMutableList()
}

private fun SharedPreferences.savePendingCards(list: List<PendingCard>) {
    val raw = list.joinToString("|") { "${it.id}^${it.userId}^${it.cardNumber}^${it.createdAt}" }
    edit().putString(KEY_PENDING_CARDS, raw).apply()
}

private fun ensureAnonymousUserId(ctx: Context): String {
    val p = ctx.prefs()
    val exist = p.getString(KEY_USER_ID, null)
    if (exist != null) return exist
    val id = UUID.randomUUID().toString()
    p.edit().putString(KEY_USER_ID, id).apply()
    return id
}

// ------------------------------
// التنقل
// ------------------------------
sealed class Screen {
    data object HOME : Screen()
    data object SERVICES : Screen()
    data class ServiceList(val section: Section) : Screen()
    data object ORDERS : Screen()
    data object BALANCE : Screen()
    data object REFERRAL : Screen()
    data object LEADERBOARD : Screen()
    data object ADMIN_LOGIN : Screen()
    data object ADMIN_DASHBOARD : Screen()
    data object PENDING_CARDS : Screen()
}

// ------------------------------
// النشاط الرئيسي
// ------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureAnonymousUserId(this)

        setContent {
            MaterialTheme {
                val ctx = LocalContext.current
                val prefs = remember { ctx.prefs() }
                val adminLogged = remember { mutableStateOf(prefs.getBoolean(KEY_ADMIN_LOGGED, false)) }
                var current by remember { mutableStateOf<Screen>(Screen.HOME) }

                Surface(Modifier.fillMaxSize()) {
                    when (current) {
                        Screen.HOME -> HomeScreen(
                            onServices = { current = Screen.SERVICES },
                            onOrders = { current = Screen.ORDERS },
                            onBalance = { current = Screen.BALANCE },
                            onReferral = { current = Screen.REFERRAL },
                            onLeaders = { current = Screen.LEADERBOARD },
                            onAdmin = { current = if (adminLogged.value) Screen.ADMIN_DASHBOARD else Screen.ADMIN_LOGIN }
                        )

                        Screen.SERVICES -> ServicesScreen(
                            onBack = { current = Screen.HOME },
                            onOpenSection = { section -> current = Screen.ServiceList(section) }
                        )

                        is Screen.ServiceList -> ServiceListScreen(
                            section = (current as Screen.ServiceList).section,
                            onBack = { current = Screen.SERVICES }
                        )

                        Screen.ORDERS -> PlaceholderScreen("طلباتي") { current = Screen.HOME }
                        Screen.REFERRAL -> PlaceholderScreen("نظام الإحالة") { current = Screen.HOME }
                        Screen.LEADERBOARD -> PlaceholderScreen("المتصدرون") { current = Screen.HOME }

                        Screen.BALANCE -> BalanceScreen(
                            onBack = { current = Screen.HOME }
                        )

                        Screen.ADMIN_LOGIN -> AdminLoginScreen(
                            onCancel = { current = Screen.HOME },
                            onSuccess = {
                                adminLogged.value = true
                                prefs.edit().putBoolean(KEY_ADMIN_LOGGED, true).apply()
                                current = Screen.ADMIN_DASHBOARD
                            }
                        )

                        Screen.ADMIN_DASHBOARD -> AdminDashboardScreen(
                            onBack = { current = Screen.HOME },
                            onOpenPendingCards = { current = Screen.PENDING_CARDS },
                            onLogout = {
                                adminLogged.value = false
                                prefs.edit().putBoolean(KEY_ADMIN_LOGGED, false).apply()
                                current = Screen.HOME
                            }
                        )

                        Screen.PENDING_CARDS -> PendingCardsScreen(
                            onBack = { current = Screen.ADMIN_DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------
// الواجهة الرئيسية
// ------------------------------
@Composable
fun HomeScreen(
    onServices: () -> Unit,
    onOrders: () -> Unit,
    onBalance: () -> Unit,
    onReferral: () -> Unit,
    onLeaders: () -> Unit,
    onAdmin: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "خدمات راتلوزن",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            GridButtons(
                listOf(
                    "الخدمات" to onServices,
                    "طلباتي" to onOrders,
                    "رصيدي" to onBalance,
                    "الإحالة" to onReferral,
                    "المتصدرين" to onLeaders
                )
            )
        }

        Text(
            text = "دخول المالك",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clickable { onAdmin() }
        )
    }
}

@Composable
fun GridButtons(items: List<Pair<String, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (title, action) ->
                    ActionCard(title = title, modifier = Modifier.weight(1f), onClick = action)
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActionCard(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Box(Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ------------------------------
// الأقسام
// ------------------------------
@Composable
fun ServicesScreen(onBack: () -> Unit, onOpenSection: (Section) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(8.dp))
            Text("الخدمات", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        GridButtons(Catalog.sections.map { it.title to { onOpenSection(it) } })
    }
}

@Composable
fun ServiceListScreen(section: Section, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(8.dp))
            Text(section.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(section.services) { s ->
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("السعر: $${"%.2f".format(s.price)}", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = {
                                scope.launch {
                                    Toast.makeText(ctx, "طلب الخدمة: ${s.name}", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("طلب الخدمة") }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------
// شاشة الرصيد + أزرار الشحن
// ------------------------------
@Composable
fun BalanceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.prefs() }
    val userId = remember { prefs.getString(KEY_USER_ID, "") ?: "" }
    var balance by remember { mutableStateOf(prefs.getUserBalance(userId)) }

    fun saveBalance(v: Double) {
        balance = v
        prefs.setUserBalance(userId, v)
    }

    // Dialogات
    var whatsappDialog by remember { mutableStateOf<String?>(null) } // message body
    var asiaDialog by remember { mutableStateOf(false) }
    var asiaCard by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(8.dp))
            Text("رصيدي", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("رصيدك الحالي:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                Spacer(Modifier.height(6.dp))
                Text("$${"%.2f".format(balance)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // اسياسيل: إدخال كارت
            ActionCard("شحن عبر اسياسيل") { asiaDialog = true }

            // البقية: رسالة واتساب + زر نسخ
            val msg = "لإكمال طلبك تواصل مع الدعم الفني عبر الواتساب، الرقم قابل للنسخ:\n+9647763410970"
            ActionCard("شحن عبر سوبيركي") { whatsappDialog = msg }
            ActionCard("شحن عبر زين كاش") { whatsappDialog = msg }
            ActionCard("شحن عبر USDT") { whatsappDialog = msg }
            ActionCard("شحن عبر نقاط سنات") { whatsappDialog = msg }
            ActionCard("شحن عبر هلا بي") { whatsappDialog = msg }
        }

        // أزرار اختبارية محلية (يمكن حذفها)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (balance >= 1.0) saveBalance(balance - 1.0) }) { Text("خصم 1$ (تجربة)") }
            OutlinedButton(onClick = { saveBalance(balance + 1.0) }) { Text("إضافة 1$ (تجربة)") }
        }
    }

    // Dialog: واتساب
    whatsappDialog?.let { text ->
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        AlertDialog(
            onDismissRequest = { whatsappDialog = null },
            title = { Text("التواصل مع الدعم") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    cm.setPrimaryClip(ClipData.newPlainText("WhatsApp", "+9647763410970"))
                    Toast.makeText(ctx, "تم نسخ الرقم", Toast.LENGTH_SHORT).show()
                    whatsappDialog = null
                }) { Text("نسخ الرقم") }
            },
            dismissButton = {
                TextButton(onClick = { whatsappDialog = null }) { Text("إغلاق") }
            }
        )
    }

    // Dialog: اسياسيل
    if (asiaDialog) {
        AlertDialog(
            onDismissRequest = { asiaDialog = false },
            title = { Text("شحن عبر اسياسيل") },
            text = {
                Column {
                    Text("أدخل رقم الكارت المكوّن من 14 أو 16 رقمًا:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = asiaCard,
                        onValueChange = { if (it.length <= 20) asiaCard = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("مثال: 12345678901234") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val valid = (asiaCard.length == 14 || asiaCard.length == 16) && asiaCard.all { it.isDigit() }
                    if (!valid) {
                        Toast.makeText(ctx, "الرقم غير صحيح", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    // إضافة إلى الكارتات المعلقة
                    val prefs = ctx.prefs()
                    val list = prefs.loadPendingCards()
                    val id = UUID.randomUUID().toString()
                    val uid = prefs.getString(KEY_USER_ID, "") ?: ""
                    list.add(PendingCard(id = id, userId = uid, cardNumber = asiaCard, createdAt = System.currentTimeMillis()))
                    prefs.savePendingCards(list)

                    Toast.makeText(ctx, "تم استلام طلبك، سيتم شحن حسابك قريبًا", Toast.LENGTH_LONG).show()
                    asiaCard = ""
                    asiaDialog = false
                }) { Text("إرسال") }
            },
            dismissButton = {
                TextButton(onClick = { asiaDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

// ------------------------------
// دخول المالك
// ------------------------------
@Composable
fun AdminLoginScreen(onCancel: () -> Unit, onSuccess: () -> Unit) {
    val ctx = LocalContext.current
    var pass by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) { Text("رجوع") }
            Spacer(Modifier.width(8.dp))
            Text("دخول المالك", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("كلمة المرور") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (pass == "2000") onSuccess() else Toast.makeText(ctx, "كلمة مرور غير صحيحة", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("دخول")
        }
    }
}

// ------------------------------
// لوحة المالك + الكارتات المعلقة
// ------------------------------
@Composable
fun AdminDashboardScreen(onBack: () -> Unit, onOpenPendingCards: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(8.dp))
            Text("لوحة تحكم المالك", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onLogout) { Text("تسجيل خروج") }
        }
        Spacer(Modifier.height(12.dp))

        GridButtons(
            listOf(
                "الطلبات المعلقة (الخدمات)" to { },
                "الكارتات المعلقة" to onOpenPendingCards,
                "طلبات شدات ببجي" to { },
                "طلبات شحن الايتونز" to { },
                "طلبات الأرصدة المعلقة" to { },
                "طلبات لودو المعلقة" to { },
                "خصم الرصيد" to { },
                "إضافة رصيد" to { },
                "فحص حالة طلب API" to { },
                "فحص رصيد API" to { },
                "رصيد المستخدمين" to { },
                "عدد المستخدمين" to { },
                "إدارة المشرفين" to { },
                "إلغاء حظر المستخدم" to { },
                "حظر المستخدم" to { },
                "إعلان التطبيق" to { },
                "أكواد خدمات API" to { },
                "نظام الإحالة" to { },
                "شرح الخصومات" to { },
                "المتصدرين" to { }
            )
        )
    }
}

@Composable
fun PendingCardsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.prefs() }
    var list by remember { mutableStateOf(prefs.loadPendingCards()) }

    // Dialog لقبول بطاقة: إدخال قيمة
    var approveFor by remember { mutableStateOf<PendingCard?>(null) }
    var amountText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(8.dp))
            Text("الكارتات المعلقة", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))

        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد كارتات معلّقة حاليًا")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { pc ->
                    ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(pc.createdAt))
                            Text("UserId: ${pc.userId}", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("رقم الكارت: ${pc.cardNumber}")
                            Spacer(Modifier.height(4.dp))
                            Text("الوقت: $date", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    // رفض
                                    list = list.filter { it.id != pc.id }.toMutableList()
                                    prefs.savePendingCards(list)
                                    Toast.makeText(ctx, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                                }) { Text("رفض") }

                                Button(onClick = {
                                    approveFor = pc
                                    amountText = ""
                                }) { Text("قبول + إضافة رصيد") }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog إدخال المبلغ
    approveFor?.let { pc ->
        AlertDialog(
            onDismissRequest = { approveFor = null },
            title = { Text("إضافة رصيد للمستخدم") },
            text = {
                Column {
                    Text("أدخل المبلغ بالدولار لإضافته إلى رصيد المستخدم:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("مثال: 5.0") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        Toast.makeText(ctx, "قيمة غير صحيحة", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    // إضافة الرصيد
                    val current = prefs.getUserBalance(pc.userId)
                    prefs.setUserBalance(pc.userId, current + amount)

                    // إزالة من المعلّق
                    val updated = list.filter { it.id != pc.id }
                    prefs.savePendingCards(updated)
                    list = prefs.loadPendingCards()

                    Toast.makeText(ctx, "تمت إضافة $$amount إلى المستخدم", Toast.LENGTH_LONG).show()
                    approveFor = null
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { approveFor = null }) { Text("إلغاء") }
            }
        )
    }
}

// ------------------------------
// شاشات مؤقتة
// ------------------------------
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(24.dp))
        Text("سيتم توفير هذه الشاشة لاحقًا", textAlign = TextAlign.Center)
    }
}
