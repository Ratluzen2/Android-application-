package com.zafer.smm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*** البيانات — كتالوج الخدمات ***/
data class Service(val id: Int, val title: String, val price: Double)
data class Section(val key: String, val title: String, val services: List<Service>)

private val catalog: List<Section> = listOf(
    Section(
        key = "followers", title = "قسم المتابعين",
        services = listOf(
            Service(1, "متابعين تيكتوك 100", 1.0),
            Service(2, "متابعين تيكتوك 200", 2.0),
            Service(3, "متابعين تيكتوك 300", 3.0),
            Service(4, "متابعين تيكتوك 400", 4.0),
            Service(5, "متابعين تيكتوك 500", 5.0),
            Service(6, "متابعين تيكتوك 1000", 9.0),
            Service(7, "متابعين تيكتوك 2000", 18.0),
            Service(8, "متابعين تيكتوك 3000", 27.0),
            Service(9, "متابعين تيكتوك 4000", 36.0),
            Service(10, "متابعين تيكتوك 5000", 45.0)
        )
    ),
    Section(
        key = "likes", title = "قسم الإعجابات",
        services = listOf(
            Service(1, "لايكات 1k", 2.5),
            Service(2, "لايكات 2k", 5.0),
            Service(3, "لايكات 3k", 7.5),
            Service(4, "لايكات 4k", 10.0),
            Service(5, "لايكات 5k", 12.5)
        )
    ),
    Section(
        key = "views", title = "قسم المشاهدات",
        services = listOf(
            Service(1, "مشاهدات تيكتوك 1k", 0.5),
            Service(2, "مشاهدات تيكتوك 2k", 1.0),
            Service(3, "مشاهدات تيكتوك 3k", 1.5),
            Service(4, "مشاهدات تيكتوك 4k", 2.0),
            Service(5, "مشاهدات تيكتوك 5k", 2.5),
            Service(6, "مشاهدات تيكتوك 10k", 4.5)
        )
    ),
    Section(
        key = "live", title = "قسم مشاهدات البث المباشر",
        services = listOf(
            Service(1, "مشاهدات بث مباشر 1k", 3.0),
            Service(2, "مشاهدات بث مباشر 2k", 6.0),
            Service(3, "مشاهدات بث مباشر 3k", 9.0),
            Service(4, "مشاهدات بث مباشر 4k", 12.0),
            Service(5, "مشاهدات بث مباشر 5k", 15.0)
        )
    ),
    Section(
        key = "pubg", title = "قسم شحن شدات ببجي",
        services = listOf(
            Service(1, "ببجي 60 UC", 1.2),
            Service(2, "ببجي 120 UC", 2.3),
            Service(3, "ببجي 180 UC", 3.5),
            Service(4, "ببجي 240 UC", 4.7),
            Service(5, "ببجي 325 UC", 6.0),
            Service(6, "ببجي 660 UC", 11.5),
            Service(7, "ببجي 1800 UC", 30.0)
        )
    ),
    Section(
        key = "itunes", title = "قسم شراء رصيد ايتونز",
        services = listOf(
            Service(1, "بطاقة iTunes $5", 4.9),
            Service(2, "بطاقة iTunes $10", 9.7),
            Service(3, "بطاقة iTunes $15", 14.4),
            Service(4, "بطاقة iTunes $20", 19.0),
            Service(5, "بطاقة iTunes $25", 23.7),
            Service(6, "بطاقة iTunes $50", 47.0)
        )
    ),
    Section(
        key = "telegram", title = "قسم خدمات التليجرام",
        services = listOf(
            Service(1, "أعضاء قناة 1k", 9.0),
            Service(2, "أعضاء قناة 2k", 17.5),
            Service(3, "أعضاء قناة 3k", 25.0),
            Service(4, "أعضاء كروب 1k", 10.0),
            Service(5, "أعضاء كروب 2k", 19.0)
        )
    ),
    Section(
        key = "ludo", title = "قسم خدمات اللودو",
        services = listOf(
            Service(1, "لودو 100 ألماسة", 0.9),
            Service(2, "لودو 200 ألماسة", 1.7),
            Service(3, "لودو 500 ألماسة", 4.1),
            Service(4, "لودو 1000 ألماسة", 8.0),
            Service(5, "لودو 2000 ألماسة", 15.5)
        )
    ),
    Section(
        key = "mobile", title = "قسم شراء رصيد الهاتف",
        services = listOf(
            Service(1, "شراء رصيد 2$ أثير", 2.0),
            Service(2, "شراء رصيد 5$ أثير", 5.0),
            Service(3, "شراء رصيد 10$ أثير", 10.0),
            Service(4, "شراء رصيد 20$ أثير", 20.0),
            Service(5, "شراء رصيد 40$ أثير", 40.0),
            Service(6, "شراء رصيد 2$ آسيا", 2.0),
            Service(7, "شراء رصيد 5$ آسيا", 5.0),
            Service(8, "شراء رصيد 10$ آسيا", 10.0),
            Service(9, "شراء رصيد 20$ آسيا", 20.0),
            Service(10, "شراء رصيد 40$ آسيا", 40.0),
            Service(11, "شراء رصيد 2$ كورك", 2.0),
            Service(12, "شراء رصيد 5$ كورك", 5.0),
            Service(13, "شراء رصيد 10$ كورك", 10.0),
            Service(14, "شراء رصيد 20$ كورك", 20.0),
            Service(15, "شراء رصيد 40$ كورك", 40.0)
        )
    )
)

