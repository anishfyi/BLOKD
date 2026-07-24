package dev.anishfyi.blokd.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsOverTlsPoolTest {

    private class FakeResolver : DnsResolver {
        val seen = mutableListOf<Int>()
        var closed = false
        override fun resolve(query: ByteArray): ByteArray? {
            seen.add(query[0].toInt())
            return byteArrayOf(1)
        }
        override fun close() {
            closed = true
        }
    }

    private fun poolOf(fakes: List<FakeResolver>): DnsOverTlsPool {
        var i = 0
        return DnsOverTlsPool(fakes.size) { fakes[i++] }
    }

    @Test
    fun distributesQueriesRoundRobinAcrossConnections() {
        val fakes = List(3) { FakeResolver() }
        val pool = poolOf(fakes)

        repeat(6) { pool.resolve(byteArrayOf(it.toByte())) }

        assertEquals(listOf(0, 3), fakes[0].seen)
        assertEquals(listOf(1, 4), fakes[1].seen)
        assertEquals(listOf(2, 5), fakes[2].seen)
    }

    @Test
    fun closeClosesEveryConnection() {
        val fakes = List(3) { FakeResolver() }
        val pool = poolOf(fakes)

        pool.close()

        assertTrue(fakes.all { it.closed })
    }

    @Test
    fun singleConnectionPoolReusesTheOneResolver() {
        val fake = FakeResolver()
        val pool = DnsOverTlsPool(1) { fake }

        pool.resolve(byteArrayOf(1))
        pool.resolve(byteArrayOf(2))

        assertEquals(listOf(1, 2), fake.seen)
    }
}
