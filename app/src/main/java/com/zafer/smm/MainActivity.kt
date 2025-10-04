package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zafer.smm.data.local.LocalCatalog
import com.zafer.smm.data.local.LocalSection
import com.zafer.smm.data.local.LocalService
import com.zafer.smm.ui.ServiceListScreen

// شاشة التنقّل
sealed interface Screen {
    data object Home : Screen
    data object Services : Screen
    data class Category(val key: String) : Screen
    data object OwnerLogin : Screen
    data object OwnerPanel : Screen
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
private fun AppRoot() {
    var screen by rememberSaveable { mutableStateOf<Screen>(Screen.Home) }

    // حالة حوار تأكيد طلب خدمة
    var pendingOrder by rememberSaveable { mutableStateOf<LocalService?>(null) }

    Surface(Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                onGoServices = { screen = Screen.Services },
                onGoOrders = { /* TODO: لاحقاً */ },
                onGoBalance = { /* TODO: لاحقاً */ },
                onGoReferral = { /* TODO: لاحقاً */ },
                onGoLeaderboard = { /* TODO: لاحقاً */ },
                onOwnerLogin = { screen = Screen.OwnerLogin }
            )

            Screen.Services -> ServicesScreen(
                onBack = { screen = Screen.Home },
                onOpenCategory = { key -> screen = Screen.Category(key) }
            )

            is Screen.Category -> {
                val section = LocalCatalog.sections.firstOrNull { it.key == s.key }
                if (section == null) {
                    screen = Screen.Services
                } else {
                    ServiceListScreen(
                        section = section,
                        onBack = { screen = Screen.Services },
                        onOrderClick = { svc -> pendingOrder = svc }
                    )
                }
            }

            Screen.OwnerLogin -> OwnerLoginScreen(
                onBack = { screen = Screen.Home },
                onSuccess = { screen = Screen.OwnerPanel }
            )

            Screen.OwnerPanel -> OwnerPanelScreen(
                onBack = { screen = Screen.Home }
            )
        }

        // حوار تأكيد الطلب (بسيط)
        if (pendingOrder != null) {
            AlertDialog(
                onDismissRequest = { pendingOrder = null },
                title = { Text("تأكيد الطلب") },
                text = {
                    Text("هل تريد طلب الخدمة: ${pendingOrder!!.name} بسعر ${pendingOrder!!.price}$ ؟")
                },
                confirmButton = {
                    TextButton(onClick = {
                        // TODO: نفّذ إنشاء الطلب الحقيقي هنا (استدعاء API أو باك-إندك)
                        pendingOrder = null
                    }) { Text("تأكيد") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingOrder = null }) { Text("إلغاء") }
                }
            )
        }
    }
}

/* ----------------------------- شاشات الواجهة ----------------------------- */

@Composable
private fun HomeScreen(
    onGoServices: () -> Unit,
    onGoOrders: () -> Unit,
    onGoBalance: () -> Unit,
    onGoReferral: () -> Unit,
    onGoLeaderboard: () -> Unit,
    onOwnerLogin: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // زر دخول المالك أعلى اليمين وبحجم صغير
        TextButton(
            onClick = onOwnerLogin,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Text("دخول المالك", style = MaterialTheme.typography.labelLarge)
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "أهلاً وسهلاً بكم في تطبيق خدمات راتلوزن",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(16.dp))

            // أزرار رئيسية كبيرة ومرتبة
            MainActionButton("الخدمات", onGoServices)
            MainActionButton("طلباتي", onGoOrders)
            MainActionButton("رصيدي", onGoBalance)
            MainActionButton("الإحالة", onGoReferral)
            MainActionButton("المتصدرين 🎉", onGoLeaderboard)
        }
    }
}

@Composable
private fun MainActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ServicesScreen(
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("الخدمات") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("رجوع") }
            }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            items(LocalCatalog.sections) { sec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = { onOpenCategory(sec.key) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(sec.title, style = MaterialTheme.typography.titleMedium)
                        Text("${sec.services.size} خدمة", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerLoginScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var pass by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("تسجيل دخول المالك") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("رجوع") }
            }
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("كلمة المرور") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (pass == "2000") {
                    onSuccess()
                } else {
                    error = "كلمة المرور غير صحيحة"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("دخول") }
    }
}

@Composable
private fun OwnerPanelScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("لوحة تحكم المالك") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("خروج") }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            item { AdminButton("تعديل الأسعار والكميات") { } }
            item { AdminButton("الطلبات المعلقة (الخدمات)") { } }
            item { AdminButton("الكارتات المعلقة") { } }
            item { AdminButton("طلبات شدّات ببجي") { } }
            item { AdminButton("طلبات شحن الآيتونز") { } }
            item { AdminButton("طلبات الأرصدة المعلقة") { } }
            item { AdminButton("طلبات لودو المعلقة") { } }
            item { AdminButton("خصم الرصيد") { } }
            item { AdminButton("إضافة رصيد") { } }
            item { AdminButton("فحص حالة طلب API") { } }
            item { AdminButton("فحص رصيد API") { } }
            item { AdminButton("رصيد المستخدمين") { } }
            item { AdminButton("عدد المستخدمين") { } }
            item { AdminButton("إدارة المشرفين") { } }
            item { AdminButton("إلغاء حظر المستخدم") { } }
            item { AdminButton("حظر المستخدم") { } }
            item { AdminButton("إعلان التطبيق") { } }
            item { AdminButton("أكواد خدمات API") { } }
            item { AdminButton("نظام الإحالة") { } }
            item { AdminButton("شرح الخصومات") { } }
            item { AdminButton("المتصدرين") { } }
        }
    }
}

@Composable
private fun AdminButton(text: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
