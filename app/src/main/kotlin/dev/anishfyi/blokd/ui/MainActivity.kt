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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.anishfyi.blokd.block.BlocklistRepository
import dev.anishfyi.blokd.block.BlocklistUpdateWorker
import dev.anishfyi.blokd.dns.DnsFilter
import dev.anishfyi.blokd.dns.DnsPreferences
import dev.anishfyi.blokd.stats.HealthStatus
import dev.anishfyi.blokd.vpn.BlokdVpnService
import dev.anishfyi.blokd.vpn.VpnController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var keepSplashVisible = true

    private val prepareVpn = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) startVpn()
    }

    private val requestNotify = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSplashVisible }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            var ready by remember { mutableStateOf(false) }
            if (!ready) {
                BlokdSplashScreen()
            } else {
                BlokdApp(
                    onToggle = { enabled -> if (enabled) enableVpn() else disableVpn() },
                    onSettingsChanged = ::restartVpnIfRunning,
                    onUpdateLists = ::enqueueBlocklistUpdate,
                )
            }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) { preloadBlocklists() }
                ready = true
                keepSplashVisible = false
            }
        }
    }

    private fun preloadBlocklists() {
        val filter = DnsFilter()
        val repo = BlocklistRepository(this, filter)
        val mode = DnsPreferences.protectionMode(this)
        val count = repo.loadForMode(mode)
        val current = VpnController.state.value
        VpnController.publish(
            status = current.status,
            mode = mode,
            adGuardEnabled = DnsPreferences.isAdGuardEnabled(this),
            blocklist = repo.meta().copy(domainCount = count),
            stats = current.stats,
            upstreamLabel = current.upstreamLabel,
        )
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

    private fun restartVpnIfRunning() {
        val status = VpnController.state.value.status
        if (status == HealthStatus.OFF || status == HealthStatus.ERROR) return
        startService(
            Intent(this, BlokdVpnService::class.java).setAction(BlokdVpnService.ACTION_RESTART),
        )
    }

    private fun enqueueBlocklistUpdate() {
        val request = OneTimeWorkRequestBuilder<BlocklistUpdateWorker>().build()
        WorkManager.getInstance(this).enqueue(request)
    }
}
