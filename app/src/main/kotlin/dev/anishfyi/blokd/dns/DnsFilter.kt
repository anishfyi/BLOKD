package dev.anishfyi.blokd.dns

import java.util.concurrent.atomic.AtomicReference

/**
 * Decides whether a domain should be blocked. A name is blocked when the name
 * itself or any of its parent domains is in the block set, unless the name (or a
 * parent) is on the allow list. Both sets are swapped atomically so the read
 * path on the packet thread never has to lock.
 */
class DnsFilter {

    private val blockSet = AtomicReference<Set<String>>(emptySet())
    private val allowSet = AtomicReference<Set<String>>(emptySet())

    fun setBlockList(domains: Set<String>) = blockSet.set(domains)

    fun setAllowList(domains: Set<String>) = allowSet.set(domains)

    fun blockedCount(): Int = blockSet.get().size

    fun isBlocked(domain: String): Boolean {
        val allow = allowSet.get()
        val block = blockSet.get()
        var candidate = domain.lowercase().trimEnd('.')
        while (true) {
            if (allow.contains(candidate)) return false
            if (block.contains(candidate)) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
    }
}
