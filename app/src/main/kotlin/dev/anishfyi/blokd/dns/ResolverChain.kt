package dev.anishfyi.blokd.dns

/**
 * Which upstream answered a DNS query. Ordered from most to least protective so
 * the UI can honestly report when protection has fallen back off the primary
 * encrypted, ad-filtering resolver. Pure JVM, so the ordering logic is unit
 * testable without Android or a live network.
 */
enum class ResolverTier {
    /** Primary encrypted, ad-filtering resolver (AdGuard DoT). */
    PRIMARY,

    /** Independent encrypted, ad-filtering fallback (a different operator). */
    SECONDARY,

    /** Plaintext system/public resolver. Reachable but does no ad filtering. */
    UNFILTERED,

    /** Nothing answered. */
    FAILED,
}

/**
 * Runs an ordered list of resolver tiers, returning the first non-null answer
 * along with the tier that produced it. Later steps are never invoked once a
 * tier answers, so a failing primary costs one extra attempt, not a broadcast.
 */
object ResolverChain {

    data class Outcome(val response: ByteArray?, val tier: ResolverTier)

    fun run(steps: List<Pair<ResolverTier, () -> ByteArray?>>): Outcome {
        for ((tier, step) in steps) {
            val response = step()
            if (response != null) return Outcome(response, tier)
        }
        return Outcome(null, ResolverTier.FAILED)
    }
}
