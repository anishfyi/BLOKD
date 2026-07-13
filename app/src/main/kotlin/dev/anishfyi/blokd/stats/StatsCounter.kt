package dev.anishfyi.blokd.stats

import java.util.concurrent.atomic.AtomicLong

class StatsCounter {
    private val blocked = AtomicLong(0)
    private val blockedCname = AtomicLong(0)
    private val allowed = AtomicLong(0)
    private val cacheHits = AtomicLong(0)
    private val failed = AtomicLong(0)

    fun onBlocked() = blocked.incrementAndGet()
    fun onBlockedCname() = blockedCname.incrementAndGet()
    fun onAllowed() = allowed.incrementAndGet()
    fun onCacheHit() = cacheHits.incrementAndGet()
    fun onFailed() = failed.incrementAndGet()

    fun snapshot(): SessionStats = SessionStats(
        blocked = blocked.get(),
        blockedCname = blockedCname.get(),
        allowed = allowed.get(),
        cacheHits = cacheHits.get(),
        failed = failed.get(),
    )
}

/** @deprecated Use [SessionStats] via [StatsCounter.snapshot]. */
data class Stats(val blocked: Long = 0, val allowed: Long = 0)
