package dev.anishfyi.blokd.dns

/**
 * Small in-memory DNS cache. Keyed on the lowercased question name plus qtype
 * and qclass. Positive answers are cached for the minimum TTL of their answer
 * records (clamped to a sane floor and ceiling); NXDOMAIN is cached briefly
 * (negative caching). On a hit the stored response has its transaction id
 * rewritten to the asking query so the client accepts it.
 *
 * Pure JVM, so the logic is unit testable off-device. get/put are synchronized
 * because a bounded LinkedHashMap in access order is not safe under concurrent
 * mutation, even though the packet loop is single threaded.
 */
class DnsCache(
    private val maxEntries: Int = 1024,
    private val minTtlMs: Long = 5_000,
    private val maxTtlMs: Long = 3_600_000,
    private val negativeTtlMs: Long = 30_000,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private class Entry(val response: ByteArray, val expiresAt: Long)

    private val map = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun get(query: ByteArray): ByteArray? {
        val key = keyOf(query) ?: return null
        val entry = map[key] ?: return null
        if (clock() >= entry.expiresAt) {
            map.remove(key)
            return null
        }
        val out = entry.response.copyOf()
        out[0] = query[0]
        out[1] = query[1]
        return out
    }

    @Synchronized
    fun put(query: ByteArray, response: ByteArray) {
        if (response.size < HEADER_LEN) return
        val key = keyOf(query) ?: return
        val ttlMs: Long = when (response[3].toInt() and 0x0F) {
            RCODE_NOERROR -> {
                val seconds = minAnswerTtlSeconds(response) ?: return
                (seconds * 1000).coerceIn(minTtlMs, maxTtlMs)
            }
            RCODE_NXDOMAIN -> negativeTtlMs
            else -> return
        }
        map[key] = Entry(response.copyOf(), clock() + ttlMs)
    }

    @Synchronized
    fun size(): Int = map.size

    /** key = lowercased qname + "|" + qtype + "|" + qclass, or null if unreadable. */
    private fun keyOf(msg: ByteArray): String? {
        if (msg.size <= HEADER_LEN) return null
        if (readU16(msg, 4) < 1) return null
        val sb = StringBuilder()
        var pos = HEADER_LEN
        while (pos < msg.size) {
            val len = msg[pos].toInt() and 0xFF
            if (len == 0) {
                pos++
                break
            }
            if (len and 0xC0 != 0) return null // no compression in a question
            pos++
            if (pos + len > msg.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until len) sb.append((msg[pos + i].toInt() and 0xFF).toChar())
            pos += len
        }
        if (pos + 4 > msg.size) return null
        val qtype = readU16(msg, pos)
        val qclass = readU16(msg, pos + 2)
        return sb.toString().lowercase() + "|" + qtype + "|" + qclass
    }

    /** Minimum TTL (seconds) across answer records, or null if none/unparseable. */
    private fun minAnswerTtlSeconds(msg: ByteArray): Long? {
        val qd = readU16(msg, 4)
        val an = readU16(msg, 6)
        if (an < 1) return null
        var pos = HEADER_LEN
        repeat(qd) {
            pos = (skipName(msg, pos) ?: return null) + 4
            if (pos > msg.size) return null
        }
        var min = Long.MAX_VALUE
        repeat(an) {
            val next = skipName(msg, pos) ?: return null
            if (next + 10 > msg.size) return null
            val ttl = readU32(msg, next + 4)
            val rdlen = readU16(msg, next + 8)
            if (ttl < min) min = ttl
            pos = next + 10 + rdlen
            if (pos > msg.size) return null
        }
        return if (min == Long.MAX_VALUE) null else min
    }

    private fun skipName(msg: ByteArray, offset: Int): Int? {
        var pos = offset
        var jumps = 0
        while (jumps < 8) {
            if (pos >= msg.size) return null
            val len = msg[pos].toInt() and 0xFF
            if (len == 0) return pos + 1
            if (len and 0xC0 == 0xC0) return pos + 2
            pos += 1 + len
            jumps++
        }
        return null
    }

    private fun readU16(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0xFF) shl 8) or (b[o + 1].toInt() and 0xFF)

    private fun readU32(b: ByteArray, o: Int): Long =
        ((b[o].toLong() and 0xFF) shl 24) or ((b[o + 1].toLong() and 0xFF) shl 16) or
            ((b[o + 2].toLong() and 0xFF) shl 8) or (b[o + 3].toLong() and 0xFF)

    companion object {
        private const val HEADER_LEN = 12
        private const val RCODE_NOERROR = 0
        private const val RCODE_NXDOMAIN = 3
    }
}
