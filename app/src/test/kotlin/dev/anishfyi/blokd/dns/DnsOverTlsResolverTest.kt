package dev.anishfyi.blokd.dns

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DnsOverTlsResolverTest {

    @Test
    fun writeQueryPrefixesTwoByteLength() {
        val query = ByteArray(12) { it.toByte() }
        val bytes = ByteArrayOutputStream()

        DnsOverTlsFraming.writeQuery(DataOutputStream(bytes), query)

        val framed = bytes.toByteArray()
        assertEquals(0, framed[0].toInt())
        assertEquals(query.size, framed[1].toInt())
        assertArrayEquals(query, framed.copyOfRange(2, framed.size))
    }

    @Test
    fun readResponseConsumesOneFramedMessage() {
        val response = ByteArray(12) { (it + 10).toByte() }
        val framed = byteArrayOf(0, response.size.toByte()) + response

        val decoded = DnsOverTlsFraming.readResponse(
            DataInputStream(ByteArrayInputStream(framed)),
        )

        assertArrayEquals(response, decoded)
    }
}
