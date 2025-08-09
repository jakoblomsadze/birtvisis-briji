
package com.birtvisi.bridge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("bridge", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                var getEndpoint by remember { mutableStateOf(prefs.getString("getEndpoint", "")) }
                var logEndpoint by remember { mutableStateOf(prefs.getString("logEndpoint", "")) }
                var token by remember { mutableStateOf(prefs.getString("token", "SECRET123")) }
                var device by remember { mutableStateOf(prefs.getString("device", "esp1")) }
                var espBase by remember { mutableStateOf(prefs.getString("espBase", "http://192.168.86.18")) }
                var intervalSec by remember { mutableStateOf(prefs.getInt("intervalSec", 2)) }
                var running by remember { mutableStateOf(BridgeService.isRunning) }

                fun save() {
                    prefs.edit().apply {
                        putString("getEndpoint", getEndpoint)
                        putString("logEndpoint", logEndpoint)
                        putString("token", token)
                        putString("device", device)
                        putString("espBase", espBase)
                        putInt("intervalSec", intervalSec)
                    }.apply()
                }

                fun start() {
                    save()
                    val i = Intent(this@MainActivity, BridgeService::class.java).apply {
                        putExtra("getEndpoint", getEndpoint ?: "")
                        putExtra("logEndpoint", logEndpoint ?: "")
                        putExtra("token", token ?: "")
                        putExtra("device", device ?: "")
                        putExtra("espBase", espBase ?: "")
                        putExtra("intervalSec", intervalSec)
                    }
                    ContextCompat.startForegroundService(this@MainActivity, i)
                    running = true
                }

                fun stop() {
                    stopService(Intent(this@MainActivity, BridgeService::class.java))
                    running = false
                }

                Scaffold(topBar = { TopAppBar(title = { Text("Birtvisi Bridge") }) }) { inner ->
                    Column(Modifier.padding(inner).padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = getEndpoint ?: "", onValueChange = { getEndpoint = it }, label = { Text("Server get.php URL") }, singleLine = true)
                        OutlinedTextField(value = logEndpoint ?: "", onValueChange = { logEndpoint = it }, label = { Text("Server log.php URL (optional)") }, singleLine = true)
                        OutlinedTextField(value = token ?: "", onValueChange = { token = it }, label = { Text("Token") }, singleLine = true)
                        OutlinedTextField(value = device ?: "", onValueChange = { device = it }, label = { Text("Device") }, singleLine = true)
                        OutlinedTextField(value = espBase ?: "", onValueChange = { espBase = it }, label = { Text("ESP Base URL (http://192.168.x.x)") }, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Interval (sec)")
                            Slider(value = intervalSec.toFloat(), onValueChange = { intervalSec = it.toInt().coerceIn(1, 10) }, valueRange = 1f..10f, steps = 8)
                            Text("${intervalSec}s")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { if (!running) start() else stop() }) {
                                Text(if (running) "Stop Bridge" else "Start Bridge")
                            }
                            Button(onClick = { save() }) { Text("Save") }
                        }
                        Text("Open your Netlify page to send commands. This app forwards them to ESP when found on the server.")
                    }
                }
            }
        }
    }
}
