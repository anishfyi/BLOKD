package dev.anishfyi.blokd.vpn

import dev.anishfyi.blokd.stats.Stats
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared, observable state bridging the running VpnService and the Compose UI. */
object VpnController {
    val running = MutableStateFlow(false)
    val stats = MutableStateFlow(Stats())
}
