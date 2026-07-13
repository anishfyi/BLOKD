package dev.anishfyi.blokd.dns

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsResponseFilterTest {

    private fun labelBytes(text: String): List<Byte> {
        val out = ArrayList<Byte>()
        for (part in text.split('.')) {
            out.add(part.length.toByte())
            for (c in part) out.add(c.code.toByte())
        }
        out.add(0)
        return out
    }

    private fun cnameAnswer(cnameTarget: List<Byte>): ByteArray {
        val rdLen = cnameTarget.size
        return byteArrayOf(
            0xC0.toByte(), 0x0C,
            0x00, 0x05,
            0x00, 0x01,
            0x00, 0x00, 0x00, 0x3C,
            (rdLen shr 8).toByte(), (rdLen and 0xFF).toByte(),
            *cnameTarget.toByteArray(),
        )
    }

    @Test
    fun detectsBlockedCnameTarget() {
        val filter = DnsFilter()
        filter.setBlockList(setOf("tracker.example.com"))

        val question = labelBytes("safe.example.com")
        val response = byteArrayOf(
            0x00, 0x01, 0x81.toByte(), 0x80.toByte(),
            0x00, 0x01, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x00,
            *question.toByteArray(),
            0x00, 0x01, 0x00, 0x01,
            *cnameAnswer(labelBytes("tracker.example.com")),
        )

        assertTrue(DnsResponseFilter.shouldBlockResponse(filter, response))
    }

    @Test
    fun allowsCleanResponse() {
        val filter = DnsFilter()
        filter.setBlockList(setOf("tracker.example.com"))
        val response = DnsCodec.buildNxDomainResponse(
            byteArrayOf(
                0x00, 0x01, 0x01, 0x00,
                0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                *labelBytes("example.com").toByteArray(),
                0x00, 0x01, 0x00, 0x01,
            ),
        )
        assertFalse(DnsResponseFilter.shouldBlockResponse(filter, response))
    }
}
