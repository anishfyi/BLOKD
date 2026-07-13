package dev.anishfyi.blokd.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsCodecExtendedTest {

    private fun query(name: String): ByteArray {
        val body = ArrayList<Byte>()
        for (label in name.split(".")) {
            body.add(label.length.toByte())
            for (c in label) body.add(c.code.toByte())
        }
        body.add(0)
        body.addAll(listOf<Byte>(0, 1, 0, 1))
        val header = byteArrayOf(0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        return header + body.toByteArray()
    }

    @Test
    fun servFailSetsRcodeTwo() {
        val response = DnsCodec.buildServFailResponse(query("example.com"))
        assertEquals(0x80, response[2].toInt() and 0x80)
        assertEquals(2, response[3].toInt() and 0x0F)
    }

    @Test
    fun nxDomainStillUsesRcodeThree() {
        val response = DnsCodec.buildNxDomainResponse(query("ads.example.com"))
        assertEquals(3, response[3].toInt() and 0x0F)
    }
}
