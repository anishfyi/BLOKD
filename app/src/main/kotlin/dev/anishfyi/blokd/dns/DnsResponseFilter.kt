package dev.anishfyi.blokd.dns

/**
 * Inspects upstream DNS answers for CNAME cloaking: a first-party query name can
 * chain to a blocked tracker domain. Returns true when the response should be
 * treated as blocked.
 */
object DnsResponseFilter {

    private const val TYPE_CNAME = 5

    fun shouldBlockResponse(filter: DnsFilter, response: ByteArray): Boolean {
        for (name in extractNames(response)) {
            if (filter.isBlocked(name)) return true
        }
        return false
    }

    internal fun extractNames(packet: ByteArray): List<String> {
        if (packet.size < 12) return emptyList()
        val qd = readU16(packet, 4)
        val an = readU16(packet, 6)
        val ns = readU16(packet, 8)
        val ar = readU16(packet, 10)
        var pos = 12
        repeat(qd) {
            val skipped = skipName(packet, pos) ?: return emptyList()
            pos = skipped + 4
            if (pos > packet.size) return emptyList()
        }

        val names = ArrayList<String>()
        repeat(an + ns + ar) {
            val (name, next) = readName(packet, pos) ?: return names
            if (next + 10 > packet.size) return names
            val type = readU16(packet, next)
            val rdlength = readU16(packet, next + 8)
            val rdataStart = next + 10
            if (rdataStart + rdlength > packet.size) return names
            when (type) {
                TYPE_CNAME -> {
                    val (target, _) = readName(packet, rdataStart) ?: return names
                    if (target.isNotEmpty()) names.add(target)
                }
            }
            pos = rdataStart + rdlength
        }
        return names
    }

    private fun readName(packet: ByteArray, offset: Int): Pair<String, Int>? {
        val sb = StringBuilder()
        var pos = offset
        var jumps = 0
        var recordEnd = offset
        var followedPointer = false
        while (jumps < 8) {
            if (pos >= packet.size) return null
            val len = packet[pos].toInt() and 0xFF
            if (len == 0) {
                if (!followedPointer) recordEnd = pos + 1
                break
            }
            if (len and 0xC0 == 0xC0) {
                if (pos + 1 >= packet.size) return null
                if (!followedPointer) recordEnd = pos + 2
                followedPointer = true
                val pointer = ((len and 0x3F) shl 8) or (packet[pos + 1].toInt() and 0xFF)
                pos = pointer
                jumps++
                continue
            }
            pos++
            if (pos + len > packet.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until len) {
                sb.append((packet[pos + i].toInt() and 0xFF).toChar())
            }
            pos += len
            if (!followedPointer) recordEnd = pos
        }
        return if (sb.isEmpty()) null else sb.toString().lowercase() to recordEnd
    }

    private fun skipName(packet: ByteArray, offset: Int): Int? {
        var pos = offset
        var jumps = 0
        while (jumps < 8) {
            if (pos >= packet.size) return null
            val len = packet[pos].toInt() and 0xFF
            if (len == 0) return pos + 1
            if (len and 0xC0 == 0xC0) return pos + 2
            pos += 1 + len
            jumps++
        }
        return null
    }

    private fun readU16(packet: ByteArray, offset: Int): Int {
        return ((packet[offset].toInt() and 0xFF) shl 8) or (packet[offset + 1].toInt() and 0xFF)
    }
}
