@file:OptIn(ExperimentalMaterial3Api::class)

package com.zafer.smm

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch

// -------------------------------
// نماذج البيانات
// -------------------------------
data class Service(val id: Int, val name: String, val price: Double)
data class Section(val key: String, val title: String, val services: List<Service>)

// كتالوج الخدمات (مطابق لصور البوت)
object Catalog {
    val sections: List<Section> = listOf(
        Section(
            key = "followers", title = "قسم المتابعين",
            services = listOf(
                // TikTok
                Service(1, "متابعين تيكتوك (1000)", 3.5),
                Service(2, "متابعين تيكتوك (2000)", 7.0),
                Service(3, "متابعين تيكتوك (3000)", 10.5),
                Service(4, "متابعين تيكتوك (4000)", 14.0),
                // Instagram
                Service(5, "متابعين انستغرام (1000)", 3.0),
                Service(6, "متابعين انستغرام (2000)", 6.0),
                Service(7, "متابعين انستغرام (3000)", 9.0),
                Service(8, "متابعين انستغرام (4000)", 12.0)
            )
        ),
        Section(
            key = "likes", title = "قسم الإعجابات",
            services = listOf(
                // TikTok
                Service(1, "لايكات تيكتوك (1000)", 1.0),
                Service(2, "لايكات تيكتوك (2000)", 2.0),
                Service(3, "لايكات تيكتوك (3000)", 3.0),
                Service(4, "لايكات تيكتوك (4000)", 4.0),
                // Instagram
                Service(5, "لايكات انستغرام (1000)", 1.0),
                Service(6, "لايكات انستغرام (2000)", 2.0),
                Service(7, "لايكات انستغرام (3000)", 3.0),
                Service(8, "لايكات انستغرام (4000)", 4.0)
            )
        ),
        Section(
            key = "views", title = "قسم المشاهدات",
            services = listOf(
                // TikTok
                Service(1, "مشاهدات تيكتوك (1000)", 0.1),
                Service(2, "مشاهدات تيكتوك (10000)", 0.8),
                Service(3, "مشاهدات تيكتوك (20000)", 1.6),
                Service(4, "مشاهدات تيكتوك (30000)", 2.4),
                Service(5, "مشاهدات تيكتوك (50000)", 3.2),
                // Instagram
                Service(6, "مشاهدات انستغرام (10000)", 0.8),
                Service(7, "مشاهدات انستغرام (20000)", 1.6),
                Service(8, "مشاهدات انستغرام (30000)", 2.4),
                Service(9, "مشاهدات انستغرام (50000)", 3.2)
            )
        ),
        Section(
            key = "live_views", title = "قسم مشاهدات البث المباشر",
            services = listOf(
                // TikTok
                Service(1, "مشاهدات بث تيكتوك (1000)", 2.0),
                Service(2, "مشاهدات بث تيكتوك (2000)", 4.0),
                Service(3, "مشاهدات بث تيكتوك (3000)", 6.0),
                Service(4, "مشاهدات بث تيكتوك (4000)", 8.0),
                // Instagram
                Service(5, "مشاهدات بث انستغرام (1000)", 2.0),
                Service(6, "مشاهدات بث انستغرام (2000)", 4.0),
                Service(7, "مشاهدات بث انستغرام (3000)", 6.0),
                Service(8, "مشاهدات بث انستغرام (4000)", 8.0)
            )
        ),
        Section(
            key = "raise_score_tiktok", title = "قسم رفع سكور تيكتوك",
            services = listOf(
                Service(1, "رفع سكور بنك (1000)", 2.0),
                Service(2, "رفع سكور بنك (2000)", 4.0),
                Service(3, "رفع سكور بنك (3000)", 6.0),
                Service(4, "رفع سكور بنك (10000)", 20.0)
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
                Service(7, "ببجي 1800 شدة", 40.0)
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
                Service(10, "شراء رصيد 50 ايتونز", 90.0)
            )
        ),
        Section(
            key = "telegram", title = "قسم خدمات التليجرام",
            services = listOf(
                // قنوات
                Service(1, "أعضاء قنوات تلي (1k)", 3.0),
                Service(2, "أعضاء قنوات تلي (2k)", 6.0),
                Service(3, "أعضاء قنوات تلي (3k)", 9.0),
                Service(4, "أعضاء قنوات تلي (4k)", 12.0),
                Service(5, "أعضاء قنوات تلي (5k)", 15.0),
                // كروبات
                Service(6, "أعضاء كروبات تلي (1k)", 3.0),
                Service(7, "أعضاء كروبات تلي (2k)", 6.0),
                Service(8, "أعضاء كروبات تلي (3k)", 9.0),
                Service(9, "أعضاء كروبات تلي (4k)", 12.0),
                Service(10, "أعضاء كروبات تلي (5k)", 15.0)
            )
        ),
        Section(
            key = "ludo", title = "قسم خدمات اللودو",
            services = listOf(
                // ألماس
                Service(1, "لودو 810 الماسة", 4.0),
                Service(2, "لودو 2280 الماسة", 8.9),
                Service(3, "لودو 5080 الماسة", 17.5),
                Service(4, "لودو 12750 الماسة", 42.7),
                // ذهب
                Service(5, "لودو 66680 ذهب", 4.0),
                Service(6, "لودو 219500 ذهب", 8.9),
                Service(7, "لودو 1443000 ذهب", 17.5),
                Service(8, "لودو 3627000 ذهب", 42.7)
            )
        ),
        Section(
            key = "mobile_recharge", title = "قسم شراء رصيد الهاتف",
            services = listOf(
                // أثير
                Service(1, "شراء رصيد 2 دولار أثير", 3.5),
                Service(2, "شراء رصيد 5 دولار أثير", 7.0),
                Service(3, "شراء رصيد 10 دولار أثير", 13.0),
                Service(4, "شراء رصيد 15 دولار أثير", 19.0),
                Service(5, "شراء رصيد 40 دولار أثير", 52.0),
                // آسيا
                Service(6, "شراء رصيد 2 دولار اسيا", 3.5),
                Service(7, "شراء رصيد 5 دولار اسيا", 7.0),
                Service(8, "شراء رصيد 10 دولار اسيا", 13.0),
                Service(9, "شراء رصيد 15 دولار اسيا", 19.0),
                Service(10, "شراء رصيد 40 دولار اسيا", 52.0),
                // كورك
                Service(11, "شراء رصيد 2 دولار كورك", 3.5),
                Service(12, "شراء رصيد 5 دولار كورك", 7.0),
                Service(13, "شراء رصيد 10 دولار كورك", 13.0),
                Service(14, "شراء رصيد 15 دولار كورك", 19.0)
            )
        )
    )

