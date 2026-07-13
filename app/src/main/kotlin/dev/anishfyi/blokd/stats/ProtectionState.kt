package dev.anishfyi.blokd.stats

enum class HealthStatus {
    OFF,
    STARTING,
    HEALTHY,
    DEGRADED,
    NO_NETWORK,
    ERROR,
}

enum class ProtectionMode {
    STANDARD,
    BERSERK,
}

data class BlocklistMeta(
    val domainCount: Int = 0,
    val lastUpdatedEpochMs: Long = 0L,
    val profile: ProtectionMode = ProtectionMode.STANDARD,
)

data class SessionStats(
    val blocked: Long = 0,
    val blockedCname: Long = 0,
    val allowed: Long = 0,
    val cacheHits: Long = 0,
    val failed: Long = 0,
)

data class ProtectionState(
    val status: HealthStatus = HealthStatus.OFF,
    val mode: ProtectionMode = ProtectionMode.STANDARD,
    val adGuardEnabled: Boolean = false,
    val blocklist: BlocklistMeta = BlocklistMeta(),
    val stats: SessionStats = SessionStats(),
    val upstreamLabel: String = "",
)
