package dev.anishfyi.blokd.ui

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.anishfyi.blokd.vpn.BlokdVpnService

/**
 * Hosts the Compose UI and owns the two system dialogs BLOKD needs: the VPN
 * consent prompt and, on Android 13 and up, the notification permission.
 */
class MainActivity : ComponentActivity() {

    private val prepareVpn = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) startVpn()
    }

    private val requestNotify = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored; the VPN still runs without notification permission */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            BlokdApp(
                onToggle = { enabled -> if (enabled) enableVpn() else disableVpn() },
            )
        }
    }

    private fun enableVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) prepareVpn.launch(intent) else startVpn()
    }

    private fun startVpn() {
        startForegroundService(
            Intent(this, BlokdVpnService::class.java).setAction(BlokdVpnService.ACTION_START),
        )
    }

    private fun disableVpn() {
        startService(
            Intent(this, BlokdVpnService::class.java).setAction(BlokdVpnService.ACTION_STOP),
        )
    }
}
