package dev.anishfyi.blokd.dns

/**
 * Minimal DNS wire-format helpers. Pulls the queried name out of a DNS query
 * and synthesizes an NXDOMAIN reply for blocked names. Deliberately free of any
 * Android import so it can be unit tested on a plain JVM.
 */
object DnsCodec {

    private const val HEADER_LEN = 12

    /** Returns the lowercased first question name, or null if it cannot be read. */
    fun extractDomain(query: ByteArray): String? {
        if (query.size <= HEADER_LEN) return null
        val qdCount = ((query[4].toInt() and 0xFF) shl 8) or (query[5].toInt() and 0xFF)
        if (qdCount < 1) return null

        val sb = StringBuilder()
        var pos = HEADER_LEN
        while (pos < query.size) {
            val len = query[pos].toInt() and 0xFF
            if (len == 0) break
            // Compression pointers do not appear in the question section of a query.
            if (len and 0xC0 != 0) return null
            pos++
            if (pos + len > query.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until len) {
                sb.append((query[pos + i].toInt() and 0xFF).toChar())
            }
            pos += len
        }
        return if (sb.isEmpty()) null else sb.toString().lowercase()
    }

    /**
     * Builds an NXDOMAIN response from the original query by turning the query
     * into a reply, clearing the answer counts, and setting RCODE = 3. The
     * question section is left intact so resolvers accept the answer.
     */
    fun buildNxDomainResponse(query: ByteArray): ByteArray = buildErrorResponse(query, rcode = 3)

    /** Immediate resolver failure — never leave the client waiting. */
    fun buildServFailResponse(query: ByteArray): ByteArray = buildErrorResponse(query, rcode = 2)

    private fun buildErrorResponse(query: ByteArray, rcode: Int): ByteArray {
        val r = query.copyOf()
        r[2] = (r[2].toInt() or 0x80).toByte()
        r[3] = ((r[3].toInt() and 0xF0) or 0x80 or (rcode and 0x0F)).toByte()
        r[6] = 0; r[7] = 0
        r[8] = 0; r[9] = 0
        r[10] = 0; r[11] = 0
        return r
    }
}
