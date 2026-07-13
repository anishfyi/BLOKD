package dev.anishfyi.blokd.dns

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsFilterTest {

    @Test
    fun blocksDomainAndSubdomains() {
        val filter = DnsFilter()
        filter.setBlockList(setOf("doubleclick.net"))
        assertTrue(filter.isBlocked("doubleclick.net"))
        assertTrue(filter.isBlocked("ads.g.doubleclick.net"))
        assertFalse(filter.isBlocked("example.com"))
    }

    @Test
    fun allowListOverridesBlockList() {
        val filter = DnsFilter()
        filter.setBlockList(setOf("example.com"))
        filter.setAllowList(setOf("safe.example.com"))
        assertTrue(filter.isBlocked("ads.example.com"))
        assertFalse(filter.isBlocked("safe.example.com"))
    }
}