    fun findSection(key: String): Section? = sections.find { it.key == key }
}

// -------------------------------
// تخزين بسيط لحالة دخول المالك
// -------------------------------
private const val PREF = "smm_prefs"
private const val KEY_ADMIN = "admin_logged_in"

private fun isAdminSaved(ctx: Context): Boolean =
    ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_ADMIN, false)

private fun setAdmin(ctx: Context, value: Boolean) {
    ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_ADMIN, value).apply()
}

// -------------------------------
// الشاشات
// -------------------------------
sealed class Screen {
    data object HOME : Screen()
    data object SERVICES : Screen()
    data class SERVICE_LIST(val sectionKey: String) : Screen()
    data object ORDERS : Screen()
    data object BALANCE : Screen()
    data object REFERRAL : Screen()
    data object LEADERBOARD : Screen()
    data object ADMIN_LOGIN : Screen()
    data object ADMIN_DASHBOARD : Screen()
}

// -------------------------------
// MainActivity
// -------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    val snackHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ابدأ بلوحة المالك إذا كان محفوظاً
    var screen by remember { mutableStateOf<Screen>(if (isAdminSaved(ctx)) Screen.ADMIN_DASHBOARD else Screen.HOME) }
    var adminPassword by remember { mutableStateOf("") }

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackHost) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "خدمات راتلوزن",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        if (screen != Screen.ADMIN_DASHBOARD) {
                            Text(
                                text = "دخول المالك",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable { screen = Screen.ADMIN_LOGIN },
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "تسجيل خروج",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable {
                                        setAdmin(ctx, false)
                                        screen = Screen.HOME
                                    },
                                fontSize = 14.sp
                            )
                        }
                    }
                )
            }
        ) { inner ->
            Box(Modifier.padding(inner)) {
                when (val s = screen) {
                    Screen.HOME -> HomeScreen(
                        onServices = { screen = Screen.SERVICES },
                        onOrders = { screen = Screen.ORDERS },
                        onBalance = { screen = Screen.BALANCE },
                        onReferral = { screen = Screen.REFERRAL },
                        onLeaders = { screen = Screen.LEADERBOARD },
                        onAdmin = { screen = Screen.ADMIN_LOGIN }
                    )

                    Screen.SERVICES -> ServicesScreen(
                        onBack = { screen = Screen.HOME },
                        onOpenSection = { screen = Screen.SERVICE_LIST(it) }
                    )

                    is Screen.SERVICE_LIST -> {
                        val section = Catalog.findSection(s.sectionKey)
                        ServiceListScreen(
                            title = section?.title ?: "",
                            services = section?.services ?: emptyList(),
                            onOrder = { service ->
                                scope.launch {
                                    snackHost.showSnackbar("تم إرسال طلب: ${service.name} بسعر ${service.price}\$")
                                }
                            },
                            onBack = { screen = Screen.SERVICES }
                        )
                    }

                    Screen.ORDERS -> SimplePlaceholder("طلباتي", onBack = { screen = Screen.HOME })
                    Screen.BALANCE -> SimplePlaceholder("رصيدي", onBack = { screen = Screen.HOME })
                    Screen.REFERRAL -> SimplePlaceholder("الإحالة", onBack = { screen = Screen.HOME })
                    Screen.LEADERBOARD -> SimplePlaceholder("المتصدرين 🎉", onBack = { screen = Screen.HOME })

                    Screen.ADMIN_LOGIN -> AdminLoginScreen(
                        password = adminPassword,
                        onPasswordChange = { adminPassword = it },
                        onSubmit = {
                            if (adminPassword == "2000") {
                                setAdmin(ctx, true)
                                adminPassword = ""
                                screen = Screen.ADMIN_DASHBOARD
                                scope.launch { snackHost.showSnackbar("تم تسجيل دخول المالك") }
                            } else {
                                scope.launch { snackHost.showSnackbar("كلمة مرور خاطئة") }
                            }
                        },
                        onBack = { screen = Screen.HOME }
                    )

                    Screen.ADMIN_DASHBOARD -> AdminDashboard(
                        onBack = { screen = Screen.HOME }
                    )
                }
            }
        }
    }
}

