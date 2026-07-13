package dev.anishfyi.blokd.block

import android.content.Context
import dev.anishfyi.blokd.stats.BlocklistMeta
import dev.anishfyi.blokd.stats.ProtectionMode
import java.io.File
import java.util.zip.GZIPInputStream

object BlocklistStore {
    private const val CACHE_NAME = "blocklist.txt"
    private const val META_NAME = "blocklist.meta"
    private const val ASSET_NAME = "blocklist.txt.gz"
    private const val MIN_DOMAINS = 10_000

    fun cacheFile(context: Context): File = File(context.filesDir, CACHE_NAME)

    fun metaFile(context: Context): File = File(context.filesDir, META_NAME)

    fun loadBundledDomains(context: Context): Set<String>? {
        return runCatching {
            context.assets.open(ASSET_NAME).use { raw ->
                GZIPInputStream(raw).bufferedReader().use { reader ->
                    HostsParser.parse(reader.lineSequence())
                }
            }
        }.getOrNull()?.takeIf { it.size >= MIN_DOMAINS }
    }

    fun loadCachedDomains(context: Context): Set<String>? {
        val file = cacheFile(context)
        if (!file.exists() || file.length() == 0L) return null
        return runCatching {
            HostsParser.parse(file.readLines().asSequence())
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    fun writeCache(context: Context, domains: Set<String>, profile: String) {
        val target = cacheFile(context)
        val temp = File(context.filesDir, "$CACHE_NAME.tmp")
        temp.writeText(domains.joinToString("\n"))
        if (!temp.renameTo(target)) {
            target.writeText(domains.joinToString("\n"))
            temp.delete()
        }
        metaFile(context).writeText("${System.currentTimeMillis()}\n$profile\n${domains.size}")
    }

    fun readMeta(context: Context): BlocklistMeta {
        val file = metaFile(context)
        if (!file.exists()) return BlocklistMeta()
        val lines = file.readLines()
        val epoch = lines.getOrNull(0)?.toLongOrNull() ?: 0L
        val profile = when (lines.getOrNull(1)) {
            ProtectionMode.BERSERK.name -> ProtectionMode.BERSERK
            else -> ProtectionMode.STANDARD
        }
        val count = lines.getOrNull(2)?.toIntOrNull() ?: 0
        return BlocklistMeta(domainCount = count, lastUpdatedEpochMs = epoch, profile = profile)
    }
}
