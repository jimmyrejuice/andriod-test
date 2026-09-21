package com.example.quickswitch

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BedtimeSwitchScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedtimeSwitchScreen() {
    val context = LocalContext.current

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestDisableBluetooth(context)
        } else {
            Toast.makeText(context, "需要蓝牙权限才能打开蓝牙设置", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndDisableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                requestDisableBluetooth(context)
            } else {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            requestDisableBluetooth(context)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("快捷开关") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("睡前开关", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "依次关闭 Wi-Fi、移动网络、蓝牙、NFC，并开启省电模式",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = { openInternetPanel(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("关闭 Wi-Fi / 移动网络")
            }

            Button(
                onClick = { checkAndDisableBluetooth() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("关闭蓝牙")
            }

            Button(
                onClick = { openNfcSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("关闭 NFC")
            }

            Button(
                onClick = { openBatterySaverSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开启省电模式")
            }
        }
    }
}

fun openInternetPanel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        context.startActivity(panelIntent)
    }
}

fun requestDisableBluetooth(context: Context) {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    if (bluetoothAdapter?.isEnabled == true) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "请在设置中关闭蓝牙", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "蓝牙已关闭", Toast.LENGTH_SHORT).show()
    }
}

fun openNfcSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NFC_SETTINGS)
    context.startActivity(intent)
}

fun openBatterySaverSettings(context: Context) {
    val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    context.startActivity(intent)
}
