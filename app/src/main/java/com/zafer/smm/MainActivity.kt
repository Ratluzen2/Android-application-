@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RATLApp() }
    }
}

private const val ADMIN_PASS = "2000"
private const val PREFS_NAME = "ratluzen_prefs"
private const val KEY_ADMIN = "is_admin"

private sealed interface Screen {
    data object Home : Screen
    data object Services : Screen
    data object AdminLogin : Screen
    data object AdminPanel : Screen
}

@Composable
private fun RATLApp() {
    val ctx = LocalContext.current
    var current by remember { mutableStateOf<Screen>(Screen.Home) }
    var isAdmin by remember { mutableStateOf(loadAdmin(ctx)) }

    // إن كان المالك مُسجّل مسبقًا ابقَ في لوحة التحكم
    LaunchedEffect(Unit) {
        if (isAdmin) current = Screen.AdminPanel
    }

    MaterialTheme(colorScheme = lightColorScheme()) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = { Text("خدمات راتلوزن", fontSize = 18.sp) },
                    actions = {
                        // زر دخول المالك أعلى يمين وبحجم أصغر
                        TextButton(
                            onClick = { current = Screen.AdminLogin },
                            enabled = !isAdmin
                        ) { Text("دخول المالك") }
                    }
                )
            }
        ) { inner ->
            Box(Modifier.padding(inner)) {
                when (current) {
                    Screen.Home -> HomeScreen(
                        onGoServices = { current = Screen.Services },
                        onGoOrders = { /* TODO: شاشة طلباتي */ },
                        onGoWallet = { /* TODO: شاشة رصيدي */ },
                        onGoReferral = { /* TODO: شاشة الإحالة */ },
                        onGoLeaders = { /* TODO: شاشة المتصدرين */ },
                        onGoOwner = { current = Screen.AdminLogin },
                        isAdmin = isAdmin
                    )

                    Screen.Services -> ServicesScreen(
                        onBack = { current = Screen.Home }
                    )

                    Screen.AdminLogin -> AdminLoginScreen(
                        onCancel = { current = if (isAdmin) Screen.AdminPanel else Screen.Home },
                        onSuccess = {
                            isAdmin = true
                            saveAdmin(ctx, true)
                            current = Screen.AdminPanel
                        }
                    )

                    Screen.AdminPanel -> AdminPanelScreen(
                        onLogout = {
                            isAdmin = false
                            saveAdmin(ctx, false)
                            current = Screen.Home
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onGoServices: () -> Unit,
    onGoOrders: () -> Unit,
    onGoWallet: () -> Unit,
    onGoReferral: () -> Unit,
    onGoLeaders: () -> Unit,
    onGoOwner: () -> Unit,
    isAdmin: Boolean
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن", fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))

        // أزرار رئيسية مرتبة
        MainButton("الخدمات", onGoServices)
        MainButton("طلباتي", onGoOrders)
        MainButton("رصيدي", onGoWallet)
        MainButton("الإحالة", onGoReferral)
        MainButton("المتصدرين 🎉", onGoLeaders)

        Spacer(Modifier.height(12.dp))
        if (isAdmin) {
            AssistChip(
                onClick = onGoOwner,
                label = { Text("لوحة تحكم المالك (مفتوحة)") }
            )
        } else {
            // تلميح صغير أسفل
            Text(
                "يمكن للمالك الدخول من الزر العلوي \"دخول المالك\"",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MainButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) { Text(text, fontSize = 16.sp) }
}

@Composable
private fun ServicesScreen(onBack: () -> Unit) {
    val categories = listOf(
        "قسم المتابعين",
        "قسم الإيكات",
        "قسم المشاهدات",
        "قسم مشاهدات البث المباشر",
        "قسم شحن شدات ببجي",
        "قسم رفع سكور تيكتوك",
        "قسم شراء رصيد ايتونز",
        "قسم خدمات التليجرام",
        "قسم خدمات اللودو",
        "قسم شراء رصيد الهاتف"
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("الخدمات", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { c ->
                ElevatedCard(
                    onClick = {
                        // TODO: افتح شاشة خدمات القسم c
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(c)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "ادخل لعرض الخدمات وطلب الخدمة",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("رجوع")
        }
    }
}

@Composable
private fun AdminLoginScreen(
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تسجيل دخول المالك", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it; error = null },
            label = { Text("كلمة المرور") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("إلغاء")
            }
            Button(
                onClick = {
                    if (pass == ADMIN_PASS) onSuccess() else error = "كلمة المرور غير صحيحة"
                },
                modifier = Modifier.weight(1f)
            ) { Text("دخول") }
        }
    }
}

@Composable
private fun AdminPanelScreen(onLogout: () -> Unit) {
    val adminButtons = listOf(
        "تعديل الأسعار والكميات",
        "الطلبات المعلّقة (الخدمات)",
        "الكارتات المعلّقة",
        "طلبات شدات ببجي",
        "طلبات شحن الآيتونز",
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("لوحة تحكم المالك", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(adminButtons) { label ->
                ElevatedCard(
                    onClick = {
                        // TODO: اربط كل زر بشاشته/واجهته
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(label)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) { Text("تسجيل الخروج من لوحة المالك") }
    }
}

/* تخزين حالة دخول المالك */
private fun loadAdmin(ctx: Context): Boolean =
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ADMIN, false)

private fun saveAdmin(ctx: Context, value: Boolean) {
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ADMIN, value)
        .apply()
}
