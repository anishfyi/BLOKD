package dev.anishfyi.blokd.dns

import org.junit.Assert.assertEquals
import org.junit.Test

class DnsCodecTest {

    private fun query(name: String): ByteArray {
        val body = ArrayList<Byte>()
        for (label in name.split(".")) {
            body.add(label.length.toByte())
            for (c in label) body.add(c.code.toByte())
        }
        body.add(0)                       // root label
        body.addAll(listOf<Byte>(0, 1))   // QTYPE A
        body.addAll(listOf<Byte>(0, 1))   // QCLASS IN
        val header = byteArrayOf(
            0x12, 0x34, // ID
            0x01, 0x00, // flags: RD
            0x00, 0x01, // QDCOUNT
            0x00, 0x00, // ANCOUNT
            0x00, 0x00, // NSCOUNT
            0x00, 0x00, // ARCOUNT
        )
        return header + body.toByteArray()
    }

    @Test
    fun extractsQuestionName() {
        assertEquals("ads.example.com", DnsCodec.extractDomain(query("ads.example.com")))
    }

    @Test
    fun nxDomainResponseSetsExpectedBits() {
        val response = DnsCodec.buildNxDomainResponse(query("ads.example.com"))
        assertEquals(0x80, response[2].toInt() and 0x80)        // QR set
        assertEquals(3, response[3].toInt() and 0x0F)           // RCODE NXDOMAIN
        val ancount = ((response[6].toInt() and 0xFF) shl 8) or (response[7].toInt() and 0xFF)
        assertEquals(0, ancount)                                // no answers
    }
}
