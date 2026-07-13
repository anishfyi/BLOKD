package dev.anishfyi.blokd.vpn

import dev.anishfyi.blokd.stats.BlocklistMeta
import dev.anishfyi.blokd.stats.HealthStatus
import dev.anishfyi.blokd.stats.ProtectionMode
import dev.anishfyi.blokd.stats.ProtectionState
import dev.anishfyi.blokd.stats.SessionStats
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared state between the VPN service and Compose UI. */
object VpnController {
    val state = MutableStateFlow(ProtectionState())

    @Deprecated("Use state.value.status != HealthStatus.OFF")
    val running = MutableStateFlow(false)

    @Deprecated("Use state.value.stats")
    val stats = MutableStateFlow(dev.anishfyi.blokd.stats.Stats())

    fun publish(
        status: HealthStatus,
        mode: ProtectionMode,
        adGuardEnabled: Boolean,
        blocklist: BlocklistMeta,
        stats: SessionStats,
        upstreamLabel: String,
    ) {
        val protection = ProtectionState(
            status = status,
            mode = mode,
            adGuardEnabled = adGuardEnabled,
            blocklist = blocklist,
            stats = stats,
            upstreamLabel = upstreamLabel,
        )
        state.value = protection
        running.value = status != HealthStatus.OFF && status != HealthStatus.ERROR
        this.stats.value = dev.anishfyi.blokd.stats.Stats(stats.blocked, stats.allowed)
    }
}
