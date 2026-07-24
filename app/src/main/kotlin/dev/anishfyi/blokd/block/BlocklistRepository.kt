package dev.anishfyi.blokd.block

import android.content.Context
import dev.anishfyi.blokd.dns.DnsFilter
import dev.anishfyi.blokd.stats.ProtectionMode
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads aggressive public blocklists into [DnsFilter]. Ships a bundled snapshot,
 * refreshes from CDN sources on demand, and always preserves connectivity-check
 * allow entries.
 */
class BlocklistRepository(
    private val context: Context,
    private val filter: DnsFilter,
) {
    fun loadForMode(mode: ProtectionMode): Int {
        val domains = BlocklistStore.loadCachedDomains(context)
            ?: BlocklistStore.loadBundledDomains(context)
            ?: HostsParser.parse(SEED.asSequence())
        apply(mode, domains)
        return filter.blockedCount()
    }

    fun update(mode: ProtectionMode = ProtectionMode.STANDARD): Int {
        val sources = when (mode) {
            ProtectionMode.STANDARD -> BlocklistSources.STANDARD
            ProtectionMode.BERSERK -> BlocklistSources.BERSERK
        }
        val merged = LinkedHashSet<String>()
        for (url in sources) {
            runCatching {
                merged += HostsParser.parse(download(url).lineSequence())
            }
        }
        if (merged.size < 1_000) {
            return filter.blockedCount()
        }
        BlocklistStore.writeCache(context, merged, mode.name)
        apply(mode, merged)
        return merged.size
    }

    fun meta() = BlocklistStore.readMeta(context).copy(
        domainCount = filter.blockedCount(),
    )

    fun loadAllowList(): Set<String> = userAllowList() + BlocklistSources.CONNECTIVITY_ALLOW

    /** Just the user-added allow entries, excluding the built-in connectivity set. */
    fun userAllowList(): Set<String> =
        if (allowFile.exists()) {
            allowFile.readLines().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        } else {
            emptySet()
        }

    fun setAllowList(domains: Set<String>) {
        val withoutSystem = domains.filter { it !in BlocklistSources.CONNECTIVITY_ALLOW }.toSet()
        allowFile.writeText(withoutSystem.joinToString("\n"))
        filter.setAllowList(withoutSystem + BlocklistSources.CONNECTIVITY_ALLOW)
    }

    private fun apply(mode: ProtectionMode, domains: Set<String>) {
        filter.setBlockList(domains + BlocklistSources.ANTI_BYPASS)
        filter.setAllowList(loadAllowList())
        if (domains.size >= 1_000) {
            BlocklistStore.writeCache(context, domains, mode.name)
        }
    }

    private val allowFile get() = java.io.File(context.filesDir, "allowlist.txt")

    private fun download(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 120_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    companion object {
        val SEED = listOf(
            "doubleclick.net",
            "googlesyndication.com",
            "pagead2.googlesyndication.com",
            "google-analytics.com",
            "googleadservices.com",
            "adservice.google.com",
            "adnxs.com",
            "amazon-adsystem.com",
            "app-measurement.com",
            "ads.yahoo.com",
            "scorecardresearch.com",
            "moatads.com",
            "facebook.net",
            "taboola.com",
            "outbrain.com",
            "criteo.com",
            "pubmatic.com",
            "rubiconproject.com",
            "openx.net",
            "adsafeprotected.com",
        )
    }
}