/*** DataStore: جلسة المالك ***/
private val KEY_OWNER = booleanPreferencesKey("is_owner")
private val android.content.Context.ownerDataStore by preferencesDataStore(name = "owner_session")

/*** النشاط ***/
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmmRoot() }
    }
}

/*** التنقّل ***/
sealed class Screen {
    data object HOME : Screen()
    data object SERVICES : Screen()
    data class ServiceList(val key: String) : Screen()
    data object ADMIN_LOGIN : Screen()
    data object ADMIN_DASHBOARD : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmmRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isOwner by remember { mutableStateOf(false) }
    // لا نستخدم rememberSaveable هنا لتفادي تعقيد Saver للـ sealed class
    var current by remember { mutableStateOf<Screen>(Screen.HOME) }

    LaunchedEffect(Unit) {
        val saved = context.ownerDataStore.data.first()[KEY_OWNER] ?: false
        isOwner = saved
        if (isOwner) current = Screen.ADMIN_DASHBOARD
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (current == Screen.ADMIN_DASHBOARD) "لوحة تحكم المالك" else "خدمات راتلوزن"
                        )
                    },
                    actions = {
                        Text(
                            text = if (isOwner) "خروج المالك" else "دخول المالك",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable {
                                    if (isOwner) {
                                        scope.launch {
                                            context.ownerDataStore.edit { it[KEY_OWNER] = false }
                                            isOwner = false
                                            current = Screen.HOME
                                        }
                                    } else {
                                        current = Screen.ADMIN_LOGIN
                                    }
                                }
                        )
                    },
                    colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (val scr = current) {
                    Screen.HOME -> WelcomeScreen(
                        onServices = { current = Screen.SERVICES },
                        onOrders = { /* لاحقًا */ },
                        onBalance = { /* لاحقًا */ },
                        onReferral = { /* لاحقًا */ },
                        onLeaders = { /* لاحقًا */ }
                    )

                    Screen.SERVICES -> ServicesScreen(
                        sections = catalog,
                        onBack = { current = Screen.HOME },
                        onOpenSection = { current = Screen.ServiceList(it.key) }
                    )

                    is Screen.ServiceList -> {
                        val section = catalog.firstOrNull { it.key == scr.key }
                        if (section == null) {
                            Text("القسم غير موجود", modifier = Modifier.align(Alignment.Center))
                        } else {
                            ServiceList(
                                section = section,
                                onBack = { current = Screen.SERVICES },
                                onOrder = { /* تنفيذ الطلب لاحقًا */ }
                            )
                        }
                    }

                    Screen.ADMIN_LOGIN -> OwnerLoginDialog(
                        onCancel = { current = if (isOwner) Screen.ADMIN_DASHBOARD else Screen.HOME },
                        onSubmit = { pass ->
                            if (pass == "2000") {
                                scope.launch {
                                    context.ownerDataStore.edit { it[KEY_OWNER] = true }
                                    isOwner = true
                                    current = Screen.ADMIN_DASHBOARD
                                }
                            } else {
                                current = Screen.HOME
                            }
                        }
                    )

                    Screen.ADMIN_DASHBOARD -> AdminDashboard(
                        onBack = { current = Screen.HOME }
                    )
                }
            }
        }
    }
}

/*** الشاشات ***/
@Composable
fun WelcomeScreen(
    onServices: () -> Unit,
    onOrders: () -> Unit,
    onBalance: () -> Unit,
    onReferral: () -> Unit,
    onLeaders: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "أهلًا وسهلًا بكم في تطبيق خدمات راتلوزن",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(16.dp))
        VerticalButtons(
            items = listOf(
                "الخدمات" to onServices,
                "طلباتي" to onOrders,
                "رصيدي" to onBalance,
                "الإحالة" to onReferral,
                "المتصدرين 🎉" to onLeaders,
            )
        )
    }
}

@Composable
fun ServicesScreen(
    sections: List<Section>,
    onBack: () -> Unit,
    onOpenSection: (Section) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("الخدمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(sections) { sec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSection(sec) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(sec.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("اضغط لعرض ${sec.services.size} خدمة", fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun ServiceList(
    section: Section,
    onBack: () -> Unit,
    onOrder: (Service) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(section.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(section.services) { svc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(svc.title, fontWeight = FontWeight.Medium)
                            Text("${svc.price} $", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { onOrder(svc) }) { Text("طلب الخدمة") }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun AdminDashboard(onBack: () -> Unit) {
    val entries = listOf(
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("لوحة تحكم المالك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(entries) { title ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) { Text(title, modifier = Modifier.padding(14.dp), fontSize = 16.sp) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}

@Composable
fun VerticalButtons(items: List<Pair<String, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        items.forEach { (label, onClick) ->
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
    }
}

@Composable
fun OwnerLoginDialog(onCancel: () -> Unit, onSubmit: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("دخول المالك") },
        text = {
            Column {
                Text("أدخل كلمة المرور الخاصة بالمالك")
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    placeholder = { Text("****") }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(pass) }) { Text("دخول") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("إلغاء") } }
    )
}
