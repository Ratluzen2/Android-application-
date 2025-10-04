package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// ---------------------- شاشات التطبيق البسيطة (تنقّل داخلي) ----------------------
private sealed class Screen {
    data object Welcome : Screen()
    data object Services : Screen()
    data object AdminLogin : Screen()
    data object AdminPanel : Screen()
}

// ---------------------- النشاط الرئيسي ----------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                // نجعل الاتجاه افتراضياً من اليمين لليسار لواجهة عربية أنيقة
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot()
                }
            }
        }
    }
}

// ---------------------- جذر التطبيق ----------------------
@Composable
private fun AppRoot() {
    var current by remember { mutableStateOf<Screen>(Screen.Welcome) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (current) {
                Screen.Welcome -> WelcomeScreen(
                    onOpenServices = { current = Screen.Services },
                    onOpenOrders = { showSoon(snackbarHostState) },
                    onOpenWallet = { showSoon(snackbarHostState) },
                    onOpenReferral = { showSoon(snackbarHostState) },
                    onOpenLeaderboard = { showSoon(snackbarHostState) },
                    onOwnerClick = { current = Screen.AdminLogin }
                )

                Screen.Services -> ServicesScreen(
                    onBack = { current = Screen.Welcome },
                    onCategoryClick = { /* افتح شاشة التفاصيل لاحقاً */ showSoon(snackbarHostState) }
                )

                Screen.AdminLogin -> AdminLoginScreen(
                    onBack = { current = Screen.Welcome },
                    onLoginOk = { current = Screen.AdminPanel },
                    onLoginFail = { msg -> showSnack(snackbarHostState, msg) }
                )

                Screen.AdminPanel -> AdminPanelScreen(
                    onBack = { current = Screen.Welcome },
                    onItemClick = { showSoon(snackbarHostState) }
                )
            }
        }
    }
}

// ---------------------- شاشة ترحيبية مع الأزرار الرئيسية ----------------------
@Composable
private fun WelcomeScreen(
    onOpenServices: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenReferral: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOwnerClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "أهلاً وسهلاً بكم في تطبيق خدمات راتلوزن",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))

        PrimaryButton(text = "الخدمات", onClick = onOpenServices)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(text = "طلباتي", onClick = onOpenOrders)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(text = "رصيدي", onClick = onOpenWallet)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(text = "الإحالة", onClick = onOpenReferral)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(text = "المتصدرين 🎉", onClick = onOpenLeaderboard)
        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = "دخول المالك",
            onClick = onOwnerClick,
            prominent = true
        )
    }
}

// ---------------------- شاشة الأقسام داخل "الخدمات" ----------------------
private data class ServiceCategory(val id: String, val title: String, val emoji: String)

@Composable
private fun ServicesScreen(
    onBack: () -> Unit,
    onCategoryClick: (ServiceCategory) -> Unit
) {
    val categories = remember {
        listOf(
            ServiceCategory("followers", "قسم المتابعين", "👥"),
            ServiceCategory("likes", "قسم الايكات", "❤️"),
            ServiceCategory("views", "قسم المشاهدات", "👁️"),
            ServiceCategory("live_views", "قسم مشاهدات البث المباشر", "🔴"),
            ServiceCategory("pubg", "قسم شحن شدات ببجي", "🎮"),
            ServiceCategory("tiktok_score", "قسم رفع سكور تيكتوك", "📈"),
            ServiceCategory("itunes", "قسم شراء رصيد ايتونز", "🎵"),
            ServiceCategory("telegram", "قسم خدمات التليجرام", "✈️"),
            ServiceCategory("ludo", "قسم خدمات الودو", "🎲"),
            ServiceCategory("mobile_topup", "قسم شراء رصيد الهاتف", "📱")
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("الأقسام") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("رجوع") }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            Text(
                "اختر قسماً للمتابعة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { cat ->
                    CategoryCard(cat) { onCategoryClick(cat) }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(cat: ServiceCategory, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                cat.emoji,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                cat.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------- شاشة تسجيل دخول المالك (كلمة مرور 2000) ----------------------
@Composable
private fun AdminLoginScreen(
    onBack: () -> Unit,
    onLoginOk: () -> Unit,
    onLoginFail: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("تسجيل دخول المالك") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("رجوع") }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("من فضلك أدخل كلمة المرور الخاصة بالمالك", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = "دخول") {
                if (password == "2000") {
                    onLoginOk()
                } else {
                    onLoginFail("كلمة المرور غير صحيحة")
                }
            }
        }
    }
}

// ---------------------- شاشة لوحة تحكم المالك (أزرار فقط الآن) ----------------------
@Composable
private fun AdminPanelScreen(
    onBack: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val items = listOf(
        "تعديل الأسعار والكميات",
        "الطلبات المعلّقة (الخدمات)",
        "الكارتات المعلّقة",
        "طلبات شدّات ببجي",
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("لوحة تحكم المالك") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("رجوع") }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            Text(
                "اختر إجراء:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { title ->
                    Card(
                        onClick = { onItemClick(title) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                title,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- عناصر مساعدة ----------------------
@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    prominent: Boolean = false
) {
    val colors = if (prominent)
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    else
        ButtonDefaults.filledTonalButtonColors()

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

private suspend fun showSnack(host: SnackbarHostState, msg: String) {
    host.showSnackbar(message = msg, withDismissAction = true)
}

private fun showSoon(host: SnackbarHostState) {
    // إطلاق كوروتين بسيط لعرض سنackbar
    LaunchedEffect(Unit) {
        host.showSnackbar("سيتوفر قريبًا")
    }
}
