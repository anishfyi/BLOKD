package dev.anishfyi.blokd.block

import dev.anishfyi.blokd.dns.DnsFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The anti-bypass set forces apps off their own encrypted DNS by blocking the
 * bootstrap hostnames, so they fall back to plaintext DNS that BLOKD filters.
 */
class AntiBypassTest {

    private val filter = DnsFilter().apply {
        setBlockList(BlocklistSources.ANTI_BYPASS)
        setAllowList(BlocklistSources.CONNECTIVITY_ALLOW)
    }

    @Test
    fun blocksWellKnownEncryptedDnsEndpoints() {
        assertTrue(filter.isBlocked("dns.google"))
        assertTrue(filter.isBlocked("cloudflare-dns.com"))
        assertTrue(filter.isBlocked("one.one.one.one"))
        assertTrue(filter.isBlocked("dns.quad9.net"))
    }

    @Test
    fun blocksEncryptedDnsSubdomainsViaParentMatch() {
        assertTrue(filter.isBlocked("mozilla.cloudflare-dns.com"))
        assertTrue(filter.isBlocked("chrome.cloudflare-dns.com"))
    }

    @Test
    fun blocksFirefoxDohCanary() {
        // NXDOMAIN on this name makes Firefox auto-disable DoH.
        assertTrue(filter.isBlocked("use-application-dns.net"))
    }

    @Test
    fun connectivityAllowListHasNoEncryptedDnsEndpoints() {
        val bypassable = setOf("dns.google", "one.one.one.one", "cloudflare-dns.com")
        for (host in bypassable) {
            assertFalse(
                "$host must not be allow-listed or DoH blocking is defeated",
                BlocklistSources.CONNECTIVITY_ALLOW.contains(host),
            )
        }
    }

    @Test
    fun stillAllowsRealConnectivityCheckHosts() {
        assertFalse(filter.isBlocked("connectivitycheck.gstatic.com"))
        assertFalse(filter.isBlocked("clients3.google.com"))
    }
}
