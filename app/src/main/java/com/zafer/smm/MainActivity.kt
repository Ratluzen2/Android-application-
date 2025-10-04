package com.zafer.smm

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*==================== تخزين/استرجاع حالة دخول المالك ====================*/
object OwnerSession {
    private const val PREFS = "app_prefs"
    private const val KEY_OWNER = "owner_logged_in"

    fun isOwner(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_OWNER, false)

    fun setOwner(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OWNER, value)
            .apply()
    }
}

/*==================== تنقل الشاشات ====================*/
private sealed class Screen {
    data object Welcome : Screen()
    data object Services : Screen()
    data object AdminLogin : Screen()
    data object AdminPanel : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // قراءة حالة المالك المحفوظة لتحديد الشاشة عند فتح التطبيق
    var isOwner by remember { mutableStateOf(OwnerSession.isOwner(ctx)) }
    var current by remember { mutableStateOf<Screen>(if (isOwner) Screen.AdminPanel else Screen.Welcome) }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (current) {
                Screen.Welcome -> WelcomeScreen(
                    onOpenServices = { current = Screen.Services },
                    onOpenOrders = { soon(scope, snackbar) },
                    onOpenWallet = { soon(scope, snackbar) },
                    onOpenReferral = { soon(scope, snackbar) },
                    onOpenLeaderboard = { soon(scope, snackbar) },
                    onOwnerClick = { current = Screen.AdminLogin }
                )

                Screen.Services -> ServicesScreen(
                    onBack = { current = Screen.Welcome },
                    onCategoryClick = { soon(scope, snackbar) }
                )

                Screen.AdminLogin -> AdminLoginScreen(
                    onBack = { current = Screen.Welcome },
                    onLoginOk = {
                        OwnerSession.setOwner(ctx, true)
                        isOwner = true
                        current = Screen.AdminPanel
                    },
                    onLoginFail = { msg -> showSnack(scope, snackbar, msg) }
                )

                Screen.AdminPanel -> AdminPanelScreen(
                    onLogout = {
                        OwnerSession.setOwner(ctx, false)
                        isOwner = false
                        current = Screen.Welcome
                    },
                    onItemClick = { soon(scope, snackbar) }
                )
            }
        }
    }
}

/*==================== شاشة الترحيب ====================*/
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // زر دخول المالك صغير أعلى اليمين
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onOwnerClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("🔒 دخول المالك", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "أهلاً وسهلاً بكم في تطبيق خدمات راتلوزن",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))

        val tiles = listOf(
            HomeTile("الخدمات", "🛍️") { onOpenServices() },
            HomeTile("طلباتي", "📦") { onOpenOrders() },
            HomeTile("رصيدي", "💳") { onOpenWallet() },
            HomeTile("الإحالة", "👥") { onOpenReferral() },
            HomeTile("المتصدرين 🎉", "🏆") { onOpenLeaderboard() }
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tiles) { tile ->
                TileCard(title = tile.title, emoji = tile.emoji, onClick = tile.onClick)
            }
        }
    }
}

private data class HomeTile(
    val title: String,
    val emoji: String,
    val onClick: () -> Unit
)

@Composable
private fun TileCard(
    title: String,
    emoji: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
        }
    }
}

/*==================== شاشة الأقسام داخل "الخدمات" (بدون ملصقات) ====================*/
private data class ServiceCategory(val id: String, val title: String)

@Composable
private fun ServicesScreen(
    onBack: () -> Unit,
    onCategoryClick: (ServiceCategory) -> Unit
) {
    val categories = remember {
        listOf(
            ServiceCategory("followers", "قسم المتابعين"),
            ServiceCategory("likes", "قسم الايكات"),
            ServiceCategory("views", "قسم المشاهدات"),
            ServiceCategory("live_views", "قسم مشاهدات البث المباشر"),
            ServiceCategory("pubg", "قسم شحن شدات ببجي"),
            ServiceCategory("tiktok_score", "قسم رفع سكور تيكتوك"),
            ServiceCategory("itunes", "قسم شراء رصيد ايتونز"),
            ServiceCategory("telegram", "قسم خدمات التليجرام"),
            ServiceCategory("ludo", "قسم خدمات الودو"),
            ServiceCategory("mobile_topup", "قسم شراء رصيد الهاتف")
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("◀ رجوع") }
            Spacer(Modifier.width(8.dp))
            Text(
                "الأقسام",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories) { cat ->
                ElevatedCard(
                    onClick = { onCategoryClick(cat) },
                    elevation = CardDefaults.elevatedCardElevation(3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/*==================== شاشة دخول المالك ====================*/
@Composable
private fun AdminLoginScreen(
    onBack: () -> Unit,
    onLoginOk: () -> Unit,
    onLoginFail: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("◀ رجوع") }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "تسجيل دخول المالك",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text("من فضلك أدخل كلمة المرور الخاصة بالمالك", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة المرور") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (password == "2000") onLoginOk() else onLoginFail("كلمة المرور غير صحيحة")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Text("دخول", fontWeight = FontWeight.SemiBold)
        }
    }
}

/*==================== لوحة تحكم المالك ====================*/
@Composable
private fun AdminPanelScreen(
    onLogout: () -> Unit,
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

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // هذا الزر يعمل كـ "تسجيل خروج من لوحة المالك"
            TextButton(onClick = onLogout) { Text("◀ خروج من لوحة المالك") }
            Spacer(Modifier.width(8.dp))
            Text(
                "لوحة تحكم المالك",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { title ->
                ElevatedCard(
                    onClick = { onItemClick(title) },
                    elevation = CardDefaults.elevatedCardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

/*==================== أدوات Snackbar ====================*/
private fun showSnack(scope: CoroutineScope, host: SnackbarHostState, msg: String) {
    scope.launch { host.showSnackbar(message = msg, withDismissAction = true) }
}

private fun soon(scope: CoroutineScope, host: SnackbarHostState) {
    scope.launch { host.showSnackbar("سيتوفر قريبًا") }
}
