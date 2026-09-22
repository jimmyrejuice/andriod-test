package com.example.quickswitch

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.nfc.NfcManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

const val GITHUB_URL = "https://github.com/jimmyrejuice/andriod-test/"
private const val PREFS_NAME = "quickswitch_prefs"
private const val KEY_ACCENT = "accent_key"

// ---------------- 主题色 ----------------

data class AccentOption(val key: String, val label: String, val color: Color)

val accentOptions = listOf(
    AccentOption("blue", "默认蓝", Color(0xFF1565C0)),
    AccentOption("purple", "紫罗兰", Color(0xFF6750A4)),
    AccentOption("green", "草木绿", Color(0xFF2E7D32)),
    AccentOption("orange", "落日橙", Color(0xFFE65100)),
    AccentOption("pink", "樱花粉", Color(0xFFC2185B)),
)

fun buildScheme(accentKey: String): ColorScheme {
    val primary = accentOptions.firstOrNull { it.key == accentKey }?.color ?: accentOptions[0].color
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = lerp(primary, Color.White, 0.85f),
        onPrimaryContainer = lerp(primary, Color.Black, 0.25f),
        secondary = primary,
        onSecondary = Color.White,
        secondaryContainer = lerp(primary, Color.White, 0.85f),
        onSecondaryContainer = lerp(primary, Color.Black, 0.25f),
        tertiary = primary,
        onTertiary = Color.White,
    )
}

// ---------------- Activity ----------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        setContent { QuickSwitchApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSwitchApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var accentKey by remember { mutableStateOf(prefs.getString(KEY_ACCENT, "blue") ?: "blue") }
    var tab by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = buildScheme(accentKey)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    accentKey = accentKey,
                    onAccentChange = { key ->
                        accentKey = key
                        prefs.edit().putString(KEY_ACCENT, key).apply()
                    },
                    onExportClick = {
                        val ok = SleepStorage.exportCsv(context)
                        Toast.makeText(
                            context,
                            if (ok) "已导出到 Downloads/sleep/sleep_data.csv"
                            else "导出失败，请重试",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    closeDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("快捷开关") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Text("☰", fontSize = 22.sp)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TabRow(selectedTabIndex = tab) {
                        Tab(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            text = { Text("开关") }
                        )
                        Tab(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            text = { Text("睡眠状态") }
                        )
                    }
                    when (tab) {
                        0 -> SwitchScreen()
                        else -> SleepScreen()
                    }
                }
            }
        }
    }
}

// ---------------- 抽屉 ----------------

@Composable
fun DrawerContent(
    accentKey: String,
    onAccentChange: (String) -> Unit,
    onExportClick: () -> Unit,
    closeDrawer: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet {
        Spacer(Modifier.height(24.dp))
        Text(
            "快捷开关",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()

        // ---- 主题 ----
        Column(Modifier.padding(16.dp)) {
            Text("主题", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "选择强调色",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                accentOptions.forEach { option ->
                    val selected = option.key == accentKey
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable { onAccentChange(option.key) }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // ---- 导出 ----
        NavigationDrawerItem(
            label = { Text("导出睡眠数据") },
            selected = false,
            onClick = {
                onExportClick()
                closeDrawer()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        HorizontalDivider()

        // ---- 关于 ----
        Column(Modifier.padding(16.dp)) {
            Text("关于", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("当前版本", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { openUrl(context, GITHUB_URL) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GitHub 项目", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "打开 ›",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ---------------- 开关页 ----------------

@Composable
fun SwitchScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var refreshTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val wifiOn = remember(refreshTick) { isWifiEnabled(context) }
    val mobileOn = remember(refreshTick) { isMobileDataEnabled(context) }
    val nfcOn = remember(refreshTick) { isNfcEnabled(context) }
    val btOn = remember(refreshTick) { isBluetoothEnabled(context) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openBluetoothSettings(context)
        } else {
            Toast.makeText(context, "未授权，无法读取蓝牙状态", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "睡前开关",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))

                SwitchItem("Wi-Fi", wifiOn) { openInternetPanel(context) }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchItem("移动网络", mobileOn) { openInternetPanel(context) }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchItem("蓝牙", btOn) {
                    val needPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                    if (needPermission) {
                        btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        openBluetoothSettings(context)
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchItem("NFC", nfcOn) { openNfcSettings(context) }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchItem("省电模式", null) { openBatterySaverSettings(context) }
            }
        }
    }
}

@Composable
private fun SwitchItem(
    title: String,
    isOn: Boolean?,
    onAction: () -> Unit
) {
    val dot = if (isOn == true) "🟢" else "⚪"
    val stateText = when (isOn) {
        true -> "已开启"
        false -> "已关闭"
        null -> "无法读取，请手动确认"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = dot, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = stateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onAction) { Text("去设置") }
    }
}

// ---------------- 状态读取 ----------------

@Suppress("DEPRECATION")
private fun isWifiEnabled(context: Context): Boolean {
    return try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.isWifiEnabled
    } catch (e: Exception) {
        false
    }
}

private fun isMobileDataEnabled(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    } catch (e: Exception) {
        false
    }
}

private fun isNfcEnabled(context: Context): Boolean {
    return try {
        val nm = context.getSystemService(Context.NFC_SERVICE) as NfcManager
        nm.defaultAdapter?.isEnabled == true
    } catch (e: Exception) {
        false
    }
}

private fun isBluetoothEnabled(context: Context): Boolean? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            null
        } else {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.isEnabled == true
        }
    } catch (e: Exception) {
        null
    }
}

// ---------------- 跳转 ----------------

private fun openInternetPanel(context: Context) {
    try {
        val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "请在面板中关闭 Wi-Fi / 移动网络", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openBluetoothSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "请在设置中关闭蓝牙", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开蓝牙设置", Toast.LENGTH_SHORT).show()
    }
}

private fun openNfcSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "请在设置中关闭 NFC", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开 NFC 设置", Toast.LENGTH_SHORT).show()
    }
}

private fun openBatterySaverSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "请开启省电模式", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开电池设置", Toast.LENGTH_SHORT).show()
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}
