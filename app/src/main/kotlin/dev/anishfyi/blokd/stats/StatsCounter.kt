package dev.anishfyi.blokd.stats

import java.util.concurrent.atomic.AtomicLong

/** Running totals for the session, safe to update from the packet thread. */
data class Stats(val blocked: Long = 0, val allowed: Long = 0)

class StatsCounter {
    private val blocked = AtomicLong(0)
    private val allowed = AtomicLong(0)

    fun onBlocked() {
        blocked.incrementAndGet()
    }

    fun onAllowed() {
        allowed.incrementAndGet()
    }

    fun snapshot(): Stats = Stats(blocked.get(), allowed.get())
}
