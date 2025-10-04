package com.zafer.smm

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

private enum class Screen {
    WELCOME, SERVICES, ORDERS, BALANCE, REFERRAL, LEADERS, ADMIN
}

@Composable
private fun AppRoot() {
    var current by remember { mutableStateOf(Screen.WELCOME) }
    var showAdminLogin by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when (current) {
            Screen.WELCOME -> WelcomeScreen(
                onOpenServices = { current = Screen.SERVICES },
                onOpenOrders   = { current = Screen.ORDERS },
                onOpenBalance  = { current = Screen.BALANCE },
                onOpenReferral = { current = Screen.REFERRAL },
                onOpenLeaders  = { current = Screen.LEADERS },
                onOpenAdmin    = { showAdminLogin = true },
            )

            Screen.SERVICES -> PlaceholderScreen(
                title = "الخدمات",
                onBack = { current = Screen.WELCOME }
            )

            Screen.ORDERS -> PlaceholderScreen(
                title = "طلباتي",
                onBack = { current = Screen.WELCOME }
            )

            Screen.BALANCE -> PlaceholderScreen(
                title = "رصيدي",
                onBack = { current = Screen.WELCOME }
            )

            Screen.REFERRAL -> PlaceholderScreen(
                title = "الإحالة",
                onBack = { current = Screen.WELCOME }
            )

            Screen.LEADERS -> PlaceholderScreen(
                title = "المتصدرون 🎉",
                onBack = { current = Screen.WELCOME }
            )

            Screen.ADMIN -> AdminPanelScreen(
                onBack = { current = Screen.WELCOME }
            )
        }

        if (showAdminLogin) {
            AdminLoginDialog(
                onDismiss = { showAdminLogin = false },
                onSuccess = {
                    showAdminLogin = false
                    current = Screen.ADMIN
                }
            )
        }
    }
}

/** شاشة الترحيب + الأزرار الرئيسية */
@Composable
private fun WelcomeScreen(
    onOpenServices: () -> Unit,
    onOpenOrders:   () -> Unit,
    onOpenBalance:  () -> Unit,
    onOpenReferral: () -> Unit,
    onOpenLeaders:  () -> Unit,
    onOpenAdmin:    () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        Text(
            "أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        MainButton("الخدمات", onClick = onOpenServices)
        MainButton("طلباتي", onClick = onOpenOrders)
        MainButton("رصيدي", onClick = onOpenBalance)
        MainButton("الإحالة", onClick = onOpenReferral)
        MainButton("المتصدرين 🎉", onClick = onOpenLeaders)
        MainButton("دخول المالك", onClick = onOpenAdmin)
    }
}

/** نافذة إدخال كلمة مرور المالك (2000) */
@Composable
private fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ElevatedButton(onClick = {
                if (pass.trim() == "2000") {
                    error = null
                    onSuccess()
                } else {
                    error = "كلمة المرور غير صحيحة"
                }
            }) { Text("دخول") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        },
        title = { Text("دخول المالك") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

/** لوحة تحكم المالك مع أزرار مرتبة في قائمة قابلة للتمرير */
@Composable
private fun AdminPanelScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current

    val items = listOf(
        "تعديل الأسعار والكميات",
        "الطلبات المعلّقة (الخدمات)",
        "الكارتات المعلّقة",
        "طلبات شدّات ببجي",
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

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("لوحة تحكم المالك", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onBack) { Text("خروج") }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(items) { label ->
                ElevatedButton(
                    onClick = {
                        Toast
                            .makeText(ctx, "$label — سيتم ربطها بالباكند لاحقًا", Toast.LENGTH_SHORT)
                            .show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** زر رئيسي موحّد الشكل */
@Composable
private fun MainButton(text: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** شاشة مؤقتة إلى حين ربط الأقسام بالباكند */
@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("$title — سيتم ربط هذه الشاشة بالباكند لاحقًا.", textAlign = TextAlign.Center)
        OutlinedButton(onClick = onBack) { Text("رجوع") }
    }
}
