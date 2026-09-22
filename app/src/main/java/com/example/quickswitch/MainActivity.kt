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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val GITHUB_URL = "https://github.com/jimmyrejuice/andriod-test/"
private const val PREFS_NAME = "quickswitch_prefs"
private const val KEY_ACCENT = "accent_key"

// ---------------- 主题色 ----------------

private data class AccentOption(val key: String, val label: String, val color: Color)

private val accentOptions = listOf(
    AccentOption("blue", "默认蓝", Color(0xFF1565C0)),
    AccentOption("purple", "紫罗兰", Color(0xFF6750A4)),
    AccentOption("green", "草木绿", Color(0xFF2E7D32)),
    AccentOption("orange", "落日橙", Color(0xFFE65100)),
    AccentOption("pink", "樱花粉", Color(0xFFC2185B)),
)

private fun buildScheme(accentKey: String): ColorScheme {
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

    MaterialTheme(colorScheme = buildScheme(accentKey)) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("快捷开关") }) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("开关") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("菜单") })
                }
                when (tab) {
                    0 -> SwitchScreen()
                    else -> MenuScreen(
                        accentKey = accentKey,
                        onAccentChange = { key ->
                            accentKey = key
                            prefs.edit().putString(KEY_ACCENT, key).apply()
                        }
                    )
                }
            }
        }
    }
}

// ---------------- 开关页 ----------------

@Composable
fun SwitchScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 从系统设置页返回时自动刷新状态
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text("睡前开关", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "依次关闭 Wi-Fi、移动网络、蓝牙、NFC，并开启省电模式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SwitchItem(
                    title = "Wi-Fi",
                    isOn = wifiOn,
                    onAction = { openInternetPanel(context) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchItem(
                    title = "移动网络",
                    isOn = mobileOn,
                    onAction = { openInternetPanel(context) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchItem(
                    title = "蓝牙",
                    isOn = btOn,
                    onAction = {
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
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchItem(
                    title = "NFC",
                    isOn = nfcOn,
                    onAction = { openNfcSettings(context) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwitchItem(
                    title = "省电模式",
                    isOn = null,
                    onAction = { openBatterySaverSettings(context) }
                )
            }
        }
    }
}

/**
 * 一行开关项。isOn = true 显示绿色圆，false 显示灰色圆，null 表示无法读取。
 */
@Composable
private fun SwitchItem(
    title: String,
    isOn: Boolean?,
    onAction: () -> Unit
) {
    val dot = when (isOn) {
        true -> "🟢"
        else -> "⚪"
    }
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

// ---------------- 菜单页 ----------------

@Composable
fun MenuScreen(
    accentKey: String,
    onAccentChange: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- 主题 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("主题", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "选择强调色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    accentOptions.forEach { option ->
                        val selected = option.key == accentKey
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                            Spacer(Modifier.height(6.dp))
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
        }

        // ---- 关于 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("当前版本", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyLarge,
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
                    Text("GitHub 项目", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "打开 ›",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = GITHUB_URL,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
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

/** 返回 null 表示没有 BLUETOOTH_CONNECT 权限，读不到状态。 */
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

// ---------------- 跳转动作 ----------------

private fun openInternetPanel(context: Context) {
    try {
        val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "请在面板中关闭 Wi-Fi / 移动网络", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        openWirelessSettings(context)
    }
}

private fun openWirelessSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开设置", Toast.LENGTH_SHORT).show()
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
