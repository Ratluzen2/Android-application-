package com.zafer.smm

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*** نماذج البيانات ***/
data class LocalService(val id: Int, val name: String, val price: Double)
data class LocalSection(val key: String, val title: String, val services: List<LocalService>)

/*** الكتالوج — مطابق لصور البوت التي أرسلتها ***/
object UnifiedCatalog {
    val sections: List<LocalSection> = listOf(

        // المتابعين
        LocalSection(
            key = "followers",
            title = "قسم المتابعين",
            services = listOf(
                // تيكتوك
                LocalService(1, "متابعين تيكتوك (1000)", 3.5),
                LocalService(2, "متابعين تيكتوك (2000)", 7.0),
                LocalService(3, "متابعين تيكتوك (3000)", 10.5),
                LocalService(4, "متابعين تيكتوك (4000)", 14.0),
                // انستغرام
                LocalService(5, "متابعين انستغرام (1000)", 3.0),
                LocalService(6, "متابعين انستغرام (2000)", 6.0),
                LocalService(7, "متابعين انستغرام (3000)", 9.0),
                LocalService(8, "متابعين انستغرام (4000)", 12.0)
            )
        ),

        // الإعجابات
        LocalSection(
            key = "likes",
            title = "قسم الإعجابات",
            services = listOf(
                // تيكتوك
                LocalService(1, "لايكات تيكتوك (1000)", 1.0),
                LocalService(2, "لايكات تيكتوك (2000)", 2.0),
                LocalService(3, "لايكات تيكتوك (3000)", 3.0),
                LocalService(4, "لايكات تيكتوك (4000)", 4.0),
                // انستغرام
                LocalService(5, "لايكات انستغرام (1000)", 1.0),
                LocalService(6, "لايكات انستغرام (2000)", 2.0),
                LocalService(7, "لايكات انستغرام (3000)", 3.0),
                LocalService(8, "لايكات انستغرام (4000)", 4.0)
            )
        ),

        // المشاهدات
        LocalSection(
            key = "views",
            title = "قسم المشاهدات",
            services = listOf(
                // تيكتوك
                LocalService(1, "مشاهدات تيكتوك (1000)", 0.1),
                LocalService(2, "مشاهدات تيكتوك (10000)", 0.8),
                LocalService(3, "مشاهدات تيكتوك (20000)", 1.6),
                LocalService(4, "مشاهدات تيكتوك (30000)", 2.4),
                LocalService(5, "مشاهدات تيكتوك (50000)", 3.2),
                // انستغرام
                LocalService(6, "مشاهدات انستغرام (10000)", 0.8),
                LocalService(7, "مشاهدات انستغرام (20000)", 1.6),
                LocalService(8, "مشاهدات انستغرام (30000)", 2.4),
                LocalService(9, "مشاهدات انستغرام (50000)", 3.2)
            )
        ),

        // مشاهدات البث المباشر
        LocalSection(
            key = "live_views",
            title = "قسم مشاهدات البث المباشر",
            services = listOf(
                // تيكتوك
                LocalService(1, "مشاهدات بث تيكتوك (1000)", 2.0),
                LocalService(2, "مشاهدات بث تيكتوك (2000)", 4.0),
                LocalService(3, "مشاهدات بث تيكتوك (3000)", 6.0),
                LocalService(4, "مشاهدات بث تيكتوك (4000)", 8.0),
                // انستغرام
                LocalService(5, "مشاهدات بث انستغرام (1000)", 2.0),
                LocalService(6, "مشاهدات بث انستغرام (2000)", 4.0),
                LocalService(7, "مشاهدات بث انستغرام (3000)", 6.0),
                LocalService(8, "مشاهدات بث انستغرام (4000)", 8.0)
            )
        ),

        // رفع سكور تيكتوك
        LocalSection(
            key = "tiktok_score",
            title = "قسم رفع سكور تيكتوك",
            services = listOf(
                LocalService(1, "رفع سكور بنك (1000)", 2.0),
                LocalService(2, "رفع سكور بنك (2000)", 4.0),
                LocalService(3, "رفع سكور بنك (3000)", 6.0),
                LocalService(4, "رفع سكور بنك (10000)", 20.0)
            )
        ),

        // شحن شدات ببجي
        LocalSection(
            key = "pubg",
            title = "قسم شحن شدات ببجي",
            services = listOf(
                LocalService(1, "ببجي 60 شدة", 2.0),
                LocalService(2, "ببجي 120 شدة", 4.0),
                LocalService(3, "ببجي 180 شدة", 6.0),
                LocalService(4, "ببجي 240 شدة", 8.0),
                LocalService(5, "ببجي 325 شدة", 9.0),
                LocalService(6, "ببجي 660 شدة", 15.0),
                LocalService(7, "ببجي 1800 شدة", 40.0)
            )
        ),

        // شراء رصيد ايتونز
        LocalSection(
            key = "itunes",
            title = "قسم شراء رصيد ايتونز",
            services = listOf(
                LocalService(1, "شراء رصيد 5 ايتونز", 9.0),
                LocalService(2, "شراء رصيد 10 ايتونز", 18.0),
                LocalService(3, "شراء رصيد 15 ايتونز", 27.0),
                LocalService(4, "شراء رصيد 20 ايتونز", 36.0),
                LocalService(5, "شراء رصيد 25 ايتونز", 45.0),
                LocalService(6, "شراء رصيد 30 ايتونز", 54.0),
                LocalService(7, "شراء رصيد 35 ايتونز", 63.0),
                LocalService(8, "شراء رصيد 40 ايتونز", 72.0),
                LocalService(9, "شراء رصيد 45 ايتونز", 81.0),
                LocalService(10, "شراء رصيد 50 ايتونز", 90.0)
            )
        ),

        // خدمات التليجرام
        LocalSection(
            key = "telegram",
            title = "قسم خدمات التليجرام",
            services = listOf(
                // قنوات
                LocalService(1, "أعضاء قنوات تيلي 1k", 3.0),
                LocalService(2, "أعضاء قنوات تيلي 2k", 6.0),
                LocalService(3, "أعضاء قنوات تيلي 3k", 9.0),
                LocalService(4, "أعضاء قنوات تيلي 4k", 12.0),
                LocalService(5, "أعضاء قنوات تيلي 5k", 15.0),
                // كروبات
                LocalService(6, "أعضاء كروبات تيلي 1k", 3.0),
                LocalService(7, "أعضاء كروبات تيلي 2k", 6.0),
                LocalService(8, "أعضاء كروبات تيلي 3k", 9.0),
                LocalService(9, "أعضاء كروبات تيلي 4k", 12.0),
                LocalService(10, "أعضاء كروبات تيلي 5k", 15.0)
            )
        ),

        // خدمات اللودو
        LocalSection(
            key = "ludo",
            title = "قسم خدمات اللودو",
            services = listOf(
                // ألماس
                LocalService(1, "لودو 810 الماسة", 4.0),
                LocalService(2, "لودو 2280 الماسة", 8.9),
                LocalService(3, "لودو 5080 الماسة", 17.5),
                LocalService(4, "لودو 12750 الماسة", 42.7),
                // ذهب
                LocalService(5, "لودو 66680 ذهب", 4.0),
                LocalService(6, "لودو 219500 ذهب", 8.9),
                LocalService(7, "لودو 1443000 ذهب", 17.5),
                LocalService(8, "لودو 3627000 ذهب", 42.7)
            )
        ),

        // شراء رصيد الهاتف
        LocalSection(
            key = "mobile_recharge",
            title = "قسم شراء رصيد الهاتف",
            services = listOf(
                // أثير
                LocalService(1, "شراء رصيد2 دولار أثير", 3.5),
                LocalService(2, "شراء رصيد5 دولار أثير", 7.0),
                LocalService(3, "شراء رصيد10 دولار أثير", 13.0),
                LocalService(4, "شراء رصيد15 دولار أثير", 19.0),
                LocalService(5, "شراء رصيد40 دولار أثير", 52.0),
                // آسيا
                LocalService(6, "شراء رصيد2 دولار اسيا", 3.5),
                LocalService(7, "شراء رصيد5 دولار اسيا", 7.0),
                LocalService(8, "شراء رصيد10 دولار اسيا", 13.0),
                LocalService(9, "شراء رصيد15 دولار اسيا", 19.0),
                LocalService(10, "شراء رصيد40 دولار اسيا", 52.0),
                // كورك
                LocalService(11, "شراء رصيد2 دولار كورك", 3.5),
                LocalService(12, "شراء رصيد5 دولار كورك", 7.0),
                LocalService(13, "شراء رصيد10 دولار كورك", 13.0),
                LocalService(14, "شراء رصيد15 دولار كورك", 19.0)
            )
        )
    )
}

