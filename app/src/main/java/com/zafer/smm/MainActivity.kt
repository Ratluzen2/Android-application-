package com.zafer.smm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.local.Catalog
import com.zafer.smm.data.local.ServiceCategory
import com.zafer.smm.data.local.ServiceEntry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // نتأكد من حفظ حالة دخول المالك
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot(prefs)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(prefs: android.content.SharedPreferences) {
    var isOwner by remember {
        mutableStateOf(prefs.getBoolean("is_owner", false))
    }
    var screen by remember { mutableStateOf(if (isOwner) Screen.OwnerPanel else Screen.Home) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("خدمات راتلوزن", fontWeight = FontWeight.SemiBold) },
                actions = {
                    // زر دخول المالك — أعلى اليمين وبحجم أصغر
                    if (!isOwner && screen == Screen.Home) {
                        TextButton(onClick = { screen = Screen.OwnerLogin }) {
                            Text("دخول المالك")
                        }
                    } else if (isOwner && screen != Screen.OwnerPanel) {
                        TextButton(onClick = { screen = Screen.OwnerPanel }) {
                            Text("لوحة المالك")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    onServices = { screen = Screen.Services },
                    onOrders = { showSnack(snackbarHostState, "قريباً: طلباتي") },
                    onWallet = { showSnack(snackbarHostState, "قريباً: رصيدي") },
                    onReferral = { showSnack(snackbarHostState, "قريباً: الإحالة") },
                    onLeaders = { showSnack(snackbarHostState, "قريباً: المتصدرين") }
                )

                Screen.Services -> ServicesScreen(
                    onBack = { screen = Screen.Home },
                    onPickCategory = { cat -> screen = Screen.Category(cat) }
                )

                is Screen.Category -> CategoryScreen(
                    category = screen.category,
                    onBack = { screen = Screen.Services },
                    onOrderClick = { entry ->
                        // هنا يمكن لاحقاً استدعاء الباكند لإرسال الطلب فعلياً
                        showSnack(snackbarHostState, "تم إرسال طلب: ${entry.name} (${entry.priceUSD}$)")
                    }
                )

                Screen.OwnerLogin -> OwnerLoginScreen(
                    onCancel = { screen = Screen.Home },
                    onSuccess = {
                        prefs.edit().putBoolean("is_owner", true).apply()
                        isOwner = true
                        screen = Screen.OwnerPanel
                    }
                )

                Screen.OwnerPanel -> OwnerPanelScreen(
                    onBackToHome = {
                        // خروج من اللوحة فقط (نبقي حالة المالك محفوظة كما هي)
                        screen = Screen.Home
                    },
                    onLogoutOwner = {
                        prefs.edit().putBoolean("is_owner", false).apply()
                        isOwner = false
                        screen = Screen.Home
                    },
                    onStubClick = { label ->
                        showSnack(snackbarHostState, "قريباً: $label")
                    }
                )
            }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object Services : Screen
    data class Category(val category: ServiceCategory) : Screen
    data object OwnerLogin : Screen
    data object OwnerPanel : Screen
}

@Composable
private fun HomeScreen(
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
            "أهلاً وسهلاً بكم في تطبيق خدمات راتلوزن",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        // أزرار رئيسية مرتبة وبسيطة
        MainButton("الخدمات", onServices)
        MainButton("طلباتي", onOrders)
        MainButton("رصيدي", onWallet)
        MainButton("الإحالة", onReferral)
        MainButton("المتصدرين 🎉", onLeaders)
    }
}

@Composable
private fun MainButton(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(52.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ServicesScreen(
    onBack: () -> Unit,
    onPickCategory: (ServiceCategory) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الأقسام", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        val cats = listOf(
            ServiceCategory.Followers,
            ServiceCategory.Likes,
            ServiceCategory.Views,
            ServiceCategory.LiveViews,
            ServiceCategory.TikTokScore,
            ServiceCategory.PUBG,
            ServiceCategory.ITunes,
            ServiceCategory.Telegram,
            ServiceCategory.Ludo,
            ServiceCategory.Mobile
        )

        cats.forEach { cat ->
            OutlinedButton(
                onClick = { onPickCategory(cat) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .height(50.dp)
            ) {
                Text(cat.title)
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("رجوع")
        }
    }
}

@Composable
private fun CategoryScreen(
    category: ServiceCategory,
    onBack: () -> Unit,
    onOrderClick: (ServiceEntry) -> Unit
) {
    val items = remember(category) {
        (Catalog[category] ?: emptyList()).sortedBy { it.priceUSD }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        if (items.isEmpty()) {
            Text("لا توجد خدمات في هذا القسم حالياً.")
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(items) { entry ->
                    ServiceRow(entry, onOrderClick)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("رجوع")
        }
    }
}

@Composable
private fun ServiceRow(entry: ServiceEntry, onOrderClick: (ServiceEntry) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("${entry.priceUSD} $", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { onOrderClick(entry) }) {
                Text("طلب الخدمة")
            }
        }
    }
}

@Composable
private fun OwnerLoginScreen(
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    var pass by remember { mutableStateOf("") }
    val correct = "2000"

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تسجيل دخول المالك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("كلمة المرور") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onCancel) { Text("إلغاء") }
            Button(onClick = {
                if (pass == correct) onSuccess()
                else pass = ""
            }) { Text("دخول") }
        }
    }
}

@Composable
private fun OwnerPanelScreen(
    onBackToHome: () -> Unit,
    onLogoutOwner: () -> Unit,
    onStubClick: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("لوحة تحكم المالك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        val adminButtons = listOf(
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

        LazyColumn(Modifier.weight(1f)) {
            items(adminButtons) { title ->
                OutlinedButton(
                    onClick = { onStubClick(title) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .height(48.dp)
                ) {
                    Text(title)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onBackToHome) { Text("الواجهة الرئيسية") }
            Button(onClick = onLogoutOwner) { Text("تسجيل خروج المالك") }
        }
    }
}

private suspend fun showSnack(host: SnackbarHostState, msg: String) {
    host.showSnackbar(message = msg, withDismissAction = true)
}
