package dev.anishfyi.blokd.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsCacheTest {

    private var now = 1_000_000L
    private fun cache(maxEntries: Int = 8) =
        DnsCache(maxEntries = maxEntries, clock = { now })

    // --- DNS message fixtures ---

    private fun encodeName(name: String): ByteArray {
        val out = ArrayList<Byte>()
        for (label in name.split(".")) {
            out.add(label.length.toByte())
            for (c in label) out.add(c.code.toByte())
        }
        out.add(0)
        return out.toByteArray()
    }

    private fun query(name: String, qtype: Int = 1, id: Int = 0x1234): ByteArray {
        val header = byteArrayOf(
            (id shr 8).toByte(), id.toByte(), 0x01, 0x00, 0, 1, 0, 0, 0, 0, 0, 0,
        )
        return header + encodeName(name) +
            byteArrayOf((qtype shr 8).toByte(), qtype.toByte(), 0, 1)
    }

    private fun response(
        name: String,
        qtype: Int = 1,
        id: Int = 0x1234,
        ttl: Long = 300,
        rcode: Int = 0,
        answers: Int = 1,
    ): ByteArray {
        val an = if (rcode == 0) answers else 0
        val header = byteArrayOf(
            (id shr 8).toByte(), id.toByte(), 0x81.toByte(), (0x80 or rcode).toByte(),
            0, 1, (an shr 8).toByte(), an.toByte(), 0, 0, 0, 0,
        )
        var msg = header + encodeName(name) +
            byteArrayOf((qtype shr 8).toByte(), qtype.toByte(), 0, 1)
        repeat(an) {
            msg += byteArrayOf(
                0xC0.toByte(), 0x0C, (qtype shr 8).toByte(), qtype.toByte(), 0, 1,
                (ttl shr 24).toByte(), (ttl shr 16).toByte(), (ttl shr 8).toByte(), ttl.toByte(),
                0, 4, 93.toByte(), 184.toByte(), 216.toByte(), 34.toByte(),
            )
        }
        return msg
    }

    // --- tests ---

    @Test
    fun missReturnsNull() {
        assertNull(cache().get(query("example.com")))
    }

    @Test
    fun putThenGetReturnsAnswer() {
        val c = cache()
        c.put(query("example.com"), response("example.com", ttl = 300))
        assertNotNull(c.get(query("example.com")))
    }

    @Test
    fun getPatchesTransactionIdToTheAskingQuery() {
        val c = cache()
        c.put(query("example.com", id = 0x1234), response("example.com", id = 0x1234))
        val hit = c.get(query("example.com", id = 0xABCD))!!
        assertEquals(0xAB, hit[0].toInt() and 0xFF)
        assertEquals(0xCD, hit[1].toInt() and 0xFF)
    }

    @Test
    fun expiredEntryReturnsNull() {
        val c = cache()
        c.put(query("example.com"), response("example.com", ttl = 60))
        now += 61_000
        assertNull(c.get(query("example.com")))
    }

    @Test
    fun differentQtypeIsASeparateEntry() {
        val c = cache()
        c.put(query("example.com", qtype = 1), response("example.com", qtype = 1))
        assertNull(c.get(query("example.com", qtype = 28))) // AAAA not cached
    }

    @Test
    fun qnameIsCaseInsensitive() {
        val c = cache()
        c.put(query("Example.COM"), response("Example.COM"))
        assertNotNull(c.get(query("example.com")))
    }

    @Test
    fun servfailIsNotCached() {
        val c = cache()
        c.put(query("example.com"), response("example.com", rcode = 2))
        assertNull(c.get(query("example.com")))
    }

    @Test
    fun nxdomainIsNegativelyCached() {
        val c = cache()
        c.put(query("nope.example.com"), response("nope.example.com", rcode = 3))
        assertNotNull(c.get(query("nope.example.com")))
    }

    @Test
    fun lruEvictsLeastRecentlyUsedOverCapacity() {
        val c = cache(maxEntries = 2)
        c.put(query("a.com"), response("a.com"))
        c.put(query("b.com"), response("b.com"))
        c.get(query("a.com")) // touch a -> b is now LRU
        c.put(query("c.com"), response("c.com")) // evicts b
        assertNotNull(c.get(query("a.com")))
        assertNotNull(c.get(query("c.com")))
        assertNull(c.get(query("b.com")))
    }
}
