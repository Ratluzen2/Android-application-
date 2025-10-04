package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    var current by remember { mutableStateOf(Screen.WELCOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("خدمات راتلوزن") },
                navigationIcon = {
                    if (current != Screen.WELCOME) {
                        TextButton(onClick = { current = Screen.WELCOME }) {
                            Text("رجوع")
                        }
                    }
                }
            )
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (current) {
                Screen.WELCOME -> WelcomeScreen(
                    onOpenServices = { current = Screen.SERVICES },
                    onOpenOrders   = { current = Screen.ORDERS },
                    onOpenBalance  = { current = Screen.BALANCE },
                    onOpenReferral = { current = Screen.REFERRAL },
                    onOpenLeaders  = { current = Screen.LEADERS },
                    onOpenAdmin    = { current = Screen.ADMIN },
                )
                Screen.SERVICES -> PlaceholderScreen("الخدمات")
                Screen.ORDERS   -> PlaceholderScreen("طلباتي")
                Screen.BALANCE  -> PlaceholderScreen("رصيدي")
                Screen.REFERRAL -> PlaceholderScreen("الإحالة")
                Screen.LEADERS  -> PlaceholderScreen("المتصدرون 🎉")
                Screen.ADMIN    -> PlaceholderScreen("دخول المالك")
            }
        }
    }
}

/** شاشة الترحيب + الأزرار الرئيسية بشكل أنيق */
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

        // أزرار رئيسية كبيرة ومرتّبة
        MainButton("الخدمات", onClick = onOpenServices)
        MainButton("طلباتي", onClick = onOpenOrders)
        MainButton("رصيدي", onClick = onOpenBalance)
        MainButton("الإحالة", onClick = onOpenReferral)
        MainButton("المتصدرين 🎉", onClick = onOpenLeaders)
        MainButton("دخول المالك", onClick = onOpenAdmin)
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

/** شاشة مؤقتة إلى أن تربط كل قسم بالباكند لاحقًا */
@Composable
private fun PlaceholderScreen(title: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("$title — سيتم ربط هذه الشاشة بالباكند لاحقًا.", textAlign = TextAlign.Center)
    }
}
