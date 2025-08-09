
package com.birtvisi.bridge

import android.app.*
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class BridgeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var client: OkHttpClient

    private var getEndpoint: String = ""
    private var logEndpoint: String = ""
    private var token: String = ""
    private var device: String = ""
    private var espBase: String = ""
    private var intervalSec: Int = 2

    companion object {
        var isRunning: Boolean = false
        private const val CHANNEL_ID = "bridge_channel"
        private fun log(msg: String) {
            // In this simplified build we omit UI logging to keep code minimal.
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        createChannel()
        startForeground(1, buildNotification("Running"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getEndpoint = intent?.getStringExtra("getEndpoint") ?: ""
        logEndpoint = intent?.getStringExtra("logEndpoint") ?: ""
        token = intent?.getStringExtra("token") ?: ""
        device = intent?.getStringExtra("device") ?: ""
        espBase = intent?.getStringExtra("espBase") ?: ""
        intervalSec = intent?.getIntExtra("intervalSec", 2) ?: 2

        scope.launch {
            while (isActive) {
                try { pollOnce() } catch (_: Exception) {}
                delay((intervalSec * 1000).toLong())
            }
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Bridge", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Birtvisi Bridge")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun httpGet(url: String): String {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            return resp.body?.string()?.trim() ?: ""
        }
    }

    private suspend fun pollOnce() {
        if (getEndpoint.isBlank() || token.isBlank() || device.isBlank() || espBase.isBlank()) return
        val sep = if ("?" in getEndpoint) "&" else "?"
        val url = "${getEndpoint}${sep}token=${token}&device=${device}"
        val cmd = runCatching { httpGet(url) }.getOrNull() ?: return
        if (cmd.isBlank() || cmd == "none") return

        when (cmd) {
            "unlock-1" -> triggerEsp("/unlock-1", "1")
            "unlock-2" -> triggerEsp("/unlock-2", "2")
        }
    }

    private fun triggerEsp(path: String, door: String) {
        runCatching { httpGet("${espBase}${path}") }
        if (logEndpoint.isNotBlank()) {
            val sep = if ("?" in logEndpoint) "&" else "?"
            runCatching { httpGet("${logEndpoint}${sep}device=${device}&door=${door}") }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
    }
}