/*** حفظ جلسة المالك ***/
private const val PREFS = "smm_prefs"
private const val KEY_IS_ADMIN = "is_admin"

private fun Context.isAdmin(): Boolean =
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_IS_ADMIN, false)

private fun Context.setAdmin(value: Boolean) {
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_ADMIN, value).apply()
}

/*** التنقّل ***/
sealed class Screen {
    data object Welcome : Screen()
    data object Services : Screen()
    data class ServiceList(val sectionKey: String) : Screen()
    data object Orders : Screen()
    data object Balance : Screen()
    data object Referral : Screen()
    data object Leaderboard : Screen()
    data object AdminLogin : Screen()
    data object AdminDashboard : Screen()
}

/*** Activity ***/
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startScreen = if (this.isAdmin()) Screen.AdminDashboard else Screen.Welcome
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFFF6EEF8)) {
                    AppRoot(startScreen)
                }
            }
        }
    }
}

/*** Root ***/
@Composable
private fun AppRoot(start: Screen) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var screen by remember { mutableStateOf(start) }

    when (screen) {
        Screen.Welcome -> WelcomeScreen(
            onGoServices = { screen = Screen.Services },
            onGoOrders = { screen = Screen.Orders },
            onGoBalance = { screen = Screen.Balance },
            onGoReferral = { screen = Screen.Referral },
            onGoLeaderboard = { screen = Screen.Leaderboard },
            onAdminClick = { screen = Screen.AdminLogin }
        )

        Screen.Services -> ServicesScreen(
            sections = UnifiedCatalog.sections,
            onBack = { screen = Screen.Welcome },
            onOpenSection = { key -> screen = Screen.ServiceList(key) }
        )

        is Screen.ServiceList -> {
            val key = (screen as Screen.ServiceList).sectionKey
            val sec = UnifiedCatalog.sections.firstOrNull { it.key == key }
            ServiceListScreen(
                section = sec,
                onBack = { screen = Screen.Services },
                onOrder = { s ->
                    Toast.makeText(ctx, "تم إرسال طلب: ${s.name} (${s.price}$)", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Screen.Orders -> PlaceholderScreen("طلباتي") { screen = Screen.Welcome }
        Screen.Balance -> PlaceholderScreen("رصيدي") { screen = Screen.Welcome }
        Screen.Referral -> PlaceholderScreen("الإحالة") { screen = Screen.Welcome }
        Screen.Leaderboard -> PlaceholderScreen("المتصدرين 🎉") { screen = Screen.Welcome }

        Screen.AdminLogin -> AdminLoginScreen(
            onCancel = { screen = Screen.Welcome },
            onSuccess = {
                ctx.setAdmin(true)
                screen = Screen.AdminDashboard
            }
        )

        Screen.AdminDashboard -> AdminDashboardScreen(
            onLogout = {
                ctx.setAdmin(false)
                screen = Screen.Welcome
            }
        )
    }
}

/*** شاشة ترحيب ***/
@Composable
private fun WelcomeScreen(
    onGoServices: () -> Unit,
    onGoOrders: () -> Unit,
    onGoBalance: () -> Unit,
    onGoReferral: () -> Unit,
    onGoLeaderboard: () -> Unit,
    onAdminClick: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onAdminClick) { Text("دخول المالك") }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "خدمات راتلوزن",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "أهلا وسهلاً بكم في تطبيق خدمات راتلوزن",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        MainGrid(
            items = listOf(
                "الخدمات" to onGoServices,
                "طلباتي" to onGoOrders,
                "رصيدي" to onGoBalance,
                "الإحالة" to onGoReferral,
                "المتصدرين 🎉" to onGoLeaderboard
            )
        )
    }
}

/*** شاشة الأقسام ***/
@Composable
private fun ServicesScreen(
    sections: List<LocalSection>,
    onBack: () -> Unit,
    onOpenSection: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الخدمات", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = onBack) { Text("رجوع") }
        }
        Spacer(Modifier.height(8.dp))
        val items = sections.map { it.title to { onOpenSection(it.key) } }
        MainGrid(items)
    }
}

