package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

/** الشاشات المتاحة داخل التطبيق */
private enum class Screen {
    HOME, SERVICES, BALANCE, LEADERS, TIKTOK, INSTAGRAM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    var current by remember { mutableStateOf(Screen.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (current) {
                            Screen.HOME -> "الرئيسية"
                            Screen.SERVICES -> "الخدمات"
                            Screen.BALANCE -> "رصيدي"
                            Screen.LEADERS -> "المتصدرون"
                            Screen.TIKTOK -> "خدمات تيكتوك"
                            Screen.INSTAGRAM -> "خدمات إنستغرام"
                        }
                    )
                },
                navigationIcon = {
                    if (current != Screen.HOME) {
                        TextButton(onClick = { current = Screen.HOME }) {
                            Text("رجوع")
                        }
                    }
                }
            )
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (current) {
                Screen.HOME -> HomeScreen(
                    onOpenServices = { current = Screen.SERVICES },
                    onOpenBalance = { current = Screen.BALANCE },
                    onOpenLeaders = { current = Screen.LEADERS },
                )
                Screen.SERVICES -> ServicesScreen(
                    onOpenTikTok = { current = Screen.TIKTOK },
                    onOpenInstagram = { current = Screen.INSTAGRAM }
                )
                Screen.BALANCE -> BalanceScreen()
                Screen.LEADERS -> LeadersScreen()
                Screen.TIKTOK -> TikTokServicesScreen()
                Screen.INSTAGRAM -> InstagramServicesScreen()
            }
        }
    }
}

/** الشاشة الرئيسية: ثلاث أزرار كبيرة */
@Composable
private fun HomeScreen(
    onOpenServices: () -> Unit,
    onOpenBalance: () -> Unit,
    onOpenLeaders: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("مرحبًا بك 👋", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onOpenServices,
            modifier = Modifier.fillMaxWidth()
        ) { Text("الخدمات") }

        Button(
            onClick = onOpenBalance,
            modifier = Modifier.fillMaxWidth()
        ) { Text("رصيدي") }

        Button(
            onClick = onOpenLeaders,
            modifier = Modifier.fillMaxWidth()
        ) { Text("المتصدرين") }
    }
}

/** شاشة اختيار قسم الخدمات */
@Composable
private fun ServicesScreen(
    onOpenTikTok: () -> Unit,
    onOpenInstagram: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("اختر القسم:", style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = onOpenTikTok,
            modifier = Modifier.fillMaxWidth()
        ) { Text("خدمات تيكتوك") }

        Button(
            onClick = onOpenInstagram,
            modifier = Modifier.fillMaxWidth()
        ) { Text("خدمات إنستغرام") }
    }
}

/** شاشة الرصيد (مبدئيًا – لاحقًا تربطها بالباكند) */
@Composable
private fun BalanceScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("رصيدك سيظهر هنا لاحقًا", style = MaterialTheme.typography.titleMedium)
        Text("اربط الشاشة مع الباكند لعرض الرصيد الحقيقي.")
    }
}

/** شاشة المتصدّرين (مبدئيًا) */
@Composable
private fun LeadersScreen() {
    val dummy = remember {
        listOf(
            "المستخدم 1 – 120$",
            "المستخدم 2 – 95$",
            "المستخدم 3 – 80$"
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("أعلى المنفقين", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(dummy) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        row, Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/** قائمة مبدئية لخدمات تيكتوك */
@Composable
private fun TikTokServicesScreen() {
    val items = remember {
        listOf(
            "متابعين تيكتوك (1000) – 3.50$",
            "لايكات تيكتوك (1000) – 2.20$",
            "مشاهدات تيكتوك (10k) – 1.80$",
            "رفع سكور تيكتوك – 4.00$"
        )
    }
    ServicesList(items)
}

/** قائمة مبدئية لخدمات إنستغرام */
@Composable
private fun InstagramServicesScreen() {
    val items = remember {
        listOf(
            "متابعين إنستغرام (1000) – 4.00$",
            "لايكات إنستغرام (1000) – 2.50$",
            "مشاهدات ريلز (10k) – 2.10$"
        )
    }
    ServicesList(items)
}

@Composable
private fun ServicesList(items: List<String>) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("اختر الخدمة:", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { label ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { /* لاحقًا: افتح تفاصيل الطلب */ }) {
                            Text("اطلب")
                        }
                    }
                }
            }
        }
    }
}
