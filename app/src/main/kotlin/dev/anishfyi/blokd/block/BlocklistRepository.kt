package dev.anishfyi.blokd.block

import android.content.Context
import dev.anishfyi.blokd.dns.DnsFilter
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads the active block list into the [DnsFilter]. On first run a small seed
 * list ships in the app so blocking works immediately; [update] pulls the full
 * public lists and caches them on disk for next launch. The allow list is stored
 * separately so user exceptions survive block-list updates.
 */
class BlocklistRepository(
    private val context: Context,
    private val filter: DnsFilter,
) {
    private val cacheFile: File get() = File(context.filesDir, "blocklist.txt")
    private val allowFile: File get() = File(context.filesDir, "allowlist.txt")

    val sources = listOf(
        "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/pro.txt",
        "https://big.oisd.nl/",
    )

    fun loadCachedOrSeed() {
        val text = if (cacheFile.exists()) cacheFile.readText() else SEED.joinToString("\n")
        filter.setBlockList(HostsParser.parse(text.lineSequence()))
        filter.setAllowList(loadAllowList())
    }

    /** Downloads every source, merges, caches, and swaps into the filter. Returns the domain count. */
    fun update(): Int {
        val merged = HashSet<String>()
        for (url in sources) {
            runCatching { merged += HostsParser.parse(download(url).lineSequence()) }
        }
        if (merged.isEmpty()) return filter.blockedCount()
        cacheFile.writeText(merged.joinToString("\n"))
        filter.setBlockList(merged)
        return merged.size
    }

    fun loadAllowList(): Set<String> =
        if (allowFile.exists()) allowFile.readLines().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        else emptySet()

    fun setAllowList(domains: Set<String>) {
        allowFile.writeText(domains.joinToString("\n"))
        filter.setAllowList(domains)
    }

    private fun download(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    companion object {
        // Small seed so common ad hosts are blocked before the first update.
        val SEED = listOf(
            "0.0.0.0 doubleclick.net",
            "0.0.0.0 googlesyndication.com",
            "0.0.0.0 pagead2.googlesyndication.com",
            "0.0.0.0 google-analytics.com",
            "0.0.0.0 googleadservices.com",
            "0.0.0.0 adservice.google.com",
            "0.0.0.0 adnxs.com",
            "0.0.0.0 amazon-adsystem.com",
            "0.0.0.0 app-measurement.com",
            "0.0.0.0 ads.yahoo.com",
            "0.0.0.0 scorecardresearch.com",
            "0.0.0.0 moatads.com",
        )
    }
}