// -------------------------------
// واجهات رئيسية
// -------------------------------
@Composable
private fun HomeScreen(
    onServices: () -> Unit,
    onOrders: () -> Unit,
    onBalance: () -> Unit,
    onReferral: () -> Unit,
    onLeaders: () -> Unit,
    onAdmin: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        // شبكة أزرار مرتبة (شكل احترافي مشابه السابق)
        MainButtonsGrid(
            listOf(
                "الخدمات" to onServices,
                "طلباتي" to onOrders,
                "رصيدي" to onBalance,
                "الإحالة" to onReferral,
                "المتصدرين 🎉" to onLeaders
            )
        )

        Spacer(Modifier.weight(1f))

        // زر دخول المالك صغير بأسفل اليسار (لدينا أيضًا في التولبار بالأعلى يمين)
        TextButton(onClick = onAdmin, modifier = Modifier.align(Alignment.End)) {
            Text("دخول المالك")
        }
    }
}

@Composable
private fun MainButtonsGrid(items: List<Pair<String, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (label, onClick) ->
                    ElevatedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text(label, fontSize = 16.sp) }
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ServicesScreen(
    onBack: () -> Unit,
    onOpenSection: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الخدمات", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        // شبكة الأقسام (نفس الشكل السابق)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Catalog.sections.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { section ->
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clickable { onOpenSection(section.key) }
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(section.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text("ادخل لعرض الخدمات وطلب الخدمة",
                                    fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
private fun ServiceListScreen(
    title: String,
    services: List<Service>,
    onOrder: (Service) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(services, key = { it.id }) { service ->
                ServiceRow(service = service, onOrder = { onOrder(service) })
                Spacer(Modifier.height(10.dp))
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
private fun ServiceRow(service: Service, onOrder: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.SemiBold)
                Text("${service.price}\$", color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onOrder) { Text("طلب الخدمة") }
        }
    }
}

@Composable
private fun SimplePlaceholder(title: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text("هذه الشاشة للعرض فقط في النسخة الحالية.")
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
private fun AdminLoginScreen(
    password: String,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تسجيل دخول المالك", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("كلمة المرور") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("دخول")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("رجوع")
        }
    }
}

@Composable
private fun AdminDashboard(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("لوحة تحكم المالك", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        val buttons = listOf(
            "تعديل الأسعار والكميات",
            "الطلبات المعلقة (الخدمات)",
            "الكارتات المعلقة",
            "طلبات شدات ببجي",
            "طلبات شحن الايتونز",
            "طلبات الأرصدة المعلقة",
            "طلبات لودو المعلقة",
            "خصم الرصيد",
            "إضافة رصيد",
            "فحص حالة طلب API",
            "فحص رصيد API",
            "رصيد المستخدمين",
            "عدد المستخدمين",
            "إدارة المشرفين",
            "إلغاء حظر المستخدم",
            "حظر المستخدم",
            "إعلان التطبيق",
            "أكواد خدمات API",
            "نظام الإحالة",
            "شرح الخصومات",
            "المتصدرين"
        )
        LazyColumn(Modifier.weight(1f)) {
            items(buttons) { label ->
                OutlinedButton(
                    onClick = { /* لاحقًا */ },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(label) }
                Spacer(Modifier.height(8.dp))
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}