/*** شاشة قائمة الخدمات ***/
@Composable
private fun ServiceListScreen(
    section: LocalSection?,
    onBack: () -> Unit,
    onOrder: (LocalService) -> Unit
) {
    if (section == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("القسم غير موجود")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack) { Text("رجوع") }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(section.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = onBack) { Text("رجوع") }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(section.services.size) { i ->
                val s = section.services[i]
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDD0F3)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("${s.price} $", color = Color(0xFF7A53C4))
                        }
                        Button(onClick = { onOrder(s) }) { Text("طلب الخدمة") }
                    }
                }
            }
        }
    }
}

/*** تسجيل دخول المالك ***/
@Composable
private fun AdminLoginScreen(
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var pass by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تسجيل دخول المالك", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("كلمة المرور") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) { Text("إلغاء") }
            Button(onClick = {
                if (pass.trim() == "2000") onSuccess()
                else Toast.makeText(ctx, "كلمة المرور غير صحيحة", Toast.LENGTH_SHORT).show()
            }) { Text("دخول") }
        }
    }
}

/*** لوحة تحكم المالك ***/
@Composable
private fun AdminDashboardScreen(onLogout: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scroll = rememberScrollState()

    fun click(msg: String) =
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(scroll)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("لوحة تحكم المالك", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = onLogout) { Text("خروج من اللوحة") }
        }
        Spacer(Modifier.height(8.dp))

        MainGrid(
            items = listOf(
                "تعديل الأسعار والكميات" to { click("تعديل الأسعار والكميات") },
                "الطلبات المعلقة (الخدمات)" to { click("الطلبات المعلقة") },
                "الكارتات المعلقة" to { click("الكارتات المعلقة") },
                "طلبات شدات ببجي" to { click("طلبات شدات ببجي") },
                "طلبات شحن الايتونز" to { click("طلبات ايتونز") },
                "طلبات الأرصدة المعلقة" to { click("طلبات الأرصدة") },
                "طلبات لودو المعلقة" to { click("طلبات لودو") },
                "خصم الرصيد" to { click("خصم الرصيد") },
                "إضافة رصيد" to { click("إضافة رصيد") },
                "فحص حالة طلب API" to { click("فحص حالة طلب API") },
                "فحص رصيد API" to { click("فحص رصيد API") },
                "رصيد المستخدمين" to { click("رصيد المستخدمين") },
                "عدد المستخدمين" to { click("عدد المستخدمين") },
                "إدارة المشرفين" to { click("إدارة المشرفين") },
                "إلغاء حظر المستخدم" to { click("إلغاء الحظر") },
                "حظر المستخدم" to { click("حظر المستخدم") },
                "إعلان التطبيق" to { click("إعلان التطبيق") },
                "أكواد خدمات API" to { click("أكواد خدمات API") },
                "نظام الإحالة" to { click("نظام الإحالة") },
                "شرح الخصومات" to { click("شرح الخصومات") },
                "المتصدرين" to { click("المتصدرين") }
            )
        )
    }
}

/*** شاشة نائبة ***/
@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = onBack) { Text("رجوع") }
        }
        Spacer(Modifier.height(16.dp))
        Text("هذه الشاشة قيد التنفيذ…", color = Color(0xFF7A53C4))
    }
}

/*** شبكة بطاقات ***/
@Composable
private fun MainGrid(items: List<Pair<String, () -> Unit>>) {
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, onClick) ->
                    DashboardCard(label = label, modifier = Modifier.weight(1f), onClick = onClick)
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DashboardCard(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .heightIn(min = 90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFDDCFF5)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(label, textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4E3B87))
        }
    }
}
