package com.brahma.connect

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.brahma.connect.core.AgentStateStore
import com.brahma.connect.pairing.PairingStorage
import com.brahma.connect.ui.BrahmaConnectApp
import com.brahma.connect.ui.BrahmaMobileApp
import com.brahma.connect.ui.theme.BrahmaConnectTheme

class MainActivity : ComponentActivity() {
    private var pendingServiceStart = false

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        // The UI will react by showing the scanner if permission is granted.
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (pendingServiceStart) {
                pendingServiceStart = false
                startGatewayService()
            }
        } else {
            pendingServiceStart = false
            AgentStateStore.setError("Notification permission is required to keep Brahma-Evo-V3 connected.")
            AgentStateStore.setStatus("Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = PairingStorage(this)
        AgentStateStore.setExperiencePreferences(
            storage.loadVisualTheme(),
            storage.loadStartupEffect(),
            storage.loadVoicePreset(),
            storage.loadSpeakReplies(),
        )
        if (!BuildConfig.IS_STANDALONE) {
            AgentStateStore.setCredential(storage.loadCredential())
            storage.loadGatewayHint()?.let {
                AgentStateStore.setPairingOffer(it)
                AgentStateStore.setGateway(
                    com.brahma.connect.core.GatewayEndpoint(
                        name = "Brahma PC",
                        host = it.host,
                        port = it.port,
                    )
                )
            }
            maybeStartService()
        }
        setContent {
            BrahmaConnectTheme {
                if (BuildConfig.IS_STANDALONE) {
                    BrahmaMobileApp()
                } else {
                    BrahmaConnectApp(
                        onRequestCameraPermission = {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        onRequestNotificationPermission = {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onStartService = { maybeStartService() },
                    )
                }
            }
        }
    }

    private fun maybeStartService() {
        val endpoint = AgentStateStore.gateway.value
        if (endpoint != null || AgentStateStore.credential.value != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingServiceStart = true
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            startGatewayService()
        }
    }

    private fun startGatewayService() {
        val endpoint = AgentStateStore.gateway.value
        if (endpoint != null || AgentStateStore.credential.value != null) {
            val intent = Intent(this, BrahmaConnectForegroundService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }
}
