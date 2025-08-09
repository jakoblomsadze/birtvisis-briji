package com.birtvisi.bridge

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class MainActivity : ComponentActivity() {

    private fun insecureOkHttp(): OkHttpClient {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val ssl = SSLContext.getInstance("TLS")
        ssl.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
        return OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true } // self-signed OK (ლოკალისთვის)
            .build()
    }

    private val client by lazy { insecureOkHttp() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var base by remember { mutableStateOf("https://192.168.86.18") }
                var last by remember { mutableStateOf("") }
                val scope = remember { CoroutineScope(Dispatchers.IO) }

                fun hit(path: String) {
                    scope.launch {
                        val url = "$base$path"
                        val req = Request.Builder().url(url).get().build()
                        runCatching { client.newCall(req).execute().use { it.body?.string()?.trim().orEmpty() } }
                            .onSuccess { resp ->
                                last = "GET $url → ${resp.ifEmpty { "<empty>" }}"
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, resp.ifEmpty { "no body" }, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .onFailure { e ->
                                last = "GET $url → ERROR: ${e.message}"
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }

                Scaffold(topBar = { TopAppBar(title = { Text("ESP HTTPS Buttons") }) }) { inner ->
                    Column(
                        Modifier.padding(inner).padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = base,
                            onValueChange = { base = it },
                            label = { Text("ESP Base (HTTPS)") },
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { hit("/unlock-1") }) { Text("Unlock 1") }
                            Button(onClick = { hit("/unlock-2") }) { Text("Unlock 2") }
                        }
                        Text("Last: $last")
                    }
                }
            }
        }
    }
}
