@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zafer.smm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** إدارة جلسة المالك (تخزين بسيط في SharedPreferences) */
object AdminSession {
    private const val PREF = "owner_prefs"
    private const val KEY = "owner_is_logged_in"
    fun isLoggedIn(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setLoggedIn(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, value).apply()
    }
}

/** شاشات التطبيق */
sealed class Screen {
    data object Home : Screen()
    data object Services : Screen()
    data object AdminLogin : Screen()
    data object AdminDashboard : Screen()
    data class Category(val title: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // إذا كان المالك مسجّل دخول مسبقًا افتح لوحة التحكم مباشرة
    var screen by remember {
        mutableStateOf<Screen>(
            if (AdminSession.isLoggedIn(ctx)) Screen.AdminDashboard else Screen.Home
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("خدمات راتلوزن", fontSize = 18.sp) },
                actions = {
                    // زر دخول المالك أعلى اليمين، صغير ومنفصل
                    TextButton(
                        onClick = { screen = Screen.AdminLogin },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("دخول المالك")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            when (val s = screen) {
                is Screen.Home -> HomeScreen(
                    onServices = { screen = Screen.Services },
                    onOrders = {
                        scope.launch { snackbarHostState.showSnackbar("سيتم تنفيذ 'طلباتي' لاحقًا") }
                    },
                    onWallet = {
                        scope.launch { snackbarHostState.showSnackbar("سيتم تنفيذ 'رصيدي' لاحقًا") }
                    },
                    onReferral = {
                        scope.launch { snackbarHostState.showSnackbar("سيتم تنفيذ 'الإحالة' لاحقًا") }
                    },
                    onLeaders = {
                        scope.launch { snackbarHostState.showSnackbar("سيتم تنفيذ 'المتصدرين' لاحقًا") }
                    }
                )

                is Screen.Services -> ServicesScreen(
                    onBack = { screen = Screen.Home },
                    onOpenCategory = { title -> screen = Screen.Category(title) }
                )

                is Screen.Category -> CategoryScreen(
                    title = s.title,
                    onBack = { screen = Screen.Services },
                    onOrder = { name ->
                        scope.launch {
                            snackbarHostState.showSnackbar("طلب خدمة: $name (تجريبي)")
                        }
                    }
                )

                is Screen.AdminLogin -> AdminLoginScreen(
                    onCancel = { screen = Screen.Home },
                    onSuccess = {
                        AdminSession.setLoggedIn(ctx, true)
                        screen = Screen.AdminDashboard
                        scope.launch { snackbarHostState.showSnackbar("تم تسجيل دخول المالك") }
                    }
                )

                is Screen.AdminDashboard -> AdminDashboardScreen(
                    onLogout = {
                        AdminSession.setLoggedIn(ctx, false)
                        screen = Screen.Home
                    },
                    onTodo = { title ->
                        scope.launch { snackbarHostState.showSnackbar("$title (قريبًا)") }
                    }
                )
            }
        }
    }
}

/** شاشة ترحيب + أزرار رئيسية مرتّبة */
@Composable
fun HomeScreen(
    onServices: () -> Unit,
    onOrders: () -> Unit,
    onWallet: () -> Unit,
    onReferral: () -> Unit,
    onLeaders: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(18.dp))

        // شبكة أزرار 2×N
        FlowRow2Cols(
            items = listOf(
                "الخدمات" to onServices,
                "طلباتي" to onOrders,
                "رصيدي" to onWallet,
                "الإحالة" to onReferral,
                "المتصدرين 🎉" to onLeaders,
            )
        )
    }
}

/** شاشة الأقسام داخل "الخدمات" */
@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(6.dp))
            Text("الأقسام", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))

        val cats = listOf(
            "قسم المتابعين",
            "قسم الإيكات",
            "قسم المشاهدات",
            "قسم مشاهدات البث المباشر",
            "قسم شحن شدات ببجي",
            "قسم رفع سكور تيكتوك",
            "قسم شراء رصيد ايتونز",
            "قسم خدمات التليجرام",
            "قسم خدمات اللودو",
            "قسم شراء رصيد الهاتف",
        )

        LazyColumn(Modifier.fillMaxSize()) {
            items(cats) { c ->
                ElevatedCard(
                    onClick = { onOpenCategory(c) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(c, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(onClick = { onOpenCategory(c) }) {
                            Text("فتح")
                        }
                    }
                }
            }
        }
    }
}

/** شاشة قائمة خدمات فئة واحدة + زر "طلب الخدمة" لكل عنصر (تجريبي) */
@Composable
fun CategoryScreen(
    title: String,
    onBack: () -> Unit,
    onOrder: (String) -> Unit
) {
    val sampleServices = remember(title) {
        // أمثلة فقط — لاحقًا ستملأ من الـAPI/قاعدة البيانات
        when (title) {
            "قسم المتابعين" -> listOf("متابعين تيكتوك 1K", "متابعين انستغرام 1K")
            "قسم الإيكات" -> listOf("لايكات تيكتوك 1K", "لايكات انستغرام 1K")
            "قسم المشاهدات" -> listOf("مشاهدات تيكتوك 5K", "مشاهدات انستغرام 5K")
            "قسم مشاهدات البث المباشر" -> listOf("مشاهدات بث مباشر 1K")
            "قسم شحن شدات ببجي" -> listOf("60 UC", "660 UC", "1800 UC")
            "قسم رفع سكور تيكتوك" -> listOf("رفع سكور سريع", "رفع سكور بطيء")
            "قسم شراء رصيد ايتونز" -> listOf("iTunes $5", "iTunes $10", "iTunes $25")
            "قسم خدمات التليجرام" -> listOf("أعضاء قناة 1K", "أعضاء كروب 2K")
            "قسم خدمات اللودو" -> listOf("ألماسة لودو 100", "ذهب لودو 1K")
            "قسم شراء رصيد الهاتف" -> listOf("أثير $5", "آسيا $10", "كورك $10")
            else -> emptyList()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(sampleServices) { s ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { onOrder(s) }) {
                            Text("طلب الخدمة")
                        }
                    }
                }
            }
        }
    }
}

/** شاشة إدخال كلمة مرور المالك */
@Composable
fun AdminLoginScreen(
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    var pass by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("دخول المالك", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("كلمة المرور") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onCancel) { Text("إلغاء") }
                Button(onClick = {
                    if (pass == "2000") onSuccess()
                    else scope.launch { snackbarHostState.showSnackbar("كلمة المرور غير صحيحة") }
                }) {
                    Text("دخول")
                }
            }
        }
    }
}

/** لوحة تحكم المالك (أزرار مرتبة وجذابة) */
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onTodo: (String) -> Unit
) {
    val items = listOf(
        "تعديل الأسعار والكميات",
        "الطلبات المعلّقة (الخدمات)",
        "الكارتات المعلّقة",
        "طلبات شدات ببجي",
        "طلبات شحن الايتونز",
        "طلبات الأرصدة المعلّقة",
        "طلبات لودو المعلّقة",
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

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("لوحة تحكم المالك", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onLogout) { Text("تسجيل الخروج") }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(items) { title ->
                ElevatedCard(
                    onClick = { onTodo(title) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(onClick = { onTodo(title) }) {
                            Text("فتح")
                        }
                    }
                }
            }
        }
    }
}

/** شبكة أزرار 2 أعمدة بسيطة */
@Composable
fun FlowRow2Cols(items: List<Pair<String, () -> Unit>>) {
    Column(Modifier.fillMaxWidth()) {
        val rows = items.chunked(2)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (label, onClick) ->
                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(label)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
