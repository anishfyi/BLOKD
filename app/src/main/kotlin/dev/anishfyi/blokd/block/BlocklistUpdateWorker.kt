package dev.anishfyi.blokd.block

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.anishfyi.blokd.dns.DnsFilter
import dev.anishfyi.blokd.dns.DnsPreferences
import dev.anishfyi.blokd.stats.ProtectionMode
import dev.anishfyi.blokd.vpn.VpnController

class BlocklistUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val filter = DnsFilter()
        val repo = BlocklistRepository(applicationContext, filter)
        val mode = DnsPreferences.protectionMode(applicationContext)
        val count = repo.update(mode)
        if (count < 1_000) return Result.retry()
        val current = VpnController.state.value
        VpnController.publish(
            status = current.status,
            mode = mode,
            adGuardEnabled = current.adGuardEnabled,
            blocklist = repo.meta(),
            stats = current.stats,
            upstreamLabel = current.upstreamLabel,
        )
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "blokd_blocklist_update"
    }
}
